package com.prison.donor;

import com.prison.database.DatabaseManager;
import com.prison.permissions.PermissionEngine;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

public class DonorPlugin extends JavaPlugin {

    // Tier order — used for display and hierarchy checks
    public static final List<String> TIER_ORDER = List.of("donor", "donorplus", "elite", "eliteplus");

    private DonorAPI api;
    private final MiniMessage mm = MiniMessage.miniMessage();

    @Override
    public void onEnable() {
        if (DatabaseManager.getInstance() == null) {
            getLogger().severe("core-database must be loaded before plugin-donor!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        if (PermissionEngine.getInstance() == null) {
            getLogger().severe("core-permissions must be loaded before plugin-donor!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();
        Map<String, DonorRankData> ranks = loadDonorConfig();
        api = new DonorAPI(ranks);

        getLogger().info("Donor rank system enabled — " + ranks.size() + " tiers loaded.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Donor rank system disabled.");
    }

    // ----------------------------------------------------------------
    // Commands
    // ----------------------------------------------------------------

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        switch (command.getName().toLowerCase()) {
            case "donorrank" -> { return cmdDonorRank(sender, args); }
            case "donoradmin" -> { return cmdDonorAdmin(sender, args); }
            default -> { return false; }
        }
    }

    private boolean cmdDonorRank(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command must be run by a player.");
            return true;
        }
        DonorRankData data = api.getDonorRankData(player.getUniqueId());
        if (data == null) {
            player.sendMessage(mm.deserialize("<gray>You don't have a donor rank."));
            player.sendMessage(mm.deserialize("<gray>Visit the store to support the server and unlock perks!"));
            return true;
        }
        player.sendMessage(mm.deserialize("<gold>═══ Donor Rank Info ═══"));
        player.sendMessage(mm.deserialize("<gray>Your rank: " + data.prefix() + " <white>" + data.display()));
        player.sendMessage(mm.deserialize("<gray>Token multiplier: <white>" + data.tokenMultiplier() + "x"));
        player.sendMessage(mm.deserialize("<gold>Perks:"));
        for (String perk : data.perks()) {
            player.sendMessage(mm.deserialize("  <green>✔ <white>" + perk));
        }
        return true;
    }

    private boolean cmdDonorAdmin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("prison.admin.donor")) {
            sender.sendMessage(mm.deserialize("<red>You don't have permission to use this command."));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(mm.deserialize(
                "<gold>Usage:\n" +
                "  <white>/donoradmin set <player> <tier>\n" +
                "  <white>/donoradmin remove <player>\n" +
                "  <white>/donoradmin check <player>\n" +
                "<gray>Tiers: donor, donorplus, elite, eliteplus"));
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("check")) {
            if (args.length < 2) { sender.sendMessage(mm.deserialize("<red>Usage: /donoradmin check <player>")); return true; }
            Player target = Bukkit.getPlayer(args[1]);
            UUID uuid = target != null ? target.getUniqueId() : resolveOfflineUUID(args[1]);
            if (uuid == null) { sender.sendMessage(mm.deserialize("<red>Player not found: " + args[1])); return true; }
            String rank = api.getDonorRank(uuid);
            if (rank == null) {
                sender.sendMessage(mm.deserialize("<gray>" + args[1] + " has no donor rank."));
            } else {
                DonorRankData data = api.getRankData(rank);
                String display = data != null ? data.display() : rank;
                sender.sendMessage(mm.deserialize("<gray>" + args[1] + "'s donor rank: <gold>" + display));
            }
            return true;
        }

        if (sub.equals("remove")) {
            if (args.length < 2) { sender.sendMessage(mm.deserialize("<red>Usage: /donoradmin remove <player>")); return true; }
            Player target = Bukkit.getPlayer(args[1]);
            UUID uuid = target != null ? target.getUniqueId() : resolveOfflineUUID(args[1]);
            if (uuid == null) { sender.sendMessage(mm.deserialize("<red>Player not found: " + args[1])); return true; }
            api.setDonorRank(uuid, null).thenRun(() ->
                getServer().getScheduler().runTask(this, () ->
                    sender.sendMessage(mm.deserialize("<green>Removed donor rank from " + args[1] + "."))));
            return true;
        }

        if (sub.equals("set")) {
            if (args.length < 3) { sender.sendMessage(mm.deserialize("<red>Usage: /donoradmin set <player> <tier>")); return true; }
            String tier = args[2].toLowerCase();
            if (!TIER_ORDER.contains(tier)) {
                sender.sendMessage(mm.deserialize("<red>Unknown tier: <white>" + tier +
                    " <red>— valid tiers: <white>" + String.join(", ", TIER_ORDER)));
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            UUID uuid = target != null ? target.getUniqueId() : resolveOfflineUUID(args[1]);
            if (uuid == null) { sender.sendMessage(mm.deserialize("<red>Player not found: " + args[1])); return true; }
            DonorRankData data = api.getRankData(tier);
            String display = data != null ? data.display() : tier;
            api.setDonorRank(uuid, tier).thenRun(() ->
                getServer().getScheduler().runTask(this, () -> {
                    sender.sendMessage(mm.deserialize("<green>Set " + args[1] + "'s donor rank to <gold>" + display + "<green>."));
                    if (target != null && target.isOnline()) {
                        target.sendMessage(mm.deserialize(
                            "<gold>✦ <yellow>You have been granted the <gold>" + display + " <yellow>donor rank!"));
                    }
                }));
            return true;
        }

        sender.sendMessage(mm.deserialize("<red>Unknown subcommand. Use /donoradmin for help."));
        return true;
    }

    /** Look up a UUID from an offline player by name. Returns null if not found. */
    @SuppressWarnings("deprecation")
    private UUID resolveOfflineUUID(String name) {
        org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(name);
        return op.hasPlayedBefore() ? op.getUniqueId() : null;
    }

    // ----------------------------------------------------------------
    // Config loading
    // ----------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private Map<String, DonorRankData> loadDonorConfig() {
        File configFile = new File(getDataFolder(), "config.yml");
        Map<String, DonorRankData> result = new LinkedHashMap<>();

        try (FileInputStream fis = new FileInputStream(configFile)) {
            Map<String, Object> root = new Yaml().load(fis);
            Map<String, Object> ranksSection = (Map<String, Object>) root.get("ranks");

            for (String key : TIER_ORDER) {
                Object entry = ranksSection.get(key);
                if (!(entry instanceof Map)) continue;
                Map<String, Object> rankData = (Map<String, Object>) entry;

                String display  = (String) rankData.getOrDefault("display", key);
                String prefix   = (String) rankData.getOrDefault("prefix", "[" + key + "]");
                double mult     = ((Number) rankData.getOrDefault("token-multiplier", 1.0)).doubleValue();
                List<String> perks = rankData.containsKey("perks")
                    ? (List<String>) rankData.get("perks")
                    : List.of();

                result.put(key, new DonorRankData(key, display, prefix, mult, perks));
            }

            getLogger().info("[Donor] Loaded " + result.size() + " donor rank tiers.");
        } catch (Exception e) {
            getLogger().severe("[Donor] Failed to load config: " + e.getMessage() + " — using defaults.");
            result = buildDefaultConfig();
        }

        return result;
    }

    private Map<String, DonorRankData> buildDefaultConfig() {
        Map<String, DonorRankData> map = new LinkedHashMap<>();
        map.put("donor",      new DonorRankData("donor",      "Donor",   "[Donor]",   1.25, List.of("1.25x token multiplier")));
        map.put("donorplus",  new DonorRankData("donorplus",  "Donor+",  "[Donor+]",  1.50, List.of("1.5x token multiplier")));
        map.put("elite",      new DonorRankData("elite",      "Elite",   "[Elite]",   1.75, List.of("1.75x token multiplier")));
        map.put("eliteplus",  new DonorRankData("eliteplus",  "Elite+",  "[Elite+]",  2.00, List.of("2.0x token multiplier")));
        return map;
    }
}
