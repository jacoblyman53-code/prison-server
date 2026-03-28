package com.prison.warps;

import com.prison.database.DatabaseManager;
import com.prison.permissions.PermissionEngine;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * WarpAPI — public interface for the warp system.
 *
 * Warps are stored in the warps table (created by core-database) and
 * cached in memory. Admin commands create/delete warps via this API.
 *
 * Usage:
 *   WarpAPI api = WarpAPI.getInstance();
 *   WarpData w = api.getWarp("spawn");
 *   api.canUseWarp(player, w);
 */
public class WarpAPI {

    private static WarpAPI instance;

    private final Logger logger;
    private final ConcurrentHashMap<String, WarpData> cache = new ConcurrentHashMap<>();

    WarpAPI(Logger logger) {
        this.logger = logger;
        instance = this;
    }

    public static WarpAPI getInstance() { return instance; }

    // ----------------------------------------------------------------
    // Loading
    // ----------------------------------------------------------------

    public CompletableFuture<Void> loadFromDatabase() {
        return CompletableFuture.runAsync(() -> {
            try {
                List<WarpData> warps = DatabaseManager.getInstance().query(
                    "SELECT id, name, world, x, y, z, yaw, pitch, permission_node, created_by_uuid FROM warps",
                    rs -> {
                        List<WarpData> list = new ArrayList<>();
                        while (rs.next()) {
                            list.add(new WarpData(
                                rs.getLong("id"),
                                rs.getString("name"),
                                rs.getString("world"),
                                rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"),
                                rs.getFloat("yaw"), rs.getFloat("pitch"),
                                rs.getString("permission_node"),
                                rs.getString("created_by_uuid")
                            ));
                        }
                        return list;
                    }
                );
                cache.clear();
                warps.forEach(w -> cache.put(w.name().toLowerCase(), w));
                logger.info("[Warps] Loaded " + cache.size() + " warps.");
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "[Warps] Failed to load warps from database", e);
            }
        });
    }

    // ----------------------------------------------------------------
    // Queries
    // ----------------------------------------------------------------

    public WarpData getWarp(String name) {
        return cache.get(name.toLowerCase());
    }

    public Collection<WarpData> getAllWarps() {
        return Collections.unmodifiableCollection(cache.values());
    }

    /** Returns all warps this player has permission to use. */
    public List<WarpData> getAccessibleWarps(Player player) {
        List<WarpData> accessible = new ArrayList<>();
        for (WarpData warp : cache.values()) {
            if (canUseWarp(player, warp)) accessible.add(warp);
        }
        accessible.sort(Comparator.comparing(WarpData::name));
        return accessible;
    }

    /** Returns true if the player may use this warp (no permission node, or they have it). */
    public boolean canUseWarp(Player player, WarpData warp) {
        if (warp.permissionNode() == null || warp.permissionNode().isBlank()) return true;
        return PermissionEngine.getInstance().hasPermission(player, warp.permissionNode())
            || PermissionEngine.getInstance().hasPermission(player, "prison.admin.*");
    }

    // ----------------------------------------------------------------
    // Mutations
    // ----------------------------------------------------------------

    public CompletableFuture<Boolean> createWarp(String name, String world,
                                                  double x, double y, double z,
                                                  float yaw, float pitch,
                                                  String permNode, String createdByUuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                DatabaseManager.getInstance().execute(
                    "INSERT INTO warps (name, world, x, y, z, yaw, pitch, permission_node, created_by_uuid) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    name, world, x, y, z, yaw, pitch, permNode, createdByUuid
                );
                // Reload from DB to get the auto-generated id
                loadFromDatabase().join();
                return true;
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "[Warps] Failed to create warp '" + name + "'", e);
                return false;
            }
        });
    }

    public CompletableFuture<Boolean> deleteWarp(String name) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                DatabaseManager.getInstance().execute(
                    "DELETE FROM warps WHERE name = ?", name
                );
                cache.remove(name.toLowerCase());
                return true;
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "[Warps] Failed to delete warp '" + name + "'", e);
                return false;
            }
        });
    }

    public CompletableFuture<Boolean> setPermission(String name, String permNode) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                DatabaseManager.getInstance().execute(
                    "UPDATE warps SET permission_node = ? WHERE name = ?",
                    permNode, name
                );
                loadFromDatabase().join();
                return true;
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "[Warps] Failed to set permission for warp '" + name + "'", e);
                return false;
            }
        });
    }
}
