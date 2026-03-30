package com.prison.auctionhouse;

import com.prison.database.DatabaseManager;
import com.prison.permissions.PermissionEngine;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.logging.Level;

/**
 * AuctionHousePlugin — main plugin class for PrisonAuctionHouse.
 *
 * Handles lifecycle (enable/disable), command routing, and event forwarding
 * to the GUI classes and AuctionManager.
 */
public class AuctionHousePlugin extends JavaPlugin implements Listener {

    private static AuctionHousePlugin instance;
    private static final MiniMessage MM = MiniMessage.miniMessage();

    public static AuctionHousePlugin getInstance() {
        return instance;
    }

    // ----------------------------------------------------------------
    // onEnable
    // ----------------------------------------------------------------

    @Override
    public void onEnable() {
        instance = this;

        // Load config defaults
        saveDefaultConfig();

        // Initialize AuctionManager singleton
        AuctionManager.initialize(this);

        // Push config values into AuctionManager
        AuctionManager mgr = AuctionManager.getInstance();
        mgr.listingFeePercent    = getConfig().getDouble("listing-fee-percent", 5.0);
        mgr.listingDurationHours = getConfig().getInt("listing-duration-hours", 48);
        mgr.maxListingsDefault   = getConfig().getInt("max-listings-default", 3);
        mgr.maxListingsDonor     = getConfig().getInt("max-listings-donor", 5);
        mgr.maxListingsDonorPlus = getConfig().getInt("max-listings-donorplus", 8);
        mgr.maxListingsElite     = getConfig().getInt("max-listings-elite", 12);
        mgr.maxListingsElitePlus = getConfig().getInt("max-listings-eliteplus", 16);

        // Run ALTER TABLE to add new columns (idempotent — safe to run every startup)
        runAlterTables();

        // Register events
        getServer().getPluginManager().registerEvents(this, this);

        // Schedule cache refresh (async, every N seconds converted to ticks)
        int cacheIntervalSecs = getConfig().getInt("cache-refresh-interval-seconds", 30);
        long cacheIntervalTicks = cacheIntervalSecs * 20L;
        getServer().getScheduler().runTaskTimerAsynchronously(
            this,
            (Runnable) mgr::refreshCache,
            cacheIntervalTicks,
            cacheIntervalTicks
        );

        // Schedule expiry processing every 60 seconds (1200 ticks)
        getServer().getScheduler().runTaskTimerAsynchronously(
            this,
            (Runnable) mgr::processExpiredListings,
            1200L,
            1200L
        );

        // Initial cache load
        getServer().getScheduler().runTaskAsynchronously(this, (Runnable) mgr::refreshCache);

        getLogger().info("[AuctionHouse] Plugin enabled successfully.");
    }

    // ----------------------------------------------------------------
    // onDisable
    // ----------------------------------------------------------------

    @Override
    public void onDisable() {
        getLogger().info("[AuctionHouse] Plugin disabled.");
    }

    // ----------------------------------------------------------------
    // ALTER TABLE (add seller_name and seller_notified columns if needed)
    // ----------------------------------------------------------------

    private void runAlterTables() {
        try {
            DatabaseManager.getInstance().execute(
                "ALTER TABLE auction_listings ADD COLUMN IF NOT EXISTS " +
                "seller_name VARCHAR(16) NOT NULL DEFAULT ''"
            );
        } catch (SQLException e) {
            // Column may already exist on older MySQL versions that don't support IF NOT EXISTS
            getLogger().log(Level.FINE,
                "[AuctionHouse] seller_name column alter skipped (may already exist): " + e.getMessage());
        }

        try {
            DatabaseManager.getInstance().execute(
                "ALTER TABLE auction_listings ADD COLUMN IF NOT EXISTS " +
                "seller_notified TINYINT(1) NOT NULL DEFAULT 0"
            );
        } catch (SQLException e) {
            getLogger().log(Level.FINE,
                "[AuctionHouse] seller_notified column alter skipped (may already exist): " + e.getMessage());
        }
    }

    // ----------------------------------------------------------------
    // Command handler
    // ----------------------------------------------------------------

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        String cmdName = command.getName().toLowerCase();

        // /auctionhouse with no args — same as /ah with no args
        if (cmdName.equals("auctionhouse") || (cmdName.equals("ah") && args.length == 0)) {
            if (!PermissionEngine.getInstance().hasPermission(player, "prison.sell")) {
                player.sendMessage(MM.deserialize("<red>You do not have permission to use the auction house."));
                return true;
            }
            AuctionGUI.open(player, 0, null);
            return true;
        }

        // /ah <subcommand>
        if (cmdName.equals("ah") && args.length > 0) {
            switch (args[0].toLowerCase()) {

                case "sell" -> {
                    if (args.length < 2) {
                        player.sendMessage(MM.deserialize("<red>Usage: /ah sell <price>"));
                        return true;
                    }
                    long price;
                    try {
                        price = Long.parseLong(args[1]);
                    } catch (NumberFormatException e) {
                        player.sendMessage(MM.deserialize("<red>Invalid price. Must be a whole number."));
                        return true;
                    }
                    if (price < 1) {
                        player.sendMessage(MM.deserialize("<red>Price must be at least $1."));
                        return true;
                    }
                    ItemStack heldItem = player.getInventory().getItemInMainHand();
                    AuctionManager.getInstance().createListing(player, heldItem, price);
                }

                case "mylistings" -> {
                    if (!PermissionEngine.getInstance().hasPermission(player, "prison.sell")) {
                        player.sendMessage(MM.deserialize(
                            "<red>You do not have permission to use the auction house."));
                        return true;
                    }
                    MyListingsGUI.open(player);
                }

                case "search" -> {
                    if (!PermissionEngine.getInstance().hasPermission(player, "prison.sell")) {
                        player.sendMessage(MM.deserialize(
                            "<red>You do not have permission to use the auction house."));
                        return true;
                    }
                    if (args.length < 2) {
                        player.sendMessage(MM.deserialize(
                            "<red>Usage: /ah search <material>"));
                        return true;
                    }
                    String matName = args[1].toUpperCase();
                    Material material = Material.matchMaterial(matName);
                    if (material == null) {
                        player.sendMessage(MM.deserialize(
                            "<red>Unknown material: <white>" + args[1]));
                        return true;
                    }
                    AuctionGUI.open(player, 0, material);
                }

                default -> {
                    player.sendMessage(MM.deserialize(
                        "<red>Usage: /ah [sell <price> | mylistings | search <material>]"));
                }
            }
        }

        return true;
    }

    // ----------------------------------------------------------------
    // Events
    // ----------------------------------------------------------------

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Async: check for pending deliveries (expired/sold/removed items)
        AuctionManager.getInstance().deliverExpiredItemsAndNotify(player);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Component title = event.getView().title();
        int rawSlot = event.getRawSlot();

        if (AuctionGUI.isTitle(title)) {
            event.setCancelled(true);
            // Only handle clicks within the top inventory
            if (rawSlot < 0 || rawSlot >= event.getView().getTopInventory().getSize()) return;
            AuctionGUI.handleClick(player, rawSlot, event.getClick());

        } else if (AuctionConfirmGUI.isTitle(title)) {
            event.setCancelled(true);
            if (rawSlot < 0 || rawSlot >= event.getView().getTopInventory().getSize()) return;
            AuctionConfirmGUI.handleClick(player, rawSlot);

        } else if (MyListingsGUI.isTitle(title)) {
            event.setCancelled(true);
            if (rawSlot < 0 || rawSlot >= event.getView().getTopInventory().getSize()) return;
            MyListingsGUI.handleClick(player, rawSlot);

        } else if (CancelConfirmGUI.isTitle(title)) {
            event.setCancelled(true);
            if (rawSlot < 0 || rawSlot >= event.getView().getTopInventory().getSize()) return;
            CancelConfirmGUI.handleClick(player, rawSlot);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Component title = event.getView().title();
        if (AuctionGUI.isTitle(title)
            || AuctionConfirmGUI.isTitle(title)
            || MyListingsGUI.isTitle(title)
            || CancelConfirmGUI.isTitle(title)) {
            event.setCancelled(true);
        }
    }
}
