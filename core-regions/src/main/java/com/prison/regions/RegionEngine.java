package com.prison.regions;

import com.prison.database.DatabaseManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.lang.reflect.Type;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * RegionEngine — in-memory region cache with chunk-based spatial index.
 *
 * How the spatial index works:
 *   Each region spans multiple 16x16 chunk columns. When a region is loaded,
 *   we calculate every chunk it overlaps and add the region's name to a bucket
 *   for that chunk. When checking a location, we look up that chunk's bucket
 *   (one map lookup) to get a small candidate list, then do exact bounds checks.
 *   This keeps movement checks fast even with hundreds of regions.
 *
 * ChunkKey format: "worldName:chunkX:chunkZ"
 */
public class RegionEngine {

    private static RegionEngine instance;
    private final Logger logger;
    private final Gson gson = new Gson();

    // All regions by name
    private final ConcurrentHashMap<String, Region> regions = new ConcurrentHashMap<>();

    // Spatial index: chunk key → region names overlapping that chunk
    private final ConcurrentHashMap<String, Set<String>> chunkIndex = new ConcurrentHashMap<>();

    private RegionEngine(Logger logger) {
        this.logger = logger;
    }

    public static RegionEngine getInstance() { return instance; }

    public static RegionEngine initialize(Logger logger) {
        if (instance != null) throw new IllegalStateException("RegionEngine already initialized");
        instance = new RegionEngine(logger);
        return instance;
    }

    // ----------------------------------------------------------------
    // Load / Reload
    // ----------------------------------------------------------------

    /**
     * Load all regions from the database into memory.
     * Called on startup and by /rg reload.
     */
    public void loadFromDatabase() throws SQLException {
        regions.clear();
        chunkIndex.clear();

        Type mapType = new TypeToken<Map<String, String>>(){}.getType();

        List<Region> loaded = DatabaseManager.getInstance().query(
            "SELECT id, name, world, x1, y1, z1, x2, y2, z2, priority, flags, entry_message, exit_message FROM regions",
            rs -> {
                List<Region> list = new ArrayList<>();
                while (rs.next()) {
                    String flagsJson = rs.getString("flags");
                    Map<String, String> flags = (flagsJson != null)
                        ? gson.fromJson(flagsJson, mapType)
                        : new HashMap<>();

                    list.add(new Region(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("world"),
                        rs.getInt("x1"), rs.getInt("y1"), rs.getInt("z1"),
                        rs.getInt("x2"), rs.getInt("y2"), rs.getInt("z2"),
                        rs.getInt("priority"),
                        flags,
                        rs.getString("entry_message"),
                        rs.getString("exit_message")
                    ));
                }
                return list;
            }
        );

        for (Region r : loaded) {
            indexRegion(r);
        }
        logger.info("[Regions] Loaded " + regions.size() + " regions into memory.");
    }

    // ----------------------------------------------------------------
    // Public API — used by other plugins
    // ----------------------------------------------------------------

    /**
     * Get all regions that contain the given location, sorted highest priority first.
     */
    public List<Region> getRegionsAt(Location loc) {
        String chunkKey = chunkKey(loc.getWorld().getName(),
                                   loc.getBlockX() >> 4,
                                   loc.getBlockZ() >> 4);
        Set<String> candidates = chunkIndex.get(chunkKey);
        if (candidates == null || candidates.isEmpty()) return Collections.emptyList();

        List<Region> result = new ArrayList<>();
        for (String name : candidates) {
            Region r = regions.get(name);
            if (r != null && r.contains(loc)) {
                result.add(r);
            }
        }
        result.sort(Comparator.comparingInt(Region::getPriority).reversed());
        return result;
    }

    /**
     * Get the effective flag value at a location (highest-priority region wins).
     * Returns null if no region at this location has the flag set.
     */
    public String getFlagAt(Location loc, String flag) {
        for (Region r : getRegionsAt(loc)) {
            String val = r.getFlag(flag);
            if (val != null) return val;
        }
        return null;
    }

    /**
     * Returns true if the given flag is denied at this location.
     * Used for protection checks: isBlocked(loc, FLAG_BUILD).
     */
    public boolean isDenied(Location loc, String flag) {
        return Region.DENY.equals(getFlagAt(loc, flag));
    }

    /**
     * Returns true if a player is allowed to enter the location.
     * Checks the "entry" flag — deny blocks all players without bypass permission.
     */
    public boolean canEnter(Location loc, Player player) {
        if (player.hasPermission("prison.admin.*") || player.hasPermission("*")) return true;
        return !isDenied(loc, Region.FLAG_ENTRY);
    }

    /**
     * Get a region by name (returns null if not found).
     */
    public Region getRegion(String name) {
        return regions.get(name.toLowerCase());
    }

    /**
     * Get all region names.
     */
    public Set<String> getRegionNames() {
        return Collections.unmodifiableSet(regions.keySet());
    }

    /**
     * Get all regions (unmodifiable).
     */
    public Collection<Region> getAllRegions() {
        return Collections.unmodifiableCollection(regions.values());
    }

    // ----------------------------------------------------------------
    // Mutation — called by admin GUI after DB write
    // ----------------------------------------------------------------

    /**
     * Add or replace a region in memory. Call after saving to database.
     */
    public void putRegion(Region region) {
        // Remove old index entries if replacing
        Region old = regions.get(region.getName().toLowerCase());
        if (old != null) removeFromIndex(old);

        indexRegion(region);
        logger.info("[Regions] Region '" + region.getName() + "' loaded into cache.");
    }

    /**
     * Remove a region from memory. Call after deleting from database.
     */
    public void removeRegion(String name) {
        Region r = regions.remove(name.toLowerCase());
        if (r != null) {
            removeFromIndex(r);
            logger.info("[Regions] Region '" + name + "' removed from cache.");
        }
    }

    // ----------------------------------------------------------------
    // Internal — spatial index management
    // ----------------------------------------------------------------

    private void indexRegion(Region r) {
        regions.put(r.getName().toLowerCase(), r);

        // Register this region in every chunk column it overlaps
        int chunkX1 = r.getX1() >> 4;
        int chunkZ1 = r.getZ1() >> 4;
        int chunkX2 = r.getX2() >> 4;
        int chunkZ2 = r.getZ2() >> 4;

        for (int cx = chunkX1; cx <= chunkX2; cx++) {
            for (int cz = chunkZ1; cz <= chunkZ2; cz++) {
                String key = chunkKey(r.getWorld(), cx, cz);
                chunkIndex.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet())
                          .add(r.getName().toLowerCase());
            }
        }
    }

    private void removeFromIndex(Region r) {
        int chunkX1 = r.getX1() >> 4;
        int chunkZ1 = r.getZ1() >> 4;
        int chunkX2 = r.getX2() >> 4;
        int chunkZ2 = r.getZ2() >> 4;

        for (int cx = chunkX1; cx <= chunkX2; cx++) {
            for (int cz = chunkZ1; cz <= chunkZ2; cz++) {
                String key = chunkKey(r.getWorld(), cx, cz);
                Set<String> bucket = chunkIndex.get(key);
                if (bucket != null) {
                    bucket.remove(r.getName().toLowerCase());
                    if (bucket.isEmpty()) chunkIndex.remove(key);
                }
            }
        }
    }

    private static String chunkKey(String world, int cx, int cz) {
        return world + ":" + cx + ":" + cz;
    }
}
