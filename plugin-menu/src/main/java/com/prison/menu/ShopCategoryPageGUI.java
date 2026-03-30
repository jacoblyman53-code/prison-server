package com.prison.menu;

import com.prison.economy.EconomyAPI;
import com.prison.economy.TransactionType;
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
import org.bukkit.inventory.ItemStack;

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
        if (slot == 8) {
            Sounds.nav(player);
            MainMenuGUI.open(player);
            return;
        }

        // Retrieve stored state
        String[] state = PAGE_STATE.get(player.getUniqueId());
        if (state == null) {
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

                if (click == ClickType.RIGHT || click == ClickType.SHIFT_RIGHT) {
                    if (!shopItem.sellable()) {
                        Sounds.deny(player);
                        player.sendMessage(MM.deserialize("<red>That item cannot be sold."));
                        return;
                    }
                    handleSell(player, shopItem, click == ClickType.SHIFT_RIGHT);
                    return;
                }

                // Left click — open quantity picker
                if (!shopItem.isInStock()) {
                    Sounds.deny(player);
                    player.sendMessage(MM.deserialize("<red>That item is out of stock."));
                    return;
                }
                Sounds.nav(player);
                ShopQuantityGUI.open(player, categoryId, shopItem.id(), page);
                return;
            }
        }
    }

    // ----------------------------------------------------------------
    // Sell
    // ----------------------------------------------------------------

    private static void handleSell(Player player, ShopItem shopItem, boolean sellAll) {
        EconomyAPI eco = EconomyAPI.getInstance();
        if (eco == null) return;

        Material mat = shopItem.item().getType();
        String displayName = shopItem.displayName() != null
            ? shopItem.displayName()
            : "<white>" + Fmt.mat(mat.name());

        // Count how many the player has in inventory
        int count = 0;
        for (ItemStack s : player.getInventory().getStorageContents()) {
            if (s != null && s.getType() == mat) count += s.getAmount();
        }
        if (count == 0) {
            Sounds.deny(player);
            player.sendMessage(MM.deserialize("<red>You don't have any " + displayName + "<red> to sell."));
            return;
        }

        int toSell = sellAll ? count : 1;

        // Remove items from inventory
        ItemStack[] contents = player.getInventory().getStorageContents();
        int removed = 0;
        for (int i = 0; i < contents.length && removed < toSell; i++) {
            ItemStack s = contents[i];
            if (s == null || s.getType() != mat) continue;
            int take = Math.min(s.getAmount(), toSell - removed);
            removed += take;
            s.setAmount(s.getAmount() - take);
            if (s.getAmount() == 0) contents[i] = null;
        }
        player.getInventory().setStorageContents(contents);

        long earned = shopItem.priceIgc() * removed;
        eco.addBalance(player.getUniqueId(), earned, TransactionType.IGC_SHOP_SELL);

        Sounds.buy(player);
        player.sendMessage(MM.deserialize(
            "<green>Sold <white>×" + removed + " " + displayName
            + " <green>for <gold>$" + Fmt.number(earned)));
    }

    // ----------------------------------------------------------------
    // Build
    // ----------------------------------------------------------------

    private static Inventory build(Player player, String categoryId, int page) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        Gui.fillAll(inv);
        TopBand.apply(inv, player);
        inv.setItem(8, Gui.back());

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
        if (page >= totalPages) page = totalPages - 1;
        if (page < 0) page = 0;

        int offset = page * ITEMS_PER_PAGE;

        for (int i = 0; i < CONTENT_SLOTS.length; i++) {
            int itemIndex = offset + i;
            if (itemIndex >= items.size()) break;
            ShopItem shopItem = items.get(itemIndex);

            String displayName = shopItem.displayName() != null
                ? shopItem.displayName()
                : "<white>" + Fmt.mat(shopItem.item().getType().name());

            List<Component> lore = new ArrayList<>();
            lore.add(MM.deserialize("<!italic><gray>Buy Price: <gold>$" + Fmt.number(shopItem.priceIgc()) + "<gray>/ea."));
            if (shopItem.sellable()) {
                lore.add(MM.deserialize("<!italic><gray>Sell Price: <green>$" + Fmt.number(shopItem.priceIgc()) + "<gray>/ea."));
            } else {
                lore.add(MM.deserialize("<!italic><gray>Sell Price: <dark_gray>Not Sellable"));
            }
            lore.add(Component.empty());
            lore.add(MM.deserialize("<!italic><green>[LEFT-CLICK] <gray>Buy Items"));
            if (shopItem.sellable()) {
                lore.add(MM.deserialize("<!italic><yellow>[RIGHT-CLICK] <gray>Sell Items"));
                lore.add(MM.deserialize("<!italic><yellow>Shift + [RIGHT-CLICK] <gray>Sell All"));
            }

            inv.setItem(CONTENT_SLOTS[i], Gui.make(shopItem.item().getType(), displayName, lore));
        }

        inv.setItem(SLOT_BACK, Gui.back());

        if (page > 0) {
            inv.setItem(SLOT_PREV, Gui.prevPage(page + 1, totalPages));
        }
        if (page < totalPages - 1) {
            inv.setItem(SLOT_NEXT, Gui.nextPage(page + 1, totalPages));
        }

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
}
