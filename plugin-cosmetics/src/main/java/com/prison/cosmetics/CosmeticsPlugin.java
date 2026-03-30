package com.prison.cosmetics;

import com.prison.database.DatabaseManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.logging.Level;

/**
 * CosmeticsPlugin — main class for the PrisonCosmetics plugin.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Load tag definitions from config.yml at startup.</li>
 *   <li>Ensure the {@code player_cosmetics} DB table exists.</li>
 *   <li>Listen for player join/quit to manage the data cache.</li>
 *   <li>Hook into {@link AsyncChatEvent} at HIGH priority to prepend the
 *       player's equipped tag display name to outgoing chat — this runs
 *       BEFORE plugin-chat's HIGHEST handler so the display name is already
 *       updated when chat builds its format string.</li>
 *   <li>Handle {@code /cosmetics}, {@code /tags}, {@code /wardrobe} (GUI open).</li>
 *   <li>Handle {@code /ctag give|remove &lt;player&gt; &lt;tagId&gt;} (admin).</li>
 * </ul>
 */
public class CosmeticsPlugin extends JavaPlugin implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private CosmeticsManager manager;
    private CosmeticsGUI     gui;
    private CosmeticsAPI     api;

    // ----------------------------------------------------------------
    // Lifecycle
    // ----------------------------------------------------------------

    @Override
    public void onEnable() {
        if (DatabaseManager.getInstance() == null) {
            getLogger().severe("PrisonDatabase must be loaded before PrisonCosmetics!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();

        manager = new CosmeticsManager(this, getLogger());
        loadTagDefinitions();

        ensureTable();

        gui = new CosmeticsGUI(manager, this);
        api = new CosmeticsAPI(manager);

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(gui, this);

        getLogger().info("[Cosmetics] Enabled — " + manager.getAllTagDefinitions().size() + " tags loaded.");
    }

    @Override
    public void onDisable() {
        CosmeticsAPI.reset();
        getLogger().info("[Cosmetics] Disabled.");
    }

    // ----------------------------------------------------------------
    // Config loading
    // ----------------------------------------------------------------

    /**
     * Reads the {@code chat-tags} section from config.yml and registers
     * each tag definition into {@link CosmeticsManager}.
     */
    private void loadTagDefinitions() {
        ConfigurationSection section = getConfig().getConfigurationSection("chat-tags");
        if (section == null) {
            getLogger().warning("[Cosmetics] No 'chat-tags' section found in config.yml!");
            return;
        }

        for (String id : section.getKeys(false)) {
            ConfigurationSection tagSec = section.getConfigurationSection(id);
            if (tagSec == null) continue;

            String  display     = tagSec.getString("display", "<gray>[?]");
            String  description = tagSec.getString("description", "A cosmetic tag.");
            Rarity  rarity      = Rarity.fromString(tagSec.getString("rarity", "COMMON"));

            ChatTag tag = new ChatTag(id.toLowerCase(), display, description, rarity);
            manager.registerTag(tag);
        }

        getLogger().info("[Cosmetics] Loaded " + manager.getAllTagDefinitions().size() + " chat tag definitions.");
    }

    // ----------------------------------------------------------------
    // Database table
    // ----------------------------------------------------------------

    private void ensureTable() {
        try {
            DatabaseManager.getInstance().execute(
                "CREATE TABLE IF NOT EXISTS player_cosmetics (" +
                "    player_uuid  VARCHAR(36)  NOT NULL," +
                "    cosmetic_id  VARCHAR(64)  NOT NULL," +
                "    cosmetic_type VARCHAR(32) NOT NULL DEFAULT 'CHAT_TAG'," +
                "    equipped     TINYINT      NOT NULL DEFAULT 0," +
                "    PRIMARY KEY (player_uuid, cosmetic_id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );
            getLogger().info("[Cosmetics] player_cosmetics table verified.");
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "[Cosmetics] Failed to create player_cosmetics table!", e);
        }
    }

    // ----------------------------------------------------------------
    // Events — join / quit
    // ----------------------------------------------------------------

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Load async; display name applied on main thread inside loadPlayerAsync
        manager.loadPlayerAsync(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        manager.unloadPlayer(event.getPlayer().getUniqueId());
    }

    // ----------------------------------------------------------------
    // Events — chat tag prepend (HIGH, before plugin-chat's HIGHEST)
    // ----------------------------------------------------------------

    /**
     * Runs at HIGH priority — before plugin-chat's HIGHEST handler.
     *
     * plugin-chat reads {@code player.displayName()} when building its chat
     * format string, so we must ensure the display name reflects the equipped
     * tag BEFORE plugin-chat processes the event.
     *
     * We call {@link CosmeticsManager#applyDisplayName(Player)} here to guarantee
     * the display name is current even if something changed between a tab-complete
     * and an actual message send.
     *
     * We do NOT cancel or modify the event itself — plugin-chat handles the full
     * formatted broadcast. Our only job is to keep the display name accurate.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onAsyncChat(AsyncChatEvent event) {
        // applyDisplayName is thread-safe: it only reads from ConcurrentHashMaps
        // and calls player.displayName() which is safe on Paper's async chat thread.
        manager.applyDisplayName(event.getPlayer());
    }

    // ----------------------------------------------------------------
    // Commands
    // ----------------------------------------------------------------

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String name = cmd.getName().toLowerCase();

        switch (name) {
            case "cosmetics" -> {
                return handleCosmeticsCommand(sender);
            }
            case "ctag" -> {
                return handleCTagCommand(sender, args);
            }
        }
        return false;
    }

    // ---- /cosmetics (aliases: /tags, /wardrobe) ----

    private boolean handleCosmeticsCommand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MM.deserialize("<red>This command must be run in-game."));
            return true;
        }
        gui.open(player);
        return true;
    }

    // ---- /ctag give|remove <player> <tagId> ----

    private boolean handleCTagCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("prison.admin.cosmetics")) {
            sender.sendMessage(MM.deserialize("<red>You do not have permission to use this command."));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(MM.deserialize(
                "<red>Usage: <white>/ctag <give|remove> <player> <tagId>"));
            return true;
        }

        String subCommand = args[0].toLowerCase();
        String targetName = args[1];
        String tagId      = args[2].toLowerCase();

        // Validate the tag ID
        if (manager.getTagDefinition(tagId) == null) {
            sender.sendMessage(MM.deserialize(
                "<red>Unknown tag ID: <white>" + tagId + "<red>. " +
                "Check config.yml for valid IDs."));
            return true;
        }

        // Resolve player — allow offline grant/revoke
        Player target = Bukkit.getPlayerExact(targetName);

        switch (subCommand) {
            case "give" -> {
                // For offline players we need their UUID from the DB or we refuse
                if (target == null) {
                    // Try to look up offline player
                    org.bukkit.OfflinePlayer offline = Bukkit.getOfflinePlayerIfCached(targetName);
                    if (offline == null || offline.getUniqueId() == null) {
                        sender.sendMessage(MM.deserialize(
                            "<red>Player <white>" + targetName + " <red>not found. " +
                            "They must have joined the server at least once."));
                        return true;
                    }
                    boolean granted = manager.grantTag(offline.getUniqueId(), tagId);
                    if (granted) {
                        sender.sendMessage(MM.deserialize(
                            "<green>Granted tag <white>" + tagId + " <green>to <white>" + targetName + "<green>."));
                    } else {
                        sender.sendMessage(MM.deserialize(
                            "<yellow>" + targetName + " already owns <white>" + tagId + "<yellow>."));
                    }
                } else {
                    boolean granted = manager.grantTag(target.getUniqueId(), tagId);
                    if (granted) {
                        sender.sendMessage(MM.deserialize(
                            "<green>Granted tag <white>" + tagId + " <green>to <white>" + target.getName() + "<green>."));
                        target.sendMessage(MM.deserialize(
                            "<light_purple>You received the <white>" + tagId +
                            " <light_purple>chat tag! Open <white>/cosmetics <light_purple>to equip it."));
                    } else {
                        sender.sendMessage(MM.deserialize(
                            "<yellow>" + target.getName() + " already owns <white>" + tagId + "<yellow>."));
                    }
                }
            }

            case "remove" -> {
                if (target == null) {
                    org.bukkit.OfflinePlayer offline = Bukkit.getOfflinePlayerIfCached(targetName);
                    if (offline == null || offline.getUniqueId() == null) {
                        sender.sendMessage(MM.deserialize(
                            "<red>Player <white>" + targetName + " <red>not found."));
                        return true;
                    }
                    boolean removed = manager.revokeTag(offline.getUniqueId(), tagId);
                    if (removed) {
                        sender.sendMessage(MM.deserialize(
                            "<green>Removed tag <white>" + tagId + " <green>from <white>" + targetName + "<green>."));
                    } else {
                        sender.sendMessage(MM.deserialize(
                            "<yellow>" + targetName + " does not own <white>" + tagId + "<yellow>."));
                    }
                } else {
                    boolean removed = manager.revokeTag(target.getUniqueId(), tagId);
                    if (removed) {
                        sender.sendMessage(MM.deserialize(
                            "<green>Removed tag <white>" + tagId + " <green>from <white>" + target.getName() + "<green>."));
                        target.sendMessage(MM.deserialize(
                            "<red>Your <white>" + tagId + " <red>chat tag has been removed."));
                    } else {
                        sender.sendMessage(MM.deserialize(
                            "<yellow>" + target.getName() + " does not own <white>" + tagId + "<yellow>."));
                    }
                }
            }

            default -> sender.sendMessage(MM.deserialize(
                "<red>Unknown sub-command. Use: <white>give <red>or <white>remove"));
        }

        return true;
    }
}
