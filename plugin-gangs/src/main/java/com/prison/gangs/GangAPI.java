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

    GangAPI(GangManager manager) {
        this.manager = manager;
        instance = this;
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
}
