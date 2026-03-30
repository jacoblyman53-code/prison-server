package com.prison.quests;

import com.prison.database.DatabaseManager;
import com.prison.economy.EconomyAPI;
import com.prison.economy.TransactionType;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ChainQuestManager — a 5-stage guided questline that walks new players through
 * the core prison loop (mine → sell → rankup → repeat).
 *
 * State is persisted in the `chain_quests` table.
 * Progress for the current stage is tracked in memory and synced to DB after
 * every increment or stage advance.
 *
 * Stages:
 *  1. Mine 250 blocks     (intro to mining)
 *  2. Sell once           (intro to selling)
 *  3. Mine 1,000 blocks   (basic grind)
 *  4. Rank up once        (intro to rankup)
 *  5. Mine 5,000 blocks   (end of early-game)
 */
public class ChainQuestManager {

    // ----------------------------------------------------------------
    // Stage definitions
    // ----------------------------------------------------------------

    public record ChainStage(int number, String title, String desc,
                              QuestType type, long goal,
                              long igcReward, long tokenReward) {}

    public static final int TOTAL_STAGES = 5;

    private static final ChainStage[] STAGES = {
        new ChainStage(1, "First Steps",      "Mine 250 blocks",       QuestType.BLOCKS_MINED,  250L,   5_000L,  50L),
        new ChainStage(2, "First Sale",       "Use /sell once",        QuestType.SELL_COMMANDS,   1L,  10_000L,  75L),
        new ChainStage(3, "Getting Serious",  "Mine 1,000 blocks",     QuestType.BLOCKS_MINED, 1000L,  25_000L, 150L),
        new ChainStage(4, "Moving Up",        "Rank up once",          QuestType.RANKUPS,         1L,  50_000L, 200L),
        new ChainStage(5, "The Grind",        "Mine 5,000 blocks",     QuestType.BLOCKS_MINED, 5000L, 100_000L, 500L),
    };

    /** Returns a stage definition (1-based). Returns null if out of range. */
    public static ChainStage getStageDefinition(int stageNumber) {
        if (stageNumber < 1 || stageNumber > TOTAL_STAGES) return null;
        return STAGES[stageNumber - 1];
    }

    // ----------------------------------------------------------------
    // Singleton
    // ----------------------------------------------------------------

    private static ChainQuestManager instance;

    public static ChainQuestManager getInstance() { return instance; }

    public static ChainQuestManager initialize(QuestPlugin plugin) {
        instance = new ChainQuestManager(plugin);
        return instance;
    }

    // ----------------------------------------------------------------
    // State
    // ----------------------------------------------------------------

    // uuid -> current stage number (1-based; > TOTAL_STAGES = all done)
    private final ConcurrentHashMap<UUID, Integer> stageCache    = new ConcurrentHashMap<>();
    // uuid -> progress within the current stage
    private final ConcurrentHashMap<UUID, Long>    progressCache = new ConcurrentHashMap<>();

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private final QuestPlugin plugin;

    private ChainQuestManager(QuestPlugin plugin) { this.plugin = plugin; }

    // ----------------------------------------------------------------
    // Schema
    // ----------------------------------------------------------------

    public void ensureTable() throws SQLException {
        DatabaseManager.getInstance().execute(
            "CREATE TABLE IF NOT EXISTS chain_quests (" +
            "  player_uuid VARCHAR(36) NOT NULL PRIMARY KEY," +
            "  stage       INT         NOT NULL DEFAULT 1," +
            "  progress    BIGINT      NOT NULL DEFAULT 0" +
            ")"
        );
    }

    // ----------------------------------------------------------------
    // Load / Unload
    // ----------------------------------------------------------------

    public void loadPlayer(UUID uuid) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                long[] row = DatabaseManager.getInstance().query(
                    "SELECT stage, progress FROM chain_quests WHERE player_uuid = ?",
                    rs -> rs.next()
                            ? new long[]{rs.getLong("stage"), rs.getLong("progress")}
                            : new long[]{1L, 0L},
                    uuid.toString()
                );
                stageCache.put(uuid, (int) row[0]);
                progressCache.put(uuid, row[1]);
            } catch (SQLException e) {
                stageCache.put(uuid, 1);
                progressCache.put(uuid, 0L);
            }
        });
    }

    public void unloadPlayer(UUID uuid) {
        stageCache.remove(uuid);
        progressCache.remove(uuid);
    }

    // ----------------------------------------------------------------
    // Queries (for GUI)
    // ----------------------------------------------------------------

    /** 1-based current stage. Returns TOTAL_STAGES+1 if questline is complete. */
    public int getCurrentStage(UUID uuid) {
        return stageCache.getOrDefault(uuid, 1);
    }

    /** Progress within the current stage. */
    public long getCurrentProgress(UUID uuid) {
        return progressCache.getOrDefault(uuid, 0L);
    }

    /** True if the player has finished all 5 stages. */
    public boolean isComplete(UUID uuid) {
        return getCurrentStage(uuid) > TOTAL_STAGES;
    }

    // ----------------------------------------------------------------
    // Progress API — called from QuestPlugin event listeners
    // ----------------------------------------------------------------

    public void addProgress(UUID uuid, QuestType type, long amount) {
        if (amount <= 0) return;
        int stage = getCurrentStage(uuid);
        if (stage > TOTAL_STAGES) return;                    // already done

        ChainStage def = STAGES[stage - 1];
        if (def.type() != type) return;                      // wrong type for this stage

        long newProgress = progressCache.merge(uuid, amount, Long::sum);
        persistAsync(uuid, stage, newProgress);

        if (newProgress >= def.goal()) {
            completeStage(uuid, stage);
        }
    }

    // ----------------------------------------------------------------
    // Stage completion
    // ----------------------------------------------------------------

    private void completeStage(UUID uuid, int stage) {
        int nextStage = stage + 1;
        stageCache.put(uuid, nextStage);
        progressCache.put(uuid, 0L);
        persistAsync(uuid, nextStage, 0L);

        ChainStage def = STAGES[stage - 1];

        // Deliver rewards (EconomyAPI may not be loaded — soft-depend)
        EconomyAPI eco = EconomyAPI.getInstance();
        if (eco != null) {
            eco.addBalance(uuid, def.igcReward(), TransactionType.ADMIN_ADD);
            eco.addTokens(uuid, def.tokenReward(), TransactionType.ADMIN_ADD);
        }

        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return;

        if (nextStage > TOTAL_STAGES) {
            // Questline complete
            player.sendMessage(MM.deserialize(
                "<gold>★ <yellow>Chain Quest Complete! <white>You've finished all 5 stages!"));
            player.sendMessage(MM.deserialize(
                "<gray>Final rewards: <gold>" + fmt(def.igcReward()) + " IGC <gray>+ <aqua>" + def.tokenReward() + " tokens"));
            player.showTitle(Title.title(
                MM.deserialize("<gold><bold>Chain Complete!"),
                MM.deserialize("<yellow>All 5 stages finished!"),
                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(500))
            ));
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        } else {
            player.sendMessage(MM.deserialize(
                "<gold>⚡ <yellow>Stage " + stage + " Complete! <white>" + def.title() +
                " <dark_gray>→ <green>Stage " + nextStage + " unlocked!"));
            player.sendMessage(MM.deserialize(
                "<gray>Rewards: <gold>" + fmt(def.igcReward()) + " IGC <gray>+ <aqua>" + def.tokenReward() + " tokens"));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.1f);
        }
    }

    // ----------------------------------------------------------------
    // DB persistence
    // ----------------------------------------------------------------

    private void persistAsync(UUID uuid, int stage, long progress) {
        DatabaseManager.getInstance().queueWrite(
            "INSERT INTO chain_quests (player_uuid, stage, progress) VALUES (?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE stage = VALUES(stage), progress = VALUES(progress)",
            uuid.toString(), stage, progress
        );
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private static String fmt(long n) { return String.format("%,d", n); }
}
