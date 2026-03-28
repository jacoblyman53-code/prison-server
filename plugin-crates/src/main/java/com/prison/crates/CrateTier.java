package com.prison.crates;

import org.bukkit.Material;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * CrateTier — configuration for one tier of crate (Common, Rare, Legendary…).
 *
 * @param id          Internal ID used in config keys and PDC tags (e.g. "common").
 * @param displayName Human-readable name (e.g. "Common Crate").
 * @param keyMaterial The material used for physical key items.
 * @param keyColor    MiniMessage color prefix for the key's display name.
 * @param rewards     The weighted reward pool.
 */
public record CrateTier(
        String id,
        String displayName,
        Material keyMaterial,
        String keyColor,
        List<CrateReward> rewards
) {
    /**
     * Weighted random roll from the reward pool.
     * Returns null if the pool is empty.
     */
    public CrateReward rollReward() {
        if (rewards.isEmpty()) return null;

        int totalWeight = rewards.stream().mapToInt(CrateReward::weight).sum();
        if (totalWeight <= 0) return rewards.get(0);

        int roll = ThreadLocalRandom.current().nextInt(totalWeight);
        int cumulative = 0;
        for (CrateReward r : rewards) {
            cumulative += r.weight();
            if (roll < cumulative) return r;
        }
        return rewards.get(rewards.size() - 1);
    }

    /** True if the pool has at least one reward defined. */
    public boolean hasRewards() {
        return !rewards.isEmpty();
    }
}
