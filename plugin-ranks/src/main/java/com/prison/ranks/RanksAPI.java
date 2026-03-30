package com.prison.ranks;

import com.prison.permissions.PermissionEngine;

import java.util.UUID;

/**
 * RanksAPI — thin static facade for cross-plugin rank progression queries.
 *
 * Initialized by RankPlugin after the RankManager singleton is ready.
 * Other plugins depend on plugin-ranks compileOnly and call:
 *
 *   RanksAPI api = RanksAPI.getInstance();
 *   if (api != null) {
 *       long cost = api.getNextRankCost(uuid);  // -1 = max rank
 *       String next = api.getNextRankName(uuid); // null = max rank
 *   }
 */
public class RanksAPI {

    private static RanksAPI instance;

    private final RankManager manager;
    private final RankConfig  config;

    private RanksAPI(RankManager manager, RankConfig config) {
        this.manager = manager;
        this.config  = config;
    }

    public static void initialize(RankManager manager, RankConfig config) {
        instance = new RanksAPI(manager, config);
    }

    public static void reset() {
        instance = null;
    }

    public static RanksAPI getInstance() {
        return instance;
    }

    // ----------------------------------------------------------------
    // Queries
    // ----------------------------------------------------------------

    /**
     * Returns the IGC cost to reach the next rank for this player,
     * or -1 if they are already at the maximum rank (Z).
     */
    public long getNextRankCost(UUID uuid) {
        String current = PermissionEngine.getInstance().getMineRank(uuid);
        String next    = config.nextRank(current);
        if (next == null) return -1L;
        RankConfig.RankData data = config.getRank(next);
        return data != null ? data.cost() : -1L;
    }

    /**
     * Returns the letter of the next rank for this player,
     * or null if they are already at the maximum rank (Z).
     */
    public String getNextRankName(UUID uuid) {
        String current = PermissionEngine.getInstance().getMineRank(uuid);
        return config.nextRank(current);
    }

    /**
     * Returns the display name (MiniMessage string) of the next rank,
     * or null if at max rank.
     */
    public String getNextRankDisplay(UUID uuid) {
        String next = getNextRankName(uuid);
        if (next == null) return null;
        RankConfig.RankData data = config.getRank(next);
        return data != null ? data.display() : next;
    }
}
