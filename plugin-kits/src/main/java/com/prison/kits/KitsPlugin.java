package com.prison.kits;

import com.prison.database.DatabaseManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class KitsPlugin extends JavaPlugin implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private KitsManager manager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Load kit definitions
        Map<String, KitData> kits = loadKitsFromConfig();
        manager = KitsManager.initialize(kits, getLogger(), System.currentTimeMillis());
        new KitsAPI();

        ensureTables();
        getServer().getPluginManager().registerEvents(this, this);

        getLogger().info("[Kits] Loaded " + kits.size() + " kits.");
    }

    @Override
    public void onDisable() {
        getLogger().info("[Kits] Plugin disabled.");
    }

    // ----------------------------------------------------------------
    // Config loading
    // ----------------------------------------------------------------

    private Map<String, KitData> loadKitsFromConfig() {
        Map<String, KitData> kits = new LinkedHashMap<>();
        org.bukkit.configuration.ConfigurationSection section = getConfig().getConfigurationSection("kits");
        if (section == null) return kits;

        for (String id : section.getKeys(false)) {
            org.bukkit.configuration.ConfigurationSection kitSection = section.getConfigurationSection(id);
            if (kitSection == null) continue;
            KitData kit = KitData.fromConfig(id.toLowerCase(), kitSection, getLogger());
            if (kit != null) kits.put(id.toLowerCase(), kit);
        }
        return kits;
    }

    // ----------------------------------------------------------------
    // DB tables
    // ----------------------------------------------------------------

    private void ensureTables() {
        try {
            DatabaseManager.getInstance().execute(
                "CREATE TABLE IF NOT EXISTS kit_cooldowns (" +
                "  player_uuid  VARCHAR(36)  NOT NULL," +
                "  kit_id       VARCHAR(64)  NOT NULL," +
                "  last_claimed TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "  PRIMARY KEY (player_uuid, kit_id)" +
                ")"
            );
            // Migration: drop legacy cooldown_duration column if it exists
            try {
                DatabaseManager.getInstance().execute(
                    "ALTER TABLE kit_cooldowns DROP COLUMN IF EXISTS cooldown_duration");
            } catch (SQLException ignored) {} // older MySQL without IF EXISTS — column may not exist, ignore
            DatabaseManager.getInstance().execute(
                "CREATE TABLE IF NOT EXISTS kit_logs (" +
                "  id           INT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                "  player_uuid  VARCHAR(36)  NOT NULL," +
                "  kit_id       VARCHAR(64)  NOT NULL," +
                "  claimed_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "  INDEX idx_kit_logs_player (player_uuid)," +
                "  INDEX idx_kit_logs_kit    (kit_id)" +
                ")"
            );
        } catch (SQLException e) {
            getLogger().severe("[Kits] Failed to create tables: " + e.getMessage());
        }
    }

    // ----------------------------------------------------------------
    // Events
    // ----------------------------------------------------------------

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        boolean firstJoin = !player.hasPlayedBefore();

        // Load kit cooldowns async, then run onboarding on main thread
        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            manager.loadPlayer(player.getUniqueId());
            // Once loaded, run the welcome sequence on the main thread
            getServer().getScheduler().runTaskLater(this, () -> {
                if (!player.isOnline()) return;
                if (firstJoin) {
                    runFirstJoinSequence(player);
                } else {
                    runReturnVisitGreeting(player);
                }
            }, 20L); // 1 second delay — enough for permissions + economy to load
        });
    }

    /** Fired only on the very first time a player joins the server. */
    private void runFirstJoinSequence(Player player) {
        // 1 — Welcome title
        player.showTitle(Title.title(
            MM.deserialize("<gradient:#FFD700:#FF8C00><bold>The Pharaoh's Prison</bold></gradient>"),
            MM.deserialize("<gray>Your soul has been claimed by the sands of Egypt"),
            Title.Times.times(Duration.ofMillis(800), Duration.ofMillis(4000), Duration.ofMillis(800))
        ));
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 0.6f);

        // 2 — Staggered chat welcome messages (ticks: 40, 60, 80, 100, 120)
        scheduleMsg(player, 40L,
            "<gold>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        scheduleMsg(player, 42L,
            "<gold><bold>Welcome to The Pharaoh's Prison!</bold></gold> <gray>A new soul awakens.");
        scheduleMsg(player, 50L,
            "<gray>● <white>Mine blocks to earn <gold>Coins</gold>. Use <gold>/sell all</gold> to cash in.");
        scheduleMsg(player, 60L,
            "<gray>● <white>Rank up with <gold>/ranks</gold>. Reach <gold>Rank Z</gold> to <light_purple>Ascend</light_purple>.");
        scheduleMsg(player, 70L,
            "<gray>● <white>Open the main menu with <gold>/menu</gold>. Your journey begins <aqua>/warp pit</aqua>.");
        scheduleMsg(player, 80L,
            "<gold>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        // 3 — Auto-deliver starter kit (at tick 25 so inventory is open)
        getServer().getScheduler().runTaskLater(this, () -> {
            if (!player.isOnline()) return;
            KitData starterKit = manager.getKit("starter");
            if (starterKit == null) {
                getLogger().warning("[Kits] No 'starter' kit found — skipping first-join delivery.");
                return;
            }
            KitsManager.ClaimResult result = manager.claimKit(player, starterKit);
            if (result == KitsManager.ClaimResult.SUCCESS) {
                player.sendMessage(MM.deserialize(
                    "<green>✦ <white>Your <gold>Starter Kit</gold> has been delivered to your inventory!"));
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.5f);
            } else if (result == KitsManager.ClaimResult.INVENTORY_FULL) {
                player.sendMessage(MM.deserialize(
                    "<yellow>⚠ <white>Your inventory is full — type <gold>/kit starter</gold> when you have space."));
            }
            // ONE_TIME_CLAIMED means the kit was already given (shouldn't happen on first join, but handle gracefully)
        }, 25L);
    }

    /** Fired on every join EXCEPT the very first. */
    private void runReturnVisitGreeting(Player player) {
        com.prison.economy.EconomyAPI eco = com.prison.economy.EconomyAPI.getInstance();
        com.prison.permissions.PermissionEngine perms = com.prison.permissions.PermissionEngine.getInstance();
        if (eco == null || perms == null) return;

        String rank      = perms.getMineRank(player.getUniqueId());
        long   balance   = eco.getBalance(player.getUniqueId());
        int    prestige  = perms.getPrestige(player.getUniqueId());

        String prestigeStr = prestige > 0 ? " <dark_purple>[P" + prestige + "]" : "";
        player.sendMessage(MM.deserialize(
            "<gold>Welcome back, <white>" + player.getName() + prestigeStr + "<gold>! " +
            "<gray>Rank: <white>" + rank + " <dark_gray>| <gray>Coins: <gold>" + String.format("%,d", balance)));
    }

    private void scheduleMsg(Player player, long ticks, String msg) {
        getServer().getScheduler().runTaskLater(this, () -> {
            if (player.isOnline()) player.sendMessage(MM.deserialize(msg));
        }, ticks);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        manager.unloadPlayer(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!KitsGUI.isTitle(player.getOpenInventory().title())) return;
        event.setCancelled(true);
        if (event.getClickedInventory() == event.getView().getTopInventory()) {
            KitsGUI.handleClick(player, event.getRawSlot(), this);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (KitsGUI.isTitle(player.getOpenInventory().title())) {
            event.setCancelled(true);
        }
    }

    // ----------------------------------------------------------------
    // Commands
    // ----------------------------------------------------------------

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("kit")) return false;

        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command must be run in-game.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            KitsGUI.open(player);
            return true;
        }

        String kitId = args[0].toLowerCase();
        boolean preview = args.length >= 2 && args[1].equalsIgnoreCase("preview");

        KitData kit = manager.getKit(kitId);
        if (kit == null) {
            player.sendMessage(MM.deserialize("<red>Unknown kit: <white>" + args[0]));
            return true;
        }

        if (preview) {
            showPreview(player, kit);
        } else {
            handleClaim(player, kit);
        }
        return true;
    }

    // ----------------------------------------------------------------
    // Command handlers
    // ----------------------------------------------------------------

    private void showKitList(Player player) {
        player.sendMessage(MM.deserialize("<gold>━━━━━━━━ Available Kits ━━━━━━━━"));

        List<KitData> accessible = manager.getAccessibleKits(player);

        if (accessible.isEmpty()) {
            player.sendMessage(MM.deserialize("<gray>No kits available to you right now."));
            return;
        }

        for (KitData kit : accessible) {
            long remaining = manager.getRemainingMs(player.getUniqueId(), kit);
            String status;
            if (remaining == 0) {
                status = "<green>[READY]";
            } else if (remaining == Long.MAX_VALUE) {
                status = "<gray>[CLAIMED]";
            } else {
                status = "<yellow>[" + formatDuration(remaining) + "]";
            }
            player.sendMessage(MM.deserialize(
                " " + status + " " + kit.display() +
                " <dark_gray>— /kit " + kit.id() + " <dark_gray>| /kit " + kit.id() + " preview"
            ));
        }

        player.sendMessage(MM.deserialize("<gold>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }

    private void showPreview(Player player, KitData kit) {
        if (!manager.meetsRequirements(player, kit)) {
            player.sendMessage(MM.deserialize("<red>You don't have access to this kit."));
            return;
        }
        player.sendMessage(MM.deserialize("<gold>━━━━━━━━ " + kit.display() + " <gold>━━━━━━━━"));

        // Type / requirement line
        switch (kit.type()) {
            case RANK ->
                player.sendMessage(MM.deserialize("<gray>Requires mine rank <white>" + kit.requiredRank() + "<gray> or higher."));
            case DONOR ->
                player.sendMessage(MM.deserialize("<gray>Requires donor rank <white>" + kit.requiredDonorRank() + "<gray> or higher."));
            default ->
                player.sendMessage(MM.deserialize("<gray>Available to all players."));
        }

        // Cooldown line
        if (kit.type() == KitData.KitType.DONOR) {
            player.sendMessage(MM.deserialize(
                "<gray>Cooldown: resets on server restart " +
                "<dark_gray>(min " + formatDuration(kit.minDonorCooldownMs()) + ")"));
        } else if (kit.cooldownMs() == 0) {
            player.sendMessage(MM.deserialize("<gray>Cooldown: <red>one-time only"));
        } else {
            player.sendMessage(MM.deserialize("<gray>Cooldown: <white>" + formatDuration(kit.cooldownMs())));
        }

        player.sendMessage(MM.deserialize("<gray>Contents:"));
        for (KitItem item : kit.contents()) {
            StringBuilder desc = new StringBuilder("  <white>");
            desc.append(item.amount()).append("x ").append(formatMaterial(item.material().name()));
            if (!item.enchants().isEmpty()) {
                desc.append(" <dark_gray>(");
                StringJoiner sj = new StringJoiner(", ");
                item.enchants().forEach((e, lvl) -> sj.add(formatEnchant(e.getKey().getKey()) + " " + lvl));
                desc.append(sj).append(")");
            }
            player.sendMessage(MM.deserialize(desc.toString()));
        }
        player.sendMessage(MM.deserialize("<gold>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }

    private void handleClaim(Player player, KitData kit) {
        // Check requirements first with a friendly message
        if (!manager.meetsRequirements(player, kit)) {
            switch (kit.type()) {
                case RANK ->
                    player.sendMessage(MM.deserialize(
                        "<red>This kit requires mine rank <white>" + kit.requiredRank() + "<red> or higher."));
                case DONOR ->
                    player.sendMessage(MM.deserialize(
                        "<red>This kit requires donor rank <white>" + kit.requiredDonorRank() + "<red> or higher."));
                default ->
                    player.sendMessage(MM.deserialize("<red>You don't have access to this kit."));
            }
            return;
        }

        // Check cooldown with a friendly message
        long remaining = manager.getRemainingMs(player.getUniqueId(), kit);
        if (remaining == Long.MAX_VALUE) {
            player.sendMessage(MM.deserialize(
                "<red>You have already claimed <white>" + kit.display() + " <red>(one-time only)."));
            return;
        }
        if (remaining > 0) {
            if (kit.type() == KitData.KitType.DONOR) {
                player.sendMessage(MM.deserialize(
                    "<red>This kit resets on server restart. " +
                    "<gray>Minimum cooldown: <white>" + formatDuration(remaining) + " <gray>remaining."));
            } else {
                player.sendMessage(MM.deserialize(
                    "<red>This kit is on cooldown. <gray>Available in: <white>" + formatDuration(remaining)));
            }
            return;
        }

        // Claim
        KitsManager.ClaimResult result = manager.claimKit(player, kit);
        switch (result) {
            case SUCCESS -> {
                player.sendMessage(MM.deserialize("<green>You claimed the " + kit.display() + " <green>kit!"));
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.2f);
            }
            case INVENTORY_FULL ->
                player.sendMessage(MM.deserialize(
                    "<red>Your inventory is too full to claim this kit. " +
                    "<gray>Free up <white>" + kit.contents().size() + " slot(s) <gray>and try again."));
            default ->
                player.sendMessage(MM.deserialize("<red>Could not claim kit. Please try again."));
        }
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    /** Format milliseconds as "Xh Ym" or "Ym Zs". */
    static String formatDuration(long ms) {
        if (ms <= 0) return "0s";
        long hours   = TimeUnit.MILLISECONDS.toHours(ms);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(ms) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60;

        if (hours > 0)   return hours + "h " + minutes + "m";
        if (minutes > 0) return minutes + "m " + seconds + "s";
        return seconds + "s";
    }

    private String formatMaterial(String name) {
        // "DIAMOND_PICKAXE" → "Diamond Pickaxe"
        String[] parts = name.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }

    private String formatEnchant(String key) {
        return formatMaterial(key.replace("minecraft:", ""));
    }
}
