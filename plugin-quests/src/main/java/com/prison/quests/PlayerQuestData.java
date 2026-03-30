package com.prison.quests;

import java.util.concurrent.atomic.AtomicLong;

/**
 * PlayerQuestData — mutable per-player state for a single quest.
 *
 * Holds the current progress, whether it has been completed (reward claimed),
 * and the epoch-second timestamp of the last reset. All progress mutations
 * are done with an AtomicLong so concurrent event handlers don't lose counts.
 */
public final class PlayerQuestData {

    private final String questId;
    private final AtomicLong progress;
    private volatile boolean completed;

    /**
     * The epoch-second at which this quest's progress was last reset.
     * For MILESTONE quests this is always 0 and never changes.
     */
    private volatile long lastReset;

    public PlayerQuestData(String questId, long progress, boolean completed, long lastReset) {
        this.questId   = questId;
        this.progress  = new AtomicLong(progress);
        this.completed = completed;
        this.lastReset = lastReset;
    }

    // ----------------------------------------------------------------
    // Accessors
    // ----------------------------------------------------------------

    public String  getQuestId()  { return questId; }
    public long    getProgress() { return progress.get(); }
    public boolean isCompleted() { return completed; }
    public long    getLastReset() { return lastReset; }

    // ----------------------------------------------------------------
    // Mutations
    // ----------------------------------------------------------------

    /**
     * Add delta to progress. Returns the new progress value.
     * Does NOT cap at the goal — that check lives in QuestManager.
     */
    public long addProgress(long delta) {
        return progress.addAndGet(delta);
    }

    /** Mark the quest as completed (reward delivered). */
    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    /**
     * Reset this quest: zero the progress, un-complete, record the reset time.
     * Called when QuestManager determines the reset interval has elapsed.
     *
     * @param nowEpochSeconds current wall-clock time in epoch seconds
     */
    public void reset(long nowEpochSeconds) {
        progress.set(0L);
        completed  = false;
        lastReset  = nowEpochSeconds;
    }

    /**
     * Determine whether this quest needs a reset given the definition's reset interval.
     *
     * For MILESTONE quests the interval is Long.MAX_VALUE, so this always returns false.
     *
     * @param def              the quest definition (contains the interval)
     * @param nowEpochSeconds  current wall-clock epoch seconds
     */
    public boolean needsReset(QuestDefinition def, long nowEpochSeconds) {
        if (!def.getTier().isResettable()) return false;
        long elapsed = nowEpochSeconds - lastReset;
        return elapsed >= def.getTier().getResetIntervalSeconds();
    }

    /**
     * Seconds remaining until the next reset. Returns 0 if a reset is already due.
     * Returns Long.MAX_VALUE for MILESTONE quests.
     */
    public long secondsUntilReset(QuestDefinition def, long nowEpochSeconds) {
        if (!def.getTier().isResettable()) return Long.MAX_VALUE;
        long elapsed  = nowEpochSeconds - lastReset;
        long interval = def.getTier().getResetIntervalSeconds();
        long remaining = interval - elapsed;
        return Math.max(0L, remaining);
    }
}
