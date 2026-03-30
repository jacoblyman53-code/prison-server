package com.prison.quests;

import com.prison.database.DatabaseManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.*;

/**
 * QuestPlugin — main class for the PrisonQuests plugin.
 *
 * Wires together config loading, DB schema creation, event listening,
 * command handling, and GUI interaction.
 */
public class QuestPlugin extends JavaPlugin implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private QuestManager      questManager;
    private QuestGUI          questGUI;
    private ChainQuestManager chainQuestManager;

    // Track which tier each player currently has open in the GUI.
    // Used to handle tab-click re-opens with the correct tab selected.
    private final Map<UUID, QuestTier> openGuiTab = new HashMap<>();

    // ----------------------------------------------------------------
    // Lifecycle
    // ----------------------------------------------------------------

    @Override
    public void onEnable() {
        saveDefaultConfig();

        ensureTables();

        Map<String, QuestDefinition> definitions = loadDefinitionsFromConfig();
        questManager      = new QuestManager(definitions, getLogger(), this);
        questGUI          = new QuestGUI(questManager);
        chainQuestManager = ChainQuestManager.initialize(this);
        try { chainQuestManager.ensureTable(); }
        catch (SQLException e) { getLogger().severe("[Quests] Failed to create chain_quests table: " + e.getMessage()); }

        new QuestsAPI(questManager, chainQuestManager);

        // Schedule a periodic online-time flush every 5 minutes for players who
        // are online for a very long time (so quests update without needing a relog).
        Bukkit.getScheduler().runTaskTimer(this, this::flushOnlineTime, 20L * 60 * 5, 20L * 60 * 5);

        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("[Quests] Loaded " + definitions.size() + " quest(s).");
    }

    @Override
    public void onDisable() {
        // Save all online players before shutdown
        for (Player player : Bukkit.getOnlinePlayers()) {
            questManager.saveAndUnload(player.getUniqueId());
        }
        QuestsAPI.reset();
        getLogger().info("[Quests] All quest data saved. Goodbye.");
    }

    // ----------------------------------------------------------------
    // DB Schema
    // ----------------------------------------------------------------

    private void ensureTables() {
        try {
            DatabaseManager.getInstance().execute(
                "CREATE TABLE IF NOT EXISTS player_quests (" +
                "  id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                "  player_uuid VARCHAR(36)  NOT NULL," +
                "  quest_id    VARCHAR(64)  NOT NULL," +
                "  progress    BIGINT       NOT NULL DEFAULT 0," +
                "  completed   TINYINT      NOT NULL DEFAULT 0," +
                "  last_reset  BIGINT       NOT NULL DEFAULT 0," +
                "  UNIQUE KEY uk_player_quest (player_uuid, quest_id)" +
                ")"
            );
        } catch (SQLException e) {
            getLogger().severe("[Quests] Failed to create player_quests table: " + e.getMessage());
        }
    }

    // ----------------------------------------------------------------
    // Config Loading
    // ----------------------------------------------------------------

    private Map<String, QuestDefinition> loadDefinitionsFromConfig() {
        Map<String, QuestDefinition> map = new LinkedHashMap<>();
        ConfigurationSection section = getConfig().getConfigurationSection("quests");
        if (section == null) {
            getLogger().warning("[Quests] No 'quests' section found in config.yml!");
            return map;
        }
        for (String id : section.getKeys(false)) {
            ConfigurationSection questSec = section.getConfigurationSection(id);
            if (questSec == null) continue;
            QuestDefinition def = QuestDefinition.fromConfig(id.toLowerCase(), questSec, getLogger());
            if (def != null) map.put(def.getId(), def);
        }
        return map;
    }

    // ----------------------------------------------------------------
    // Player Events
    // ----------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        questManager.loadPlayer(uuid).thenRun(() ->
            questManager.recordSessionStart(uuid)
        );
        chainQuestManager.loadPlayer(uuid);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        openGuiTab.remove(uuid);
        questManager.saveAndUnload(uuid);
        chainQuestManager.unloadPlayer(uuid);
    }

    // ----------------------------------------------------------------
    // Block Break — BLOCKS_MINED
    // ----------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block  block  = event.getBlock();

        // Only natural blocks count (not player-placed).
        // We check if it's a solid, non-air block. Most mine blocks qualify.
        // A more precise check would use BlockMetadata, but that would require
        // the mines plugin to tag blocks — keep it simple here.
        if (!block.getType().isSolid() || block.getType().isAir()) return;

        UUID uuid = player.getUniqueId();
        questManager.addProgress(uuid, QuestType.BLOCKS_MINED, 1L);
        chainQuestManager.addProgress(uuid, QuestType.BLOCKS_MINED, 1L);
    }

    // ----------------------------------------------------------------
    // Command Preprocess — SELL_COMMANDS and RANKUPS
    // ----------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String msg = event.getMessage().toLowerCase();

        // Normalise: strip leading '/' and grab the base command word
        if (!msg.startsWith("/")) return;
        String stripped = msg.substring(1).trim();
        String baseCmd  = stripped.split("\\s+")[0];

        UUID uuid = event.getPlayer().getUniqueId();

        if (baseCmd.equals("sell") || baseCmd.equals("sellall")) {
            questManager.addProgress(uuid, QuestType.SELL_COMMANDS, 1L);
            chainQuestManager.addProgress(uuid, QuestType.SELL_COMMANDS, 1L);
            return;
        }

        if (baseCmd.equals("rankup")) {
            // Skip "/rankup max" — RankPlugin calls QuestsAPI directly with the exact
            // number of ranks gained, so we don't double-count or under-count.
            if (stripped.startsWith("rankup max")) return;
            questManager.addProgress(uuid, QuestType.RANKUPS, 1L);
            chainQuestManager.addProgress(uuid, QuestType.RANKUPS, 1L);
        }
    }

    // ----------------------------------------------------------------
    // GUI — Command
    // ----------------------------------------------------------------

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command must be run in-game.");
            return true;
        }

        switch (cmd.getName().toLowerCase()) {
            case "quests" -> {
                QuestTier tab = openGuiTab.getOrDefault(player.getUniqueId(), QuestTier.DAILY);
                openGuiTab.put(player.getUniqueId(), tab);
                questGUI.open(player, tab);
            }
            case "chainquests" -> ChainQuestGUI.open(player);
            default -> { return false; }
        }
        return true;
    }

    // ----------------------------------------------------------------
    // GUI — Inventory Click
    // ----------------------------------------------------------------

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // Chain quest GUI
        var chainTitle = player.getOpenInventory().title();
        if (ChainQuestGUI.isTitle(chainTitle)) {
            event.setCancelled(true);
            if (event.getClickedInventory() == event.getView().getTopInventory()) {
                ChainQuestGUI.handleClick(player, event.getRawSlot());
            }
            return;
        }

        Inventory topInv = event.getView().getTopInventory();
        if (!isQuestGUI(topInv)) return;

        // Cancel all clicks inside the quest GUI
        event.setCancelled(true);

        // Only act on clicks in the top inventory
        if (!event.getClickedInventory().equals(topInv)) return;

        int slot = event.getSlot();

        // Close button
        if (slot == QuestGUI.SLOT_CLOSE) {
            player.closeInventory();
            return;
        }

        // Tab switches
        QuestTier newTab = null;
        if (slot == QuestGUI.SLOT_TAB_DAILY)     newTab = QuestTier.DAILY;
        if (slot == QuestGUI.SLOT_TAB_WEEKLY)     newTab = QuestTier.WEEKLY;
        if (slot == QuestGUI.SLOT_TAB_MILESTONE)  newTab = QuestTier.MILESTONE;

        if (newTab != null) {
            QuestTier current = openGuiTab.get(player.getUniqueId());
            if (newTab != current) {
                openGuiTab.put(player.getUniqueId(), newTab);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                questGUI.open(player, newTab);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (isQuestGUI(event.getInventory())) {
            openGuiTab.remove(player.getUniqueId());
        }
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    /**
     * Check if the given inventory is the quest GUI by comparing its title
     * to the known title string.
     *
     * Paper's InventoryView.title() returns a Component; we serialise it back
     * via MiniMessage and compare against the constant TITLE_STRING.
     */
    private boolean isQuestGUI(Inventory inv) {
        if (inv == null || inv.getViewers().isEmpty()) return false;
        try {
            net.kyori.adventure.text.Component viewTitle =
                inv.getViewers().get(0).getOpenInventory().title();
            String serialised = MM.serialize(viewTitle);
            return QuestGUI.TITLE_STRING.equals(serialised);
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * Periodic flush of ONLINE_TIME progress for all online players.
     * Resets their session start timestamp so time isn't double-counted on quit.
     */
    private void flushOnlineTime() {
        long now = System.currentTimeMillis() / 1000L;
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            // We can't access sessionStart directly here; instead we call
            // a dedicated flush method exposed on QuestManager.
            questManager.flushOnlineTime(uuid, now);
        }
    }
}
