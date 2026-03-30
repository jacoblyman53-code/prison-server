package com.prison.gangs;

import java.util.UUID;

/**
 * GangAPI — public facade for other plugins to query gang data.
 *
 * Usage:
 *   GangAPI api = GangAPI.getInstance(); // null until GangPlugin enables
 *   String tag = api.getGangTag(uuid);   // null if not in a gang
 */
public class GangAPI {

    private static GangAPI instance;

    private final GangManager manager;

    // Per-level sell and token bonus multipliers (index = level - 1)
    private double[] sellBonuses  = new double[]{1.0};
    private double[] tokenBonuses = new double[]{1.0};

    GangAPI(GangManager manager) {
        this.manager = manager;
        instance = this;
    }

    void setBonuses(double[] sellBonuses, double[] tokenBonuses) {
        this.sellBonuses  = sellBonuses;
        this.tokenBonuses = tokenBonuses;
    }

    public static GangAPI getInstance() { return instance; }

    /** Returns the gang name of a player, or null if not in a gang. */
    public String getGangName(UUID uuid) {
        GangData gang = manager.getGangOf(uuid);
        return gang == null ? null : gang.name();
    }

    /** Returns the gang tag (e.g. "PVP") of a player, or null if not in a gang. */
    public String getGangTag(UUID uuid) {
        GangData gang = manager.getGangOf(uuid);
        return gang == null ? null : gang.tag();
    }

    /** Returns the player's GangRole, or null if not in a gang. */
    public GangRole getGangRole(UUID uuid) {
        return manager.getMemberRole(uuid);
    }

    /** Returns the gang level of a player's gang, or -1 if not in a gang. */
    public int getGangLevel(UUID uuid) {
        GangData gang = manager.getGangOf(uuid);
        return gang == null ? -1 : gang.level();
    }

    /**
     * Returns the IGC sell multiplier bonus for this player based on their gang level.
     * Returns 1.0 if not in a gang or no bonus configured for their level.
     */
    public double getSellBonus(UUID uuid) {
        int level = getGangLevel(uuid);
        if (level <= 0 || sellBonuses == null) return 1.0;
        int idx = Math.min(level - 1, sellBonuses.length - 1);
        return sellBonuses[idx];
    }

    /**
     * Returns the token earn multiplier bonus for this player based on their gang level.
     * Returns 1.0 if not in a gang or no bonus configured for their level.
     */
    public double getTokenBonus(UUID uuid) {
        int level = getGangLevel(uuid);
        if (level <= 0 || tokenBonuses == null) return 1.0;
        int idx = Math.min(level - 1, tokenBonuses.length - 1);
        return tokenBonuses[idx];
    }
}
