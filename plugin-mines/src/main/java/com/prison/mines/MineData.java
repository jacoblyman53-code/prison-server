package com.prison.mines;

import org.bukkit.Material;

import java.util.Map;

/**
 * MineData — immutable snapshot of a mine's configuration.
 * Reloaded from config.yml when /mine reload is called.
 */
public record MineData(
    String id,
    String display,           // MiniMessage
    String world,
    int x1, int y1, int z1,  // normalised so x1 ≤ x2 etc.
    int x2, int y2, int z2,
    double spawnX, double spawnY, double spawnZ,
    float  spawnYaw,  float spawnPitch,
    Map<Material, Double> composition, // material → weight (sum to 100)
    Map<Material, Long>   sellPrices,  // per-mine overrides (empty = use global)
    int    resetTimerMins,             // 0 = timer disabled
    double resetThreshold,             // 0.0 = threshold disabled, 0.8 = 80%
    String permissionNode,
    String mineType,                   // STANDARD | DONOR | PRESTIGE
    int    prestigeRequired,
    int    donorSessionMins,           // DONOR type only: session length in minutes (0 = unlimited)
    boolean voteCrate,                 // true = this mine awards a vote crate on /sell
    long   targetAvgInventory          // spec target avg inventory value ($); 0 = compute dynamically
) {
    /** True when the mine has non-zero volume (corners have been set). */
    public boolean isConfigured() {
        return !(x1 == 0 && y1 == 64 && z1 == 0 && x2 == 0 && y2 == 64 && z2 == 0);
    }

    public boolean contains(int x, int y, int z) {
        return x >= x1 && x <= x2
            && y >= y1 && y <= y2
            && z >= z1 && z <= z2;
    }

    public int totalBlocks() {
        return (x2 - x1 + 1) * (y2 - y1 + 1) * (z2 - z1 + 1);
    }
}
