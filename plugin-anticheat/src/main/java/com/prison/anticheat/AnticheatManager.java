package com.prison.anticheat;

import com.prison.database.DatabaseManager;
import com.prison.permissions.PermissionEngine;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * AnticheatManager — all detection state and flagging logic.
 *
 * Detection systems:
 *   1. Sell Rate Limiter   — intercepts /sellall spam
 *   2. Block Break Rate    — catches AoE enchant abuse / macros
 *   3. Token Earn Rate     — periodic DB query sanity-check
 */
public class AnticheatManager {

    private static AnticheatManager instance;
    private static final MiniMessage MM = MiniMessage.miniMessage();

    // ----------------------------------------------------------------
    // Config (set from AnticheatPlugin after loading)
    // ----------------------------------------------------------------

    long sellCooldownMs;
    int sellViolationThreshold;
    long sellViolationWindowMs;

    int blockBreakMaxPerSecond;
    int blockBreakConsecutiveThreshold;

    long maxTokensPerMinute;

    int alertEveryNViolations;
    boolean logAllFlags;

    // ----------------------------------------------------------------
    // Detection state — one entry per online player
    // ----------------------------------------------------------------

    // Sell rate
    /** Last time the player executed /sellall (ms). */
    private final ConcurrentHashMap<UUID, Long> lastSellMs = new ConcurrentHashMap<>();
    /** Timestamps of recent sell violations within the window. */
    private final ConcurrentHashMap<UUID, Deque<Long>> sellViolationWindow = new ConcurrentHashMap<>();

    // Block break rate
    /** Count of blocks broken in the current 1-second bucket. */
    private final ConcurrentHashMap<UUID, long[]> blockBucket = new ConcurrentHashMap<>();
    // long[]: [0] = count, [1] = bucket start ms
    /** Consecutive seconds over the threshold. */
    private final ConcurrentHashMap<UUID, Integer> blockConsecutive = new ConcurrentHashMap<>();

    // Session violation totals (for escalating alerts)
    private final ConcurrentHashMap<UUID, Integer> sessionViolations = new ConcurrentHashMap<>();

    private final Logger logger;

    private AnticheatManager(Logger logger) {
        this.logger = logger;
        instance = this;
    }

    public static AnticheatManager initialize(Logger logger) {
        if (instance != null) throw new IllegalStateException("AnticheatManager already initialized");
        return new AnticheatManager(logger);
    }

    public static AnticheatManager getInstance() { return instance; }

    public static void reset() { instance = null; }

    // ----------------------------------------------------------------
    // 1. Sell Rate Check
    // ----------------------------------------------------------------

    /**
     * Called when a player attempts a sell operation (command or auto-sell).
     *
     * @return true if the sell is allowed, false if it should be blocked.
     */
    public boolean checkSell(Player player) {
        UUID uuid = player.getUniqueId();
        long now  = System.currentTimeMillis();

        Long last = lastSellMs.get(uuid);
        if (last != null && now - last < sellCooldownMs) {
            recordSellViolation(player, now, "interval=" + (now - last) + "ms < " + sellCooldownMs + "ms");
            return false; // block the sell
        }

        lastSellMs.put(uuid, now);
        return true;
    }

    private void recordSellViolation(Player player, long now, String detail) {
        UUID uuid = player.getUniqueId();

        Deque<Long> window = sellViolationWindow.computeIfAbsent(uuid, k -> new ArrayDeque<>());
        window.addLast(now);
        // Prune stale entries
        while (!window.isEmpty() && now - window.peekFirst() > sellViolationWindowMs) {
            window.pollFirst();
        }

        int countInWindow = window.size();
        if (countInWindow >= sellViolationThreshold) {
            window.clear(); // reset window to avoid alerting every single violation
            flag(player, FlagType.SELL_RATE, detail + " | " + countInWindow + " violations in window");
        } else if (logAllFlags) {
            // Log quietly but don't alert staff yet
            logFlagQuiet(uuid, FlagType.SELL_RATE, detail);
        }
    }

    // ----------------------------------------------------------------
    // 2. Block Break Rate Check
    // ----------------------------------------------------------------

    /**
     * Called from BlockBreakEvent for every block a player breaks (including AoE).
     * Pass the count of blocks actually broken in this single event.
     */
    public void recordBlockBreaks(Player player, int count) {
        UUID uuid = player.getUniqueId();
        long now  = System.currentTimeMillis();

        long[] bucket = blockBucket.compute(uuid, (k, existing) -> {
            if (existing == null) return new long[]{ count, now };
            if (now - existing[1] >= 1000L) {
                // New second — evaluate the old bucket first
                return new long[]{ count, now };
            }
            existing[0] += count;
            return existing;
        });

        // Check if we just rolled over a second and the previous bucket was over threshold
        long[] finalBucket = blockBucket.get(uuid);
        if (finalBucket != null && now - finalBucket[1] >= 1000L) {
            evaluateBlockBucket(player, uuid, finalBucket[0]);
            blockBucket.put(uuid, new long[]{ count, now });
        } else {
            // Mid-second — check running total
            if (finalBucket != null && finalBucket[0] >= blockBreakMaxPerSecond) {
                int consecutive = blockConsecutive.merge(uuid, 1, Integer::sum);
                if (consecutive >= blockBreakConsecutiveThreshold) {
                    blockConsecutive.put(uuid, 0);
                    flag(player, FlagType.BLOCK_BREAK_RATE,
                        "rate=" + finalBucket[0] + "/s threshold=" + blockBreakMaxPerSecond);
                }
            } else {
                blockConsecutive.put(uuid, 0);
            }
        }
    }

    private void evaluateBlockBucket(Player player, UUID uuid, long bucketCount) {
        if (bucketCount >= blockBreakMaxPerSecond) {
            int consecutive = blockConsecutive.merge(uuid, 1, Integer::sum);
            if (consecutive >= blockBreakConsecutiveThreshold) {
                blockConsecutive.put(uuid, 0);
                flag(player, FlagType.BLOCK_BREAK_RATE,
                    "rate=" + bucketCount + "/s threshold=" + blockBreakMaxPerSecond);
            }
        } else {
            blockConsecutive.put(uuid, 0);
        }
    }

    // ----------------------------------------------------------------
    // 3. Token Earn Rate (periodic check — called async every N seconds)
    // ----------------------------------------------------------------

    /**
     * Checks token earnings for all online players over the last {@code windowSeconds}.
     * If any player earned more than the configured max-per-minute × (windowSeconds/60), flag them.
     */
    public void runTokenRateCheck(int windowSeconds) {
        if (maxTokensPerMinute <= 0) return; // disabled

        long maxAllowed = (long) (maxTokensPerMinute * (windowSeconds / 60.0));
        long windowMs   = windowSeconds * 1000L;

        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();

            try {
                long earned = DatabaseManager.getInstance().query(
                    "SELECT COALESCE(SUM(amount), 0) FROM transactions " +
                    "WHERE player_uuid = ? AND currency_type = 'TOKEN' AND type = 'TOKEN_EARN' " +
                    "AND timestamp > FROM_UNIXTIME(?)",
                    rs -> {
                        if (rs.next()) return rs.getLong(1);
                        return 0L;
                    },
                    uuid.toString(),
                    (System.currentTimeMillis() - windowMs) / 1000L
                );

                if (earned > maxAllowed) {
                    flag(player, FlagType.TOKEN_EARN_RATE,
                        "earned=" + earned + " max=" + maxAllowed + " window=" + windowSeconds + "s");
                }

            } catch (SQLException e) {
                logger.log(Level.WARNING, "[AC] Token rate check failed for " + player.getName(), e);
            }
        }
    }

    // ----------------------------------------------------------------
    // Flagging
    // ----------------------------------------------------------------

    /**
     * Records a flag: logs to DB (if configured), increments session violations,
     * and alerts staff if the alert threshold is hit.
     */
    public void flag(Player player, FlagType type, String details) {
        UUID uuid = player.getUniqueId();

        // Increment session violation count
        int total = sessionViolations.merge(uuid, 1, Integer::sum);

        // Log to DB
        String detailsJson = "{\"detail\":\"" + escapeJson(details) + "\"}";
        DatabaseManager.getInstance().queueWrite(
            "INSERT INTO anticheat_flags (player_uuid, flag_type, details) VALUES (?, ?, ?)",
            uuid.toString(), type.name(), detailsJson
        );

        logger.warning("[AC] Flag: " + player.getName() + " | " + type.display + " | " + details
            + " (session total: " + total + ")");

        // Alert staff at threshold and every N after
        if (total % alertEveryNViolations == 0) {
            notifyStaff(player.getName(), type, details, total);
        }
    }

    /** Log a flag quietly to DB without counting toward the alert threshold. */
    private void logFlagQuiet(UUID uuid, FlagType type, String details) {
        if (!logAllFlags) return;
        String detailsJson = "{\"detail\":\"" + escapeJson(details) + "\",\"quiet\":true}";
        DatabaseManager.getInstance().queueWrite(
            "INSERT INTO anticheat_flags (player_uuid, flag_type, details) VALUES (?, ?, ?)",
            uuid.toString(), type.name(), detailsJson
        );
    }

    private void notifyStaff(String playerName, FlagType type, String details, int total) {
        String msg = "<dark_red>[AC] <white>" + playerName + "</white> flagged for <yellow>"
            + type.display + "</yellow> — <gray>" + details + " <dark_gray>(session: " + total + ")";

        Bukkit.getScheduler().runTask(AnticheatPlugin.getInstance(), () -> {
            for (Player staff : Bukkit.getOnlinePlayers()) {
                if (PermissionEngine.getInstance().hasPermission(staff, "prison.staff.helper")) {
                    staff.sendMessage(MM.deserialize(msg));
                }
            }
        });
    }

    // ----------------------------------------------------------------
    // Session management
    // ----------------------------------------------------------------

    public void onPlayerJoin(UUID uuid) {
        // Nothing to pre-load — all state starts fresh
    }

    public void onPlayerQuit(UUID uuid) {
        lastSellMs.remove(uuid);
        sellViolationWindow.remove(uuid);
        blockBucket.remove(uuid);
        blockConsecutive.remove(uuid);
        sessionViolations.remove(uuid);
    }

    /** Clear session counters for a player (admin command). */
    public void clearSession(UUID uuid) {
        lastSellMs.remove(uuid);
        sellViolationWindow.remove(uuid);
        blockBucket.remove(uuid);
        blockConsecutive.remove(uuid);
        sessionViolations.remove(uuid);
    }

    /** Returns the current session violation count for a player. */
    public int getSessionViolations(UUID uuid) {
        return sessionViolations.getOrDefault(uuid, 0);
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
