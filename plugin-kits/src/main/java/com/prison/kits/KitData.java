package com.prison.kits;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * KitData — immutable definition of a single kit loaded from config.yml.
 */
public record KitData(
    String id,
    String display,          // MiniMessage formatted display name
    KitType type,
    String requiredRank,     // mine rank (RANK type only); null = no requirement
    String requiredDonorRank,// donor rank key (DONOR type only); null = no requirement
    long cooldownMs,         // 0 = one-time only; for DONOR type ignored — uses restart logic
    long minDonorCooldownMs, // minimum time since last claim for DONOR kits even after restart
    List<KitItem> contents
) {

    public enum KitType { FREE, RANK, DONOR }

    /** Build from a config section. Returns null if the section is malformed. */
    public static KitData fromConfig(String id, ConfigurationSection section, Logger logger) {
        String display = section.getString("display", id);

        KitType type;
        try {
            type = KitType.valueOf(section.getString("type", "FREE").toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.warning("[Kits] Unknown kit type for '" + id + "' — defaulting to FREE.");
            type = KitType.FREE;
        }

        String requiredRank      = type == KitType.RANK  ? section.getString("required-rank")       : null;
        String requiredDonorRank = type == KitType.DONOR ? section.getString("required-donor-rank")  : null;

        long cooldownMs = section.getInt("cooldown-hours", 24) * 3600_000L;
        long minDonorCooldownMs = section.getInt("min-cooldown-hours", 1) * 3600_000L;

        List<KitItem> contents = new ArrayList<>();
        List<? extends ConfigurationSection> itemList =
            (List<? extends ConfigurationSection>) (List<?>) section.getMapList("contents");
        // getMapList returns List<Map<?,?>> — use getConfigurationSection path approach instead:
        ConfigurationSection contentSection = section.getConfigurationSection("contents");

        // config.yml stores contents as a YAML list of maps — use getList then iterate
        List<?> rawContents = section.getList("contents");
        if (rawContents != null) {
            for (Object obj : rawContents) {
                if (obj instanceof java.util.Map<?, ?> map) {
                    org.bukkit.configuration.MemoryConfiguration ms = new org.bukkit.configuration.MemoryConfiguration();
                    for (Map.Entry<?, ?> e : map.entrySet()) {
                        ms.set(String.valueOf(e.getKey()), e.getValue());
                    }
                    KitItem item = KitItem.fromConfig(ms, logger);
                    if (item != null) contents.add(item);
                }
            }
        }

        if (contents.isEmpty()) {
            logger.warning("[Kits] Kit '" + id + "' has no valid contents.");
        }

        return new KitData(id, display, type, requiredRank, requiredDonorRank,
                           cooldownMs, minDonorCooldownMs, Collections.unmodifiableList(contents));
    }
}
