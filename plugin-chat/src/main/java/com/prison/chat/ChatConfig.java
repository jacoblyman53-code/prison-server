package com.prison.chat;

import java.util.List;
import java.util.Map;

/**
 * ChatConfig — immutable snapshot of config.yml values.
 *
 * Extended to include join-sequence, sidebar, and auto-announcer settings.
 */
public record ChatConfig(
    // ---- Existing chat / tablist ----
    String chatFormat,
    String tablistFormat,
    String tablistHeader,
    String tablistFooter,
    String prefixSeparator,
    Map<String, String> minePrefixOverrides,
    Map<String, String> donorPrefixOverrides,
    Map<String, String> staffPrefixOverrides,

    // ---- Join sequence ----
    String joinTitleMain,
    String joinTitleSub,
    String joinWelcomeHeader,
    String joinWelcomeBody,
    String joinAnnouncement,
    String quitAnnouncement,
    boolean joinSoundEnabled,

    // ---- Sidebar ----
    boolean sidebarEnabled,
    String  sidebarTitle,
    String  sidebarDivider,
    String  sidebarServerIp,

    // ---- Auto-Announcer ----
    boolean        announcerEnabled,
    int            announcerIntervalSeconds,
    String         announcerPrefix,
    List<String>   announcerMessages
) {

    /** Fallback used if config.yml fails to load. */
    public static ChatConfig defaults() {
        return new ChatConfig(
            // chat / tablist
            "{prestige}{donor}{mine}{staff}<white>{name}</white><dark_gray>: </dark_gray><gray>{message}",
            "{prestige}{donor}{mine}{name}",
            "<gradient:#FFD700:#FFA500><bold>⛏ PRISON ⛏</bold></gradient>\n" +
                "<gray>{online}<dark_gray>/<gray>{max} <white>players online",
            "<dark_gray>▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬\n" +
                "<gold>play.server.com  <dark_gray>|  <aqua>discord.gg/server",
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
            ),

            // join sequence
            "<gradient:#FFD700:#FFA500><bold>⛏ PRISON ⛏</bold></gradient>",
            "<gray>Welcome back, <white>{name}</white>!</gray>",
            "<dark_gray>▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
            "<gold> Welcome to <white>Prison</white>! <gray>Rank up and dominate the mines.",
            "<dark_gray>[<green>+</green><dark_gray>] <gray>{rank} <white>{name}</white> <gray>joined the server.",
            "<dark_gray>[<red>-</red><dark_gray>] <gray>{rank} <white>{name}</white> <gray>left the server.",
            true,

            // sidebar
            true,
            "<gradient:#FFD700:#FFA500><bold>⛏ PRISON ⛏</bold></gradient>",
            "<dark_gray>▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
            "play.server.com",

            // announcer
            true,
            120,
            "\n<dark_gray>[<gold><bold>!</bold></gold><dark_gray>] <gold>",
            List.of(
                "Use <white>/rankup</white> to advance to the next mine rank!",
                "Earn <white>tokens</white> by mining and spend them on pickaxe enchants.",
                "Join our Discord at <aqua>discord.gg/server</aqua> for updates!",
                "Type <white>/kit starter</white> to claim your free starter kit.",
                "Prestige with <white>/prestige</white> once you reach rank <white>Z</white>!"
            )
        );
    }
}
