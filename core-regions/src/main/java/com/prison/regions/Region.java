package com.prison.regions;

import org.bukkit.Location;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a cuboid region in the world with protection flags.
 *
 * Regions are axis-aligned cuboids (rectangular boxes) defined by two corners.
 * Flags control what is allowed inside the region (pvp, building, mob spawning, etc.).
 * When two regions overlap, the one with the higher priority value wins on a flag conflict.
 */
public class Region {

    // Supported flag names
    public static final String FLAG_PVP          = "pvp";
    public static final String FLAG_BUILD        = "build";
    public static final String FLAG_ENTRY        = "entry";
    public static final String FLAG_MOB_SPAWNING = "mob-spawning";

    // Flag values
    public static final String ALLOW = "allow";
    public static final String DENY  = "deny";

    private final long   id;
    private final String name;
    private final String world;
    private final int    x1, y1, z1;
    private final int    x2, y2, z2;
    private final int    priority;
    private final Map<String, String> flags;
    private final String entryMessage;
    private final String exitMessage;

    public Region(long id, String name, String world,
                  int x1, int y1, int z1,
                  int x2, int y2, int z2,
                  int priority, Map<String, String> flags,
                  String entryMessage, String exitMessage) {
        this.id           = id;
        this.name         = name;
        this.world        = world;
        // Normalize so min corner is always (x1,y1,z1)
        this.x1           = Math.min(x1, x2);
        this.y1           = Math.min(y1, y2);
        this.z1           = Math.min(z1, z2);
        this.x2           = Math.max(x1, x2);
        this.y2           = Math.max(y1, y2);
        this.z2           = Math.max(z1, z2);
        this.priority     = priority;
        this.flags        = flags != null ? new HashMap<>(flags) : new HashMap<>();
        this.entryMessage = entryMessage;
        this.exitMessage  = exitMessage;
    }

    /**
     * Returns true if the given location is inside this region.
     * The check is inclusive of all borders.
     */
    public boolean contains(Location loc) {
        if (!loc.getWorld().getName().equals(world)) return false;
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        return x >= x1 && x <= x2
            && y >= y1 && y <= y2
            && z >= z1 && z <= z2;
    }

    /**
     * Get the value of a flag ("allow", "deny", or null if not set).
     */
    public String getFlag(String flag) {
        return flags.get(flag);
    }

    /**
     * Returns true if the flag is explicitly set to "allow".
     */
    public boolean allows(String flag) {
        return ALLOW.equals(flags.get(flag));
    }

    /**
     * Returns true if the flag is explicitly set to "deny".
     */
    public boolean denies(String flag) {
        return DENY.equals(flags.get(flag));
    }

    // ---- Getters ----

    public long   getId()           { return id; }
    public String getName()         { return name; }
    public String getWorld()        { return world; }
    public int    getX1()           { return x1; }
    public int    getY1()           { return y1; }
    public int    getZ1()           { return z1; }
    public int    getX2()           { return x2; }
    public int    getY2()           { return y2; }
    public int    getZ2()           { return z2; }
    public int    getPriority()     { return priority; }
    public Map<String, String> getFlags() { return Collections.unmodifiableMap(flags); }
    public String getEntryMessage() { return entryMessage; }
    public String getExitMessage()  { return exitMessage; }
}
