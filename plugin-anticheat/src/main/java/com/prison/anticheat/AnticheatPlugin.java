package com.prison.anticheat;

import com.prison.database.DatabaseManager;
import com.prison.permissions.PermissionEngine;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * AnticheatPlugin — lightweight prison-scoped anti-cheat.
 *
 * Monitors:
 *   1. /sellall rate — catches macro sell spam
 *   2. Block break rate — catches AoE enchant abuse / auto-clickers
 *   3. Token earn rate — periodic sanity check via transaction DB
 *
 * No automatic bans. All flags go to the anticheat_flags table and are
 * broadcast to staff chat for human review.
 */
public class AnticheatPlugin extends JavaPlugin implements Listener {

    private static AnticheatPlugin instance;
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private AnticheatManager manager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        manager = AnticheatManager.initialize(getLogger());
        loadConfig();

        getServer().getPluginManager().registerEvents(this, this);

        // Schedule periodic token-rate check
        int tokenCheckSecs = getConfig().getInt("token-check-interval-seconds", 300);
        if (manager.maxTokensPerMinute > 0 && tokenCheckSecs > 0) {
            long ticks = tokenCheckSecs * 20L;
            Bukkit.getScheduler().runTaskTimerAsynchronously(this,
                () -> manager.runTokenRateCheck(tokenCheckSecs),
                ticks, ticks
            );
        }

        getLogger().info("AnticheatPlugin enabled.");
    }

    @Override
    public void onDisable() {
        AnticheatManager.reset();
        instance = null;
        getLogger().info("AnticheatPlugin disabled.");
    }

    public static AnticheatPlugin getInstance() { return instance; }

    private void loadConfig() {
        manager.sellCooldownMs            = getConfig().getLong("sell-cooldown-ms", 500L);
        manager.sellViolationThreshold    = getConfig().getInt("sell-violation-threshold", 5);
        manager.sellViolationWindowMs     = getConfig().getLong("sell-violation-window-ms", 30000L);
        manager.blockBreakMaxPerSecond    = getConfig().getInt("block-break-max-per-second", 1500);
        manager.blockBreakConsecutiveThreshold = getConfig().getInt("block-break-consecutive-threshold", 3);
        manager.maxTokensPerMinute        = getConfig().getLong("max-tokens-per-minute", 10000L);
        manager.alertEveryNViolations     = getConfig().getInt("alert-every-n-violations", 10);
        manager.logAllFlags               = getConfig().getBoolean("log-all-flags", true);
    }

    // ----------------------------------------------------------------
    // Events
    // ----------------------------------------------------------------

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        manager.onPlayerJoin(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        manager.onPlayerQuit(event.getPlayer().getUniqueId());
    }

    /**
     * Intercept /sellall — cancel the command if it's being spammed.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String msg = event.getMessage();
        // Match /sellall (with or without arguments, case-insensitive)
        if (!msg.toLowerCase().startsWith("/sellall")) return;

        Player player = event.getPlayer();
        if (!manager.checkSell(player)) {
            event.setCancelled(true);
            player.sendMessage(MM.deserialize("<red>You are selling too fast. Please wait."));
        }
    }

    /**
     * Track block break counts per player per second.
     *
     * We estimate the total blocks actually removed (including AoE from enchants)
     * because explosive/laser fire setType(AIR) directly — not BlockBreakEvent.
     * Runs at MONITOR so we see the final state — we do not cancel breaks.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        int count = estimateBlockCount(event.getPlayer());
        manager.recordBlockBreaks(event.getPlayer(), count);
    }

    /**
     * Estimate how many blocks will be removed by this break event.
     * Looks at the player's server pickaxe enchant levels if PickaxeAPI is available.
     *
     * Explosive L{n}: removes a (2n+1)³ cube centred on the broken block.
     * Laser L{n}:     removes n*2 + 1 blocks in a line.
     * No enchants:    1 block.
     */
    private int estimateBlockCount(Player player) {
        com.prison.pickaxe.PickaxeAPI pickaxeApi = com.prison.pickaxe.PickaxeAPI.getInstance();
        if (pickaxeApi == null) return 1;

        org.bukkit.inventory.ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (!pickaxeApi.isServerPickaxe(heldItem)) return 1;

        int explosive = pickaxeApi.getEnchantLevel(heldItem, "explosive");
        if (explosive > 0) {
            int side = 2 * explosive + 1;
            return side * side * side; // cubic AoE (conservative over-estimate — some blocks may be air)
        }

        int laser = pickaxeApi.getEnchantLevel(heldItem, "laser");
        if (laser > 0) {
            return laser * 2 + 1;
        }

        return 1;
    }

    // ----------------------------------------------------------------
    // Commands
    // ----------------------------------------------------------------

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        switch (command.getName().toLowerCase()) {
            case "acflags" -> handleFlags(sender, args);
            case "acclear" -> handleClear(sender, args);
        }
        return true;
    }

    private void handleFlags(CommandSender sender, String[] args) {
        if (!checkStaffPerm(sender, "prison.staff.moderator")) return;
        if (args.length < 1) {
            sender.sendMessage(MM.deserialize("<red>Usage: /acflags <player> [page]"));
            return;
        }

        String targetName = args[0];
        int page = args.length >= 2 ? parsePageArg(args[1]) : 1;
        int offset = (page - 1) * 10;

        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                // Look up UUID from name
                String uuidStr = null;
                org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(targetName);
                if (op.hasPlayedBefore()) uuidStr = op.getUniqueId().toString();

                if (uuidStr == null) {
                    final String missing = targetName;
                    Bukkit.getScheduler().runTask(this, () ->
                        sender.sendMessage(MM.deserialize("<red>Player not found: " + missing)));
                    return;
                }

                final String finalUuid = uuidStr;
                List<String[]> rows = DatabaseManager.getInstance().query(
                    "SELECT flag_type, details, timestamp FROM anticheat_flags " +
                    "WHERE player_uuid = ? ORDER BY timestamp DESC LIMIT 10 OFFSET ?",
                    rs -> {
                        List<String[]> list = new ArrayList<>();
                        while (rs.next()) {
                            list.add(new String[]{
                                rs.getString("flag_type"),
                                rs.getString("details"),
                                rs.getTimestamp("timestamp").toString()
                            });
                        }
                        return list;
                    },
                    finalUuid, offset
                );

                Player online = Bukkit.getPlayer(op.getUniqueId());
                int sessionVio = online != null ? manager.getSessionViolations(online.getUniqueId()) : 0;

                Bukkit.getScheduler().runTask(this, () -> {
                    sender.sendMessage(MM.deserialize(
                        "<gold><bold>AC Flags — " + targetName + " (session: " + sessionVio + ")</bold></gold>"));
                    if (rows.isEmpty()) {
                        sender.sendMessage(MM.deserialize("<gray>No flags found on page " + page + "."));
                        return;
                    }
                    for (String[] row : rows) {
                        sender.sendMessage(MM.deserialize(
                            "<yellow>" + row[0] + " <gray>| " + row[1] + " <dark_gray>@ " + row[2]));
                    }
                    sender.sendMessage(MM.deserialize(
                        "<dark_gray>Page " + page + " — use /acflags " + targetName + " " + (page + 1) + " for more."));
                });

            } catch (SQLException e) {
                getLogger().log(java.util.logging.Level.SEVERE, "Failed to query AC flags", e);
                Bukkit.getScheduler().runTask(this, () ->
                    sender.sendMessage(MM.deserialize("<red>Database error. Check console.")));
            }
        });
    }

    private void handleClear(CommandSender sender, String[] args) {
        if (!checkStaffPerm(sender, "prison.staff.admin")) return;
        if (args.length < 1) {
            sender.sendMessage(MM.deserialize("<red>Usage: /acclear <player>"));
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(MM.deserialize("<red>Player not online."));
            return;
        }

        manager.clearSession(target.getUniqueId());
        sender.sendMessage(MM.deserialize(
            "<green>Cleared AC session counters for <white>" + target.getName() + "</white>."));
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private boolean checkStaffPerm(CommandSender sender, String perm) {
        if (!(sender instanceof Player player)) return true; // console always passes
        if (!PermissionEngine.getInstance().hasPermission(player, perm)) {
            player.sendMessage(MM.deserialize("<red>No permission."));
            return false;
        }
        return true;
    }

    private static int parsePageArg(String s) {
        try {
            int p = Integer.parseInt(s);
            return Math.max(1, p);
        } catch (NumberFormatException e) {
            return 1;
        }
    }
}
