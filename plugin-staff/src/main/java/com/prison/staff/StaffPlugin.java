package com.prison.staff;

import com.prison.database.DatabaseManager;
import com.prison.permissions.PermissionEngine;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class StaffPlugin extends JavaPlugin implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private StaffManager manager;

    // Pending report confirm state: player UUID → {targetName, targetUUID, reason}
    private final ConcurrentHashMap<UUID, String[]> pendingReports = new ConcurrentHashMap<>();

    // Pending report-close note: player UUID → report ID awaiting resolution note
    private final ConcurrentHashMap<UUID, Long> pendingClose = new ConcurrentHashMap<>();

    // Cached report list for GUI (refreshed when /reports is opened)
    private final ConcurrentHashMap<UUID, List<ReportData>> reportListCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer> reportPageCache = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        manager = StaffManager.initialize(getLogger());
        manager.loadActivePunishments().thenRun(() ->
            getLogger().info("[Staff] Punishment cache loaded."));

        ensureTables();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("[Staff] Plugin enabled.");
    }

    @Override
    public void onDisable() {
        manager.unfreezeAll();
        getLogger().info("[Staff] Plugin disabled.");
    }

    // ----------------------------------------------------------------
    // DB tables
    // ----------------------------------------------------------------

    private void ensureTables() {
        try {
            DatabaseManager.getInstance().execute(
                "CREATE TABLE IF NOT EXISTS punishments (" +
                "  id             INT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                "  player_uuid    VARCHAR(36) NOT NULL," +
                "  ip_address     VARCHAR(45) NULL," +
                "  type           ENUM('ban','tempban','ipban','mute','tempmute','kick') NOT NULL," +
                "  reason         TEXT NOT NULL," +
                "  issued_by_uuid VARCHAR(36) NOT NULL," +
                "  issued_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "  expires_at     TIMESTAMP NULL," +
                "  active         TINYINT(1) NOT NULL DEFAULT 1," +
                "  INDEX idx_punishments_player (player_uuid)," +
                "  INDEX idx_punishments_ip (ip_address)," +
                "  INDEX idx_punishments_active (active)" +
                ")"
            );
            DatabaseManager.getInstance().execute(
                "CREATE TABLE IF NOT EXISTS staff_actions (" +
                "  id          INT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                "  actor_uuid  VARCHAR(36) NOT NULL," +
                "  target_uuid VARCHAR(36) NULL," +
                "  action_type VARCHAR(64) NOT NULL," +
                "  details     JSON NULL," +
                "  timestamp   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "  INDEX idx_staff_actions_actor  (actor_uuid)," +
                "  INDEX idx_staff_actions_target (target_uuid)" +
                ")"
            );
            DatabaseManager.getInstance().execute(
                "CREATE TABLE IF NOT EXISTS reports (" +
                "  id               INT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                "  reporter_uuid    VARCHAR(36) NOT NULL," +
                "  reported_uuid    VARCHAR(36) NOT NULL," +
                "  reporter_name    VARCHAR(32) NOT NULL," +
                "  reported_name    VARCHAR(32) NOT NULL," +
                "  reason           TEXT NOT NULL," +
                "  status           ENUM('pending','reviewed','closed') NOT NULL DEFAULT 'pending'," +
                "  resolution_note  TEXT NULL," +
                "  assigned_to_uuid VARCHAR(36) NULL," +
                "  created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "  INDEX idx_reports_reported (reported_uuid)," +
                "  INDEX idx_reports_status   (status)" +
                ")"
            );
        } catch (SQLException e) {
            getLogger().severe("[Staff] Failed to create tables: " + e.getMessage());
        }
    }

    // ----------------------------------------------------------------
    // Events — login/join
    // ----------------------------------------------------------------

    /** Block banned players and IP-banned players before they connect. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        String uuid = event.getUniqueId().toString();
        String ip   = event.getAddress().getHostAddress();

        // IP ban check
        if (manager.isIpBanned(ip)) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                MM.deserialize(banMessage("Your IP address is banned.", "Never (permanent)")));
            return;
        }

        // UUID ban check
        PunishmentData ban = manager.getActiveBan(uuid);
        if (ban != null) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                MM.deserialize(banMessage(ban.reason(), ban.expiryString())));
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Apply staff mode if staff rank
        if (manager.isStaff(player.getUniqueId())) {
            applyStaffMode(player);
        }

        // Hide staff-mode players from non-staff
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other == player) continue;
            if (manager.isStaff(other.getUniqueId()) && !manager.isStaff(player.getUniqueId())) {
                player.hidePlayer(this, other); // non-staff can't see staff
            }
            if (manager.isStaff(player.getUniqueId()) && !manager.isStaff(other.getUniqueId())) {
                // staff can still see all non-staff — nothing to do
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        manager.unfreezePlayer(player.getUniqueId());
        pendingReports.remove(player.getUniqueId());
        pendingClose.remove(player.getUniqueId());
        reportListCache.remove(player.getUniqueId());
        reportPageCache.remove(player.getUniqueId());
    }

    // ----------------------------------------------------------------
    // Events — freeze
    // ----------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!event.hasChangedPosition()) return;
        if (manager.isFrozen(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(MM.deserialize(
                "<red>You are frozen by a staff member. Do not log off."));
        }
    }

    // ----------------------------------------------------------------
    // Events — mute check
    // ----------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();

        // Pending close-resolution note
        if (pendingClose.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
            long reportId = pendingClose.remove(player.getUniqueId());
            String note = PlainTextComponentSerializer.plainText().serialize(event.message());
            manager.closeReport(reportId, player.getUniqueId().toString(), note)
                .thenAccept(ok -> {
                    if (ok) {
                        player.sendMessage(MM.deserialize("<green>Report #" + reportId + " closed."));
                    } else {
                        player.sendMessage(MM.deserialize("<red>Failed to close report. Try again."));
                    }
                });
            return;
        }

        // Staff chat routing
        String plain = PlainTextComponentSerializer.plainText().serialize(event.message());
        if (plain.startsWith("@staff ") && manager.isStaff(player.getUniqueId())) {
            event.setCancelled(true);
            broadcastStaffChat(player, plain.substring(7));
            return;
        }

        // Mute check
        PunishmentData mute = manager.getActiveMute(player.getUniqueId().toString());
        if (mute != null) {
            event.setCancelled(true);
            String msg = getConfig().getString("mute-message", "<red>You are muted. Expires: {expires}")
                .replace("{expires}", mute.expiryString());
            player.sendMessage(MM.deserialize(msg));
        }
    }

    // ----------------------------------------------------------------
    // Events — GUI
    // ----------------------------------------------------------------

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = PlainTextComponentSerializer.plainText()
            .serialize(event.getView().title());

        if (title.equals(ReportGUI.CONFIRM_TITLE)) {
            event.setCancelled(true);
            handleConfirmClick(player, event.getRawSlot());

        } else if (title.equals(ReportGUI.QUEUE_TITLE)) {
            event.setCancelled(true);
            handleQueueClick(player, event.getRawSlot(), event.isRightClick());

        } else if (title.startsWith(INVSEE_TITLE_PREFIX)) {
            // Prevent staff from stealing items out of the viewed inventory
            event.setCancelled(true);
        }
    }

    private void handleConfirmClick(Player player, int slot) {
        String[] pending = pendingReports.get(player.getUniqueId());
        if (pending == null) { player.closeInventory(); return; }

        if (slot == ReportGUI.CANCEL_SLOT) {
            pendingReports.remove(player.getUniqueId());
            player.closeInventory();
            player.sendMessage(MM.deserialize("<gray>Report cancelled."));
            return;
        }
        if (slot != ReportGUI.CONFIRM_SLOT) return;

        String targetUuid = pending[0];
        String targetName = pending[1];
        String reason     = pending[2];
        pendingReports.remove(player.getUniqueId());
        player.closeInventory();

        long cooldownMs = getConfig().getLong("report-cooldown-seconds", 300) * 1000L;
        UUID targetUUID = UUID.fromString(targetUuid);
        if (!manager.canReport(player.getUniqueId(), targetUUID, cooldownMs)) {
            player.sendMessage(MM.deserialize("<red>You've already reported this player recently. Please wait before reporting again."));
            return;
        }

        manager.recordReportCooldown(player.getUniqueId(), targetUUID);
        manager.submitReport(
            player.getUniqueId().toString(), targetUuid,
            player.getName(), targetName, reason
        ).thenAccept(id -> {
            if (id > 0) {
                player.sendMessage(MM.deserialize("<green>Report submitted (ID: #" + id + "). Thank you."));
                broadcastStaffMessage(MM.deserialize(
                    "<red>[REPORT] <gray>" + player.getName() + " reported " + targetName +
                    ": <white>" + reason + " <dark_gray>(#" + id + ")"));
            } else {
                player.sendMessage(MM.deserialize("<red>Failed to submit report. Please try again."));
            }
        });
    }

    private void handleQueueClick(Player player, int slot, boolean rightClick) {
        List<ReportData> reports = reportListCache.get(player.getUniqueId());
        int page = reportPageCache.getOrDefault(player.getUniqueId(), 0);
        if (reports == null) { player.closeInventory(); return; }

        int totalPages = Math.max(1, (int) Math.ceil(reports.size() / (double) ReportGUI.PAGE_SIZE));

        // Prev/Next page buttons
        if (slot == 45 && page > 0) {
            page--;
            reportPageCache.put(player.getUniqueId(), page);
            ReportGUI.openQueue(player, reports, page);
            return;
        }
        if (slot == 53 && page < totalPages - 1) {
            page++;
            reportPageCache.put(player.getUniqueId(), page);
            ReportGUI.openQueue(player, reports, page);
            return;
        }

        // Report item click
        int[] innerSlots = buildInnerSlots();
        int itemIndex = -1;
        for (int i = 0; i < innerSlots.length; i++) {
            if (innerSlots[i] == slot) { itemIndex = i; break; }
        }
        if (itemIndex < 0) return;

        int reportIndex = page * ReportGUI.PAGE_SIZE + itemIndex;
        if (reportIndex >= reports.size()) return;
        ReportData report = reports.get(reportIndex);

        if (!rightClick) {
            // Left-click: TP to reported player
            player.closeInventory();
            Player target = Bukkit.getPlayer(UUID.fromString(report.reportedUuid()));
            if (target != null) {
                player.teleport(target.getLocation());
                player.sendMessage(MM.deserialize("<green>Teleported to <white>" + report.reportedName()));
            } else {
                player.sendMessage(MM.deserialize("<red>" + report.reportedName() + " is not online."));
            }
        } else {
            // Right-click: close report — prompt for resolution note via chat
            player.closeInventory();
            pendingClose.put(player.getUniqueId(), report.id());
            player.sendMessage(MM.deserialize(
                "<yellow>Closing report #" + report.id() + " against <white>" + report.reportedName() +
                "<yellow>. Type your <white>resolution note</white> in chat <yellow>(or <red>cancel</red><yellow>):"));
        }
    }

    private int[] buildInnerSlots() {
        int[] slots = new int[ReportGUI.PAGE_SIZE];
        int si = 0;
        for (int row = 1; row <= 4; row++) {
            for (int col = 1; col <= 7; col++) {
                slots[si++] = row * 9 + col;
            }
        }
        return slots;
    }

    // ----------------------------------------------------------------
    // Commands
    // ----------------------------------------------------------------

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        return switch (cmd.getName().toLowerCase()) {
            case "staff"   -> cmdStaff(sender);
            case "sc"      -> cmdStaffChat(sender, args);
            case "mute"    -> cmdMute(sender, args, false);
            case "tempmute"-> cmdMute(sender, args, true);
            case "unmute"  -> cmdUnpunish(sender, args, false);
            case "kick"    -> cmdKick(sender, args);
            case "ban"     -> cmdBan(sender, args, false, false);
            case "tempban" -> cmdBan(sender, args, true, false);
            case "ipban"   -> cmdBan(sender, args, false, true);
            case "unban"   -> cmdUnpunish(sender, args, true);
            case "freeze"  -> cmdFreeze(sender, args);
            case "invsee"  -> cmdInvsee(sender, args);
            case "stafftp" -> cmdStaffTp(sender, args);
            case "report"  -> cmdReport(sender, args);
            case "reports" -> cmdReports(sender, args);
            default -> false;
        };
    }

    private boolean cmdStaff(CommandSender sender) {
        sender.sendMessage(MM.deserialize("<gold>━━━━━━━━ Online Staff ━━━━━━━━"));
        boolean anyOnline = false;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (manager.isStaff(p.getUniqueId())) {
                String rank = getStaffRankDisplay(p);
                sender.sendMessage(MM.deserialize("<gray> " + rank + " <white>" + p.getName()));
                anyOnline = true;
            }
        }
        if (!anyOnline) {
            sender.sendMessage(MM.deserialize("<gray> No staff currently online."));
        }
        sender.sendMessage(MM.deserialize("<gold>━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        return true;
    }

    private boolean cmdStaffChat(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("In-game only."); return true; }
        if (!manager.hasStaffPermPlayer(player, "helper")) {
            player.sendMessage(MM.deserialize("<red>No permission.")); return true;
        }
        if (args.length == 0) { player.sendMessage(MM.deserialize("<red>Usage: /sc <message>")); return true; }
        broadcastStaffChat(player, String.join(" ", args));
        return true;
    }

    private boolean cmdMute(CommandSender sender, String[] args, boolean temp) {
        if (!(sender instanceof Player player)) { sender.sendMessage("In-game only."); return true; }
        if (!manager.hasStaffPermPlayer(player, "helper")) {
            player.sendMessage(MM.deserialize("<red>No permission.")); return true;
        }

        int minArgs = temp ? 3 : 2;
        if (args.length < minArgs) {
            player.sendMessage(MM.deserialize("<red>Usage: /" + (temp ? "tempmute <player> <duration> <reason>" : "mute <player> <reason>")));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) { player.sendMessage(MM.deserialize("<red>Player not found.")); return true; }

        LocalDateTime expiresAt = null;
        int reasonStart = 1;
        if (temp) {
            try {
                expiresAt = DurationParser.parseExpiry(args[1]);
                reasonStart = 2;
            } catch (IllegalArgumentException e) {
                player.sendMessage(MM.deserialize("<red>Invalid duration. Use: 30m, 2h, 1d, 1w"));
                return true;
            }
        }
        String reason = joinFrom(args, reasonStart);
        PunishmentType type = temp ? PunishmentType.TEMPMUTE : PunishmentType.MUTE;

        LocalDateTime finalExpiresAt = expiresAt;
        manager.issuePunishment(
            target.getUniqueId().toString(), target.getName(), null,
            type, reason, player.getUniqueId().toString(), expiresAt
        ).thenAccept(p -> {
            if (p == null) return;
            String duration = finalExpiresAt != null ? " for " + formatExpiry(finalExpiresAt) : " permanently";
            target.sendMessage(MM.deserialize("<red>You have been muted" + duration + ". Reason: <white>" + reason));
            broadcastStaff(MM.deserialize(
                "<gray>[Staff] <red>" + player.getName() + " muted " + target.getName() + duration));
        });
        return true;
    }

    private boolean cmdUnpunish(CommandSender sender, String[] args, boolean isBan) {
        if (!(sender instanceof Player player)) { sender.sendMessage("In-game only."); return true; }
        String required = isBan ? "seniormod" : "helper";
        if (!manager.hasStaffPermPlayer(player, required)) {
            player.sendMessage(MM.deserialize("<red>No permission.")); return true;
        }
        if (args.length < 1) {
            player.sendMessage(MM.deserialize("<red>Usage: /" + (isBan ? "unban" : "unmute") + " <player>"));
            return true;
        }

        // Look up UUID by name from DB
        String targetName = args[0];
        lookupUuid(targetName).thenAccept(uuid -> {
            if (uuid == null) {
                player.sendMessage(MM.deserialize("<red>Player not found in database."));
                return;
            }
            manager.removePunishment(uuid, isBan, player.getUniqueId().toString())
                .thenAccept(ok -> {
                    if (ok) {
                        player.sendMessage(MM.deserialize("<green>" + targetName + " has been " + (isBan ? "unbanned" : "unmuted") + "."));
                    } else {
                        player.sendMessage(MM.deserialize("<red>Failed. They may not be " + (isBan ? "banned" : "muted") + "."));
                    }
                });
        });
        return true;
    }

    private boolean cmdKick(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("In-game only."); return true; }
        if (!manager.hasStaffPermPlayer(player, "moderator")) {
            player.sendMessage(MM.deserialize("<red>No permission.")); return true;
        }
        if (args.length < 2) { player.sendMessage(MM.deserialize("<red>Usage: /kick <player> <reason>")); return true; }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) { player.sendMessage(MM.deserialize("<red>Player not found.")); return true; }

        String reason = joinFrom(args, 1);
        target.kick(MM.deserialize("<red>You were kicked.\n<gray>Reason: <white>" + reason));
        manager.logStaffAction(player.getUniqueId().toString(), target.getUniqueId().toString(),
            "kick", "{\"reason\":\"" + reason + "\"}");
        broadcastStaff(MM.deserialize("<gray>[Staff] <yellow>" + player.getName() + " kicked " + target.getName() + ": <white>" + reason));
        return true;
    }

    private boolean cmdBan(CommandSender sender, String[] args, boolean temp, boolean ip) {
        if (!(sender instanceof Player player)) { sender.sendMessage("In-game only."); return true; }
        String required = ip || !temp ? "seniormod" : "moderator";
        if (!manager.hasStaffPermPlayer(player, required)) {
            player.sendMessage(MM.deserialize("<red>No permission.")); return true;
        }

        if (temp && args.length < 3) {
            player.sendMessage(MM.deserialize("<red>Usage: /tempban <player> <duration> <reason>")); return true;
        }
        if (!temp && args.length < 2) {
            player.sendMessage(MM.deserialize("<red>Usage: /" + (ip ? "ipban" : "ban") + " <player> <reason>")); return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        String targetName = args[0];

        LocalDateTime expiresAt = null;
        int reasonStart = 1;
        if (temp) {
            try {
                expiresAt = DurationParser.parseExpiry(args[1]);
                reasonStart = 2;
            } catch (IllegalArgumentException e) {
                player.sendMessage(MM.deserialize("<red>Invalid duration. Use: 30m, 2h, 1d, 1w"));
                return true;
            }
        }

        String reason = joinFrom(args, reasonStart);
        PunishmentType type = ip ? PunishmentType.IPBAN : (temp ? PunishmentType.TEMPBAN : PunishmentType.BAN);
        LocalDateTime finalExpires = expiresAt;

        if (target != null) {
            // Player is online — ban immediately and kick
            String targetUuid = target.getUniqueId().toString();
            String targetIp   = (ip && target.getAddress() != null)
                ? target.getAddress().getAddress().getHostAddress() : null;
            String finalTargetIp = targetIp;

            manager.issuePunishment(targetUuid, targetName, targetIp, type, reason,
                player.getUniqueId().toString(), expiresAt)
            .thenAccept(p -> {
                if (p == null) return;
                String duration = finalExpires != null
                    ? " until " + finalExpires.toString().replace("T", " ").substring(0, 16) : " permanently";
                target.kick(MM.deserialize(banMessage(reason, p.expiryString())));
                broadcastStaff(MM.deserialize(
                    "<gray>[Staff] <red>" + player.getName() + " " + type.name().toLowerCase() + "ned "
                    + targetName + duration));
            });

        } else {
            // Player is offline — look up UUID from DB then ban
            if (ip) {
                player.sendMessage(MM.deserialize("<red>/ipban requires the player to be online (need their IP)."));
                return true;
            }
            player.sendMessage(MM.deserialize("<yellow>Player is offline. Looking up UUID..."));
            lookupUuid(targetName).thenAccept(uuid -> {
                if (uuid == null) {
                    player.sendMessage(MM.deserialize("<red>Player not found in database. They must have joined at least once."));
                    return;
                }
                manager.issuePunishment(uuid, targetName, null, type, reason,
                    player.getUniqueId().toString(), finalExpires)
                .thenAccept(p -> {
                    if (p == null) {
                        player.sendMessage(MM.deserialize("<red>Failed to issue ban."));
                        return;
                    }
                    String duration = finalExpires != null
                        ? " until " + finalExpires.toString().replace("T", " ").substring(0, 16) : " permanently";
                    player.sendMessage(MM.deserialize(
                        "<green>Banned offline player <white>" + targetName + "</white>" + duration + "."));
                    broadcastStaff(MM.deserialize(
                        "<gray>[Staff] <red>" + player.getName() + " " + type.name().toLowerCase() + "ned "
                        + targetName + duration + " <dark_gray>(offline)"));
                });
            });
        }
        return true;
    }

    private boolean cmdFreeze(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("In-game only."); return true; }
        if (!manager.hasStaffPermPlayer(player, "moderator")) {
            player.sendMessage(MM.deserialize("<red>No permission.")); return true;
        }
        if (args.length < 1) { player.sendMessage(MM.deserialize("<red>Usage: /freeze <player>")); return true; }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) { player.sendMessage(MM.deserialize("<red>Player not found.")); return true; }

        boolean frozen = manager.toggleFreeze(target.getUniqueId());
        if (frozen) {
            target.sendMessage(MM.deserialize("<red><bold>You have been frozen by a staff member. Do not log off."));
            player.sendMessage(MM.deserialize("<green>" + target.getName() + " has been frozen."));
        } else {
            target.sendMessage(MM.deserialize("<green>You have been unfrozen."));
            player.sendMessage(MM.deserialize("<green>" + target.getName() + " has been unfrozen."));
        }
        manager.logStaffAction(player.getUniqueId().toString(), target.getUniqueId().toString(),
            frozen ? "freeze" : "unfreeze", "{}");
        return true;
    }

    // Prefix used for /invsee GUI title — also used by click handler to block item theft
    private static final String INVSEE_TITLE_PREFIX = "Inventory: ";

    private boolean cmdInvsee(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("In-game only."); return true; }
        if (!manager.hasStaffPermPlayer(player, "moderator")) {
            player.sendMessage(MM.deserialize("<red>No permission.")); return true;
        }
        if (args.length < 1) { player.sendMessage(MM.deserialize("<red>Usage: /invsee <player>")); return true; }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) { player.sendMessage(MM.deserialize("<red>Player not found.")); return true; }

        // Open a copy of the target's inventory (read-only — clicks blocked via INVSEE_TITLE_PREFIX check)
        Inventory copy = Bukkit.createInventory(null, 54,
            Component.text(INVSEE_TITLE_PREFIX + target.getName()));
        ItemStack[] contents = target.getInventory().getContents();
        for (int i = 0; i < Math.min(contents.length, 36); i++) {
            if (contents[i] != null) copy.setItem(i, contents[i].clone());
        }
        player.openInventory(copy);
        manager.logStaffAction(player.getUniqueId().toString(), target.getUniqueId().toString(),
            "invsee", "{}");
        return true;
    }

    private boolean cmdStaffTp(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("In-game only."); return true; }
        if (!manager.hasStaffPermPlayer(player, "helper")) {
            player.sendMessage(MM.deserialize("<red>No permission.")); return true;
        }
        if (args.length < 1) { player.sendMessage(MM.deserialize("<red>Usage: /stafftp <player>")); return true; }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) { player.sendMessage(MM.deserialize("<red>Player not found.")); return true; }

        player.teleport(target.getLocation());
        player.sendMessage(MM.deserialize("<green>Teleported to <white>" + target.getName()));
        manager.logStaffAction(player.getUniqueId().toString(), target.getUniqueId().toString(), "stafftp", "{}");
        return true;
    }

    private boolean cmdReport(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("In-game only."); return true; }
        if (args.length < 2) {
            player.sendMessage(MM.deserialize("<red>Usage: /report <player> <reason>")); return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) { player.sendMessage(MM.deserialize("<red>That player is not online.")); return true; }
        if (target.equals(player)) { player.sendMessage(MM.deserialize("<red>You cannot report yourself.")); return true; }

        String reason = joinFrom(args, 1);
        String[] state = { target.getUniqueId().toString(), target.getName(), reason };
        pendingReports.put(player.getUniqueId(), state);
        ReportGUI.openConfirm(player, target.getName(), reason);
        return true;
    }

    private boolean cmdReports(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("In-game only."); return true; }
        if (!manager.hasStaffPermPlayer(player, "moderator")) {
            player.sendMessage(MM.deserialize("<red>No permission.")); return true;
        }

        // /reports close <id> <resolution>
        if (args.length >= 3 && args[0].equalsIgnoreCase("close")) {
            try {
                long id = Long.parseLong(args[1]);
                String note = joinFrom(args, 2);
                manager.closeReport(id, player.getUniqueId().toString(), note)
                    .thenAccept(ok -> {
                        if (ok) player.sendMessage(MM.deserialize("<green>Report #" + id + " closed."));
                        else    player.sendMessage(MM.deserialize("<red>Failed to close report #" + id));
                    });
            } catch (NumberFormatException e) {
                player.sendMessage(MM.deserialize("<red>Invalid report ID."));
            }
            return true;
        }

        int page = 0;
        if (args.length >= 1 && args[0].matches("\\d+")) {
            page = Math.max(0, Integer.parseInt(args[0]) - 1);
        }

        final int finalPage = page;
        manager.getPendingReports().thenAccept(reports -> {
            reportListCache.put(player.getUniqueId(), reports);
            reportPageCache.put(player.getUniqueId(), finalPage);
            getServer().getScheduler().runTask(this, () ->
                ReportGUI.openQueue(player, reports, finalPage));
        });
        return true;
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private void applyStaffMode(Player player) {
        if (getConfig().getBoolean("staff-mode.god-mode", true)) {
            player.setInvulnerable(true);
        }
        if (getConfig().getBoolean("staff-mode.flight", true)) {
            player.setAllowFlight(true);
        }
        // Vanish is applied via hiding (see onPlayerJoin)
    }

    private void broadcastStaffChat(Player from, String message) {
        Component msg = MM.deserialize(
            "<dark_gray>[<gold>Staff Chat</gold>] <gray>" + from.getName() + ": <white>" + message);
        broadcastStaff(msg);
        getLogger().info("[StaffChat] " + from.getName() + ": " + message);
    }

    private void broadcastStaff(Component msg) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (manager.isStaff(p.getUniqueId())) {
                p.sendMessage(msg);
            }
        }
    }

    private void broadcastStaffMessage(Component msg) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (manager.isStaff(p.getUniqueId())) {
                p.sendMessage(msg);
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
            }
        }
    }

    private String banMessage(String reason, String expires) {
        return getConfig().getString("ban-message",
            "<red><bold>You are banned.\n<gray>Reason: <white>{reason}\n<gray>Expires: <white>{expires}")
            .replace("{reason}", reason)
            .replace("{expires}", expires)
            .replace("{id}", "");
    }

    private String getStaffRankDisplay(Player player) {
        if (manager.hasStaffPermPlayer(player, "owner"))     return "<dark_red>[Owner]</dark_red>";
        if (manager.hasStaffPermPlayer(player, "senioradmin")) return "<red>[Sr.Admin]</red>";
        if (manager.hasStaffPermPlayer(player, "admin"))     return "<gold>[Admin]</gold>";
        if (manager.hasStaffPermPlayer(player, "seniormod")) return "<blue>[Sr.Mod]</blue>";
        if (manager.hasStaffPermPlayer(player, "moderator")) return "<aqua>[Mod]</aqua>";
        return "<green>[Helper]</green>";
    }

    private String joinFrom(String[] args, int start) {
        return String.join(" ", Arrays.copyOfRange(args, start, args.length));
    }

    private String formatExpiry(LocalDateTime dt) {
        long secs = java.time.Duration.between(LocalDateTime.now(), dt).getSeconds();
        return DurationParser.format(secs);
    }

    private java.util.concurrent.CompletableFuture<String> lookupUuid(String name) {
        return java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            try {
                return DatabaseManager.getInstance().query(
                    "SELECT uuid FROM players WHERE username = ? LIMIT 1",
                    rs -> rs.next() ? rs.getString("uuid") : null,
                    name
                );
            } catch (SQLException e) {
                return null;
            }
        });
    }

    // Called from StaffManager when we need to unfreeze on quit
    // (bridging method so StaffManager doesn't need a plugin ref)
    private void unfreezeOnQuit(UUID uuid) {
        manager.unfreezePlayer(uuid);
    }
}
