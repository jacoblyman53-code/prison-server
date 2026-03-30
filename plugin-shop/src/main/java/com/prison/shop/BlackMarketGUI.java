package com.prison.shop;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Renders and handles clicks for the Black Market 54-slot GUI.
 *
 * Layout:
 *   Row 0 (slots 0-8)  — dark red glass border; slot 4 = info clock
 *   Content slots       — [10, 12, 14, 19, 21, 23] (2 rows of 3, with spacing)
 *   Slot 49             — BARRIER close button
 *   Everything else     — dark red glass filler
 */
public class BlackMarketGUI {

    // ----------------------------------------------------------------
    // Constants
    // ----------------------------------------------------------------

    private static final String TITLE_RAW = "<dark_red><bold>☠ Black Market";
    private static final Component TITLE = MiniMessage.miniMessage().deserialize(TITLE_RAW);

    private static final int SIZE = 54;
    private static final int INFO_SLOT = 4;
    private static final int CLOSE_SLOT = 49;

    /** Ordered list of slots where Black Market items are displayed. */
    private static final int[] ITEM_SLOTS = {10, 12, 14, 19, 21, 23};

    // ----------------------------------------------------------------
    // Player state — tracks which UUIDs currently have a BM GUI open
    // ----------------------------------------------------------------

    private static final Map<UUID, Object> openSessions = new HashMap<>();

    private static final Object SENTINEL = new Object();

    // ----------------------------------------------------------------
    // Public API
    // ----------------------------------------------------------------

    /** Opens (or refreshes) the Black Market GUI for the given player. */
    public static void open(Player player) {
        openSessions.put(player.getUniqueId(), SENTINEL);

        Inventory inv = Bukkit.createInventory(null, SIZE, TITLE);
        populate(inv);
        player.openInventory(inv);
    }

    /** Returns true when the given title matches the Black Market GUI title. */
    public static boolean isTitle(Component title) {
        return TITLE.equals(title);
    }

    /** Called on PlayerQuitEvent to prevent memory leaks. */
    public static void cleanup(UUID uuid) {
        openSessions.remove(uuid);
    }

    /**
     * Handles a click in the top inventory of the Black Market GUI.
     *
     * @param player  the clicking player
     * @param slot    raw slot index
     */
    public static void handleClick(Player player, int slot) {
        if (slot == CLOSE_SLOT) {
            player.closeInventory();
            return;
        }

        // Map slot → item index
        int itemIndex = -1;
        for (int i = 0; i < ITEM_SLOTS.length; i++) {
            if (ITEM_SLOTS[i] == slot) {
                itemIndex = i;
                break;
            }
        }
        if (itemIndex < 0) return; // clicked a filler / info slot

        List<BlackMarketItem> current = BlackMarketManager.getInstance().getCurrentItems();
        if (itemIndex >= current.size()) return;

        BlackMarketItem bmi = current.get(itemIndex);

        BlackMarketManager.PurchaseResult result =
                BlackMarketManager.getInstance().purchaseItem(player, bmi.getId());

        MiniMessage mm = MiniMessage.miniMessage();
        switch (result) {
            case OK -> {
                String display = mm.stripTags(bmi.getDisplay());
                player.sendMessage(mm.deserialize(
                        "<dark_red>☠ <white>You purchased <gold>" + display
                        + "<white> from the Black Market for <gold>"
                        + formatPrice(bmi.getPriceIgc()) + " IGC<white>."));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_TRADE, 1.0f, 1.2f);
                // Refresh the GUI so the stock counter updates
                refreshFor(player);
            }
            case OUT_OF_STOCK ->
                player.sendMessage(mm.deserialize("<red>That item is sold out!"));
            case INSUFFICIENT_FUNDS ->
                player.sendMessage(mm.deserialize("<red>You can't afford that item."));
            case ITEM_NOT_FOUND ->
                player.sendMessage(mm.deserialize("<red>That item is no longer available."));
        }
    }

    // ----------------------------------------------------------------
    // Internal rendering
    // ----------------------------------------------------------------

    /** Rebuilds the inventory contents in-place (used for refresh). */
    private static void populate(Inventory inv) {
        MiniMessage mm = MiniMessage.miniMessage();
        BlackMarketManager mgr = BlackMarketManager.getInstance();

        // --- Filler ---
        ItemStack filler = buildFiller();
        for (int i = 0; i < SIZE; i++) {
            inv.setItem(i, filler);
        }

        // --- Info clock (slot 4) ---
        long msLeft = mgr.getTimeUntilRefresh();
        String countdown = mgr.formatCountdown(msLeft);
        inv.setItem(INFO_SLOT, buildInfoItem(countdown));

        // --- Close button (slot 49) ---
        inv.setItem(CLOSE_SLOT, buildCloseItem());

        // --- Black Market items ---
        List<BlackMarketItem> items = mgr.getCurrentItems();
        for (int i = 0; i < ITEM_SLOTS.length; i++) {
            int slot = ITEM_SLOTS[i];
            if (i < items.size()) {
                inv.setItem(slot, buildItemStack(items.get(i)));
            } else {
                inv.setItem(slot, buildEmptySlot());
            }
        }
    }

    /**
     * Re-opens a fresh inventory for the player to reflect updated stock.
     * Because Bukkit inventories are live views, we close and reopen to guarantee
     * title and all slots are correct.
     */
    private static void refreshFor(Player player) {
        if (!openSessions.containsKey(player.getUniqueId())) return;
        // Schedule 1-tick delay so the current click event completes first
        Bukkit.getScheduler().runTask(ShopPlugin.getInstance(), () -> {
            if (player.isOnline() && openSessions.containsKey(player.getUniqueId())) {
                open(player);
            }
        });
    }

    // ----------------------------------------------------------------
    // Item builders
    // ----------------------------------------------------------------

    private static ItemStack buildFiller() {
        ItemStack item = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(MiniMessage.miniMessage().deserialize("<!italic><dark_gray> "));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildEmptySlot() {
        ItemStack item = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(MiniMessage.miniMessage().deserialize("<!italic><dark_gray>—"));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildCloseItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(MiniMessage.miniMessage().deserialize("<!italic><red>Close"));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildInfoItem(String countdown) {
        MiniMessage mm = MiniMessage.miniMessage();
        ItemStack item = new ItemStack(Material.CLOCK);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(mm.deserialize("<!italic><dark_red>☠ Black Market"));

        List<Component> lore = new ArrayList<>();
        lore.add(mm.deserialize("<!italic><gray>Refreshes in: <white>" + countdown));
        lore.add(mm.deserialize("<!italic><dark_gray>Items rotate every 6 hours"));
        lore.add(mm.deserialize("<!italic><dark_gray>Limited stock — buy before it's gone!"));
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildItemStack(BlackMarketItem bmi) {
        MiniMessage mm = MiniMessage.miniMessage();

        ItemStack item = new ItemStack(bmi.getMaterial(), bmi.getAmount());
        ItemMeta meta = item.getItemMeta();

        meta.displayName(mm.deserialize(bmi.getDisplay()));

        List<Component> lore = new ArrayList<>();

        // Header
        lore.add(mm.deserialize("<!italic><dark_gray>☠ Exclusive"));

        // Original lore lines
        for (String line : bmi.getLore()) {
            lore.add(mm.deserialize(line));
        }

        // Blank separator
        lore.add(Component.empty());

        // Price
        lore.add(mm.deserialize("<!italic><yellow>Price: <gold>" + formatPrice(bmi.getPriceIgc()) + " IGC"));

        // Stock
        lore.add(mm.deserialize("<!italic><gray>Stock: <white>" + bmi.getCurrentStock()));

        // Blank separator
        lore.add(Component.empty());

        // Call to action
        if (bmi.isInStock()) {
            lore.add(mm.deserialize("<!italic><green>Click to purchase"));
        } else {
            lore.add(mm.deserialize("<!italic><red>Sold Out"));
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    /** Formats a long price with thousands separators, e.g. 1500000 → "1,500,000". */
    private static String formatPrice(long price) {
        return String.format("%,d", price);
    }
}
