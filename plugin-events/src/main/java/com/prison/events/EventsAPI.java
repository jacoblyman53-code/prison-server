package com.prison.events;

import java.util.List;

/**
 * Public API singleton that other plugins query for active event multipliers.
 *
 * <p>Usage:
 * <pre>{@code
 * double sellMult = EventsAPI.getInstance().getSellMultiplier();
 * double tokMult  = EventsAPI.getInstance().getTokenMultiplier();
 * }</pre>
 *
 * The instance is set by {@link EventPlugin} during {@code onEnable} and cleared
 * during {@code onDisable}. External plugins should guard against {@code null}
 * if they soft-depend on PrisonEvents.
 */
public class EventsAPI {

    private static EventsAPI instance;

    private final EventManager manager;

    EventsAPI(EventManager manager) {
        this.manager = manager;
    }

    // -------------------------------------------------------------------------
    // Internal lifecycle (package-private)
    // -------------------------------------------------------------------------

    static void init(EventManager manager) {
        instance = new EventsAPI(manager);
    }

    static void shutdown() {
        instance = null;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Returns the singleton, or {@code null} if PrisonEvents is not loaded.
     */
    public static EventsAPI getInstance() {
        return instance;
    }

    /**
     * Returns the combined sell multiplier — the product of every active
     * {@link EventType#SELL_BOOST} and {@link EventType#JACKPOT_HOUR} event.
     * Returns {@code 1.0} when no such event is running.
     */
    public double getSellMultiplier() {
        double result = 1.0;
        for (ActiveEvent event : manager.getActiveEvents()) {
            EventType t = event.config().type();
            if (t == EventType.SELL_BOOST || t == EventType.JACKPOT_HOUR) {
                result *= event.config().multiplier();
            }
        }
        return result;
    }

    /**
     * Returns the combined token multiplier — the product of every active
     * {@link EventType#TOKEN_STORM} event.
     * Returns {@code 1.0} when no such event is running.
     */
    public double getTokenMultiplier() {
        double result = 1.0;
        for (ActiveEvent event : manager.getActiveEvents()) {
            if (event.config().type() == EventType.TOKEN_STORM) {
                result *= event.config().multiplier();
            }
        }
        return result;
    }

    /**
     * Returns {@code true} if at least one event is currently active.
     */
    public boolean hasActiveEvent() {
        return !manager.getActiveEvents().isEmpty();
    }

    /**
     * Returns an unmodifiable snapshot of all currently active events.
     */
    public List<ActiveEvent> getActiveEvents() {
        return List.copyOf(manager.getActiveEvents());
    }

    /**
     * Starts the configured event with the given id.
     * Does nothing if the id is not found in config.
     *
     * @param eventId The config key of the event to start (e.g. "sell_boost_2x").
     */
    public void startEvent(String eventId) {
        manager.startEvent(eventId);
    }

    /**
     * Force-stops the active event with the given config id.
     * Does nothing if no event with that id is running.
     *
     * @param eventId The config key of the event to stop.
     */
    public void stopEvent(String eventId) {
        manager.stopEvent(eventId);
    }
}
