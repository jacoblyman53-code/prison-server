package com.prison.shop;

import com.prison.permissions.PermissionEngine;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class ShopPlugin extends JavaPlugin implements Listener {

    private static ShopPlugin instance;

    public static ShopPlugin getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        ShopManager.initialize(this);
        ShopManager.getInstance().loadCategories();

        if (ShopManager.getInstance().getCategories().isEmpty()) {
            ShopDefaults.populate(this);
        }

        BlackMarketManager.initialize(this);

        getServer().getPluginManager().registerEvents(this, this);

        int catCount = ShopManager.getInstance().getCategories().size();
        getLogger().info("Shop enabled \u2014 " + catCount + " categories loaded.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Shop disabled.");
    }

    // ----------------------------------------------------------------
    // Commands
    // ----------------------------------------------------------------

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        switch (command.getName().toLowerCase()) {
            case "shop" -> ShopGUI.openCategoryPicker(player);
            case "shopadmin" -> {
                if (!PermissionEngine.getInstance().hasPermission(player, "prison.admintoolkit.use")) {
                    player.sendMessage(MiniMessage.miniMessage().deserialize("<red>You don't have permission to use this command."));
                    return true;
                }
                ShopAdminGUI.open(player);
            }
            case "blackmarket" -> BlackMarketGUI.open(player);
            case "tokenshop"  -> TokenShopGUI.open(player);
        }
        return true;
    }

    // ----------------------------------------------------------------
    // Events
    // ----------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // Anvil input sessions take priority
        if (ShopAnvilInput.isSession(player)) {
            ShopAnvilInput.handleClick(player, event);
            return;
        }

        if (!(event.getView().getTopInventory() == event.getInventory()
            || event.getClickedInventory() == event.getView().getTopInventory())) {
            // Allow bottom inventory clicks unless we're in a shop GUI
            // We still need to check if the top is a shop GUI
        }

        // Use top inventory title for GUI identification
        var topInv = event.getView().getTopInventory();
        if (topInv == null) return;

        // Only handle clicks in the top (GUI) inventory for shop GUIs, not player inv
        // But we cancel all clicks when in a shop GUI
        var openTitle = player.getOpenInventory().title();

        if (ShopGUI.isTitle(openTitle)) {
            event.setCancelled(true);
            // Only handle clicks in top inventory
            if (event.getClickedInventory() == topInv) {
                ShopGUI.handleClick(player, event.getRawSlot(), event.getClick());
            }
            return;
        }

        if (ShopAdminGUI.isTitle(openTitle)) {
            event.setCancelled(true);
            if (event.getClickedInventory() == topInv) {
                ShopAdminGUI.handleClick(player, event.getRawSlot(), event);
            }
            return;
        }

        if (BlackMarketGUI.isTitle(openTitle)) {
            event.setCancelled(true);
            if (event.getClickedInventory() == topInv) {
                BlackMarketGUI.handleClick(player, event.getRawSlot());
            }
            return;
        }

        if (TokenShopGUI.isTitle(openTitle)) {
            event.setCancelled(true);
            if (event.getClickedInventory() == topInv) {
                TokenShopGUI.handleClick(player, event.getRawSlot());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        var openTitle = player.getOpenInventory().title();
        if (ShopGUI.isTitle(openTitle) || ShopAdminGUI.isTitle(openTitle) || BlackMarketGUI.isTitle(openTitle) || TokenShopGUI.isTitle(openTitle)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        if (!(event.getView().getPlayer() instanceof Player player)) return;
        if (ShopAnvilInput.isSession(player)) {
            ShopAnvilInput.handlePrepare(player, event);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (ShopAnvilInput.isSession(player)) {
            ShopAnvilInput.handleClose(player);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        var uuid = event.getPlayer().getUniqueId();
        ShopGUI.cleanup(uuid);
        ShopAdminGUI.cleanup(uuid);
        ShopAnvilInput.cleanup(uuid);
        BlackMarketGUI.cleanup(uuid);
        TokenShopGUI.cleanup(uuid);
    }
}
