package com.prison.quests;

import com.prison.database.DatabaseManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * QuestManager — central coordinator for all quest state.
 *
 * Responsibilities:
 *  - Hold the ordered list of QuestDefinitions loaded from config.
 *  - Cache per-player quest data in memory (loaded on join, saved on quit).
 *  - Receive progress notifications and trigger completions.
 *  - Deliver rewards via EconomyAPI when available, or fall back gracefully.
 *  - Persist state to the player_quests table.
 *
 * All DB writes use queueWrite (fire-and-forget batch).
 * DB reads (load) run async so the main thread is never blocked.
 */
public class QuestManager {

    private static QuestManager instance;

    private static final MiniMessage MM = MiniMessage.miniMessage();

    // quest_id -> definition, insertion order preserved
    private final Map<String, QuestDefinition> definitions;

    // uuid -> { quest_id -> data }
    private final ConcurrentHashMap<UUID, Map<String, PlayerQuestData>> playerCache
            = new ConcurrentHashMap<>();

    // uuid -> session start epoch-second (for ONLINE_TIME tracking)
    private final ConcurrentHashMap<UUID, Long> sessionStart = new ConcurrentHashMap<>();

    private final Logger logger;
    private final QuestPlugin plugin;

    // Lazily resolved on first reward delivery to avoid hard startup dependency.
    private volatile boolean economyChecked = false;
    private volatile com.prison.economy.EconomyAPI economyAPI = null;

    QuestManager(Map<String, QuestDefinition> definitions, Logger logger, QuestPlugin plugin) {
        this.definitions = Collections.unmodifiableMap(new LinkedHashMap<>(definitions));
        this.logger      = logger;
        this.plugin      = plugin;
        instance = this;
    }

    public static QuestManager getInstance() { return instance; }

    // ----------------------------------------------------------------
    // Definition Queries
    // ----------------------------------------------------------------

    /** All quest definitions in config order. */
    public Collection<QuestDefinition> getAllDefinitions() {
        return definitions.values();
    }

    /** Definitions filtered by tier, in config order. */
    public List<QuestDefinition> getDefinitionsByTier(QuestTier tier) {
        List<QuestDefinition> result = new ArrayList<>();
        for (QuestDefinition def : definitions.values()) {
            if (def.getTier() == tier) result.add(def);
        }
        return result;
    }

    public QuestDefinition getDefinition(String questId) {
        return definitions.get(questId);
    }

    // ----------------------------------------------------------------
    // Player Cache
    // ----------------------------------------------------------------

    /**
     * Load all quest rows for a player from the DB into memory.
     * Called async-safe from PlayerJoinEvent via a CompletableFuture.
     */
    public CompletableFuture<Void> loadPlayer(UUID uuid) {
        return CompletableFuture.runAsync(() -> {
            try {
                Map<String, PlayerQuestData> data = DatabaseManager.getInstance().query(
                    "SELECT quest_id, progress, completed, last_reset " +
                    "FROM player_quests WHERE player_uuid = ?",
                    rs -> {
                        Map<String, PlayerQuestData> map = new LinkedHashMap<>();
                        while (rs.next()) {
                            String questId  = rs.getString("quest_id");
                            long   progress = rs.getLong("progress");
                            boolean done    = rs.getInt("completed") == 1;
                            long   lastRst  = rs.getLong("last_reset");
                            map.put(questId, new PlayerQuestData(questId, progress, done, lastRst));
                        }
                        return map;
                    },
                    uuid.toString()
                );
                // Fill in any quests that have no row yet (new quests added after player joined)
                for (QuestDefinition def : definitions.values()) {
                    data.computeIfAbsent(def.getId(),
                        id -> new PlayerQuestData(id, 0L, false, 0L));
                }
                playerCache.put(uuid, data);
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "[Quests] Failed to load quests for " + uuid, e);
                // Put empty map so the player isn't locked out
                Map<String, PlayerQuestData> empty = new LinkedHashMap<>();
                for (QuestDefinition def : definitions.values()) {
                    empty.put(def.getId(), new PlayerQuestData(def.getId(), 0L, false, 0L));
                }
                playerCache.put(uuid, empty);
            }
        });
    }

    /**
     * Flush online-time progress, persist all rows, and remove from cache.
     * Called on PlayerQuitEvent (main thread — DB writes are queued async).
     */
    public void saveAndUnload(UUID uuid) {
        // Flush online-time before saving
        Long start = sessionStart.remove(uuid);
        if (start != null) {
            long minutes = (epochSeconds() - start) / 60;
            if (minutes > 0) addProgress(uuid, QuestType.ONLINE_TIME, minutes);
        }

        Map<String, PlayerQuestData> data = playerCache.remove(uuid);
        if (data == null) return;
        persistAll(uuid, data);
    }

    /** Record session start time so online-time can be tracked on quit. */
    public void recordSessionStart(UUID uuid) {
        sessionStart.put(uuid, epochSeconds());
    }

    // ----------------------------------------------------------------
    // Progress API
    // ----------------------------------------------------------------

    /**
     * Public entry point called by event listeners and external plugins.
     * Increments progress for every quest of the matching type this player has active.
     *
     * Must be called on the main thread (reward delivery dispatches commands on main).
     */
    public void addProgress(UUID uuid, QuestType type, long amount) {
        if (amount <= 0) return;
        Map<String, PlayerQuestData> playerData = playerCache.get(uuid);
        if (playerData == null) return;  // player not loaded yet

        long now = epochSeconds();

        for (QuestDefinition def : definitions.values()) {
            if (def.getType() != type) continue;

            PlayerQuestData data = playerData.get(def.getId());
            if (data == null) continue;

            // Reset check — must happen before progress so we don't give credit after a
            // stale completed state from the previous period.
            if (data.needsReset(def, now)) {
                data.reset(now);
                persistRow(uuid, data);
            }

            // Skip if already completed in current period (MILESTONE stays completed forever)
            if (data.isCompleted()) continue;

            long newProgress = data.addProgress(amount);
            persistRow(uuid, data);

            // Check for completion
            if (newProgress >= def.getGoal()) {
                data.setCompleted(true);
                persistRow(uuid, data);
                deliverRewards(uuid, def);
            }
        }
    }

    // ----------------------------------------------------------------
    // Per-player data accessors (for GUI)
    // ----------------------------------------------------------------

    /**
     * Get PlayerQuestData for a specific player + quest, applying reset if due.
     * Returns null only if the player is not loaded.
     */
    public PlayerQuestData getQuestData(UUID uuid, String questId) {
        Map<String, PlayerQuestData> map = playerCache.get(uuid);
        if (map == null) return null;
        PlayerQuestData data = map.get(questId);
        if (data == null) return null;

        QuestDefinition def = definitions.get(questId);
        if (def != null && data.needsReset(def, epochSeconds())) {
            data.reset(epochSeconds());
            persistRow(uuid, data);
        }
        return data;
    }

    /**
     * Seconds until the next reset for a given quest for this player.
     * Returns 0 if a reset is already due, Long.MAX_VALUE for milestones.
     */
    public long secondsUntilReset(UUID uuid, String questId) {
        PlayerQuestData data = getQuestData(uuid, questId);
        QuestDefinition def  = definitions.get(questId);
        if (data == null || def == null) return 0L;
        return data.secondsUntilReset(def, epochSeconds());
    }

    // ----------------------------------------------------------------
    // Reward Delivery
    // ----------------------------------------------------------------

    /**
     * Deliver IGC + token rewards to the player using EconomyAPI if loaded,
     * then send a completion notification to the player in-game.
     *
     * Must run on the main thread.
     */
    private void deliverRewards(UUID uuid, QuestDefinition def) {
        Player player = Bukkit.getPlayer(uuid);

        // Resolve EconomyAPI once
        if (!economyChecked) {
            economyChecked = true;
            Plugin ecoPlugin = Bukkit.getPluginManager().getPlugin("PrisonEconomy");
            if (ecoPlugin != null) {
                try {
                    economyAPI = com.prison.economy.EconomyAPI.getInstance();
                } catch (Exception e) {
                    logger.warning("[Quests] PrisonEconomy loaded but EconomyAPI unavailable: " + e.getMessage());
                }
            } else {
                logger.warning("[Quests] PrisonEconomy not found — quest rewards will not be delivered.");
            }
        }

        if (economyAPI != null) {
            if (def.hasIgcReward()) {
                economyAPI.addBalance(uuid, def.getIgcReward(), com.prison.economy.TransactionType.ADMIN_ADD);
            }
            if (def.hasTokenReward()) {
                economyAPI.addTokens(uuid, def.getTokenReward(), com.prison.economy.TransactionType.ADMIN_ADD);
            }
        }

        // Notify the player (if online)
        if (player != null && player.isOnline()) {
            player.sendMessage(MM.deserialize(
                "<gold>⚑ <yellow>Quest Complete! <white>" + def.getTitle()));

            if (def.hasIgcReward() || def.hasTokenReward()) {
                StringBuilder rewards = new StringBuilder("<gray>Rewards: ");
                if (def.hasIgcReward()) {
                    rewards.append("<gold>").append(formatAmount(def.getIgcReward())).append(" IGC");
                }
                if (def.hasIgcReward() && def.hasTokenReward()) {
                    rewards.append("<gray> + ");
                }
                if (def.hasTokenReward()) {
                    rewards.append("<aqua>").append(formatAmount(def.getTokenReward())).append(" Tokens");
                }
                player.sendMessage(MM.deserialize(rewards.toString()));
            }

            // Title + subtitle splash
            player.showTitle(net.kyori.adventure.title.Title.title(
                MM.deserialize("<gold><bold>Quest Complete!"),
                MM.deserialize("<yellow>" + def.getTitle()),
                net.kyori.adventure.title.Title.Times.times(
                    java.time.Duration.ofMillis(500),
                    java.time.Duration.ofMillis(2500),
                    java.time.Duration.ofMillis(500)
                )
            ));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.0f);
        }
    }

    // ----------------------------------------------------------------
    // DB Persistence
    // ----------------------------------------------------------------

    /** Upsert a single quest row for a player. Uses queueWrite (async batched). */
    private void persistRow(UUID uuid, PlayerQuestData data) {
        DatabaseManager.getInstance().queueWrite(
            "INSERT INTO player_quests (player_uuid, quest_id, progress, completed, last_reset) " +
            "VALUES (?, ?, ?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE progress = VALUES(progress), " +
            "completed = VALUES(completed), last_reset = VALUES(last_reset)",
            uuid.toString(),
            data.getQuestId(),
            data.getProgress(),
            data.isCompleted() ? 1 : 0,
            data.getLastReset()
        );
    }

    /** Persist all rows for a player (called on quit). */
    private void persistAll(UUID uuid, Map<String, PlayerQuestData> data) {
        for (PlayerQuestData row : data.values()) {
            persistRow(uuid, row);
        }
    }

    // ----------------------------------------------------------------
    // Online-Time Flush (called from periodic task in QuestPlugin)
    // ----------------------------------------------------------------

    /**
     * Flush accumulated online-time for the given player without unloading them.
     * Resets their session start to nowEpochSeconds so time is not double-counted on quit.
     *
     * @param uuid            the online player
     * @param nowEpochSeconds current wall-clock epoch seconds
     */
    public void flushOnlineTime(UUID uuid, long nowEpochSeconds) {
        Long start = sessionStart.get(uuid);
        if (start == null) return;
        long minutes = (nowEpochSeconds - start) / 60;
        if (minutes > 0) {
            sessionStart.put(uuid, nowEpochSeconds);  // reset so we don't double-count
            addProgress(uuid, QuestType.ONLINE_TIME, minutes);
        }
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private static long epochSeconds() {
        return System.currentTimeMillis() / 1000L;
    }

    /**
     * Format a large number with commas: 1500000 → "1,500,000"
     */
    static String formatAmount(long amount) {
        return String.format("%,d", amount);
    }
}
