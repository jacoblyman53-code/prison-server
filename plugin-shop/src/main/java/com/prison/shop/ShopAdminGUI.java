package com.prison.shop;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ShopAdminGUI {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    static final Component TITLE_MAIN     = MM.deserialize("<dark_red><bold>Shop Admin");
    static final Component TITLE_CATEGORY = MM.deserialize("<dark_red><bold>Shop Admin \u2014 Category");
    static final Component TITLE_ITEM     = MM.deserialize("<dark_red><bold>Shop Admin \u2014 Item");

    // Usable slots in a 54-slot GUI with borders at rows 0 and 5, columns 0 and 8
    private static final int[] ADMIN_CONTENT_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34
    };

    // ----------------------------------------------------------------
    // State
    // ----------------------------------------------------------------

    private static class AdminState {
        String categoryId;
        String itemId;
        ItemStack pendingNewItem;
        Long pendingNewItemPrice;
    }

    private static final Map<UUID, AdminState> adminStates = new ConcurrentHashMap<>();

    public static boolean isTitle(Component title) {
        return TITLE_MAIN.equals(title) || TITLE_CATEGORY.equals(title) || TITLE_ITEM.equals(title);
    }

    // ----------------------------------------------------------------
    // Open: Main View
    // ----------------------------------------------------------------

    public static void open(Player player) {
        AdminState state = adminStates.computeIfAbsent(player.getUniqueId(), k -> new AdminState());
        state.categoryId = null;
        state.itemId = null;

        Inventory inv = Bukkit.createInventory(null, 54, TITLE_MAIN);

        // Fill borders with dark red glass panes
        ItemStack border = makeItem(Material.RED_STAINED_GLASS_PANE, "<dark_red> ");
        fillBorders(inv, 54, border);

        List<ShopCategory> categories = ShopManager.getInstance().getCategories();
        for (int i = 0; i < Math.min(categories.size(), ADMIN_CONTENT_SLOTS.length); i++) {
            ShopCategory cat = categories.get(i);
            ItemStack catItem = makeItem(cat.icon(),
                cat.displayName(),
                "<gray>" + cat.items().size() + " items",
                "<dark_gray>Click to manage",
                "<red>Shift-click to delete");
            inv.setItem(ADMIN_CONTENT_SLOTS[i], catItem);
        }

        // Slot 45: Add Category
        inv.setItem(45, makeItem(Material.LIME_DYE,
            "<green>Add Category",
            "<gray>Click to add a new shop category.",
            "<dark_gray>You will be prompted for a name."));

        // Slot 53: Close
        inv.setItem(53, makeItem(Material.BARRIER, "<gray>Close"));

        player.openInventory(inv);
    }

    // ----------------------------------------------------------------
    // Open: Category Editor
    // ----------------------------------------------------------------

    public static void openCategoryEditor(Player player, String catId) {
        AdminState state = adminStates.computeIfAbsent(player.getUniqueId(), k -> new AdminState());
        state.categoryId = catId;
        state.itemId = null;

        Optional<ShopCategory> catOpt = ShopManager.getInstance().getCategory(catId);
        if (catOpt.isEmpty()) {
            player.sendMessage(MM.deserialize("<red>Category not found."));
            open(player);
            return;
        }
        ShopCategory cat = catOpt.get();

        Inventory inv = Bukkit.createInventory(null, 54, TITLE_CATEGORY);

        ItemStack border = makeItem(Material.RED_STAINED_GLASS_PANE, "<dark_red> ");
        fillBorders(inv, 54, border);

        List<ShopItem> items = cat.items();
        for (int i = 0; i < Math.min(items.size(), ADMIN_CONTENT_SLOTS.length); i++) {
            ShopItem shopItem = items.get(i);
            ItemStack display = shopItem.item().clone();
            ItemMeta meta = display.getItemMeta();

            String nameStr = shopItem.displayName() != null && !shopItem.displayName().isBlank()
                ? shopItem.displayName()
                : "<white>" + formatMaterialName(display.getType());
            meta.displayName(MM.deserialize(nameStr));

            String stockStr = shopItem.stock() == -1 ? "\u221e" : String.valueOf(shopItem.stock());
            List<Component> lore = new ArrayList<>();
            lore.add(MM.deserialize("<gray>Price: <gold>$" + String.format("%,d", shopItem.priceIgc())));
            lore.add(MM.deserialize("<gray>Stock: <white>" + stockStr));
            lore.add(MM.deserialize("<dark_gray>Click to edit price/stock"));
            lore.add(MM.deserialize("<red>Shift-click to remove"));
            meta.lore(lore);
            display.setItemMeta(meta);
            inv.setItem(ADMIN_CONTENT_SLOTS[i], display);
        }

        // Slot 45: Add Item
        inv.setItem(45, makeItem(Material.LIME_DYE,
            "<green>Add Item",
            "<gray>Hold item in hand and click.",
            "<dark_gray>You will be prompted for price and stock."));

        // Slot 46: Back
        inv.setItem(46, makeItem(Material.ARROW, "<gray>\u2190 Back"));

        // Slot 53: Close
        inv.setItem(53, makeItem(Material.BARRIER, "<gray>Close"));

        player.openInventory(inv);
    }

    // ----------------------------------------------------------------
    // Open: Item Editor
    // ----------------------------------------------------------------

    public static void openItemEditor(Player player, String catId, String itemId) {
        AdminState state = adminStates.computeIfAbsent(player.getUniqueId(), k -> new AdminState());
        state.categoryId = catId;
        state.itemId = itemId;

        Optional<ShopItem> itemOpt = ShopManager.getInstance().getItem(catId, itemId);
        if (itemOpt.isEmpty()) {
            player.sendMessage(MM.deserialize("<red>Item not found."));
            openCategoryEditor(player, catId);
            return;
        }
        ShopItem shopItem = itemOpt.get();

        Inventory inv = Bukkit.createInventory(null, 27, TITLE_ITEM);

        // Fill borders with orange glass panes
        ItemStack border = makeItem(Material.ORANGE_STAINED_GLASS_PANE, "<orange> ");
        fillBorders(inv, 27, border);

        // Slot 13: Item preview
        ItemStack preview = shopItem.item().clone();
        ItemMeta previewMeta = preview.getItemMeta();
        String nameStr = shopItem.displayName() != null && !shopItem.displayName().isBlank()
            ? shopItem.displayName()
            : "<white>" + formatMaterialName(preview.getType());
        previewMeta.displayName(MM.deserialize(nameStr));
        String stockStr = shopItem.stock() == -1 ? "\u221e Unlimited" : String.valueOf(shopItem.stock());
        List<Component> previewLore = new ArrayList<>();
        previewLore.add(MM.deserialize("<gray>Price: <gold>$" + String.format("%,d", shopItem.priceIgc())));
        previewLore.add(MM.deserialize("<gray>Stock: <white>" + stockStr));
        previewMeta.lore(previewLore);
        preview.setItemMeta(previewMeta);
        inv.setItem(13, preview);

        // Slot 10: Edit Price
        inv.setItem(10, makeItem(Material.GOLD_NUGGET,
            "<gold>Edit Price",
            "<gray>Current: <white>$" + String.format("%,d", shopItem.priceIgc()),
            "<dark_gray>Click to change."));

        // Slot 12: Edit Stock
        String currentStock = shopItem.stock() == -1 ? "Unlimited" : String.valueOf(shopItem.stock());
        inv.setItem(12, makeItem(Material.COMPARATOR,
            "<aqua>Edit Stock",
            "<gray>Current: <white>" + currentStock,
            "<dark_gray>Click to change.",
            "<dark_gray>Enter -1 for unlimited."));

        // Slot 14: Remove Item
        inv.setItem(14, makeItem(Material.RED_STAINED_GLASS_PANE,
            "<red>Remove Item",
            "<red>Permanently removes this item.",
            "<dark_gray>Cannot be undone."));

        // Slot 16: Back
        inv.setItem(16, makeItem(Material.ARROW, "<gray>\u2190 Back to Category"));

        player.openInventory(inv);
    }

    // ----------------------------------------------------------------
    // Handle Click
    // ----------------------------------------------------------------

    public static void handleClick(Player player, int slot, InventoryClickEvent event) {
        AdminState state = adminStates.computeIfAbsent(player.getUniqueId(), k -> new AdminState());
        Component title = player.getOpenInventory().title();
        ClickType click = event.getClick();

        if (TITLE_MAIN.equals(title)) {
            handleMainClick(player, slot, click, state);
        } else if (TITLE_CATEGORY.equals(title)) {
            handleCategoryClick(player, slot, click, state, event);
        } else if (TITLE_ITEM.equals(title)) {
            handleItemClick(player, slot, state);
        }
    }

    private static void handleMainClick(Player player, int slot, ClickType click, AdminState state) {
        // Check category content slots
        for (int i = 0; i < ADMIN_CONTENT_SLOTS.length; i++) {
            if (ADMIN_CONTENT_SLOTS[i] == slot) {
                List<ShopCategory> categories = ShopManager.getInstance().getCategories();
                if (i >= categories.size()) return;
                ShopCategory cat = categories.get(i);

                if (click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT) {
                    // Delete category
                    ShopManager.getInstance().removeCategory(cat.id());
                    player.sendMessage(MM.deserialize("<yellow>Category <white>" + cat.id() + "<yellow> deleted."));
                    open(player);
                } else {
                    openCategoryEditor(player, cat.id());
                }
                return;
            }
        }

        if (slot == 45) {
            // Add category via anvil
            ShopAnvilInput.open(player, "category-id", text -> addCategory(player, text));
        } else if (slot == 53) {
            player.closeInventory();
        }
    }

    private static void handleCategoryClick(Player player, int slot, ClickType click, AdminState state, InventoryClickEvent event) {
        String catId = state.categoryId;
        if (catId == null) return;

        // Check item content slots
        for (int i = 0; i < ADMIN_CONTENT_SLOTS.length; i++) {
            if (ADMIN_CONTENT_SLOTS[i] == slot) {
                Optional<ShopCategory> catOpt = ShopManager.getInstance().getCategory(catId);
                if (catOpt.isEmpty()) return;
                List<ShopItem> items = catOpt.get().items();
                if (i >= items.size()) return;
                ShopItem shopItem = items.get(i);

                if (click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT) {
                    ShopManager.getInstance().removeItem(catId, shopItem.id());
                    player.sendMessage(MM.deserialize("<yellow>Item <white>" + shopItem.id() + "<yellow> removed."));
                    Bukkit.getScheduler().runTask(ShopPlugin.getInstance(),
                        (Runnable) () -> openCategoryEditor(player, catId));
                } else {
                    openItemEditor(player, catId, shopItem.id());
                }
                return;
            }
        }

        if (slot == 45) {
            // Add item — check main hand
            ItemStack held = player.getInventory().getItemInMainHand();
            if (held.getType() == Material.AIR) {
                player.sendMessage(MM.deserialize("<red>Hold the item you want to sell in your main hand."));
                return;
            }
            ItemStack cloned = held.clone();
            state.pendingNewItem = cloned;
            state.pendingNewItemPrice = null;

            // First: ask for price
            ShopAnvilInput.open(player, "5000", priceText -> {
                long price;
                try {
                    price = Long.parseLong(priceText.trim());
                } catch (NumberFormatException e) {
                    player.sendMessage(MM.deserialize("<red>Invalid price. Item not added."));
                    Bukkit.getScheduler().runTask(ShopPlugin.getInstance(),
                        (Runnable) () -> openCategoryEditor(player, catId));
                    return;
                }
                state.pendingNewItemPrice = price;

                // Second: ask for stock
                ShopAnvilInput.open(player, "-1", stockText -> {
                    int stock;
                    try {
                        stock = Integer.parseInt(stockText.trim());
                    } catch (NumberFormatException e2) {
                        player.sendMessage(MM.deserialize("<red>Invalid stock. Item not added."));
                        Bukkit.getScheduler().runTask(ShopPlugin.getInstance(),
                            (Runnable) () -> openCategoryEditor(player, catId));
                        return;
                    }

                    ItemStack finalItem = state.pendingNewItem;
                    long finalPrice = state.pendingNewItemPrice != null ? state.pendingNewItemPrice : 0L;
                    state.pendingNewItem = null;
                    state.pendingNewItemPrice = null;

                    ShopManager.getInstance().addItem(catId, finalItem, null, finalPrice, stock, false);
                    player.sendMessage(MM.deserialize("<green>Item added to shop."));
                    Bukkit.getScheduler().runTask(ShopPlugin.getInstance(),
                        (Runnable) () -> openCategoryEditor(player, catId));
                });
            });

        } else if (slot == 46) {
            // Back to main
            open(player);
        } else if (slot == 53) {
            player.closeInventory();
        }
    }

    private static void handleItemClick(Player player, int slot, AdminState state) {
        String catId = state.categoryId;
        String itemId = state.itemId;
        if (catId == null || itemId == null) return;

        if (slot == 10) {
            // Edit price
            Optional<ShopItem> itemOpt = ShopManager.getInstance().getItem(catId, itemId);
            if (itemOpt.isEmpty()) return;
            String currentPrice = String.valueOf(itemOpt.get().priceIgc());

            ShopAnvilInput.open(player, currentPrice, text -> {
                long newPrice;
                try {
                    newPrice = Long.parseLong(text.trim());
                } catch (NumberFormatException e) {
                    player.sendMessage(MM.deserialize("<red>Invalid price."));
                    Bukkit.getScheduler().runTask(ShopPlugin.getInstance(),
                        (Runnable) () -> openItemEditor(player, catId, itemId));
                    return;
                }
                ShopManager.getInstance().updateItemPrice(catId, itemId, newPrice);
                player.sendMessage(MM.deserialize("<green>Price updated."));
                Bukkit.getScheduler().runTask(ShopPlugin.getInstance(),
                    (Runnable) () -> openItemEditor(player, catId, itemId));
            });

        } else if (slot == 12) {
            // Edit stock
            Optional<ShopItem> itemOpt = ShopManager.getInstance().getItem(catId, itemId);
            if (itemOpt.isEmpty()) return;
            String currentStock = String.valueOf(itemOpt.get().stock());

            ShopAnvilInput.open(player, currentStock, text -> {
                int newStock;
                try {
                    newStock = Integer.parseInt(text.trim());
                } catch (NumberFormatException e) {
                    player.sendMessage(MM.deserialize("<red>Invalid stock value."));
                    Bukkit.getScheduler().runTask(ShopPlugin.getInstance(),
                        (Runnable) () -> openItemEditor(player, catId, itemId));
                    return;
                }
                ShopManager.getInstance().updateItemStock(catId, itemId, newStock);
                player.sendMessage(MM.deserialize("<green>Stock updated."));
                Bukkit.getScheduler().runTask(ShopPlugin.getInstance(),
                    (Runnable) () -> openItemEditor(player, catId, itemId));
            });

        } else if (slot == 14) {
            // Remove item
            ShopManager.getInstance().removeItem(catId, itemId);
            player.sendMessage(MM.deserialize("<yellow>Item removed."));
            openCategoryEditor(player, catId);

        } else if (slot == 16) {
            // Back to category editor
            openCategoryEditor(player, catId);
        }
    }

    // ----------------------------------------------------------------
    // Add Category (after anvil input)
    // ----------------------------------------------------------------

    private static void addCategory(Player player, String rawText) {
        String id = rawText.toLowerCase().replaceAll("[^a-z0-9_]", "_")
            .replaceAll("^_+|_+$", "");

        if (id.isBlank()) {
            player.sendMessage(MM.deserialize("<red>Invalid category name."));
            Bukkit.getScheduler().runTask(ShopPlugin.getInstance(), (Runnable) () -> open(player));
            return;
        }

        if (ShopManager.getInstance().getCategory(id).isPresent()) {
            player.sendMessage(MM.deserialize("<red>A category with that ID already exists."));
            Bukkit.getScheduler().runTask(ShopPlugin.getInstance(), (Runnable) () -> open(player));
            return;
        }

        // Build a display name from the id
        String[] words = id.split("_");
        StringBuilder displayName = new StringBuilder("<white>");
        for (int i = 0; i < words.length; i++) {
            if (i > 0) displayName.append(" ");
            if (!words[i].isEmpty()) {
                displayName.append(Character.toUpperCase(words[i].charAt(0)));
                displayName.append(words[i].substring(1));
            }
        }

        ShopManager.getInstance().addCategory(id, displayName.toString(), Material.CHEST);
        player.sendMessage(MM.deserialize("<green>Category <white>" + id + "<green> created."));
        Bukkit.getScheduler().runTask(ShopPlugin.getInstance(), (Runnable) () -> open(player));
    }

    // ----------------------------------------------------------------
    // Cleanup
    // ----------------------------------------------------------------

    public static void cleanup(UUID uuid) {
        adminStates.remove(uuid);
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private static void fillBorders(Inventory inv, int size, ItemStack border) {
        int rows = size / 9;
        for (int i = 0; i < size; i++) {
            int row = i / 9;
            int col = i % 9;
            if (row == 0 || row == rows - 1 || col == 0 || col == 8) {
                inv.setItem(i, border);
            }
        }
    }

    private static ItemStack makeItem(Material material, String name, String... loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(MiniMessage.miniMessage().deserialize(name));
        if (loreLines.length > 0) {
            List<Component> lore = new ArrayList<>();
            for (String line : loreLines) {
                lore.add(MiniMessage.miniMessage().deserialize(line));
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
