package com.prison.auctionhouse;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AuctionGUI — the main browse window for the auction house.
 *
 * 54 slots: slots 0-44 are listing items, slots 45-53 are the navigation bar.
 * A second mode (FILTER_TITLE) allows players to pick a material filter.
 */
public class AuctionGUI {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    static final Component TITLE        = MM.deserialize("Auction House");
    static final Component FILTER_TITLE = MM.deserialize("Filter by Material");

    // State: page + filter per player
    private static final Map<UUID, AuctionGUIState> states = new ConcurrentHashMap<>();

    static class AuctionGUIState {
        int page;
        Material filter;
        AuctionGUIState(int page, Material filter) {
            this.page = page;
            this.filter = filter;
        }
    }


    // ----------------------------------------------------------------
    // Open
    // ----------------------------------------------------------------

    public static void open(Player player, int page, Material filter) {
        states.put(player.getUniqueId(), new AuctionGUIState(page, filter));
        render(player);
    }

    // ----------------------------------------------------------------
    // Render main GUI
    // ----------------------------------------------------------------

    private static void render(Player player) {
        AuctionGUIState state = states.getOrDefault(player.getUniqueId(), new AuctionGUIState(0, null));

        List<AuctionListing> listings = AuctionManager.getInstance().getActiveListings(state.filter);
        int totalListings = listings.size();
        int totalPages    = Math.max(1, (int) Math.ceil((double) totalListings / 45));

        // Clamp page
        if (state.page < 0) state.page = 0;
        if (state.page >= totalPages) state.page = totalPages - 1;

        int startIndex = state.page * 45;
        int endIndex   = Math.min(startIndex + 45, totalListings);

        Inventory inv = Bukkit.createInventory(null, 54, TITLE);

        // Fill listing slots 0-44
        for (int slot = 0; slot < 45; slot++) {
            int listingIndex = startIndex + slot;
            if (listingIndex < endIndex) {
                AuctionListing listing = listings.get(listingIndex);
                inv.setItem(slot, buildListingDisplay(listing, true));
            }
            // Empty slots stay empty — no filler
        }

        // --- Navigation bar (slots 45-53) ---

        // Slot 45: Previous page
        if (state.page > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta m = prev.getItemMeta();
            m.displayName(MM.deserialize("<gray>\u2190 Previous Page"));
            List<Component> lore = new ArrayList<>();
            lore.add(MM.deserialize("<gray>Page <white>" + state.page + " <gray>\u2190 <white>" + (state.page + 1)));
            m.lore(lore);
            prev.setItemMeta(m);
            inv.setItem(45, prev);
        }

        // Slot 46: My Listings
        {
            UUID uuid = player.getUniqueId();
            int  myCount   = (int) AuctionManager.getInstance().getPlayerActiveListingCount(uuid);
            int  myMax     = AuctionManager.getInstance().getMaxListingsForPlayer(uuid);
            ItemStack myListings = new ItemStack(Material.LIME_DYE);
            ItemMeta m = myListings.getItemMeta();
            m.displayName(MM.deserialize("<aqua>My Listings"));
            List<Component> lore = new ArrayList<>();
            lore.add(MM.deserialize("<gray>View and cancel your <green>active listings</green>."));
            lore.add(MM.deserialize("<aqua>\u2756 <gray>Active: <white>" + myCount + "<gray>/<white>" + myMax));
            lore.add(Component.empty());
            lore.add(MM.deserialize("<green>\u2192 Click to view your listings!"));
            m.lore(lore);
            myListings.setItemMeta(m);
            inv.setItem(46, myListings);
        }

        // Slot 47: Filter
        {
            String filterLabel = state.filter == null ? "All" :
                AuctionManager.formatMaterialName(state.filter.name());
            int filteredCount = listings.size();
            ItemStack filterItem = new ItemStack(Material.PAPER);
            ItemMeta m = filterItem.getItemMeta();
            m.displayName(MM.deserialize("<aqua>Filter: <yellow>" + filterLabel));
            List<Component> lore = new ArrayList<>();
            lore.add(MM.deserialize("<aqua>\u2756 <gray>Showing: <white>" + filteredCount + " listing(s)"));
            lore.add(Component.empty());
            lore.add(MM.deserialize("<green>\u2192 Click to change the material filter!"));
            m.lore(lore);
            filterItem.setItemMeta(m);
            inv.setItem(47, filterItem);
        }

        // Slot 48: Page indicator
        {
            ItemStack pageItem = new ItemStack(Material.BOOK);
            ItemMeta m = pageItem.getItemMeta();
            m.displayName(MM.deserialize("<aqua>\u2756 <gray>Page <white>" + (state.page + 1) + " <gray>/ <white>" + totalPages));
            m.lore(List.of());
            pageItem.setItemMeta(m);
            inv.setItem(48, pageItem);
        }

        // Slot 50: Sell reminder
        {
            ItemStack sell = new ItemStack(Material.EMERALD);
            ItemMeta m = sell.getItemMeta();
            m.displayName(MM.deserialize("<aqua>Sell an Item"));
            List<Component> lore = new ArrayList<>();
            lore.add(MM.deserialize("<gray>Hold an item and type <green>/ah sell <price></green>."));
            lore.add(MM.deserialize("<aqua>\u2756 <gray>Listing fee: <white>" +
                AuctionManager.getInstance().listingFeePercent + "% <gray>of price"));
            lore.add(Component.empty());
            lore.add(MM.deserialize("<green>\u2192 Click for a reminder!"));
            m.lore(lore);
            sell.setItemMeta(m);
            inv.setItem(50, sell);
        }

        // Slot 53: Next page
        boolean hasNext = state.page < totalPages - 1;
        if (hasNext) {
            ItemStack next = new ItemStack(Material.LIME_DYE);
            ItemMeta m = next.getItemMeta();
            m.displayName(MM.deserialize("<green>\u2192 Next Page"));
            List<Component> lore = new ArrayList<>();
            lore.add(MM.deserialize("<gray>Page <white>" + (state.page + 1) + " <gray>\u2192 <white>" + (state.page + 2)));
            m.lore(lore);
            next.setItemMeta(m);
            inv.setItem(53, next);
        }

        player.openInventory(inv);
    }

    // ----------------------------------------------------------------
    // Build listing display item
    // ----------------------------------------------------------------

    static ItemStack buildListingDisplay(AuctionListing listing, boolean showBuyPrompt) {
        ItemStack display = listing.item().clone();
        ItemMeta meta = display.getItemMeta();
        if (meta == null) return display;

        List<Component> lore = meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        // Spec: divider, then AH metadata block
        lore.add(MM.deserialize("<dark_gray><st>--------------------"));
        lore.add(MM.deserialize("<aqua>\u2756 <gray>Seller: <white>" + listing.sellerName()));
        lore.add(MM.deserialize("<aqua>\u2756 <gray>Price: <gold>" + String.format("%,d", listing.priceIgc()) + " tokens"));
        lore.add(MM.deserialize("<aqua>\u2756 <gray>Listed: <white>" + listing.formattedTimeRemaining()));
        lore.add(Component.empty());
        if (showBuyPrompt) {
            lore.add(MM.deserialize("<green>\u2192 Click to <underlined>buy</underlined> this listing!"));
        }
        meta.lore(lore);

        // Ensure display name is set
        if (!meta.hasDisplayName()) {
            meta.displayName(MM.deserialize("<white>" +
                AuctionManager.formatMaterialName(listing.item().getType().name())));
        }

        display.setItemMeta(meta);
        return display;
    }

    // ----------------------------------------------------------------
    // isTitle (handles both main and filter picker)
    // ----------------------------------------------------------------

    public static boolean isTitle(Component title) {
        return TITLE.equals(title) || FILTER_TITLE.equals(title);
    }

    // ----------------------------------------------------------------
    // handleClick — dispatches based on which GUI is open
    // ----------------------------------------------------------------

    public static void handleClick(Player player, int slot, ClickType click) {
        Component currentTitle = player.getOpenInventory().title();
        if (FILTER_TITLE.equals(currentTitle)) {
            handleFilterPickerClick(player, slot);
        } else {
            handleMainClick(player, slot, click);
        }
    }

    // ----------------------------------------------------------------
    // Main GUI click handler
    // ----------------------------------------------------------------

    private static void handleMainClick(Player player, int slot, ClickType click) {
        AuctionGUIState state = states.get(player.getUniqueId());
        if (state == null) return;

        List<AuctionListing> listings = AuctionManager.getInstance().getActiveListings(state.filter);
        int totalPages = Math.max(1, (int) Math.ceil((double) listings.size() / 45));

        if (slot <= 44) {
            // Check it's not an empty slot
            ItemStack clicked = player.getOpenInventory().getTopInventory().getItem(slot);
            if (clicked == null || clicked.getType() == Material.AIR) return;

            int listingIndex = (state.page * 45) + slot;
            if (listingIndex < listings.size()) {
                AuctionListing listing = listings.get(listingIndex);
                AuctionConfirmGUI.open(player, listing);
            }
        } else if (slot == 45) {
            // Previous page
            if (state.page > 0) {
                state.page--;
                render(player);
            }
        } else if (slot == 46) {
            // My listings
            MyListingsGUI.open(player);
        } else if (slot == 47) {
            // Filter picker
            openFilterPicker(player);
        } else if (slot == 50) {
            // Sell reminder
            player.sendMessage(MM.deserialize(
                "<gray>Hold an item in your main hand and type <white>/ah sell <price></white> to list it."));
        } else if (slot == 53) {
            // Next page
            if (state.page < totalPages - 1) {
                state.page++;
                render(player);
            }
        }
    }

    // ----------------------------------------------------------------
    // Filter picker GUI
    // ----------------------------------------------------------------

    private static void openFilterPicker(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, FILTER_TITLE);

        // Slot 0: clear filter
        ItemStack clear = new ItemStack(Material.BARRIER);
        ItemMeta cm = clear.getItemMeta();
        cm.displayName(MM.deserialize("<red>\u2717 Clear Filter"));
        List<Component> clearLore = new ArrayList<>();
        clearLore.add(MM.deserialize("<gray>Show <green>all listings</green> regardless of type."));
        clearLore.add(Component.empty());
        clearLore.add(MM.deserialize("<green>\u2192 Click to clear the filter!"));
        cm.lore(clearLore);
        clear.setItemMeta(cm);
        inv.setItem(0, clear);

        // Gather distinct materials from current active listings
        Set<Material> materials = new LinkedHashSet<>();
        for (AuctionListing l : AuctionManager.getInstance().getActiveListings()) {
            materials.add(l.item().getType());
        }

        int slot = 1;
        for (Material mat : materials) {
            if (slot >= 54) break;
            ItemStack matItem = new ItemStack(mat);
            ItemMeta mm2 = matItem.getItemMeta();
            mm2.displayName(MM.deserialize("<aqua>" + AuctionManager.formatMaterialName(mat.name())));
            List<Component> matLore = new ArrayList<>();
            long count = AuctionManager.getInstance().getActiveListings().stream()
                .filter(l -> l.item().getType() == mat).count();
            matLore.add(MM.deserialize("<aqua>\u2756 <gray>Listings: <white>" + count));
            matLore.add(Component.empty());
            matLore.add(MM.deserialize("<green>\u2192 Click to filter by this item!"));
            mm2.lore(matLore);
            matItem.setItemMeta(mm2);
            inv.setItem(slot++, matItem);
        }
        // Remaining slots stay empty — no filler

        player.openInventory(inv);
    }

    // ----------------------------------------------------------------
    // Filter picker click handler
    // ----------------------------------------------------------------

    private static void handleFilterPickerClick(Player player, int slot) {
        ItemStack clicked = player.getOpenInventory().getTopInventory().getItem(slot);
        if (clicked == null || clicked.getType() == Material.AIR) return;

        Material filter;
        if (clicked.getType() == Material.BARRIER) {
            filter = null; // clear filter
        } else {
            filter = clicked.getType();
        }

        // Re-open main GUI with new filter
        open(player, 0, filter);
    }

    // ----------------------------------------------------------------
    // Cleanup
    // ----------------------------------------------------------------

    public static void cleanup(UUID uuid) {
        states.remove(uuid);
    }

    // ----------------------------------------------------------------
    // Helper: get plugin reference for scheduling
    // ----------------------------------------------------------------

    static JavaPlugin getPlugin() {
        return AuctionHousePlugin.getInstance();
    }
}
