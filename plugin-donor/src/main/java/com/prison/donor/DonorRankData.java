package com.prison.donor;

import java.util.List;

/**
 * Holds the config data for a single donor rank tier.
 */
public record DonorRankData(
    String key,           // e.g. "donor", "donorplus", "elite", "eliteplus"
    String display,       // e.g. "Elite+"
    String prefix,        // MiniMessage format string for chat/tablist
    double tokenMultiplier,
    List<String> perks    // Human-readable perk descriptions for /donorrank
) {}
