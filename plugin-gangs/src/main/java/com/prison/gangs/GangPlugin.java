package com.prison.gangs;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GangPlugin — main plugin class for the Gang / Crew system.
 */
public class GangPlugin extends JavaPlugin {

    private static GangPlugin instance;
    private GangManager manager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        // ----------------------------------------------------------------
        // Read config
        // ----------------------------------------------------------------
        int defaultMax = getConfig().getInt("max-members", 10);

        Map<String, Integer> donorMax = new HashMap<>();
        org.bukkit.configuration.ConfigurationSection donorSec =
            getConfig().getConfigurationSection("donor-max-members");
        if (donorSec != null) {
            for (String key : donorSec.getKeys(false)) {
                donorMax.put(key.toLowerCase(), donorSec.getInt(key, defaultMax));
            }
        }

        List<?> rawThresholds = getConfig().getList("level-thresholds");
        long[] thresholds;
        if (rawThresholds != null && !rawThresholds.isEmpty()) {
            thresholds = new long[rawThresholds.size()];
            for (int i = 0; i < rawThresholds.size(); i++) {
                Object val = rawThresholds.get(i);
                thresholds[i] = val instanceof Number ? ((Number) val).longValue() : 0L;
            }
        } else {
            // defaults
            thresholds = new long[]{50000, 150000, 350000, 750000, 1500000, 3000000, 5000000, 8000000, 12000000};
        }

        // ----------------------------------------------------------------
        // Read sell / token bonus arrays (index = level-1; level 1 = no bonus)
        // ----------------------------------------------------------------
        org.bukkit.configuration.ConfigurationSection sellSec =
            getConfig().getConfigurationSection("sell-bonuses");
        org.bukkit.configuration.ConfigurationSection tokenSec =
            getConfig().getConfigurationSection("token-bonuses");

        int maxLevel = thresholds.length + 1; // e.g. 9 thresholds → levels 1-10
        double[] sellBonuses  = new double[maxLevel];
        double[] tokenBonuses = new double[maxLevel];
        for (int lv = 1; lv <= maxLevel; lv++) {
            sellBonuses[lv - 1]  = (sellSec  != null) ? sellSec.getDouble(String.valueOf(lv),  1.0) : 1.0;
            tokenBonuses[lv - 1] = (tokenSec != null) ? tokenSec.getDouble(String.valueOf(lv), 1.0) : 1.0;
        }

        // ----------------------------------------------------------------
        // Initialize manager & API
        // ----------------------------------------------------------------
        manager = GangManager.initialize(defaultMax, donorMax, thresholds, getLogger());
        GangAPI gangAPI = new GangAPI(manager);
        gangAPI.setBonuses(sellBonuses, tokenBonuses);

        // Register gang bonus providers with EconomyAPI (soft-dep — safe if not loaded yet)
        org.bukkit.plugin.Plugin ecoPlugin =
            getServer().getPluginManager().getPlugin("PrisonEconomy");
        if (ecoPlugin != null && ecoPlugin.isEnabled()) {
            try {
                com.prison.economy.EconomyAPI eco = com.prison.economy.EconomyAPI.getInstance();
                if (eco != null) {
                    eco.setGangSellBonusProvider(uuid -> GangAPI.getInstance().getSellBonus(uuid));
                    eco.setGangTokenBonusProvider(uuid -> GangAPI.getInstance().getTokenBonus(uuid));
                    getLogger().info("[Gangs] Registered sell + token bonus providers with EconomyAPI.");
                }
            } catch (Exception e) {
                getLogger().warning("[Gangs] Could not register EconomyAPI bonus providers: " + e.getMessage());
            }
        }

        // ----------------------------------------------------------------
        // Load data
        // ----------------------------------------------------------------
        manager.loadAll().thenRun(() -> {
            getLogger().info("[Gangs] Data loaded successfully.");
        }).exceptionally(ex -> {
            getLogger().severe("[Gangs] Failed to load data: " + ex.getMessage());
            return null;
        });

        // ----------------------------------------------------------------
        // Register commands
        // ----------------------------------------------------------------
        GangCommand cmd = new GangCommand(this, manager);
        var gangCmd = getCommand("gang");
        if (gangCmd != null) gangCmd.setExecutor(cmd);
        var gcCmd = getCommand("gc");
        if (gcCmd != null) gcCmd.setExecutor(cmd);

        // ----------------------------------------------------------------
        // Schedule: level recalculation every 5 minutes (6000 ticks)
        // ----------------------------------------------------------------
        Bukkit.getScheduler().runTaskTimerAsynchronously(this,
            manager::recalculateLevels, 6000L, 6000L);

        // ----------------------------------------------------------------
        // Schedule: leaderboard refresh every 60 seconds (1200 ticks)
        // ----------------------------------------------------------------
        Bukkit.getScheduler().runTaskTimerAsynchronously(this,
            manager::rebuildLeaderboard, 1200L, 1200L);

        getLogger().info("GangPlugin enabled.");
    }

    @Override
    public void onDisable() {
        GangManager.reset();
        instance = null;
        getLogger().info("GangPlugin disabled.");
    }

    public static GangPlugin getInstance() { return instance; }
}
