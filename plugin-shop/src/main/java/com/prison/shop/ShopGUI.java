package com.prison.shop;

import com.prison.economy.EconomyAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ShopGUI {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    static final Component TITLE_CATEGORIES = MM.deserialize("<!italic>Shop");
    static final Component TITLE_ITEMS      = MM.deserialize("<!italic>Shop \u2014 Browse");
    static final Component TITLE_QTY        = MM.deserialize("<!italic>Buy \u2014 Select Quantity");

    // Category picker slots (27-slot GUI): rows 1 and 2 inner slots
    private static final int[] CATEGORY_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25
    };

    // Quantity selector: slots and amounts (0 = compute max)
    private static final int[] QTY_SLOTS   = {10, 11, 12, 14, 15, 16};
    private static final int[] QTY_AMOUNTS = { 1,  8, 16, 32, 64,  0};

    // ----------------------------------------------------------------
    // State
    // ----------------------------------------------------------------

    private static class ShopGUIState {
        String categoryId;
        int    page;
        String pendingItemId;
    }

    private static final Map<UUID, ShopGUIState> states = new ConcurrentHashMap<>();

    public static boolean isTitle(Component title) {
        return TITLE_CATEGORIES.equals(title)
            || TITLE_ITEMS.equals(title)
            || TITLE_QTY.equals(title);
    }

    // ----------------------------------------------------------------
    // Open: Category Picker
    // ----------------------------------------------------------------

    public static void openCategoryPicker(Player player) {
        ShopGUIState state = states.computeIfAbsent(player.getUniqueId(), k -> new ShopGUIState());
        state.categoryId   = null;
        state.page         = 0;
        state.pendingItemId = null;

        Inventory inv = Bukkit.createInventory(null, 27, TITLE_CATEGORIES);

        List<ShopCategory> categories = ShopManager.getInstance().getCategories();
        if (categories.isEmpty()) {
            inv.setItem(13, makeItem(Material.PAPER,
                "<!italic><gray>No items available yet",
                "<!italic><gray>Check back later!"));
        } else {
            for (int i = 0; i < Math.min(categories.size(), CATEGORY_SLOTS.length); i++) {
                ShopCategory cat = categories.get(i);
                inv.setItem(CATEGORY_SLOTS[i], makeItem(cat.icon(),
                    "<!italic><aqua>" + cat.displayName(),
                    "<!italic><gray>" + cat.items().size() + " <green>items</green> available",
                    "",
                    "<!italic><green>\u2192 Click to browse " + cat.displayName() + "!"));
            }
        }

        player.openInventory(inv);
    }

    // ----------------------------------------------------------------
    // Open: Item Browser
    // ----------------------------------------------------------------

    public static void openItemBrowser(Player player, String catId, int page) {
        ShopGUIState state = states.computeIfAbsent(player.getUniqueId(), k -> new ShopGUIState());
        state.categoryId   = catId;
        state.page         = page;
        state.pendingItemId = null;

        Optional<ShopCategory> catOpt = ShopManager.getInstance().getCategory(catId);
        if (catOpt.isEmpty()) {
            player.sendMessage(MM.deserialize("<red>Category not found."));
            openCategoryPicker(player);
            return;
        }
        ShopCategory cat   = catOpt.get();
        List<ShopItem> items = cat.items();

        int totalPages = items.isEmpty() ? 1 : (int) Math.ceil(items.size() / 45.0);
        if (page >= totalPages) page = totalPages - 1;
        if (page < 0) page = 0;
        state.page = page;

        Inventory inv    = Bukkit.createInventory(null, 54, TITLE_ITEMS);
        EconomyAPI eco   = EconomyAPI.getInstance();

        int startIdx = page * 45;
        for (int slot = 0; slot < 45; slot++) {
            int itemIdx = startIdx + slot;
            if (itemIdx >= items.size()) {
                continue;
            }
            ShopItem shopItem = items.get(itemIdx);
            ItemStack display = shopItem.item().clone();
            ItemMeta meta     = display.getItemMeta();

            // Name
            if (shopItem.displayName() != null && !shopItem.displayName().isBlank()) {
                meta.displayName(MM.deserialize("<!italic>" + shopItem.displayName()));
            } else {
                meta.displayName(MM.deserialize("<!italic><white>" + formatMaterialName(display.getType())));
            }

            // Lore — spec Section 8 shop item format
            List<Component> lore = new ArrayList<>();

            lore.add(MM.deserialize("<!italic><aqua>\u2756 <gray>Buy: <white>" + fmt(shopItem.priceIgc()) + " <gold>tokens"));

            if (shopItem.sellable() && eco != null) {
                long sp = eco.getSellPrice(shopItem.item().getType(), player);
                if (sp > 0) lore.add(MM.deserialize("<!italic><aqua>\u2756 <gray>Sell: <white>" + fmt(sp) + " <gold>tokens"));
                else        lore.add(MM.deserialize("<!italic><aqua>\u2756 <gray>Sell: <red>Not Sellable"));
            } else {
                lore.add(MM.deserialize("<!italic><aqua>\u2756 <gray>Sell: <red>Not Sellable"));
            }

            lore.add(Component.empty());
            if (shopItem.isInStock()) {
                lore.add(MM.deserialize("<!italic><green>\u2192 <green>Left-click <underlined>to buy</underlined>."));
            } else {
                lore.add(MM.deserialize("<!italic><red>\u2717 Out of Stock"));
            }
            if (shopItem.sellable()) {
                lore.add(MM.deserialize("<!italic><green>\u2192 <green>Right-click <underlined>to sell</underlined>."));
            }

            meta.lore(lore);
            meta.addItemFlags(
                org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES,
                org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS,
                org.bukkit.inventory.ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            display.setItemMeta(meta);
            inv.setItem(slot, display);
        }

        // Navigation bar (row 6, slots 45-53)
        boolean hasPrev = page > 0;
        boolean hasNext = (page + 1) < totalPages;

        if (hasPrev) {
            inv.setItem(45, makeItem(Material.ARROW,
                "<!italic><gray>\u2190 Previous Page",
                "<!italic><gray>Page <white>" + page + " <gray>\u2190 <white>" + (page + 1)));
        }
        inv.setItem(46, makeItem(Material.BARRIER,
            "<!italic><red>\u2190 Back to Shop",
            "<!italic><gray>Return to Shop."));
        inv.setItem(49, makeItem(Material.BOOK, "<!italic><gray>Page <white>" + (page + 1) + " <gray>/ <white>" + totalPages));
        if (hasNext) {
            inv.setItem(53, makeItem(Material.LIME_DYE,
                "<!italic><green>\u2192 Next Page",
                "<!italic><gray>Page <white>" + (page + 1) + " <gray>\u2192 <white>" + (page + 2)));
        }

        player.openInventory(inv);
    }

    // ----------------------------------------------------------------
    // Open: Quantity Selector  (replaces old confirm-purchase screen)
    // ----------------------------------------------------------------

    public static void openQuantitySelector(Player player, String catId, String itemId) {
        ShopGUIState state = states.computeIfAbsent(player.getUniqueId(), k -> new ShopGUIState());
        state.categoryId   = catId;
        state.pendingItemId = itemId;

        Optional<ShopItem> itemOpt = ShopManager.getInstance().getItem(catId, itemId);
        if (itemOpt.isEmpty()) {
            player.sendMessage(MM.deserialize("<red>Item not found."));
            openCategoryPicker(player);
            return;
        }
        ShopItem shopItem = itemOpt.get();
        long balance = EconomyAPI.getInstance().getBalance(player.getUniqueId());
        long maxQty  = shopItem.priceIgc() > 0 ? balance / shopItem.priceIgc() : 64L;

        Inventory inv = Bukkit.createInventory(null, 27, TITLE_QTY);

        // Slot 13: item preview
        ItemStack preview = shopItem.item().clone();
        ItemMeta  pmeta   = preview.getItemMeta();
        if (shopItem.displayName() != null && !shopItem.displayName().isBlank()) {
            pmeta.displayName(MM.deserialize("<!italic>" + shopItem.displayName()));
        } else {
            pmeta.displayName(MM.deserialize("<!italic><white>" + formatMaterialName(preview.getType())));
        }
        List<Component> pLore = new ArrayList<>();
        pLore.add(MM.deserialize("<!italic><aqua>\u2756 <gray>Buy: <white>" + fmt(shopItem.priceIgc()) + " <gold>tokens <gray>each"));
        pLore.add(MM.deserialize("<!italic><aqua>\u2756 <gray>Balance: <white>" + fmt(balance) + " <gold>tokens"));
        pmeta.lore(pLore);
        pmeta.addItemFlags(
            org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES,
            org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS,
            org.bukkit.inventory.ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        preview.setItemMeta(pmeta);
        inv.setItem(13, preview);

        // Quantity buttons at slots 10, 11, 12, 14, 15, 16
        String[] labels = {"Buy 1", "Buy 8", "Buy 16", "Buy 32", "Buy 64", "Buy Max"};
        for (int i = 0; i < QTY_SLOTS.length; i++) {
            long qty = (QTY_AMOUNTS[i] == 0) ? maxQty : QTY_AMOUNTS[i];
            if (qty <= 0) qty = 0;
            long cost       = shopItem.priceIgc() * qty;
            boolean afford  = balance >= cost && qty > 0;

            String qtyLabel = (QTY_AMOUNTS[i] == 0) ? "Buy Max (" + qty + ")" : labels[i];
            Material btnMat = afford ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
            String nameTag  = "<!italic>" + (afford ? "<green>" : "<red>") + qtyLabel;
            String costTag  = afford
                ? "<!italic><gold>$ <gold>Cost: <white>" + fmt(cost) + " tokens"
                : "<!italic><red>\u2717 Cannot afford <dark_gray>(" + fmt(cost) + " tokens)";

            inv.setItem(QTY_SLOTS[i], makeItem(btnMat, nameTag, costTag));
        }

        // Slot 22: cancel
        inv.setItem(22, makeItem(Material.BARRIER,
            "<!italic><red>\u2717 Cancel",
            "<!italic><gray>Return to the item browser."));

        player.openInventory(inv);
    }

    // ----------------------------------------------------------------
    // Handle Click
    // ----------------------------------------------------------------

    public static void handleClick(Player player, int slot, ClickType click) {
        ShopGUIState state = states.get(player.getUniqueId());
        if (state == null) return;

        Component title = player.getOpenInventory().title();

        // ── Category picker ──────────────────────────────────────────
        if (TITLE_CATEGORIES.equals(title)) {
            for (int i = 0; i < CATEGORY_SLOTS.length; i++) {
                if (CATEGORY_SLOTS[i] == slot) {
                    List<ShopCategory> cats = ShopManager.getInstance().getCategories();
                    if (i < cats.size()) openItemBrowser(player, cats.get(i).id(), 0);
                    return;
                }
            }

        // ── Item browser ─────────────────────────────────────────────
        } else if (TITLE_ITEMS.equals(title)) {
            if (slot >= 0 && slot < 45) {
                Optional<ShopCategory> catOpt = ShopManager.getInstance().getCategory(state.categoryId);
                if (catOpt.isEmpty()) return;
                List<ShopItem> items = catOpt.get().items();
                int itemIdx = (state.page * 45) + slot;
                if (itemIdx >= items.size()) return;

                ItemStack clicked = player.getOpenInventory().getItem(slot);
                if (clicked == null || clicked.getType() == Material.AIR) return;

                ShopItem shopItem = items.get(itemIdx);

                if (click == ClickType.RIGHT || click == ClickType.SHIFT_RIGHT) {
                    handleSell(player, shopItem);
                } else {
                    if (!shopItem.isInStock()) {
                        player.sendMessage(MM.deserialize("<red>This item is out of stock."));
                        return;
                    }
                    openQuantitySelector(player, state.categoryId, shopItem.id());
                }

            } else if (slot == 45 && state.page > 0) {
                openItemBrowser(player, state.categoryId, state.page - 1);
            } else if (slot == 46) {
                openCategoryPicker(player);
            } else if (slot == 53) {
                Optional<ShopCategory> catOpt = ShopManager.getInstance().getCategory(state.categoryId);
                if (catOpt.isEmpty()) return;
                int totalPages = (int) Math.ceil(catOpt.get().items().size() / 45.0);
                if ((state.page + 1) < totalPages) openItemBrowser(player, state.categoryId, state.page + 1);
            }

        // ── Quantity selector ────────────────────────────────────────
        } else if (TITLE_QTY.equals(title)) {
            if (slot == 22) {
                // Cancel → back to item browser
                String catId = state.categoryId;
                int    pg    = state.page;
                player.closeInventory();
                Bukkit.getScheduler().runTask(ShopPlugin.getInstance(),
                    (Runnable) () -> openItemBrowser(player, catId, pg));
                return;
            }

            for (int i = 0; i < QTY_SLOTS.length; i++) {
                if (QTY_SLOTS[i] != slot) continue;

                Optional<ShopItem> itemOpt = ShopManager.getInstance().getItem(state.categoryId, state.pendingItemId);
                if (itemOpt.isEmpty()) { player.sendMessage(MM.deserialize("<red>Item not found.")); player.closeInventory(); return; }
                ShopItem shopItem = itemOpt.get();

                EconomyAPI eco  = EconomyAPI.getInstance();
                long balance    = eco.getBalance(player.getUniqueId());
                long qty;
                if (QTY_AMOUNTS[i] == 0) {
                    qty = shopItem.priceIgc() > 0 ? balance / shopItem.priceIgc() : 64L;
                } else {
                    qty = QTY_AMOUNTS[i];
                }
                if (qty <= 0) {
                    player.sendMessage(MM.deserialize("<red>You cannot afford any of this item."));
                    return;
                }

                ShopManager.PurchaseResult result = ShopManager.getInstance()
                    .purchaseMulti(player, state.categoryId, state.pendingItemId, (int) qty);

                String catId = state.categoryId;
                switch (result) {
                    case OK -> {
                        String name = itemDisplayName(shopItem);
                        player.sendMessage(MM.deserialize("<green>Purchased <white>" + qty + "x " + name
                            + "<green> for <gold>$" + fmt(shopItem.priceIgc() * qty) + "<green>."));
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_TRADE, 1.0f, 1.2f);
                        player.closeInventory();
                    }
                    case INSUFFICIENT_FUNDS -> {
                        player.sendMessage(MM.deserialize("<red>Insufficient funds."));
                        openQuantitySelector(player, state.categoryId, state.pendingItemId);
                    }
                    case OUT_OF_STOCK -> {
                        player.sendMessage(MM.deserialize("<red>Item is sold out."));
                        player.closeInventory();
                        Bukkit.getScheduler().runTask(ShopPlugin.getInstance(),
                            (Runnable) () -> openItemBrowser(player, catId, state.page));
                    }
                    case ITEM_NOT_FOUND -> {
                        player.sendMessage(MM.deserialize("<red>Item not found."));
                        player.closeInventory();
                    }
                    case INVENTORY_ERROR -> {
                        player.sendMessage(MM.deserialize("<red>Inventory full — free up space and try again."));
                        player.closeInventory();
                    }
                }
                return;
            }
        }
    }

    // ----------------------------------------------------------------
    // Right-click sell helper
    // ----------------------------------------------------------------

    private static void handleSell(Player player, ShopItem shopItem) {
        if (!shopItem.sellable()) {
            player.sendMessage(MM.deserialize("<red>This item is not sellable."));
            return;
        }
        EconomyAPI eco = EconomyAPI.getInstance();
        if (eco == null) {
            player.sendMessage(MM.deserialize("<red>Economy is not available."));
            return;
        }
        Material mat   = shopItem.item().getType();
        long unitPrice = eco.getSellPrice(mat, player);
        if (unitPrice <= 0) {
            player.sendMessage(MM.deserialize("<red>That item has no sell value."));
            return;
        }

        // Remove all stacks of this material from inventory
        long rawTotal = 0;
        int  count    = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack != null && stack.getType() == mat) {
                rawTotal += unitPrice * stack.getAmount();
                count    += stack.getAmount();
                stack.setAmount(0);
            }
        }

        if (count == 0) {
            player.sendMessage(MM.deserialize("<red>You don't have any of that item to sell."));
            return;
        }

        double mult  = eco.getExternalSellMultiplier(player.getUniqueId());
        long earned  = (long)(rawTotal * mult);
        eco.addBalance(player.getUniqueId(), earned);

        String name = itemDisplayName(shopItem);
        player.sendMessage(MM.deserialize(
            "<green>Sold <white>" + count + "x " + name + "<green> for <gold>$" + fmt(earned) + "<green>."));
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_TRADE, 1.0f, 1.0f);
    }

    // ----------------------------------------------------------------
    // Cleanup
    // ----------------------------------------------------------------

    public static void cleanup(UUID uuid) {
        states.remove(uuid);
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    static ItemStack makeItem(Material material, String name, String... loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta  meta = item.getItemMeta();
        meta.displayName(MM.deserialize(name));
        if (loreLines.length > 0) {
            List<Component> lore = new ArrayList<>();
            for (String line : loreLines) lore.add(MM.deserialize(line));
            meta.lore(lore);
        }
        item.setItemMeta(meta);
        return item;
    }

    private static String itemDisplayName(ShopItem shopItem) {
        if (shopItem.displayName() != null && !shopItem.displayName().isBlank())
            return shopItem.displayName();
        return formatMaterialName(shopItem.item().getType());
    }

    private static String formatMaterialName(Material material) {
        String[] words = material.name().toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                if (sb.length() > 0) sb.append(' ');
                sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
            }
        }
        return sb.toString();
    }

    private static String fmt(long n) {
        return String.format("%,d", n);
    }
}
