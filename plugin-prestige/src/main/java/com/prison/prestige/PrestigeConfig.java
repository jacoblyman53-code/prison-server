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

    public PrestigeConfig(double tokenMultiplierPerPrestige,
                          String broadcastMessage,
                          String prefixFormat,
                          Map<Integer, List<String>> rewardCommands,
                          int maxPrestigePerms) {
        this.tokenMultiplierPerPrestige = tokenMultiplierPerPrestige;
        this.broadcastMessage           = broadcastMessage;
        this.prefixFormat               = prefixFormat;
        this.rewardCommands             = Collections.unmodifiableMap(rewardCommands);
        this.maxPrestigePerms           = maxPrestigePerms;
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

    /** Console commands to run when a player reaches this prestige level (empty list = none). */
    public List<String> getRewardCommands(int prestigeLevel) {
        return rewardCommands.getOrDefault(prestigeLevel, List.of());
    }

    /** Format the prestige prefix for a given level. */
    public String formatPrefix(int level) {
        return prefixFormat.replace("{level}", String.valueOf(level));
    }
}
