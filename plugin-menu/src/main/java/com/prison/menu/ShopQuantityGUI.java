package com.prison.menu;

import com.prison.economy.EconomyAPI;
import com.prison.menu.util.*;
import com.prison.shop.ShopItem;
import com.prison.shop.ShopManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ShopQuantityGUI — 27-slot quantity picker opened after clicking a shop item.
 *
 * Slot  4 : item preview (name + per-unit price)
 * Slots 9–14 : ×1, ×4, ×8, ×16, ×32, ×64 — green if affordable, red if not
 * Slot 22 : back → return to the category page
 */
public class ShopQuantityGUI {

    public static final Component TITLE =
        MiniMessage.miniMessage().deserialize("<!italic><dark_gray>[ <yellow>How many? <dark_gray>]");
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private static final int   SLOT_PREVIEW = 4;
    private static final int[] QTY_SLOTS    = { 9, 10, 11, 12, 13, 14 };
    private static final int[] QTY_VALUES   = { 1,  4,  8, 16, 32, 64 };
    private static final int   SLOT_BACK    = 22;

    // Per-player state: [categoryId, itemId, pageNumber]
    private static final Map<UUID, String[]> STATE = new ConcurrentHashMap<>();

    // ----------------------------------------------------------------

    public static void open(Player player, String categoryId, String itemId, int page) {
        STATE.put(player.getUniqueId(), new String[]{ categoryId, itemId, String.valueOf(page) });
        player.openInventory(build(player, categoryId, itemId));
        Sounds.nav(player);
    }

    public static void handleClick(Player player, int slot, MenuPlugin plugin) {
        String[] state = STATE.get(player.getUniqueId());

        if (slot == SLOT_BACK) {
            Sounds.nav(player);
            STATE.remove(player.getUniqueId());
            if (state != null) {
                ShopCategoryPageGUI.open(player, state[0], Integer.parseInt(state[2]));
            } else {
                ShopCategoryPickerGUI.open(player);
            }
            return;
        }

        if (state == null) return;

        for (int i = 0; i < QTY_SLOTS.length; i++) {
            if (QTY_SLOTS[i] == slot) {
                handleBuy(player, state[0], state[1], QTY_VALUES[i], Integer.parseInt(state[2]));
                return;
            }
        }
    }

    // ----------------------------------------------------------------

    private static void handleBuy(Player player, String catId, String itemId, int qty, int returnPage) {
        ShopManager sm = ShopManager.getInstance();
        if (sm == null) return;

        ShopManager.PurchaseResult result = sm.purchaseMulti(player, catId, itemId, qty);

        switch (result) {
            case OK -> {
                Sounds.buy(player);
                player.sendMessage(MM.deserialize("<green>Purchased <white>×" + qty + "</white> successfully!"));
                player.showTitle(net.kyori.adventure.title.Title.title(
                    MM.deserialize("<green><bold>✓ Purchased!"),
                    MM.deserialize("<gray>×" + qty),
                    net.kyori.adventure.title.Title.Times.times(
                        Duration.ofMillis(80),
                        Duration.ofMillis(1200),
                        Duration.ofMillis(300)
                    )
                ));
                // Refresh qty selector (balance may have changed)
                open(player, catId, itemId, returnPage);
            }
            case INSUFFICIENT_FUNDS -> {
                Sounds.deny(player);
                player.sendMessage(MM.deserialize("<red>Not enough IGC to buy <white>×" + qty + "</white>."));
                open(player, catId, itemId, returnPage);
            }
            case OUT_OF_STOCK -> {
                Sounds.deny(player);
                player.sendMessage(MM.deserialize("<red>That item is out of stock."));
                STATE.remove(player.getUniqueId());
                ShopCategoryPageGUI.open(player, catId, returnPage);
            }
            case INVENTORY_ERROR -> {
                Sounds.deny(player);
                player.sendMessage(MM.deserialize("<red>Your inventory is too full. Free up some space."));
                open(player, catId, itemId, returnPage);
            }
            case ITEM_NOT_FOUND -> {
                Sounds.deny(player);
                player.sendMessage(MM.deserialize("<red>Item not found."));
            }
        }
    }

    // ----------------------------------------------------------------

    private static Inventory build(Player player, String categoryId, String itemId) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);
        Gui.fillAll(inv);

        ShopManager sm = ShopManager.getInstance();
        if (sm == null) {
            inv.setItem(SLOT_BACK, Gui.back());
            return inv;
        }

        ShopItem shopItem = sm.getItem(categoryId, itemId).orElse(null);
        if (shopItem == null) {
            inv.setItem(SLOT_BACK, Gui.back());
            return inv;
        }

        long balance = 0;
        try {
            EconomyAPI api = EconomyAPI.getInstance();
            if (api != null) balance = api.getBalance(player.getUniqueId());
        } catch (Exception ignored) {}

        String displayName = shopItem.displayName() != null
            ? shopItem.displayName()
            : "<white>" + Fmt.mat(shopItem.item().getType().name());
        long unitPrice = shopItem.priceIgc();

        // ── Slot 4: item preview ────────────────────────────────────
        List<Component> previewLore = new ArrayList<>();
        previewLore.add(MM.deserialize("<!italic><gray>Price: <gold>" + Fmt.number(unitPrice) + " IGC <gray>each"));
        previewLore.add(MM.deserialize("<!italic><gray>Your balance: <gold>" + Fmt.number(balance) + " IGC"));
        previewLore.add(Component.empty());
        previewLore.add(MM.deserialize("<!italic><yellow>Pick a quantity below."));
        inv.setItem(SLOT_PREVIEW, Gui.make(shopItem.item().getType(), displayName, previewLore));

        // ── Slots 9–14: quantity buttons ────────────────────────────
        for (int i = 0; i < QTY_SLOTS.length; i++) {
            int  qty       = QTY_VALUES[i];
            long total     = unitPrice * qty;
            boolean afford = balance >= total;

            Material mat  = afford ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
            String   name = (afford ? "<green>" : "<red>") + "×" + qty;
            String total2 = "<gray>Total: " + (afford ? "<gold>" : "<red>") + Fmt.number(total) + " IGC";
            String  hint  = afford ? "<green>Click to buy!" : "<red>Cannot afford.";

            inv.setItem(QTY_SLOTS[i], Gui.make(mat, name, total2, hint));
        }

        inv.setItem(SLOT_BACK, Gui.back());
        return inv;
    }
}
