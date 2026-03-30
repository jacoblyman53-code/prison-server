package com.prison.events;

/**
 * Immutable configuration for a named event as read from config.yml.
 *
 * @param id              The config key used to identify this event (e.g. "sell_boost_2x").
 * @param type            The category of boost this event applies.
 * @param multiplier      The reward multiplier (e.g. 2.0 for double rewards).
 * @param durationMinutes How long the event runs before it automatically ends.
 * @param displayName     MiniMessage string used in GUIs and action-bar displays.
 * @param announcement    MiniMessage string broadcast to all players when the event starts.
 */
public record EventConfig(
        String id,
        EventType type,
        double multiplier,
        int durationMinutes,
        String displayName,
        String announcement
) {}
