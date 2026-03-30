package com.prison.leaderboards;

import com.prison.database.DatabaseManager;
import com.prison.permissions.PermissionEngine;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class LeaderboardPlugin extends JavaPlugin implements Listener {

    private static LeaderboardPlugin instance;

    private final MiniMessage mm = MiniMessage.miniMessage();
    private LeaderboardManager manager;
    private BukkitTask refreshTask;
    private BukkitTask flushTask;

    @Override
    public void onEnable() {
        if (DatabaseManager.getInstance() == null) {
            getLogger().severe("PrisonDatabase must be loaded before PrisonLeaderboards!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        if (PermissionEngine.getInstance() == null) {
            getLogger().severe("PrisonPermissions must be loaded before PrisonLeaderboards!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        instance = this;
        saveDefaultConfig();

        manager = new LeaderboardManager(this);

        // Ensure schema (adds blocks_mined column if absent) — run async so we
        // don't block the main thread during startup.
        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            manager.ensureSchema();
            // Kick off the first leaderboard refresh right after schema is ready.
            manager.refreshAll();
        });

        getServer().getPluginManager().registerEvents(this, this);

        // Periodic leaderboard refresh
        int refreshSeconds = getConfig().getInt("refresh-interval-seconds", 300);
        refreshTask = getServer().getScheduler().runTaskTimerAsynchronously(this,
                () -> manager.refreshAll(),
                refreshSeconds * 20L,
                refreshSeconds * 20L);

        // Periodic flush of in-memory block counts to DB
        int flushSeconds = getConfig().getInt("flush-interval-seconds", 30);
        flushTask = getServer().getScheduler().runTaskTimerAsynchronously(this,
                () -> manager.flushBlocksMined(),
                flushSeconds * 20L,
                flushSeconds * 20L);

        getLogger().info("Leaderboard system enabled.");
    }

    @Override
    public void onDisable() {
        // Cancel timers first so no async tasks can fire after the JAR starts closing.
        if (refreshTask != null) refreshTask.cancel();
        if (flushTask  != null) flushTask.cancel();

        if (manager != null) {
            // Flush any remaining in-memory block counts synchronously on shutdown.
            manager.flushBlocksMined();
        }
        LeaderboardGUI.cleanupAll();
        getLogger().info("Leaderboard system disabled — block counts flushed.");
    }

    // ----------------------------------------------------------------
    // Events
    // ----------------------------------------------------------------

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) return;
        manager.recordBlockMined(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // Only handle our GUI title
        if (!LeaderboardGUI.isOurInventory(player.getOpenInventory().title())) return;

        event.setCancelled(true);

        if (event.getRawSlot() < 0 || event.getRawSlot() >= 54) return;

        LeaderboardGUI.handleClick(player, event.getRawSlot());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        LeaderboardGUI.cleanup(event.getPlayer().getUniqueId());
    }

    // ----------------------------------------------------------------
    // Commands
    // ----------------------------------------------------------------

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command must be run by a player.");
            return true;
        }

        switch (command.getName().toLowerCase()) {
            case "leaderboard" -> LeaderboardGUI.open(player, manager, "richest");
            case "stats" -> sendStats(player);
            default -> { return false; }
        }
        return true;
    }

    private void sendStats(Player player) {
        player.sendMessage(mm.deserialize("<dark_gray>⌛ <gray>Loading your stats..."));
        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            LeaderboardManager.PlayerStats stats = manager.fetchPlayerStats(player.getUniqueId());
            getServer().getScheduler().runTask(this, () -> {
                if (stats == null) {
                    player.sendMessage(mm.deserialize("<red>Could not load your stats. Try again."));
                    return;
                }
                player.sendMessage(mm.deserialize("<dark_gray>━━━━━━━━━━━━━━━━━━━━━━"));
                player.sendMessage(mm.deserialize("<aqua><bold>  Your Stats"));
                player.sendMessage(mm.deserialize("<dark_gray>━━━━━━━━━━━━━━━━━━━━━━"));
                player.sendMessage(mm.deserialize(
                    "<gray>Balance:     <gold>$" + fmt(stats.igcBalance()) +
                    "  <dark_gray>(#<yellow>" + stats.rankRichest() + "<dark_gray>)"));
                player.sendMessage(mm.deserialize(
                    "<gray>Tokens:      <aqua>" + fmt(stats.tokenBalance()) +
                    "  <dark_gray>(#<yellow>" + stats.rankTokens() + "<dark_gray>)"));
                player.sendMessage(mm.deserialize(
                    "<gray>Prestige:    <light_purple>P" + stats.prestige() +
                    "  <dark_gray>(#<yellow>" + stats.rankPrestige() + "<dark_gray>)"));
                player.sendMessage(mm.deserialize(
                    "<gray>Blocks Mined: <white>" + fmt(stats.blocksMined()) +
                    "  <dark_gray>(#<yellow>" + stats.rankBlocks() + "<dark_gray>)"));
                player.sendMessage(mm.deserialize("<dark_gray>━━━━━━━━━━━━━━━━━━━━━━"));
            });
        });
    }

    private static String fmt(long n) { return String.format("%,d", n); }

    // ----------------------------------------------------------------
    // Accessors
    // ----------------------------------------------------------------

    public static LeaderboardPlugin getInstance() {
        return instance;
    }

    public LeaderboardManager getLeaderboardManager() {
        return manager;
    }
}
