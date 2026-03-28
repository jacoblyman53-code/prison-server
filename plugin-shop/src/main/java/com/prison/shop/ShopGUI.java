package com.prison.shop;

import com.prison.economy.EconomyAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
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

    static final Component TITLE_CATEGORIES = MM.deserialize("<dark_green><bold>IGC Shop");
    static final Component TITLE_ITEMS      = MM.deserialize("<dark_green><bold>IGC Shop \u2014 Browse");
    static final Component TITLE_CONFIRM    = MM.deserialize("<green><bold>Confirm Purchase");

    // Category picker slots (27-slot GUI): row 1 and row 2 inner slots
    private static final int[] CATEGORY_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25
    };

    // ----------------------------------------------------------------
    // State
    // ----------------------------------------------------------------

    private static class ShopGUIState {
        String categoryId;
        int page;
        String pendingItemId;
    }

    private static final Map<UUID, ShopGUIState> states = new ConcurrentHashMap<>();

    public static boolean isTitle(Component title) {
        return TITLE_CATEGORIES.equals(title) || TITLE_ITEMS.equals(title) || TITLE_CONFIRM.equals(title);
    }

    // ----------------------------------------------------------------
    // Open: Category Picker
    // ----------------------------------------------------------------

    public static void openCategoryPicker(Player player) {
        ShopGUIState state = states.computeIfAbsent(player.getUniqueId(), k -> new ShopGUIState());
        state.categoryId = null;
        state.page = 0;
        state.pendingItemId = null;

        Inventory inv = Bukkit.createInventory(null, 27, TITLE_CATEGORIES);

        // Fill border with green glass panes
        ItemStack border = makeItem(Material.GREEN_STAINED_GLASS_PANE, "<green> ");
        for (int i = 0; i < 27; i++) {
            int row = i / 9;
            int col = i % 9;
            if (row == 0 || row == 2 || col == 0 || col == 8) {
                inv.setItem(i, border);
            }
        }

        List<ShopCategory> categories = ShopManager.getInstance().getCategories();

        if (categories.isEmpty()) {
            inv.setItem(13, makeItem(Material.GRAY_STAINED_GLASS_PANE,
                "<gray>No items available yet",
                "<dark_gray>Check back later!"));
        } else {
            for (int i = 0; i < Math.min(categories.size(), CATEGORY_SLOTS.length); i++) {
                ShopCategory cat = categories.get(i);
                int itemCount = cat.items().size();
                ItemStack catItem = makeItem(cat.icon(),
                    cat.displayName(),
                    "<gray>" + itemCount + " items available",
                    "<dark_gray>Click to browse");
                inv.setItem(CATEGORY_SLOTS[i], catItem);
            }
        }

        player.openInventory(inv);
    }

    // ----------------------------------------------------------------
    // Open: Item Browser
    // ----------------------------------------------------------------

    public static void openItemBrowser(Player player, String catId, int page) {
        ShopGUIState state = states.computeIfAbsent(player.getUniqueId(), k -> new ShopGUIState());
        state.categoryId = catId;
        state.page = page;
        state.pendingItemId = null;

        Optional<ShopCategory> catOpt = ShopManager.getInstance().getCategory(catId);
        if (catOpt.isEmpty()) {
            player.sendMessage(MM.deserialize("<red>Category not found."));
            openCategoryPicker(player);
            return;
        }
        ShopCategory cat = catOpt.get();
        List<ShopItem> items = cat.items();

        int totalPages = items.isEmpty() ? 1 : (int) Math.ceil(items.size() / 45.0);
        if (page >= totalPages) page = totalPages - 1;
        if (page < 0) page = 0;
        state.page = page;

        Inventory inv = Bukkit.createInventory(null, 54, TITLE_ITEMS);

        // Fill slots 0-44 with items
        ItemStack filler = makeItem(Material.GRAY_STAINED_GLASS_PANE, "<gray> ");
        int startIdx = page * 45;

        for (int slot = 0; slot < 45; slot++) {
            int itemIdx = startIdx + slot;
            if (itemIdx < items.size()) {
                ShopItem shopItem = items.get(itemIdx);
                ItemStack display = shopItem.item().clone();
                ItemMeta meta = display.getItemMeta();

                // Set display name
                if (shopItem.displayName() != null && !shopItem.displayName().isBlank()) {
                    meta.displayName(MM.deserialize(shopItem.displayName()));
                } else {
                    // Format material name
                    String matName = formatMaterialName(display.getType());
                    meta.displayName(MM.deserialize("<white>" + matName));
                }

                // Build lore
                List<Component> lore = new ArrayList<>();
                lore.add(MM.deserialize("<yellow>Price: <gold>" + String.format("%,d", shopItem.priceIgc()) + " IGC"));
                if (shopItem.stock() == -1) {
                    lore.add(MM.deserialize("<gray>Stock: <green>\u221e Unlimited"));
                } else if (shopItem.stock() > 0) {
                    lore.add(MM.deserialize("<gray>Stock: <white>" + shopItem.stock()));
                } else {
                    lore.add(MM.deserialize("<red>SOLD OUT"));
                }
                lore.add(Component.empty());
                if (shopItem.isInStock()) {
                    lore.add(MM.deserialize("<green>Click to purchase"));
                } else {
                    lore.add(MM.deserialize("<red>Out of Stock"));
                }
                meta.lore(lore);
                display.setItemMeta(meta);
                inv.setItem(slot, display);
            } else {
                inv.setItem(slot, filler);
            }
        }

        // Bottom row navigation
        boolean hasPrev = page > 0;
        boolean hasNext = (page + 1) < totalPages;

        inv.setItem(45, hasPrev
            ? makeItem(Material.ARROW, "<white>\u25c4 Previous")
            : filler.clone());

        inv.setItem(46, makeItem(Material.BARRIER, "<gray>\u2190 Back to Categories"));

        // Fill slots 47, 48 with filler
        inv.setItem(47, filler.clone());
        inv.setItem(48, filler.clone());

        inv.setItem(49, makeItem(Material.BOOK, "<gray>Page " + (page + 1) + "/" + totalPages));

        // Fill slots 50, 51, 52 with filler
        inv.setItem(50, filler.clone());
        inv.setItem(51, filler.clone());
        inv.setItem(52, filler.clone());

        inv.setItem(53, hasNext
            ? makeItem(Material.ARROW, "<white>Next \u25ba")
            : filler.clone());

        player.openInventory(inv);
    }

    // ----------------------------------------------------------------
    // Open: Confirm Purchase
    // ----------------------------------------------------------------

    public static void openConfirmPurchase(Player player, String catId, String itemId) {
        ShopGUIState state = states.computeIfAbsent(player.getUniqueId(), k -> new ShopGUIState());
        state.categoryId = catId;
        state.pendingItemId = itemId;

        Optional<ShopItem> itemOpt = ShopManager.getInstance().getItem(catId, itemId);
        if (itemOpt.isEmpty()) {
            player.sendMessage(MM.deserialize("<red>Item not found."));
            openCategoryPicker(player);
            return;
        }
        ShopItem shopItem = itemOpt.get();

        long balance = EconomyAPI.getInstance().getBalance(player.getUniqueId());
        boolean canAfford = balance >= shopItem.priceIgc();

        Inventory inv = Bukkit.createInventory(null, 27, TITLE_CONFIRM);

        // Fill borders with cyan glass panes
        ItemStack border = makeItem(Material.CYAN_STAINED_GLASS_PANE, "<cyan> ");
        for (int i = 0; i < 27; i++) {
            int row = i / 9;
            int col = i % 9;
            if (row == 0 || row == 2 || col == 0 || col == 8) {
                inv.setItem(i, border);
            }
        }

        // Slot 13: item preview
        ItemStack preview = shopItem.item().clone();
        ItemMeta previewMeta = preview.getItemMeta();
        if (shopItem.displayName() != null && !shopItem.displayName().isBlank()) {
            previewMeta.displayName(MM.deserialize(shopItem.displayName()));
        } else {
            previewMeta.displayName(MM.deserialize("<white>" + formatMaterialName(preview.getType())));
        }
        List<Component> previewLore = new ArrayList<>();
        previewLore.add(MM.deserialize("<yellow>Price: <gold>" + String.format("%,d", shopItem.priceIgc()) + " IGC"));
        previewLore.add(MM.deserialize("<gray>Your balance: <white>" + String.format("%,d", balance) + " IGC"));
        previewLore.add(Component.empty());
        previewLore.add(MM.deserialize("<green>Confirm: slot 11 <red>| Cancel: slot 15"));
        previewMeta.lore(previewLore);
        preview.setItemMeta(previewMeta);
        inv.setItem(13, preview);

        // Slot 11: buy button
        if (canAfford) {
            long balAfter = balance - shopItem.priceIgc();
            inv.setItem(11, makeItem(Material.LIME_STAINED_GLASS_PANE,
                "<green>\u2713 Buy for " + String.format("%,d", shopItem.priceIgc()) + " IGC",
                "<gray>Balance after: <white>" + String.format("%,d", balAfter) + " IGC"));
        } else {
            inv.setItem(11, makeItem(Material.GRAY_STAINED_GLASS_PANE,
                "<red>\u2717 Cannot Afford",
                "<red>Need <gold>" + String.format("%,d", shopItem.priceIgc()) + " IGC<red>, have <white>" + String.format("%,d", balance) + " IGC"));
        }

        // Slot 15: cancel button
        inv.setItem(15, makeItem(Material.RED_STAINED_GLASS_PANE,
            "<red>\u2717 Cancel",
            "<gray>Return to shop."));

        player.openInventory(inv);
    }

    // ----------------------------------------------------------------
    // Handle Click
    // ----------------------------------------------------------------

    public static void handleClick(Player player, int slot, ClickType click) {
        ShopGUIState state = states.get(player.getUniqueId());
        if (state == null) return;

        Component title = player.getOpenInventory().title();

        if (TITLE_CATEGORIES.equals(title)) {
            // Category picker click
            for (int i = 0; i < CATEGORY_SLOTS.length; i++) {
                if (CATEGORY_SLOTS[i] == slot) {
                    List<ShopCategory> categories = ShopManager.getInstance().getCategories();
                    if (i < categories.size()) {
                        openItemBrowser(player, categories.get(i).id(), 0);
                    }
                    return;
                }
            }

        } else if (TITLE_ITEMS.equals(title)) {
            // Item browser click
            if (slot >= 0 && slot < 45) {
                Optional<ShopCategory> catOpt = ShopManager.getInstance().getCategory(state.categoryId);
                if (catOpt.isEmpty()) return;
                List<ShopItem> items = catOpt.get().items();
                int itemIdx = (state.page * 45) + slot;
                if (itemIdx >= items.size()) return;

                // Check it's not a filler
                ItemStack clickedItem = player.getOpenInventory().getItem(slot);
                if (clickedItem == null
                    || clickedItem.getType() == Material.GRAY_STAINED_GLASS_PANE
                    || clickedItem.getType() == Material.AIR) return;

                ShopItem shopItem = items.get(itemIdx);
                openConfirmPurchase(player, state.categoryId, shopItem.id());

            } else if (slot == 45) {
                // Previous page
                if (state.page > 0) {
                    openItemBrowser(player, state.categoryId, state.page - 1);
                }
            } else if (slot == 46) {
                // Back to categories
                openCategoryPicker(player);
            } else if (slot == 53) {
                // Next page
                Optional<ShopCategory> catOpt = ShopManager.getInstance().getCategory(state.categoryId);
                if (catOpt.isEmpty()) return;
                List<ShopItem> items = catOpt.get().items();
                int totalPages = (int) Math.ceil(items.size() / 45.0);
                if ((state.page + 1) < totalPages) {
                    openItemBrowser(player, state.categoryId, state.page + 1);
                }
            }

        } else if (TITLE_CONFIRM.equals(title)) {
            // Confirm purchase click
            if (slot == 11) {
                // Buy
                ShopManager.PurchaseResult result = ShopManager.getInstance()
                    .purchase(player, state.categoryId, state.pendingItemId);

                String catId = state.categoryId;

                switch (result) {
                    case OK -> {
                        Optional<ShopItem> itemOpt = ShopManager.getInstance().getItem(catId, state.pendingItemId);
                        long price = itemOpt.map(ShopItem::priceIgc).orElse(0L);
                        player.sendMessage(MM.deserialize("<green>Purchased! <gold>" + String.format("%,d", price) + " IGC<green> deducted."));
                        player.closeInventory();
                    }
                    case INSUFFICIENT_FUNDS -> {
                        player.sendMessage(MM.deserialize("<red>Insufficient funds."));
                        player.closeInventory();
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
                        player.sendMessage(MM.deserialize("<red>Inventory error."));
                        player.closeInventory();
                    }
                }

            } else if (slot == 15) {
                // Cancel — reopen item browser on next tick
                String catId = state.categoryId;
                int page = state.page;
                player.closeInventory();
                Bukkit.getScheduler().runTask(ShopPlugin.getInstance(),
                    (Runnable) () -> openItemBrowser(player, catId, page));
            }
        }
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
        ItemMeta meta = item.getItemMeta();
        meta.displayName(MM.deserialize(name));
        if (loreLines.length > 0) {
            List<Component> lore = new ArrayList<>();
            for (String line : loreLines) {
                lore.add(MM.deserialize(line));
            }
            meta.lore(lore);
        }
        item.setItemMeta(meta);
        return item;
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
}
