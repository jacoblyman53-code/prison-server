package com.prison.pickaxe;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

/**
 * PickaxeConfig — typed wrapper around config.yml values.
 */
public class PickaxeConfig {

    private final double prestigeCostMultiplier;
    private final long baseTokensPerBlock;
    private final String upgradeTitle;
    private final String confirmTitle;

    private final List<EnchantDef> customEnchants;
    private final List<EnchantDef> vanillaEnchants;

    // Pre-parsed sub-maps for hot-path lookups
    private final Map<Integer, Double> tokenatorMultipliers;
    private final Map<Integer, Integer> sellallThresholds;
    private final Map<Integer, Double> jackpotChances;

    public PickaxeConfig(FileConfiguration cfg) {
        prestigeCostMultiplier = cfg.getDouble("prestige-cost-multiplier", 0.10);
        baseTokensPerBlock     = cfg.getLong("base-tokens-per-block", 1);
        upgradeTitle = cfg.getString("gui.upgrade-title", "<dark_gray>[ <gold>Pickaxe Upgrades</gold> ]");
        confirmTitle = cfg.getString("gui.confirm-title", "<dark_gray>[ <red>Confirm Purchase</red> ]");

        customEnchants = loadEnchants(cfg.getConfigurationSection("custom-enchants"), false);
        vanillaEnchants = loadEnchants(cfg.getConfigurationSection("vanilla-enchants"), true);

        // Pre-parse special enchant sub-maps
        tokenatorMultipliers = loadDoubleMap(cfg.getConfigurationSection("custom-enchants.tokenator.multipliers"));
        sellallThresholds    = loadIntMap(cfg.getConfigurationSection("custom-enchants.sellall.thresholds"));
        jackpotChances       = loadDoubleMap(cfg.getConfigurationSection("custom-enchants.jackpot.chances"));
    }

    private List<EnchantDef> loadEnchants(ConfigurationSection section, boolean isVanilla) {
        List<EnchantDef> list = new ArrayList<>();
        if (section == null) return list;
        for (String key : section.getKeys(false)) {
            ConfigurationSection s = section.getConfigurationSection(key);
            if (s != null) list.add(EnchantDef.fromConfig(key, s, isVanilla));
        }
        return list;
    }

    private Map<Integer, Double> loadDoubleMap(ConfigurationSection section) {
        Map<Integer, Double> map = new TreeMap<>();
        if (section == null) return map;
        for (String key : section.getKeys(false)) {
            try { map.put(Integer.parseInt(key), section.getDouble(key)); }
            catch (NumberFormatException ignored) {}
        }
        return map;
    }

    private Map<Integer, Integer> loadIntMap(ConfigurationSection section) {
        Map<Integer, Integer> map = new TreeMap<>();
        if (section == null) return map;
        for (String key : section.getKeys(false)) {
            try { map.put(Integer.parseInt(key), section.getInt(key)); }
            catch (NumberFormatException ignored) {}
        }
        return map;
    }

    // ----------------------------------------------------------------
    // Getters
    // ----------------------------------------------------------------

    public double getPrestigeCostMultiplier() { return prestigeCostMultiplier; }
    public long getBaseTokensPerBlock()       { return baseTokensPerBlock; }
    public String getUpgradeTitle()           { return upgradeTitle; }
    public String getConfirmTitle()           { return confirmTitle; }
    public List<EnchantDef> getCustomEnchants()  { return customEnchants; }
    public List<EnchantDef> getVanillaEnchants() { return vanillaEnchants; }
    public Map<Integer, Double>  getTokenatorMultipliers() { return tokenatorMultipliers; }
    public Map<Integer, Integer> getSellallThresholds()    { return sellallThresholds; }
    public Map<Integer, Double>  getJackpotChances()       { return jackpotChances; }

    /** All enchant IDs in order: custom first, then vanilla. */
    public List<String> allEnchantIds() {
        List<String> ids = new ArrayList<>();
        customEnchants.forEach(e -> ids.add(e.id()));
        vanillaEnchants.forEach(e -> ids.add(e.id()));
        return ids;
    }

    /** Look up any enchant by id. */
    public EnchantDef getEnchant(String id) {
        for (EnchantDef def : customEnchants) {
            if (def.id().equals(id)) return def;
        }
        for (EnchantDef def : vanillaEnchants) {
            if (def.id().equals(id)) return def;
        }
        return null;
    }

    /**
     * Token cost to upgrade an enchant, scaled by the player's prestige level.
     * prestige 0 → no multiplier; prestige N → cost * (1 + N * multiplier)
     */
    public long scaledCost(EnchantDef def, int currentLevel, int prestigeLevel) {
        long base = def.upgradeCost(currentLevel);
        if (base <= 0) return base;
        double scale = 1.0 + (prestigeLevel * prestigeCostMultiplier);
        return Math.round(base * scale);
    }
}
