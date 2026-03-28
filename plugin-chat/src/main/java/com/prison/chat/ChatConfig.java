package com.prison.chat;

import java.util.Map;

/**
 * ChatConfig — immutable snapshot of config.yml values.
 */
public record ChatConfig(
    String chatFormat,
    String tablistFormat,
    String tablistHeader,
    String tablistFooter,
    String prefixSeparator,
    Map<String, String> minePrefixOverrides,
    Map<String, String> donorPrefixOverrides,
    Map<String, String> staffPrefixOverrides
) {

    /** Fallback used if config.yml fails to load. */
    public static ChatConfig defaults() {
        return new ChatConfig(
            "{prestige}{donor}{mine}{staff}<white>{name}</white><dark_gray>: </dark_gray><gray>{message}",
            "{prestige}{donor}{mine}{name}",
            "<gold><bold>PRISON</bold></gold>\n<gray>{online} players online",
            "<dark_gray>Store • Discord • Forums",
            " ",
            Map.of(),
            Map.of(),
            Map.of(
                "helper",      "<dark_green>[Helper]</dark_green>",
                "moderator",   "<green>[Mod]</green>",
                "seniormod",   "<green>[Sr.Mod]</green>",
                "admin",       "<red>[Admin]</red>",
                "senioradmin", "<dark_red>[Sr.Admin]</dark_red>",
                "owner",       "<dark_red>[Owner]</dark_red>"
            )
        );
    }
}
