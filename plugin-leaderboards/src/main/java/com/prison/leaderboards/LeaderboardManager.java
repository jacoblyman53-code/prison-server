package com.prison.leaderboards;

import com.prison.database.DatabaseManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Manages leaderboard data: block-mine tracking, periodic DB flush, and
 * cached leaderboard entries for all four categories.
 */
public class LeaderboardManager {

    // ----------------------------------------------------------------
    // LeaderboardEntry record
    // ----------------------------------------------------------------

    /** A single leaderboard row: the player name and their numeric value. */
    public record LeaderboardEntry(String name, long value) {}

    // ----------------------------------------------------------------
    // Fields
    // ----------------------------------------------------------------

    private final JavaPlugin plugin;
    private final Logger log;

    /**
     * In-memory pending block counts keyed by player UUID.
     * Incremented on the main thread via BlockBreakEvent; flushed async.
     * ConcurrentHashMap so that the async flush thread can iterate safely
     * while the main thread continues to write.
     */
    private final ConcurrentHashMap<UUID, Long> pendingBlocksMined = new ConcurrentHashMap<>();

    // Cached leaderboard results — updated by refreshAll()
    private volatile List<LeaderboardEntry> cacheRichest  = Collections.emptyList();
    private volatile List<LeaderboardEntry> cachePrestige = Collections.emptyList();
    private volatile List<LeaderboardEntry> cacheBlocks   = Collections.emptyList();
    private volatile List<LeaderboardEntry> cacheTokens   = Collections.emptyList();

    // ----------------------------------------------------------------
    // Constructor
    // ----------------------------------------------------------------

    public LeaderboardManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.log    = plugin.getLogger();
    }

    // ----------------------------------------------------------------
    // Schema
    // ----------------------------------------------------------------

    /**
     * Adds the {@code blocks_mined} column to the {@code players} table if it
     * does not already exist.  Errors for duplicate columns are silently swallowed.
     * Must be called off the main thread.
     */
    public void ensureSchema() {
        try {
            DatabaseManager.getInstance().execute(
                "ALTER TABLE players ADD COLUMN blocks_mined BIGINT NOT NULL DEFAULT 0");
            log.info("[Leaderboards] Added blocks_mined column to players table.");
        } catch (Exception e) {
            // MySQL/MariaDB: error 1060 "Duplicate column name" — safe to ignore.
            // Any other error is also non-fatal; the column either exists or there
            // is a genuine DB problem that will surface elsewhere.
            String msg = e.getMessage();
            if (msg != null && msg.toLowerCase().contains("duplicate column")) {
                log.fine("[Leaderboards] blocks_mined column already exists — skipping.");
            } else {
                log.warning("[Leaderboards] ensureSchema warning: " + msg);
            }
        }
    }

    // ----------------------------------------------------------------
    // Block tracking
    // ----------------------------------------------------------------

    /**
     * Called on the main thread for every block a player breaks.
     * Atomically increments the player's pending count.
     */
    public void recordBlockMined(UUID uuid) {
        pendingBlocksMined.merge(uuid, 1L, Long::sum);
    }

    /**
     * Drains the in-memory pending counts and issues batch UPDATE statements to
     * the database via {@link DatabaseManager#queueWrite}.
     * Safe to call from any thread; designed to be called from the async flush task.
     */
    public void flushBlocksMined() {
        if (pendingBlocksMined.isEmpty()) return;

        // Snapshot and drain atomically per entry
        for (Map.Entry<UUID, Long> entry : pendingBlocksMined.entrySet()) {
            UUID uuid = entry.getKey();
            // Remove first, then use the snapshot value to avoid losing counts
            // that arrive between remove() and the DB write.
            Long count = pendingBlocksMined.remove(uuid);
            if (count == null || count == 0) continue;

            final long toAdd = count;
            try {
                DatabaseManager.getInstance().queueWrite(
                    "UPDATE players SET blocks_mined = blocks_mined + ? WHERE uuid = ?",
                    toAdd,
                    uuid.toString());
            } catch (Exception e) {
                log.warning("[Leaderboards] flushBlocksMined failed for " + uuid + ": " + e.getMessage());
                // Re-add the count so it isn't permanently lost.
                pendingBlocksMined.merge(uuid, toAdd, Long::sum);
            }
        }
    }

    // ----------------------------------------------------------------
    // Leaderboard refresh
    // ----------------------------------------------------------------

    /**
     * Queries all four leaderboard categories from the database and updates the
     * in-memory caches.  Must be called off the main thread.
     */
    public void refreshAll() {
        cacheRichest  = queryLeaderboard("SELECT username, igc_balance    FROM players ORDER BY igc_balance    DESC LIMIT 10");
        cachePrestige = queryLeaderboard("SELECT username, prestige FROM players ORDER BY prestige DESC LIMIT 10");
        cacheBlocks   = queryLeaderboard("SELECT username, blocks_mined    FROM players ORDER BY blocks_mined    DESC LIMIT 10");
        cacheTokens   = queryLeaderboard("SELECT username, token_balance   FROM players ORDER BY token_balance   DESC LIMIT 10");
    }

    /**
     * Executes a two-column SELECT (username, value) and returns the results as
     * an immutable list of {@link LeaderboardEntry}.
     */
    private List<LeaderboardEntry> queryLeaderboard(String sql) {
        try {
            return DatabaseManager.getInstance().query(sql, rs -> {
                List<LeaderboardEntry> entries = new ArrayList<>();
                while (rs.next()) {
                    entries.add(new LeaderboardEntry(rs.getString(1), rs.getLong(2)));
                }
                return Collections.unmodifiableList(entries);
            });
        } catch (Exception e) {
            log.warning("[Leaderboards] refreshAll query failed: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    // ----------------------------------------------------------------
    // Personal stats
    // ----------------------------------------------------------------

    public record PlayerStats(long igcBalance, long tokenBalance, int prestige,
                               long blocksMined,
                               int rankRichest, int rankTokens, int rankPrestige, int rankBlocks) {}

    /**
     * Fetches personal stats for a player from the DB (async-safe).
     * Returns null if the player is not found.
     */
    public PlayerStats fetchPlayerStats(java.util.UUID uuid) {
        try {
            return DatabaseManager.getInstance().query(
                "SELECT igc_balance, token_balance, prestige, blocks_mined FROM players WHERE uuid = ?",
                rs -> {
                    if (!rs.next()) return null;
                    long igc    = rs.getLong("igc_balance");
                    long tokens = rs.getLong("token_balance");
                    int  prest  = rs.getInt("prestige");
                    long blocks = rs.getLong("blocks_mined");

                    int rankRichest  = rankFor("igc_balance",    igc);
                    int rankTokens   = rankFor("token_balance",  tokens);
                    int rankPrestige = rankFor("prestige",        prest);
                    int rankBlocks   = rankFor("blocks_mined",   blocks);

                    return new PlayerStats(igc, tokens, prest, blocks,
                                          rankRichest, rankTokens, rankPrestige, rankBlocks);
                },
                uuid.toString()
            );
        } catch (Exception e) {
            log.warning("[Leaderboards] fetchPlayerStats failed: " + e.getMessage());
            return null;
        }
    }

    /** Returns the 1-based rank of a player given their value in a column. */
    private int rankFor(String column, long value) {
        try {
            return DatabaseManager.getInstance().query(
                "SELECT COUNT(*) + 1 FROM players WHERE " + column + " > ?",
                rs -> rs.next() ? rs.getInt(1) : 0,
                value
            );
        } catch (Exception e) {
            return 0;
        }
    }

    // ----------------------------------------------------------------
    // Cache accessors
    // ----------------------------------------------------------------

    /**
     * Returns the cached leaderboard for the given category id.
     *
     * @param category one of {@code "richest"}, {@code "prestige"}, {@code "blocks"}, {@code "tokens"}
     * @return an immutable, possibly empty list of up to 10 entries
     */
    public List<LeaderboardEntry> getLeaderboard(String category) {
        return switch (category) {
            case "richest"  -> cacheRichest;
            case "prestige" -> cachePrestige;
            case "blocks"   -> cacheBlocks;
            case "tokens"   -> cacheTokens;
            default         -> Collections.emptyList();
        };
    }
}
