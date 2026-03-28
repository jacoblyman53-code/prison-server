package com.prison.pickaxe;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * EnchantDef — definition of one enchant loaded from config.yml.
 *
 * Both custom and vanilla enchants use this structure.
 * Vanilla enchants are identified by isVanilla = true and map directly to
 * Bukkit Enchantment constants (EFFICIENCY, FORTUNE, SILK_TOUCH).
 */
public record EnchantDef(
    String id,            // config key, e.g. "explosive", "efficiency"
    String display,       // display name
    int maxLevel,
    Material icon,
    Map<Integer, Long> tokenCosts,    // level → token cost
    Map<Integer, String> descriptions, // level → description shown in GUI
    boolean isVanilla
) {

    /** Build an EnchantDef from a config section under custom-enchants or vanilla-enchants. */
    public static EnchantDef fromConfig(String id, ConfigurationSection section, boolean isVanilla) {
        String display = section.getString("display", id);
        int maxLevel   = section.getInt("max-level", 1);

        Material icon;
        try {
            icon = Material.valueOf(section.getString("icon", "DIAMOND_PICKAXE").toUpperCase());
        } catch (IllegalArgumentException e) {
            icon = Material.DIAMOND_PICKAXE;
        }

        Map<Integer, Long>   costs  = new TreeMap<>();
        Map<Integer, String> descs  = new TreeMap<>();

        ConfigurationSection costsSection = section.getConfigurationSection("token-costs");
        if (costsSection != null) {
            for (String key : costsSection.getKeys(false)) {
                try {
                    int level = Integer.parseInt(key);
                    costs.put(level, costsSection.getLong(key));
                } catch (NumberFormatException ignored) {}
            }
        }

        ConfigurationSection descSection = section.getConfigurationSection("descriptions");
        if (descSection != null) {
            for (String key : descSection.getKeys(false)) {
                try {
                    int level = Integer.parseInt(key);
                    descs.put(level, descSection.getString(key, ""));
                } catch (NumberFormatException ignored) {}
            }
        }

        return new EnchantDef(id, display, maxLevel, icon, costs, descs, isVanilla);
    }

    /** Token cost to upgrade from currentLevel to currentLevel+1. Returns -1 if already maxed. */
    public long upgradeCost(int currentLevel) {
        if (currentLevel >= maxLevel) return -1;
        return tokenCosts.getOrDefault(currentLevel + 1, -1L);
    }

    /** Description for the given level. */
    public String description(int level) {
        return descriptions.getOrDefault(level, "Level " + level);
    }

    /** Build the lore lines for the GUI item at the player's current level. */
    public List<String> buildLore(int currentLevel) {
        List<String> lore = new ArrayList<>();
        lore.add("<gray>Level: <white>" + currentLevel + "<gray>/" + maxLevel);
        if (currentLevel > 0) {
            lore.add("<gray>Current: <green>" + description(currentLevel));
        }
        if (currentLevel < maxLevel) {
            long cost = upgradeCost(currentLevel);
            lore.add("<gray>Next: <yellow>" + description(currentLevel + 1));
            lore.add("<gray>Cost: <gold>" + cost + " tokens");
            lore.add("");
            lore.add("<yellow>Click to upgrade");
        } else {
            lore.add("");
            lore.add("<green>MAXED OUT");
        }
        return lore;
    }
}
