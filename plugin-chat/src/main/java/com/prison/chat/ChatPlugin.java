package com.prison.chat;

import com.prison.economy.EconomyAPI;
import com.prison.permissions.PermissionEngine;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ChatPlugin extends JavaPlugin implements Listener {

    private ChatConfig chatConfig;
    private SidebarManager sidebarManager;
    private AutoAnnouncer autoAnnouncer;

    private final MiniMessage mm = MiniMessage.miniMessage();

    // ----------------------------------------------------------------
    // Lifecycle
    // ----------------------------------------------------------------

    @Override
    public void onEnable() {
        if (PermissionEngine.getInstance() == null) {
            getLogger().severe("PrisonPermissions must be loaded before PrisonChat!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();
        chatConfig = loadChatConfig();

        sidebarManager = new SidebarManager(this);
        autoAnnouncer  = new AutoAnnouncer(this);

        getServer().getPluginManager().registerEvents(this, this);

        if (chatConfig.announcerEnabled()) {
            autoAnnouncer.start();
        }

        // Start repeating scoreboard refresh (every 40 ticks = 2 s)
        if (chatConfig.sidebarEnabled()) {
            getServer().getScheduler().runTaskTimer(this, () -> {
                for (Player p : getServer().getOnlinePlayers()) {
                    sidebarManager.updateBoard(p);
                }
            }, 40L, 40L);
        }

        getLogger().info("Chat + premium experience system enabled.");
    }

    @Override
    public void onDisable() {
        if (autoAnnouncer != null) {
            autoAnnouncer.stop();
        }
        getLogger().info("Chat formatting system disabled.");
    }

    // ----------------------------------------------------------------
    // Accessor — used by SidebarManager and AutoAnnouncer
    // ----------------------------------------------------------------

    public ChatConfig getChatConfig() {
        return chatConfig;
    }

    // ----------------------------------------------------------------
    // Chat Event
    // ----------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncChatEvent event) {
        event.setCancelled(true);

        Player player = event.getPlayer();

        // Strip to plain text — prevents players from injecting MiniMessage tags
        String rawMessage = PlainTextComponentSerializer.plainText().serialize(event.message());
        // Escape any remaining tag characters so they display literally
        String safeMessage = mm.escapeTags(rawMessage);

        String formatted = buildChatLine(player, safeMessage);
        Component component = mm.deserialize(formatted);

        // Broadcast to all players (Adventure sendMessage is thread-safe on Paper)
        for (Player online : getServer().getOnlinePlayers()) {
            online.sendMessage(component);
        }
    }

    // ----------------------------------------------------------------
    // Join / Quit Events
    // ----------------------------------------------------------------

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Capture first-join state before the playerdata file is written
        boolean isFirstJoin = !player.hasPlayedBefore();

        // Suppress the default Bukkit join message — we send our own
        event.joinMessage(null);

        // Announce to all OTHER players immediately (they're already online)
        String mineRank = PermissionEngine.getInstance().getMineRank(player.getUniqueId());
        String rankLabel = formatRankLabel(mineRank);

        String joinMsg = chatConfig.joinAnnouncement()
            .replace("{name}", player.getName())
            .replace("{rank}", rankLabel);
        Component joinComponent = mm.deserialize(joinMsg);

        for (Player other : getServer().getOnlinePlayers()) {
            if (!other.getUniqueId().equals(player.getUniqueId())) {
                other.sendMessage(joinComponent);
            }
        }

        // Delay 2 ticks: permission cache is guaranteed warm, economy wallet loaded
        getServer().getScheduler().runTaskLater(this, () -> {
            // 1. Tablist name
            updateTablistName(player);
            refreshTablistHeaderFooter();

            // 2. Sidebar
            if (chatConfig.sidebarEnabled()) {
                sidebarManager.buildBoard(player);
            }

            // 3. Title screen — first-join players see a different subtitle
            String titleMain = chatConfig.joinTitleMain();
            String subTemplate = isFirstJoin
                ? chatConfig.firstJoinTitleSub()
                : chatConfig.joinTitleSub();
            String titleSub = subTemplate
                .replace("{name}", player.getName())
                .replace("{rank}", rankLabel);

            Title title = Title.title(
                mm.deserialize(titleMain),
                mm.deserialize(titleSub),
                Title.Times.times(
                    Duration.ofMillis(500),   // fade in
                    Duration.ofSeconds(3),    // stay
                    Duration.ofMillis(500)    // fade out
                )
            );
            player.showTitle(title);

            // 4. Welcome message in chat
            String header = chatConfig.joinWelcomeHeader();
            String body   = chatConfig.joinWelcomeBody()
                .replace("{name}", player.getName())
                .replace("{rank}", rankLabel);

            player.sendMessage(mm.deserialize(header));
            player.sendMessage(mm.deserialize(body));
            player.sendMessage(mm.deserialize(header));

            // 5. First-join tips panel (click to run commands)
            if (isFirstJoin) {
                player.sendMessage(mm.deserialize(
                    "\n<gold><bold>⛏ Getting Started</bold></gold>\n" +
                    "<dark_gray>▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬\n" +
                    " <gray>You start at Mine <white><bold>A</bold></white><gray>. Mine blocks to earn IGC!\n" +
                    " <dark_gray>▪ <click:run_command:/kit starter><white>/kit starter</white></click>" +
                        " <gray>— Claim your free starter kit\n" +
                    " <dark_gray>▪ <click:run_command:/mines><white>/mines</white></click>" +
                        " <gray>— View and teleport to mines\n" +
                    " <dark_gray>▪ <click:run_command:/rankup><white>/rankup</white></click>" +
                        " <gray>— Rank up when you have enough IGC\n" +
                    " <dark_gray>▪ <click:run_command:/sell><white>/sell</white></click>" +
                        " <gray>— Sell held items for IGC\n" +
                    " <dark_gray>▪ <click:suggest_command:/pay ><white>/pay <player> <amount></white></click>" +
                        " <gray>— Send IGC to another player\n" +
                    "<dark_gray>▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
            }

            // 6. Join sound
            if (chatConfig.joinSoundEnabled()) {
                player.playSound(
                    Sound.sound(
                        org.bukkit.Sound.ENTITY_PLAYER_LEVELUP,
                        Sound.Source.MASTER,
                        1.0f,
                        1.0f
                    )
                );
            }
        }, 2L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Suppress the default quit message — we send our own
        event.quitMessage(null);

        String mineRank  = PermissionEngine.getInstance().getMineRank(player.getUniqueId());
        String rankLabel = formatRankLabel(mineRank);

        String quitMsg = chatConfig.quitAnnouncement()
            .replace("{name}", player.getName())
            .replace("{rank}", rankLabel);
        Component quitComponent = mm.deserialize(quitMsg);

        for (Player other : getServer().getOnlinePlayers()) {
            // player is still technically "online" during PlayerQuitEvent
            if (!other.getUniqueId().equals(player.getUniqueId())) {
                other.sendMessage(quitComponent);
            }
        }

        // Refresh remaining players' tablist counts after the player fully disconnects
        getServer().getScheduler().runTaskLater(this, this::refreshTablistHeaderFooter, 1L);
    }

    // ----------------------------------------------------------------
    // Chat Line Building
    // ----------------------------------------------------------------

    /**
     * Builds the full formatted chat line for a player's message.
     */
    private String buildChatLine(Player player, String safeMessage) {
        String format = chatConfig.chatFormat();
        format = format.replace("{prestige}", prestigePrefix(player));
        format = format.replace("{donor}",    donorPrefix(player));
        format = format.replace("{mine}",     minePrefix(player));
        format = format.replace("{staff}",    staffPrefix(player));
        format = format.replace("{name}",     player.getName());
        format = format.replace("{message}",  safeMessage);
        return format;
    }

    private String buildTablistName(Player player) {
        String format = chatConfig.tablistFormat();
        format = format.replace("{prestige}", prestigePrefix(player));
        format = format.replace("{donor}",    donorPrefix(player));
        format = format.replace("{mine}",     minePrefix(player));
        format = format.replace("{staff}",    staffPrefix(player));
        format = format.replace("{name}",     player.getName());
        return format;
    }

    // ----------------------------------------------------------------
    // Per-tier Prefix Builders
    // ----------------------------------------------------------------

    private String prestigePrefix(Player player) {
        int prestige = PermissionEngine.getInstance().getPrestige(player.getUniqueId());
        if (prestige <= 0) return "";
        // Show as colored number + bullet separator — mine rank follows directly
        // e.g. "P3 • A" — same pattern as Complex Gaming's "VI • A"
        String color = prestigeColor(prestige);
        return "<" + color + "><bold>P" + prestige + "</bold></" + color + "><dark_gray> • </dark_gray>";
    }

    private String donorPrefix(Player player) {
        String rank = PermissionEngine.getInstance().getDonorRank(player.getUniqueId());
        if (rank == null) return "";

        Map<String, String> overrides = chatConfig.donorPrefixOverrides();
        if (overrides.containsKey(rank.toLowerCase())) {
            return overrides.get(rank.toLowerCase()) + chatConfig.prefixSeparator();
        }

        String prefix = switch (rank.toLowerCase()) {
            case "donor"     -> "<gold>[<yellow>Donor</yellow>]</gold>";
            case "donorplus" -> "<gold>[<yellow>Donor<gold>+</gold>]</gold>";
            case "elite"     -> "<aqua>[<white>Elite</white>]</aqua>";
            case "eliteplus" -> "<light_purple>[<white>Elite<light_purple>+</light_purple>]</light_purple>";
            default          -> "<yellow>[" + formatDonorDisplay(rank) + "]</yellow>";
        };
        return prefix + chatConfig.prefixSeparator();
    }

    private String minePrefix(Player player) {
        String rank = PermissionEngine.getInstance().getMineRank(player.getUniqueId());
        if (rank == null || rank.isEmpty()) return "";

        Map<String, String> overrides = chatConfig.minePrefixOverrides();
        if (overrides.containsKey(rank.toUpperCase())) {
            return overrides.get(rank.toUpperCase()) + chatConfig.prefixSeparator();
        }

        // Color the rank letter by progression tier — no brackets, just the letter
        String color = rankTierColor(rank);
        return "<" + color + "><bold>" + rank.toUpperCase() + "</bold></" + color + ">"
            + chatConfig.prefixSeparator();
    }

    private String staffPrefix(Player player) {
        String rank = PermissionEngine.getInstance().getStaffRank(player.getUniqueId());
        if (rank == null) return "";

        Map<String, String> overrides = chatConfig.staffPrefixOverrides();
        String prefix = overrides.containsKey(rank.toLowerCase())
            ? overrides.get(rank.toLowerCase())
            : "<red>[" + capitalize(rank) + "]</red>";
        return prefix + chatConfig.prefixSeparator();
    }

    /** Returns a MiniMessage color tag name based on mine rank tier (A-Z progression). */
    private static String rankTierColor(String rank) {
        if (rank == null || rank.isEmpty()) return "gray";
        char c = Character.toUpperCase(rank.charAt(0));
        if (c <= 'F') return "gray";
        if (c <= 'L') return "yellow";
        if (c <= 'R') return "gold";
        if (c <= 'V') return "red";
        return "dark_red";
    }

    /** Returns a MiniMessage color tag name based on prestige level. */
    private static String prestigeColor(int level) {
        return switch (level) {
            case 1  -> "light_purple";
            case 2  -> "aqua";
            case 3  -> "green";
            case 4  -> "gold";
            case 5  -> "red";
            default -> "white";
        };
    }

    // ----------------------------------------------------------------
    // Tablist Helpers
    // ----------------------------------------------------------------

    private void updateTablistName(Player player) {
        Component name = mm.deserialize(buildTablistName(player));
        player.playerListName(name);
    }

    private void refreshTablistHeaderFooter() {
        int online = getServer().getOnlinePlayers().size();
        int max    = getServer().getMaxPlayers();

        Component header = mm.deserialize(chatConfig.tablistHeader()
            .replace("{online}", String.valueOf(online))
            .replace("{max}",    String.valueOf(max)));

        Component footer = mm.deserialize(chatConfig.tablistFooter()
            .replace("{online}", String.valueOf(online))
            .replace("{max}",    String.valueOf(max)));

        for (Player p : getServer().getOnlinePlayers()) {
            p.sendPlayerListHeaderAndFooter(header, footer);
        }
    }

    // ----------------------------------------------------------------
    // Config Loading
    // ----------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private ChatConfig loadChatConfig() {
        File configFile = new File(getDataFolder(), "config.yml");
        try (FileInputStream fis = new FileInputStream(configFile)) {
            Map<String, Object> root = new Yaml().load(fis);

            // ---- Existing fields ----
            String chatFormat    = str(root, "chat-format",
                "{prestige}{donor}{mine}{staff}<white>{name}</white><dark_gray>: </dark_gray><gray>{message}");
            String tablistFormat = str(root, "tablist-format", "{prestige}{donor}{mine}{name}");
            String tablistHeader = str(root, "tablist-header",
                "<gradient:#FFD700:#FFA500><bold>⛏ PRISON ⛏</bold></gradient>\\n" +
                "<gray>{online}<dark_gray>/<gray>{max} <white>players online");
            String tablistFooter = str(root, "tablist-footer",
                "<dark_gray>▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬\\n" +
                "<gold>play.server.com  <dark_gray>|  <aqua>discord.gg/server");
            String prefixSep     = str(root, "prefix-separator", " ");

            // YAML stores literal \n — convert to real newlines for Adventure
            tablistHeader = tablistHeader.replace("\\n", "\n");
            tablistFooter = tablistFooter.replace("\\n", "\n");

            Map<String, String> mineOverrides  = extractStringMap(root, "mine-prefix-overrides");
            Map<String, String> donorOverrides  = extractStringMap(root, "donor-prefix-overrides");
            Map<String, String> staffOverrides  = extractStringMap(root, "staff-prefix-overrides");

            // ---- Join sequence ----
            Map<String, Object> joinSec = section(root, "join-sequence");
            String joinTitleMain    = str(joinSec, "title-main",
                "<gradient:#FFD700:#FFA500><bold>⛏ PRISON ⛏</bold></gradient>");
            String joinTitleSub     = str(joinSec, "title-sub",
                "<gray>Welcome back, <white>{name}</white>!</gray>");
            String firstJoinTitleSub = str(joinSec, "first-join-title-sub",
                "<gray>Welcome to <white>Prison</white>, <white>{name}</white>!</gray>");
            String joinWelcomeHdr   = str(joinSec, "welcome-header",
                "<dark_gray>▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            String joinWelcomeBody  = str(joinSec, "welcome-body",
                "<gold> Welcome to <white>Prison</white>! <gray>Rank up and dominate the mines.");
            String joinAnnounce     = str(joinSec, "join-announcement",
                "<dark_gray>[<green>+</green><dark_gray>] <gray>{rank} <white>{name}</white> <gray>joined the server.");
            String quitAnnounce     = str(joinSec, "quit-announcement",
                "<dark_gray>[<red>-</red><dark_gray>] <gray>{rank} <white>{name}</white> <gray>left the server.");
            boolean joinSound       = bool(joinSec, "sound-enabled", true);

            // ---- Sidebar ----
            Map<String, Object> sidebarSec = section(root, "sidebar");
            boolean sidebarEnabled  = bool(sidebarSec, "enabled", true);
            String  sidebarTitle    = str(sidebarSec, "title",
                "<gradient:#FFD700:#FFA500><bold>⛏ PRISON ⛏</bold></gradient>");
            String  sidebarDivider  = str(sidebarSec, "divider",
                "<dark_gray>▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            String  sidebarIp       = str(sidebarSec, "server-ip", "play.server.com");

            // ---- Announcer ----
            Map<String, Object> announceSec = section(root, "announcer");
            boolean announcerEnabled   = bool(announceSec, "enabled", true);
            int     announcerInterval  = intVal(announceSec, "interval-seconds", 120);
            String  announcerPrefix    = str(announceSec, "prefix",
                "\n<dark_gray>[<gold><bold>!</bold></gold><dark_gray>] <gold>");
            List<String> announcerMsgs = stringList(announceSec, "messages");

            getLogger().info("[Chat] Loaded config. Format: " + chatFormat);

            return new ChatConfig(
                chatFormat, tablistFormat, tablistHeader, tablistFooter, prefixSep,
                mineOverrides, donorOverrides, staffOverrides,
                joinTitleMain, joinTitleSub, firstJoinTitleSub, joinWelcomeHdr, joinWelcomeBody,
                joinAnnounce, quitAnnounce, joinSound,
                sidebarEnabled, sidebarTitle, sidebarDivider, sidebarIp,
                announcerEnabled, announcerInterval, announcerPrefix, announcerMsgs
            );

        } catch (Exception e) {
            getLogger().severe("[Chat] Failed to load config: " + e.getMessage() + " — using defaults.");
            return ChatConfig.defaults();
        }
    }

    // ---- Config helper methods ----

    @SuppressWarnings("unchecked")
    private Map<String, Object> section(Map<String, Object> root, String key) {
        Object val = root.get(key);
        if (val instanceof Map) return (Map<String, Object>) val;
        return Map.of();
    }

    private String str(Map<String, Object> map, String key, String def) {
        Object v = map.get(key);
        return (v instanceof String s) ? s : def;
    }

    private boolean bool(Map<String, Object> map, String key, boolean def) {
        Object v = map.get(key);
        return (v instanceof Boolean b) ? b : def;
    }

    private int intVal(Map<String, Object> map, String key, int def) {
        Object v = map.get(key);
        if (v instanceof Integer i) return i;
        if (v instanceof Number n)  return n.intValue();
        return def;
    }

    @SuppressWarnings("unchecked")
    private List<String> stringList(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (!(val instanceof List<?> raw)) return new ArrayList<>();
        List<String> result = new ArrayList<>();
        for (Object item : raw) {
            if (item instanceof String s) result.add(s);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> extractStringMap(Map<String, Object> root, String key) {
        Object val = root.get(key);
        if (!(val instanceof Map)) return Map.of();

        Map<String, Object> raw = (Map<String, Object>) val;
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : raw.entrySet()) {
            if (entry.getValue() instanceof String s) {
                result.put(entry.getKey(), s);
            }
        }
        return result;
    }

    // ----------------------------------------------------------------
    // Utilities
    // ----------------------------------------------------------------

    /**
     * Formats a mine rank string into a bracketed label for announcements.
     * Returns empty string if rank is null/empty.
     * e.g. "A" → "[A]", null → ""
     */
    private String formatRankLabel(String mineRank) {
        if (mineRank == null || mineRank.isBlank()) return "";
        return "<gray>[<white>" + mineRank.toUpperCase() + "</white>]</gray>";
    }

    /** "donorplus" → "Donor+" for default donor prefix display. */
    private String formatDonorDisplay(String rank) {
        return capitalize(rank.replace("plus", "+"));
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }
}
