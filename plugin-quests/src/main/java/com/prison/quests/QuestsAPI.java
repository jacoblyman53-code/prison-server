package com.prison.quests;

import java.util.UUID;

/**
 * QuestsAPI — static facade for cross-plugin quest progress injection.
 *
 * Follows the same singleton pattern as EconomyAPI, RanksAPI, DonorAPI.
 * Other plugins (e.g. plugin-ranks) can soft-depend on plugin-quests and
 * call QuestsAPI.getInstance() at runtime — returns null if not loaded.
 */
public class QuestsAPI {

    private static QuestsAPI instance;

    private final QuestManager  questManager;
    private final ChainQuestManager chainManager;

    QuestsAPI(QuestManager questManager, ChainQuestManager chainManager) {
        this.questManager = questManager;
        this.chainManager = chainManager;
        instance = this;
    }

    /** @return the API instance, or null if plugin-quests is not loaded. */
    public static QuestsAPI getInstance() {
        return instance;
    }

    static void reset() {
        instance = null;
    }

    // ----------------------------------------------------------------
    // Public API
    // ----------------------------------------------------------------

    /**
     * Add quest progress for a player.
     * Safe to call from any plugin that soft-depends on PrisonQuests.
     *
     * @param uuid   the player's UUID
     * @param type   the quest type to advance
     * @param amount how much progress to add (must be &gt; 0)
     */
    public void addProgress(UUID uuid, QuestType type, long amount) {
        if (amount <= 0) return;
        questManager.addProgress(uuid, type, amount);
        chainManager.addProgress(uuid, type, amount);
    }
}
