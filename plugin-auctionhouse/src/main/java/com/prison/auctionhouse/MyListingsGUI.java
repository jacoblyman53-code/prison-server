package com.prison.auctionhouse;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * MyListingsGUI — shows the player's own active listings (54 slots).
 *
 * Slots 0-44: active listings with shift-click to cancel prompt
 * Slot 45: Back arrow
 * Slot 22 (fallback): "No listings" message when empty
 */
public class MyListingsGUI {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    static final Component TITLE = MM.deserialize("<gold><bold>My Listings");

    // We store the listings list per player so handleClick can look up by slot index.
    // This avoids re-querying; it's refreshed on every open().
    private static final java.util.Map<UUID, List<AuctionListing>> playerListingsCache =
        new java.util.concurrent.ConcurrentHashMap<>();

    // Filler
    private static ItemStack makeFiller() {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta  = pane.getItemMeta();
        meta.displayName(Component.empty());
        pane.setItemMeta(meta);
        return pane;
    }

    // ----------------------------------------------------------------
    // Open
    // ----------------------------------------------------------------

    public static void open(Player player) {
        UUID uuid = player.getUniqueId();

        // Get this player's active listings from the cache
        List<AuctionListing> allActive = AuctionManager.getInstance().getActiveListings();
        List<AuctionListing> myListings = new ArrayList<>();
        for (AuctionListing l : allActive) {
            if (l.sellerUuid().equals(uuid)) myListings.add(l);
        }
        playerListingsCache.put(uuid, myListings);

        Inventory inv = Bukkit.createInventory(null, 54, TITLE);

        if (myListings.isEmpty()) {
            // Fill with filler
            ItemStack filler = makeFiller();
            for (int i = 0; i < 54; i++) inv.setItem(i, filler.clone());

            // Slot 22: No listings message
            ItemStack noListings = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta nm = noListings.getItemMeta();
            nm.displayName(MM.deserialize("<gray>No active listings"));
            List<Component> nLore = new ArrayList<>();
            nLore.add(MM.deserialize("<gray>Use /ah sell <price> to list an item."));
            nm.lore(nLore);
            noListings.setItemMeta(nm);
            inv.setItem(22, noListings);
        } else {
            // Slots 0-44: listing items
            ItemStack filler = makeFiller();
            for (int slot = 0; slot < 45; slot++) {
                if (slot < myListings.size()) {
                    AuctionListing listing = myListings.get(slot);
                    inv.setItem(slot, buildMyListingDisplay(listing));
                } else {
                    inv.setItem(slot, filler.clone());
                }
            }
            // Fill nav row with filler first
            for (int i = 45; i < 54; i++) inv.setItem(i, filler.clone());
        }

        // Slot 45: Back button (always shown)
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta bm = back.getItemMeta();
        bm.displayName(MM.deserialize("<gray>← Back"));
        List<Component> backLore = new ArrayList<>();
        backLore.add(MM.deserialize("<gray>Return to auction house."));
        bm.lore(backLore);
        back.setItemMeta(bm);
        inv.setItem(45, back);

        player.openInventory(inv);
    }

    // ----------------------------------------------------------------
    // Build display item for own listing
    // ----------------------------------------------------------------

    private static ItemStack buildMyListingDisplay(AuctionListing listing) {
        ItemStack display = listing.item().clone();
        ItemMeta meta = display.getItemMeta();
        if (meta == null) return display;

        if (!meta.hasDisplayName()) {
            meta.displayName(MM.deserialize("<white>" +
                AuctionManager.formatMaterialName(listing.item().getType().name())));
        }

        List<Component> lore = meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        lore.add(Component.empty());
        lore.add(MM.deserialize("<yellow>Price: <gold>" +
            String.format("%,d", listing.priceIgc()) + " IGC"));
        lore.add(MM.deserialize("<gray>Expires: <white>" + listing.formattedTimeRemaining()));
        lore.add(Component.empty());
        lore.add(MM.deserialize("<red>Click to cancel listing"));
        lore.add(MM.deserialize("<dark_gray>Listing fee is non-refundable."));
        meta.lore(lore);
        display.setItemMeta(meta);
        return display;
    }

    // ----------------------------------------------------------------
    // isTitle
    // ----------------------------------------------------------------

    public static boolean isTitle(Component title) {
        return TITLE.equals(title);
    }

    // ----------------------------------------------------------------
    // handleClick
    // ----------------------------------------------------------------

    public static void handleClick(Player player, int slot) {
        UUID uuid = player.getUniqueId();

        if (slot <= 44) {
            List<AuctionListing> myListings = playerListingsCache.getOrDefault(uuid, List.of());
            if (slot < myListings.size()) {
                AuctionListing listing = myListings.get(slot);
                // Verify it's still a valid item slot (not a filler)
                ItemStack clicked = player.getOpenInventory().getTopInventory().getItem(slot);
                if (clicked == null || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) return;
                CancelConfirmGUI.open(player, listing);
            }
        } else if (slot == 45) {
            // Back to auction house
            player.closeInventory();
            Bukkit.getScheduler().runTask(AuctionHousePlugin.getInstance(), (Runnable) () ->
                AuctionGUI.open(player, 0, null)
            );
        }
    }

    // ----------------------------------------------------------------
    // Cleanup
    // ----------------------------------------------------------------

    public static void cleanup(UUID uuid) {
        playerListingsCache.remove(uuid);
    }
}
