package com.prison.kits;

import com.prison.database.DatabaseManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
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
        manager.loadPlayer(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        manager.unloadPlayer(event.getPlayer().getUniqueId());
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
            showKitList(player);
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
        if (result == KitsManager.ClaimResult.SUCCESS) {
            player.sendMessage(MM.deserialize("<green>You claimed the " + kit.display() + " <green>kit!"));
        } else {
            // Should not happen if we checked above, but safety fallback
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
