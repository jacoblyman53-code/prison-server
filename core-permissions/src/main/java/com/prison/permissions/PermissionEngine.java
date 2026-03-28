package com.prison.permissions;

import com.prison.database.DatabaseManager;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * PermissionEngine — the core permission system for the prison server.
 *
 * How it works (simple version):
 * - Every player has three completely separate rank types: mine rank, donor rank, staff rank
 * - When a player joins, we load all their permissions into memory (the "cache")
 * - Every permission check reads from memory only — never touches the database
 * - When a rank changes, we rebuild that player's cache immediately
 *
 * This means permission checks are instant (under 0.01ms) because they
 * never wait for a database response.
 */
public class PermissionEngine {

    private static PermissionEngine instance;
    private final Logger logger;

    // The permission cache — one entry per online player
    // ConcurrentHashMap is used because multiple threads read this simultaneously
    private final ConcurrentHashMap<UUID, PlayerPermissions> cache = new ConcurrentHashMap<>();

    // Group definitions — loaded from config, defines what each rank grants
    private final Map<String, Set<String>> groupPermissions = new HashMap<>();

    private PermissionEngine(Logger logger) {
        this.logger = logger;
    }

    public static PermissionEngine getInstance() {
        return instance;
    }

    public static PermissionEngine initialize(Logger logger) {
        if (instance != null) {
            throw new IllegalStateException("PermissionEngine already initialized");
        }
        instance = new PermissionEngine(logger);
        return instance;
    }

    // ----------------------------------------------------------------
    // Group Registration
    // Called on startup to define what permissions each rank grants
    // ----------------------------------------------------------------

    /**
     * Register a group and the permissions it grants.
     * Called during plugin startup for every rank in config.yml.
     *
     * Example: registerGroup("prison.rank.a", Set.of("prison.mine.a", "prison.sell"))
     */
    public void registerGroup(String groupNode, Set<String> permissions) {
        groupPermissions.put(groupNode, new HashSet<>(permissions));
    }

    // ----------------------------------------------------------------
    // Cache Management
    // ----------------------------------------------------------------

    /**
     * Load a player's permissions into the cache.
     * Called async when a player joins the server.
     * Reads their ranks from the database and resolves all permissions.
     */
    public CompletableFuture<Void> loadPlayer(UUID uuid) {
        return CompletableFuture.runAsync(() -> {
            try {
                PlayerRankData ranks = loadRanksFromDatabase(uuid);
                PlayerPermissions perms = resolvePermissions(ranks);
                cache.put(uuid, perms);
                logger.fine("[Permissions] Loaded cache for " + uuid);
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "[Permissions] Failed to load permissions for " + uuid, e);
                // Give the player empty permissions rather than crashing
                cache.put(uuid, new PlayerPermissions(new PlayerRankData(), new HashSet<>()));
            }
        });
    }

    /**
     * Remove a player's permissions from the cache when they disconnect.
     * Called when a player leaves the server.
     */
    public void unloadPlayer(UUID uuid) {
        cache.remove(uuid);
        logger.fine("[Permissions] Unloaded cache for " + uuid);
    }

    /**
     * Rebuild a player's permission cache after a rank change.
     * Called async whenever a mine rank, donor rank, or staff rank changes.
     * The player keeps their old permissions until the rebuild is complete.
     */
    public CompletableFuture<Void> invalidateCache(UUID uuid) {
        return loadPlayer(uuid);
    }

    // ----------------------------------------------------------------
    // Permission Checks — these are called constantly, must be instant
    // ----------------------------------------------------------------

    /**
     * Check if a player has a specific permission node.
     * Always reads from cache — never touches the database.
     * Returns false if the player is not online or cache isn't loaded yet.
     */
    public boolean hasPermission(UUID uuid, String permission) {
        PlayerPermissions perms = cache.get(uuid);
        if (perms == null) return false;
        return perms.permissions().contains(permission) || perms.permissions().contains("*");
    }

    /**
     * Convenience method that takes a Player object instead of UUID.
     */
    public boolean hasPermission(Player player, String permission) {
        return hasPermission(player.getUniqueId(), permission);
    }

    /**
     * Get a player's current mine rank (A-Z).
     */
    public String getMineRank(UUID uuid) {
        PlayerPermissions perms = cache.get(uuid);
        return perms != null ? perms.ranks().mineRank() : "A";
    }

    /**
     * Get a player's current donor rank, or null if they have none.
     */
    public String getDonorRank(UUID uuid) {
        PlayerPermissions perms = cache.get(uuid);
        return perms != null ? perms.ranks().donorRank() : null;
    }

    /**
     * Get a player's current staff rank, or null if they are not staff.
     */
    public String getStaffRank(UUID uuid) {
        PlayerPermissions perms = cache.get(uuid);
        return perms != null ? perms.ranks().staffRank() : null;
    }

    /**
     * Get a player's prestige level.
     */
    public int getPrestige(UUID uuid) {
        PlayerPermissions perms = cache.get(uuid);
        return perms != null ? perms.ranks().prestige() : 0;
    }

    /**
     * Check if a player is staff (any staff rank).
     */
    public boolean isStaff(UUID uuid) {
        return getStaffRank(uuid) != null;
    }

    // ----------------------------------------------------------------
    // Rank Setters — update database then rebuild cache
    // ----------------------------------------------------------------

    /**
     * Set a player's mine rank and rebuild their permission cache.
     * This is called by the rank progression plugin on /rankup.
     */
    public CompletableFuture<Void> setMineRank(UUID uuid, String rank) {
        return CompletableFuture.runAsync(() -> {
            try {
                DatabaseManager.getInstance().execute(
                    "UPDATE players SET mine_rank = ? WHERE uuid = ?",
                    rank.toUpperCase(), uuid.toString()
                );
                invalidateCache(uuid).join();
                logger.info("[Permissions] Set mine rank for " + uuid + " to " + rank);
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "[Permissions] Failed to set mine rank for " + uuid, e);
            }
        });
    }

    /**
     * Set a player's donor rank and rebuild their permission cache.
     * Called by the Tebex delivery plugin when a purchase is processed.
     */
    public CompletableFuture<Void> setDonorRank(UUID uuid, String rank) {
        return CompletableFuture.runAsync(() -> {
            try {
                DatabaseManager.getInstance().execute(
                    "UPDATE players SET donor_rank = ? WHERE uuid = ?",
                    rank, uuid.toString()
                );
                invalidateCache(uuid).join();
                logger.info("[Permissions] Set donor rank for " + uuid + " to " + rank);
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "[Permissions] Failed to set donor rank for " + uuid, e);
            }
        });
    }

    /**
     * Set a player's staff rank and rebuild their permission cache.
     * Called by the staff management plugin.
     */
    public CompletableFuture<Void> setStaffRank(UUID uuid, String rank) {
        return CompletableFuture.runAsync(() -> {
            try {
                String sql = (rank == null)
                    ? "UPDATE players SET staff_rank = NULL WHERE uuid = ?"
                    : "UPDATE players SET staff_rank = ? WHERE uuid = ?";

                if (rank == null) {
                    DatabaseManager.getInstance().execute(sql, uuid.toString());
                } else {
                    DatabaseManager.getInstance().execute(sql, rank, uuid.toString());
                }

                invalidateCache(uuid).join();
                logger.info("[Permissions] Set staff rank for " + uuid + " to " + rank);
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "[Permissions] Failed to set staff rank for " + uuid, e);
            }
        });
    }

    /**
     * Set a player's prestige level and rebuild their permission cache.
     */
    public CompletableFuture<Void> setPrestige(UUID uuid, int prestige) {
        return CompletableFuture.runAsync(() -> {
            try {
                DatabaseManager.getInstance().execute(
                    "UPDATE players SET prestige = ? WHERE uuid = ?",
                    prestige, uuid.toString()
                );
                invalidateCache(uuid).join();
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "[Permissions] Failed to set prestige for " + uuid, e);
            }
        });
    }

    // ----------------------------------------------------------------
    // Internal — Database and Permission Resolution
    // ----------------------------------------------------------------

    /**
     * Load a player's rank data from the database.
     * This is the only place the permission engine reads from MySQL.
     */
    private PlayerRankData loadRanksFromDatabase(UUID uuid) throws SQLException {
        return DatabaseManager.getInstance().query(
            "SELECT mine_rank, donor_rank, staff_rank, prestige FROM players WHERE uuid = ?",
            rs -> {
                if (rs.next()) {
                    return new PlayerRankData(
                        rs.getString("mine_rank"),
                        rs.getString("donor_rank"),
                        rs.getString("staff_rank"),
                        rs.getInt("prestige")
                    );
                }
                // New player not yet in database — defaults
                return new PlayerRankData();
            },
            uuid.toString()
        );
    }

    /**
     * Resolve a player's full permission set from their rank data.
     *
     * This takes their mine rank, donor rank, staff rank, and prestige,
     * looks up what permissions each grants, and combines them into
     * one flat set. This computation happens once on login, not on
     * every permission check.
     */
    private PlayerPermissions resolvePermissions(PlayerRankData ranks) {
        Set<String> resolved = new HashSet<>();

        // Mine rank permissions
        String mineNode = "prison.rank." + ranks.mineRank().toLowerCase();
        if (groupPermissions.containsKey(mineNode)) {
            resolved.addAll(groupPermissions.get(mineNode));
        }
        // Always grant the mine access node for the current rank
        resolved.add("prison.mine." + ranks.mineRank().toLowerCase());
        resolved.add("prison.rank." + ranks.mineRank().toLowerCase());

        // Donor rank permissions (completely separate tree)
        if (ranks.donorRank() != null) {
            String donorNode = "prison.donor." + ranks.donorRank().toLowerCase();
            resolved.add(donorNode);
            if (groupPermissions.containsKey(donorNode)) {
                resolved.addAll(groupPermissions.get(donorNode));
            }
        }

        // Staff rank permissions (completely separate tree)
        if (ranks.staffRank() != null) {
            String staffNode = "prison.staff." + ranks.staffRank().toLowerCase();
            resolved.add(staffNode);
            if (groupPermissions.containsKey(staffNode)) {
                resolved.addAll(groupPermissions.get(staffNode));
            }
            // Staff inherit all lower staff permissions
            resolved.addAll(resolveStaffInheritance(ranks.staffRank()));
        }

        // Prestige permissions
        if (ranks.prestige() > 0) {
            resolved.add("prison.prestige." + ranks.prestige());
        }

        return new PlayerPermissions(ranks, resolved);
    }

    /**
     * Staff ranks are additive — a Moderator has all Helper permissions too.
     * This resolves the inheritance chain down to the lowest tier.
     */
    private Set<String> resolveStaffInheritance(String staffRank) {
        Set<String> inherited = new HashSet<>();
        List<String> tiers = List.of("helper", "moderator", "seniormod", "admin", "senioradmin", "owner");

        int rankIndex = tiers.indexOf(staffRank.toLowerCase());
        // Add permissions from all tiers below current rank
        for (int i = 0; i <= rankIndex; i++) {
            String node = "prison.staff." + tiers.get(i);
            if (groupPermissions.containsKey(node)) {
                inherited.addAll(groupPermissions.get(node));
            }
        }
        return inherited;
    }

    // ----------------------------------------------------------------
    // Data records
    // ----------------------------------------------------------------

    public record PlayerRankData(String mineRank, String donorRank, String staffRank, int prestige) {
        // Default constructor for new players
        public PlayerRankData() {
            this("A", null, null, 0);
        }
    }

    public record PlayerPermissions(PlayerRankData ranks, Set<String> permissions) {}
}
