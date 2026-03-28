package com.prison.chat;

import com.prison.permissions.PermissionEngine;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
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
import java.util.LinkedHashMap;
import java.util.Map;

public class ChatPlugin extends JavaPlugin implements Listener {

    private ChatConfig chatConfig;
    private final MiniMessage mm = MiniMessage.miniMessage();

    @Override
    public void onEnable() {
        if (PermissionEngine.getInstance() == null) {
            getLogger().severe("PrisonPermissions must be loaded before PrisonChat!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();
        chatConfig = loadChatConfig();

        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("Chat formatting system enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Chat formatting system disabled.");
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
    // Tablist Events
    // ----------------------------------------------------------------

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Delay 1 tick to ensure the permission cache is loaded before we read it
        getServer().getScheduler().runTaskLater(this, () -> {
            updateTablistName(event.getPlayer());
            refreshTablistHeaderFooter();
        }, 1L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Refresh online count in header/footer for remaining players
        getServer().getScheduler().runTaskLater(this, this::refreshTablistHeaderFooter, 1L);
    }

    // ----------------------------------------------------------------
    // Chat Line Building
    // ----------------------------------------------------------------

    /**
     * Builds the full formatted chat line for a player's message.
     *
     * Each prefix token is replaced with "prefix + separator" when the player
     * has that rank, or "" when they don't. This means no separator cleanup is
     * needed — a player with only a mine rank gets "[A] Name: message", a
     * player with no ranks gets "Name: message".
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
    //
    // Each method returns the formatted prefix with a trailing separator
    // appended, or "" if the player has no rank in that tier.
    // This trailing separator approach means tokens collapse naturally
    // when adjacent tokens are empty — no post-processing needed.
    // ----------------------------------------------------------------

    private String prestigePrefix(Player player) {
        int prestige = PermissionEngine.getInstance().getPrestige(player.getUniqueId());
        if (prestige <= 0) return "";
        String prefix = "<dark_purple>[<light_purple>P" + prestige + "</light_purple>]</dark_purple>";
        return prefix + chatConfig.prefixSeparator();
    }

    private String donorPrefix(Player player) {
        String rank = PermissionEngine.getInstance().getDonorRank(player.getUniqueId());
        if (rank == null) return "";

        Map<String, String> overrides = chatConfig.donorPrefixOverrides();
        String prefix = overrides.containsKey(rank.toLowerCase())
            ? overrides.get(rank.toLowerCase())
            : "<yellow>[" + formatDonorDisplay(rank) + "]</yellow>";
        return prefix + chatConfig.prefixSeparator();
    }

    private String minePrefix(Player player) {
        String rank = PermissionEngine.getInstance().getMineRank(player.getUniqueId());
        if (rank == null || rank.isEmpty()) return "";

        Map<String, String> overrides = chatConfig.minePrefixOverrides();
        String prefix = overrides.containsKey(rank.toUpperCase())
            ? overrides.get(rank.toUpperCase())
            : "<gray>[<white>" + rank.toUpperCase() + "</white>]</gray>";
        return prefix + chatConfig.prefixSeparator();
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

            String chatFormat    = (String) root.getOrDefault("chat-format",
                "{prestige}{donor}{mine}{staff}<white>{name}</white><dark_gray>: </dark_gray><gray>{message}");
            String tablistFormat = (String) root.getOrDefault("tablist-format",
                "{prestige}{donor}{mine}{name}");
            String tablistHeader = (String) root.getOrDefault("tablist-header",
                "<gold><bold>PRISON</bold></gold>\\n<gray>{online} players online");
            String tablistFooter = (String) root.getOrDefault("tablist-footer",
                "<dark_gray>Store • Discord • Forums");
            String prefixSep     = (String) root.getOrDefault("prefix-separator", " ");

            // YAML stores literal \n — convert to real newlines for Adventure
            tablistHeader = tablistHeader.replace("\\n", "\n");
            tablistFooter = tablistFooter.replace("\\n", "\n");

            Map<String, String> mineOverrides  = extractStringMap(root, "mine-prefix-overrides");
            Map<String, String> donorOverrides  = extractStringMap(root, "donor-prefix-overrides");
            Map<String, String> staffOverrides  = extractStringMap(root, "staff-prefix-overrides");

            getLogger().info("[Chat] Loaded config. Format: " + chatFormat);
            return new ChatConfig(chatFormat, tablistFormat, tablistHeader, tablistFooter,
                prefixSep, mineOverrides, donorOverrides, staffOverrides);

        } catch (Exception e) {
            getLogger().severe("[Chat] Failed to load config: " + e.getMessage() + " — using defaults.");
            return ChatConfig.defaults();
        }
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

    /** "donorplus" → "Donor+" for default donor prefix display. */
    private String formatDonorDisplay(String rank) {
        return capitalize(rank.replace("plus", "+"));
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }
}
