package com.prison.menu;

import com.prison.menu.util.Sounds;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class MenuPlugin extends JavaPlugin implements Listener {

    private static MenuPlugin instance;
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private PlayerSettingsManager settingsManager;
    private MenuListener menuListener;

    @Override
    public void onEnable() {
        instance = this;
        settingsManager = PlayerSettingsManager.initialize(this);
        menuListener    = new MenuListener(this);

        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("[Menu] Plugin enabled — unified GUI layer ready.");
    }

    @Override
    public void onDisable() {
        getLogger().info("[Menu] Plugin disabled.");
    }

    public static MenuPlugin getInstance() { return instance; }

    // ----------------------------------------------------------------
    // Player lifecycle
    // ----------------------------------------------------------------

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        getServer().getScheduler().runTaskAsynchronously(this,
            () -> settingsManager.loadPlayer(event.getPlayer().getUniqueId()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        settingsManager.unloadPlayer(event.getPlayer().getUniqueId());
    }

    // ----------------------------------------------------------------
    // Inventory events — forward to MenuListener
    // ----------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (menuListener.handles(player.getOpenInventory().title())) {
            event.setCancelled(true);
            if (event.getClickedInventory() == event.getView().getTopInventory()) {
                menuListener.onClick(player, event.getRawSlot(), event.getClick());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (menuListener.handles(player.getOpenInventory().title())) {
            event.setCancelled(true);
        }
    }

    // ----------------------------------------------------------------
    // Commands
    // ----------------------------------------------------------------

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command must be run in-game.");
            return true;
        }

        switch (cmd.getName().toLowerCase()) {
            case "menu" -> {
                Sounds.nav(player);
                MainMenuGUI.open(player);
            }
            case "sell", "sellmenu" -> {
                Sounds.nav(player);
                SellCenterGUI.open(player);
            }
            case "mines" -> {
                Sounds.nav(player);
                MineBrowserGUI.open(player);
            }
            default -> { return false; }
        }
        return true;
    }

    // ----------------------------------------------------------------
    // Accessors
    // ----------------------------------------------------------------

    public PlayerSettingsManager getSettingsManager() { return settingsManager; }
}
