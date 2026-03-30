package com.prison.quests;

/**
 * QuestTier — controls how (and whether) quest progress resets over time.
 *
 * DAILY     — resets every 86,400 seconds (24 hours)
 * WEEKLY    — resets every 604,800 seconds (7 days)
 * MILESTONE — never resets; completed once per player lifetime
 */
public enum QuestTier {

    DAILY(86_400L),
    WEEKLY(604_800L),
    MILESTONE(Long.MAX_VALUE);   // sentinel: no reset

    private final long resetIntervalSeconds;

    QuestTier(long resetIntervalSeconds) {
        this.resetIntervalSeconds = resetIntervalSeconds;
    }

    /** Seconds between resets. MILESTONE returns Long.MAX_VALUE — treat as "never". */
    public long getResetIntervalSeconds() {
        return resetIntervalSeconds;
    }

    /** True if this tier resets periodically (i.e. not a milestone). */
    public boolean isResettable() {
        return this != MILESTONE;
    }
}
