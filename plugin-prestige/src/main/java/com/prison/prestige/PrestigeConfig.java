package com.prison.prestige;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * PrestigeConfig — holds all values loaded from config.yml.
 */
public class PrestigeConfig {

    private final double tokenMultiplierPerPrestige;
    private final String broadcastMessage;
    private final String prefixFormat;
    private final Map<Integer, List<String>> rewardCommands; // prestige level → commands
    private final int maxPrestigePerms;

    // Ascension costs — keyed by target prestige level (1-based).
    // If a level isn't in the map, default values are used.
    private final long defaultCoinCost;
    private final long defaultTokenCost;
    private final Map<Integer, long[]> perLevelCosts; // level → [coinCost, tokenCost]

    public PrestigeConfig(double tokenMultiplierPerPrestige,
                          String broadcastMessage,
                          String prefixFormat,
                          Map<Integer, List<String>> rewardCommands,
                          int maxPrestigePerms,
                          long defaultCoinCost,
                          long defaultTokenCost,
                          Map<Integer, long[]> perLevelCosts) {
        this.tokenMultiplierPerPrestige = tokenMultiplierPerPrestige;
        this.broadcastMessage           = broadcastMessage;
        this.prefixFormat               = prefixFormat;
        this.rewardCommands             = Collections.unmodifiableMap(rewardCommands);
        this.maxPrestigePerms           = maxPrestigePerms;
        this.defaultCoinCost            = defaultCoinCost;
        this.defaultTokenCost           = defaultTokenCost;
        this.perLevelCosts              = Collections.unmodifiableMap(perLevelCosts);
    }

    /** Token earn bonus added per prestige level (e.g. 0.02 = +2%). */
    public double getTokenMultiplierPerPrestige() { return tokenMultiplierPerPrestige; }

    /** Total additive token bonus for a given prestige level. */
    public double getTotalTokenBonus(int prestigeLevel) {
        return tokenMultiplierPerPrestige * prestigeLevel;
    }

    public String getBroadcastMessage() { return broadcastMessage; }
    public String getPrefixFormat()     { return prefixFormat; }
    public int    getMaxPrestigePerms() { return maxPrestigePerms; }

    /**
     * Returns the Coin (IGC) cost to reach a given prestige level.
     * @param targetLevel the new prestige level (1-based, i.e. what the player will become)
     */
    public long getCoinCost(int targetLevel) {
        long[] costs = perLevelCosts.get(targetLevel);
        return costs != null ? costs[0] : defaultCoinCost;
    }

    /**
     * Returns the Relic (Token) cost to reach a given prestige level.
     * @param targetLevel the new prestige level (1-based)
     */
    public long getTokenCost(int targetLevel) {
        long[] costs = perLevelCosts.get(targetLevel);
        return costs != null ? costs[1] : defaultTokenCost;
    }

    /** Console commands to run when a player reaches this prestige level (empty list = none). */
    public List<String> getRewardCommands(int prestigeLevel) {
        return rewardCommands.getOrDefault(prestigeLevel, List.of());
    }

    /** Format the prestige prefix for a given level. */
    public String formatPrefix(int level) {
        return prefixFormat.replace("{level}", String.valueOf(level));
    }
}
