package com.prison.ranks;

import com.prison.database.DatabaseManager;
import com.prison.permissions.PermissionEngine;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * RankManager — handles rankup logic and per-player autoteleport preferences.
 *
 * IGC balances are owned by the economy system (build order #8). Until that
 * plugin is built, this class reads/writes igc_balance directly from the
 * players table. Once the economy plugin is live, other plugins call
 * EconomyAPI to read balances — this class will be updated then.
 *
 * Autoteleport preference is stored per-player in memory only (resets on
 * restart). Persisting it to the database is deferred until the warp system
 * (build #10) is in place.
 */
public class RankManager {

    private static RankManager instance;

    private RankConfig config;
    private final Logger logger;
    private final MiniMessage mm = MiniMessage.miniMessage();

    // Per-player autoteleport preference (true = auto-tp on rankup)
    private final ConcurrentHashMap<UUID, Boolean> autoteleport = new ConcurrentHashMap<>();

    private RankManager(RankConfig config, Logger logger) {
        this.config = config;
        this.logger = logger;
    }

    public static RankManager getInstance() { return instance; }

    public static RankManager initialize(RankConfig config, Logger logger) {
        if (instance != null) throw new IllegalStateException("RankManager already initialized");
        instance = new RankManager(config, logger);
        return instance;
    }

    // ----------------------------------------------------------------
    // Public API
    // ----------------------------------------------------------------

    /**
     * Attempt to rank up a player. Deducts IGC, updates DB, refreshes permissions.
     * Returns the result so the caller can send the appropriate message.
     */
    public RankupResult rankUp(Player player) {
        UUID uuid = player.getUniqueId();
        String currentRank = PermissionEngine.getInstance().getMineRank(uuid);
        String nextRank    = config.nextRank(currentRank);

        if (nextRank == null) {
            return RankupResult.MAX_RANK;
        }

        // Cost is the cost of the NEXT rank (what you pay to enter it)
        RankConfig.RankData nextData = config.getRank(nextRank);
        if (nextData == null) return RankupResult.ERROR;

        long cost = nextData.cost();

        try {
            // Read current IGC balance
            long balance = getIgcBalance(uuid);

            if (balance < cost) {
                return new RankupResult(RankupResult.Type.CANNOT_AFFORD, cost, balance, null);
            }

            // Deduct and update
            long newBalance = balance - cost;
            DatabaseManager.getInstance().execute(
                "UPDATE players SET igc_balance = ?, mine_rank = ? WHERE uuid = ?",
                newBalance, nextRank, uuid.toString()
            );

            // Log transaction
            DatabaseManager.getInstance().execute(
                "INSERT INTO transactions (player_uuid, currency_type, type, amount, balance_after) VALUES (?, 'IGC', 'rankup', ?, ?)",
                uuid.toString(), cost, newBalance
            );

            // Rebuild permission cache async
            PermissionEngine.getInstance().setMineRank(uuid, nextRank);

            logger.info("[Ranks] " + player.getName() + " ranked up " + currentRank + " → " + nextRank + " (cost: " + cost + " IGC)");
            return new RankupResult(RankupResult.Type.SUCCESS, cost, newBalance, nextRank);

        } catch (SQLException e) {
            logger.severe("[Ranks] Failed to process rankup for " + player.getName() + ": " + e.getMessage());
            return RankupResult.ERROR;
        }
    }

    /**
     * Returns true if the player can afford the next rankup.
     */
    public boolean canRankUp(UUID uuid) {
        String currentRank = PermissionEngine.getInstance().getMineRank(uuid);
        String nextRank    = config.nextRank(currentRank);
        if (nextRank == null) return false;

        RankConfig.RankData nextData = config.getRank(nextRank);
        if (nextData == null) return false;

        try {
            return getIgcBalance(uuid) >= nextData.cost();
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Toggle autoteleport preference for a player.
     */
    public boolean toggleAutoteleport(UUID uuid) {
        boolean current = autoteleport.getOrDefault(uuid, config.isAutoteleportDefault());
        autoteleport.put(uuid, !current);
        return !current;
    }

    public boolean getAutoteleport(UUID uuid) {
        return autoteleport.getOrDefault(uuid, config.isAutoteleportDefault());
    }

    public RankConfig getConfig() { return config; }

    /**
     * Replace the active rank config with a newly loaded one.
     * Called by RankPlugin after saving changes to the config file.
     */
    public void reloadConfig(RankConfig newConfig) {
        this.config = newConfig;
    }

    // ----------------------------------------------------------------
    // Internal
    // ----------------------------------------------------------------

    private long getIgcBalance(UUID uuid) throws SQLException {
        Long balance = DatabaseManager.getInstance().query(
            "SELECT igc_balance FROM players WHERE uuid = ?",
            rs -> rs.next() ? rs.getLong("igc_balance") : 0L,
            uuid.toString()
        );
        return balance != null ? balance : 0L;
    }

    // ----------------------------------------------------------------
    // Result type
    // ----------------------------------------------------------------

    public record RankupResult(Type type, long cost, long balance, String newRank) {
        public static final RankupResult MAX_RANK = new RankupResult(Type.MAX_RANK, 0, 0, null);
        public static final RankupResult ERROR    = new RankupResult(Type.ERROR, 0, 0, null);

        public enum Type { SUCCESS, CANNOT_AFFORD, MAX_RANK, ERROR }
    }

    /**
     * Format a large number as a readable string (1000000 → "1,000,000").
     */
    public static String formatNumber(long n) {
        return String.format("%,d", n);
    }
}
