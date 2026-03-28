package com.prison.donor;

import com.prison.database.DatabaseManager;
import com.prison.permissions.PermissionEngine;
import net.kyori.adventure.text.minimessage.MiniMessage;
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
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command must be run by a player.");
            return true;
        }

        if (!command.getName().equalsIgnoreCase("donorrank")) return false;

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
