package com.prison.coinflip;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class CoinflipPlugin extends JavaPlugin implements Listener {

    private static CoinflipPlugin instance;
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private CoinflipCommand command;

    @Override
    public void onEnable() {
        instance = this;
        CoinflipManager.initialize(this);
        command = new CoinflipCommand(this);
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("[Coinflip] Plugin enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("[Coinflip] Plugin disabled.");
    }

    public static CoinflipPlugin getInstance() { return instance; }

    // ----------------------------------------------------------------
    // Commands
    // ----------------------------------------------------------------

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("coinflip")) return false;
        return command.onCommand(sender, cmd, label, args);
    }

    // ----------------------------------------------------------------
    // Events
    // ----------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Component title = player.getOpenInventory().title();

        boolean isCoinflipGui = title.equals(CoinflipBrowserGUI.TITLE)
            || title.equals(CoinflipCreateGUI.TITLE)
            || title.equals(CoinflipAcceptGUI.TITLE)
            || title.equals(CoinflipAnimationGUI.TITLE);

        if (!isCoinflipGui) return;
        event.setCancelled(true);
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;

        int slot = event.getRawSlot();
        if (title.equals(CoinflipBrowserGUI.TITLE))  { CoinflipBrowserGUI.handleClick(player, slot, this); return; }
        if (title.equals(CoinflipCreateGUI.TITLE))   { CoinflipCreateGUI.handleClick(player, slot, this); return; }
        if (title.equals(CoinflipAcceptGUI.TITLE))   { CoinflipAcceptGUI.handleClick(player, slot, this); return; }
        // Animation GUI is read-only — no click handler needed
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Component title = player.getOpenInventory().title();
        if (title.equals(CoinflipBrowserGUI.TITLE)
         || title.equals(CoinflipCreateGUI.TITLE)
         || title.equals(CoinflipAcceptGUI.TITLE)
         || title.equals(CoinflipAnimationGUI.TITLE)) {
            event.setCancelled(true);
        }
    }
}
