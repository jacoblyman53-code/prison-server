package com.prison.donor;

import com.prison.permissions.PermissionEngine;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * DonorAPI — public interface for other plugins to query donor rank data.
 *
 * Other plugins (mines, kits, chat, tokens) call this instead of
 * reading the permission engine directly, so the donor concept stays
 * in one place.
 *
 * Usage:
 *   DonorAPI api = DonorAPI.getInstance();
 *   double mult = api.getTokenMultiplier(uuid);   // 1.0 if no donor rank
 *   String rank = api.getDonorRank(uuid);          // null if no donor rank
 */
public class DonorAPI {

    private static DonorAPI instance;

    private final Map<String, DonorRankData> rankData; // key → data

    DonorAPI(Map<String, DonorRankData> rankData) {
        this.rankData = rankData;
        instance = this;
    }

    public static DonorAPI getInstance() { return instance; }

    // ----------------------------------------------------------------
    // Rank queries
    // ----------------------------------------------------------------

    /**
     * Get a player's donor rank key (e.g. "elite"), or null if they have none.
     */
    public String getDonorRank(UUID uuid) {
        return PermissionEngine.getInstance().getDonorRank(uuid);
    }

    /**
     * Get the full DonorRankData for a player, or null if they have no donor rank.
     */
    public DonorRankData getDonorRankData(UUID uuid) {
        String rank = getDonorRank(uuid);
        return rank != null ? rankData.get(rank.toLowerCase()) : null;
    }

    /**
     * Get a player's token earn multiplier from their donor rank.
     * Returns 1.0 if the player has no donor rank.
     */
    public double getTokenMultiplier(UUID uuid) {
        DonorRankData data = getDonorRankData(uuid);
        return data != null ? data.tokenMultiplier() : 1.0;
    }

    /**
     * Returns true if the player has any donor rank.
     */
    public boolean isDonor(UUID uuid) {
        return getDonorRank(uuid) != null;
    }

    /**
     * Set a player's donor rank and rebuild their permission cache.
     * Called by the Tebex delivery plugin when a purchase is processed.
     *
     * @param rank the rank key ("donor", "donorplus", "elite", "eliteplus"),
     *             or null to remove the donor rank.
     */
    public CompletableFuture<Void> setDonorRank(UUID uuid, String rank) {
        return PermissionEngine.getInstance().setDonorRank(uuid, rank);
    }

    /**
     * Get rank data by key (for admin tools and display).
     */
    public DonorRankData getRankData(String key) {
        return key != null ? rankData.get(key.toLowerCase()) : null;
    }

    /**
     * All configured donor ranks, in order of tier.
     */
    public Map<String, DonorRankData> getAllRanks() {
        return rankData;
    }
}
