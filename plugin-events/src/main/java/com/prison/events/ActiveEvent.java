package com.prison.events;

/**
 * Represents a currently-running event instance.
 *
 * @param instanceId  A unique runtime identifier for this specific run (UUID string).
 * @param config      The static configuration this event was started from.
 * @param startTimeMs Epoch milliseconds at which this event started.
 */
public record ActiveEvent(
        String instanceId,
        EventConfig config,
        long startTimeMs
) {

    /**
     * Returns how many milliseconds remain before this event expires.
     * Returns 0 if the event has already expired.
     */
    public long remainingMillis() {
        long endMs = startTimeMs + ((long) config.durationMinutes() * 60_000L);
        return Math.max(0L, endMs - System.currentTimeMillis());
    }

    /**
     * Returns remaining time rounded up to the nearest whole minute.
     */
    public int remainingMinutes() {
        long millis = remainingMillis();
        return (int) Math.ceil(millis / 60_000.0);
    }

    /**
     * Returns {@code true} if the event has passed its scheduled end time.
     */
    public boolean isExpired() {
        return remainingMillis() == 0L;
    }
}
