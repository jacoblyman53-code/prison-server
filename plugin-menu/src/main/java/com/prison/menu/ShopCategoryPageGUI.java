package com.prison.menu;

import com.prison.menu.util.*;
import com.prison.shop.ShopCategory;
import com.prison.shop.ShopItem;
import com.prison.shop.ShopManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ShopCategoryPageGUI {

    public static final Component TITLE = MiniMessage.miniMessage().deserialize("<!italic><dark_gray>[ <yellow>Shop: Browse <dark_gray>]");
    private static final MiniMessage MM = MiniMessage.miniMessage();

    // 28 content slots across 4 rows of 7
    private static final int[] CONTENT_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43
    };
    private static final int ITEMS_PER_PAGE = CONTENT_SLOTS.length; // 28

    private static final int SLOT_BACK      = 45;
    private static final int SLOT_PREV      = 46;
    private static final int SLOT_INFO      = 49;
    private static final int SLOT_NEXT      = 52;

    // Per-player state: maps UUID -> [categoryId, pageNumber]
    private static final Map<UUID, String[]> PAGE_STATE = new ConcurrentHashMap<>();

    public static void open(Player player, String categoryId, int page) {
        PAGE_STATE.put(player.getUniqueId(), new String[]{categoryId, String.valueOf(page)});
        player.openInventory(build(player, categoryId, page));
        Sounds.nav(player);
    }

    public static void handleClick(Player player, int slot, ClickType click, MenuPlugin plugin) {
        // Retrieve stored state
        String[] state = PAGE_STATE.get(player.getUniqueId());
        if (state == null) {
            // Fallback: return to picker
            ShopCategoryPickerGUI.open(player);
            return;
        }
        String categoryId = state[0];
        int page = Integer.parseInt(state[1]);

        if (slot == SLOT_BACK) {
            Sounds.nav(player);
            ShopCategoryPickerGUI.open(player);
            return;
        }

        ShopManager sm = ShopManager.getInstance();
        if (sm == null) return;

        ShopCategory category = findCategory(sm, categoryId);
        if (category == null) {
            ShopCategoryPickerGUI.open(player);
            return;
        }

        List<ShopItem> items = category.items();
        int totalPages = Math.max(1, (int) Math.ceil((double) items.size() / ITEMS_PER_PAGE));

        if (slot == SLOT_PREV && page > 0) {
            Sounds.nav(player);
            open(player, categoryId, page - 1);
            return;
        }

        if (slot == SLOT_NEXT && page < totalPages - 1) {
            Sounds.nav(player);
            open(player, categoryId, page + 1);
            return;
        }

        // Check content slot clicks
        int offset = page * ITEMS_PER_PAGE;
        for (int i = 0; i < CONTENT_SLOTS.length; i++) {
            if (CONTENT_SLOTS[i] == slot) {
                int itemIndex = offset + i;
                if (itemIndex >= items.size()) return;
                ShopItem shopItem = items.get(itemIndex);
                if (!shopItem.isInStock()) {
                    Sounds.deny(player);
                    player.sendMessage(MM.deserialize("<red>That item is out of stock."));
                    return;
                }
                ShopManager.PurchaseResult result = sm.purchase(player, categoryId, shopItem.id());
                handlePurchaseResult(player, result, shopItem);
                // Refresh the GUI to show updated stock
                open(player, categoryId, page);
                return;
            }
        }
    }

    // ----------------------------------------------------------------

    private static Inventory build(Player player, String categoryId, int page) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        Gui.fillAll(inv);

        ShopManager sm = ShopManager.getInstance();
        if (sm == null) {
            inv.setItem(SLOT_BACK, Gui.back());
            return inv;
        }

        ShopCategory category = findCategory(sm, categoryId);
        if (category == null) {
            inv.setItem(SLOT_BACK, Gui.back());
            return inv;
        }

        List<ShopItem> items = category.items();
        int totalPages = Math.max(1, (int) Math.ceil((double) items.size() / ITEMS_PER_PAGE));
        // Clamp page to valid range
        if (page >= totalPages) page = totalPages - 1;
        if (page < 0) page = 0;

        int offset = page * ITEMS_PER_PAGE;

        for (int i = 0; i < CONTENT_SLOTS.length; i++) {
            int itemIndex = offset + i;
            if (itemIndex >= items.size()) break;
            ShopItem shopItem = items.get(itemIndex);

            if (!shopItem.isInStock()) {
                // Sold-out placeholder
                inv.setItem(CONTENT_SLOTS[i], Gui.make(Material.GRAY_STAINED_GLASS_PANE,
                    "<red>Sold Out",
                    "<gray>This item is currently out of stock."));
            } else {
                String displayName = shopItem.displayName() != null
                    ? shopItem.displayName()
                    : "<white>" + Fmt.mat(shopItem.item().getType().name());

                String stockLine;
                if (shopItem.stock() == -1) {
                    stockLine = "<gray>Stock: <white>Unlimited";
                } else {
                    stockLine = "<gray>Stock: <white>" + shopItem.stock();
                }

                List<Component> lore = new ArrayList<>();
                lore.add(MM.deserialize("<!italic><gray>Price: <gold>" + Fmt.number(shopItem.priceIgc()) + "<gray> IGC"));
                lore.add(MM.deserialize("<!italic>" + stockLine));
                lore.add(Component.empty());
                lore.add(MM.deserialize("<!italic><green>Click to purchase!"));

                inv.setItem(CONTENT_SLOTS[i], Gui.make(shopItem.item().getType(), displayName, lore));
            }
        }

        // Back
        inv.setItem(SLOT_BACK, Gui.back());

        // Prev page
        if (page > 0) {
            inv.setItem(SLOT_PREV, Gui.prevPage(page + 1, totalPages));
        }

        // Next page
        if (page < totalPages - 1) {
            inv.setItem(SLOT_NEXT, Gui.nextPage(page + 1, totalPages));
        }

        // Info item
        inv.setItem(SLOT_INFO, Gui.make(Material.CHEST,
            category.displayName(),
            "<gray>Page <white>" + (page + 1) + "<gray> of <white>" + totalPages,
            "<gray>" + items.size() + "<gray> items in this category."));

        return inv;
    }

    private static ShopCategory findCategory(ShopManager sm, String categoryId) {
        for (ShopCategory cat : sm.getCategories()) {
            if (cat.id().equals(categoryId)) return cat;
        }
        return null;
    }

    private static void handlePurchaseResult(Player player, ShopManager.PurchaseResult result, ShopItem shopItem) {
        switch (result) {
            case OK -> {
                Sounds.buy(player);
                player.sendMessage(MM.deserialize("<green>Purchased successfully!"));
            }
            case INSUFFICIENT_FUNDS -> {
                Sounds.deny(player);
                player.sendMessage(MM.deserialize(
                    "<red>You need <gold>" + Fmt.number(shopItem.priceIgc()) + " IGC<red> to buy that."));
            }
            case OUT_OF_STOCK -> {
                Sounds.deny(player);
                player.sendMessage(MM.deserialize("<red>That item is out of stock."));
            }
            case INVENTORY_ERROR -> {
                Sounds.deny(player);
                player.sendMessage(MM.deserialize("<red>Your inventory is full."));
            }
            case ITEM_NOT_FOUND -> {
                Sounds.deny(player);
                player.sendMessage(MM.deserialize("<red>That item could not be found."));
            }
        }
    }
}
