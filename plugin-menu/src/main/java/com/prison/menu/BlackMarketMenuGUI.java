package com.prison.menu;

import com.prison.menu.util.*;
import com.prison.shop.BlackMarketItem;
import com.prison.shop.BlackMarketManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;

public class BlackMarketMenuGUI {

    public static final Component TITLE = MiniMessage.miniMessage().deserialize("<!italic>Black Market");
    private static final MiniMessage MM = MiniMessage.miniMessage();

    // 27-slot layout: row 1 = slots 9-17, content items at slots 10-15 (6 items max)
    private static final int[] ITEM_SLOTS = {10, 11, 12, 13, 14, 15};
    private static final int SLOT_BACK   = 18;
    private static final int SLOT_TIMER  = 22;

    public static void open(Player player) {
        player.openInventory(build(player));
        Sounds.nav(player);
    }

    public static void handleClick(Player player, int slot, MenuPlugin plugin) {
        if (slot == SLOT_BACK) {
            Sounds.nav(player);
            MainMenuGUI.open(player);
            return;
        }

        BlackMarketManager bmm = BlackMarketManager.getInstance();
        if (bmm == null) return;

        List<BlackMarketItem> items = bmm.getCurrentItems();

        for (int i = 0; i < ITEM_SLOTS.length && i < items.size(); i++) {
            if (ITEM_SLOTS[i] == slot) {
                BlackMarketItem bmi = items.get(i);
                if (!bmi.isInStock()) {
                    Sounds.deny(player);
                    player.sendMessage(MM.deserialize("<red>That item is out of stock."));
                    return;
                }
                BlackMarketManager.PurchaseResult result = bmm.purchaseItem(player, bmi.getId());
                handlePurchaseResult(player, result, bmi);
                // Refresh GUI to reflect updated stock
                open(player);
                return;
            }
        }
    }

    // ----------------------------------------------------------------

    private static Inventory build(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);

        // Slot 0: back
        inv.setItem(0, Gui.back());

        BlackMarketManager bmm = BlackMarketManager.getInstance();

        if (bmm != null) {
            List<BlackMarketItem> items = bmm.getCurrentItems();

            if (items.isEmpty()) {
                inv.setItem(SLOT_TIMER, Gui.make(Material.CLOCK, "<gray>No Items Available",
                    "<gray>✦ Check back later for new deals."));
            } else {
                long timeUntil = bmm.getTimeUntilRefresh();
                inv.setItem(SLOT_TIMER, Gui.make(Material.CLOCK,
                    "<aqua>✦ Refreshes In: <white>" + Fmt.duration(timeUntil),
                    "<gray>✦ New deals <aqua>rotate<gray> periodically.",
                    "<gray>✦ Limited stock — buy fast!"));

                for (int i = 0; i < ITEM_SLOTS.length && i < items.size(); i++) {
                    BlackMarketItem bmi = items.get(i);

                    List<Component> lore = new ArrayList<>();

                    // Item-defined lore lines
                    for (String line : bmi.getLore()) {
                        lore.add(line.isEmpty()
                            ? Component.empty()
                            : MM.deserialize("<!italic>" + line));
                    }
                    if (!bmi.getLore().isEmpty()) {
                        lore.add(Component.empty());
                    }

                    lore.add(MM.deserialize("<!italic><gray>✦ Price: <gold>$" + Fmt.number(bmi.getPriceIgc())));

                    if (bmi.isInStock()) {
                        lore.add(MM.deserialize("<!italic><gray>✦ Stock: <white>" + bmi.getCurrentStock()));
                    } else {
                        lore.add(MM.deserialize("<!italic><red>✗ Sold Out"));
                    }

                    lore.add(Component.empty());

                    if (bmi.isInStock()) {
                        lore.add(MM.deserialize("<!italic><green>→ Click to purchase this item!"));
                    } else {
                        lore.add(MM.deserialize("<!italic><red>✗ Out of stock."));
                    }

                    inv.setItem(ITEM_SLOTS[i], Gui.make(bmi.getMaterial(), bmi.getDisplay(), lore));
                }
            }
        } else {
            // Manager unavailable
            inv.setItem(SLOT_TIMER, Gui.make(Material.BARRIER, "<red>✗ Unavailable",
                "<gray>✦ Black Market is not available right now."));
        }

        inv.setItem(SLOT_BACK, Gui.back());
        return inv;
    }

    private static void handlePurchaseResult(Player player, BlackMarketManager.PurchaseResult result, BlackMarketItem bmi) {
        switch (result) {
            case OK -> {
                Sounds.buy(player);
                player.sendMessage(MM.deserialize("<green>Purchased <white>" + bmi.getDisplay() + "<green> from the Black Market!"));
            }
            case INSUFFICIENT_FUNDS -> {
                Sounds.deny(player);
                player.sendMessage(MM.deserialize(
                    "<red>You need <gold>$" + Fmt.number(bmi.getPriceIgc()) + "<red> to buy that."));
            }
            case OUT_OF_STOCK -> {
                Sounds.deny(player);
                player.sendMessage(MM.deserialize("<red>That item just sold out."));
            }
            case ITEM_NOT_FOUND -> {
                Sounds.deny(player);
                player.sendMessage(MM.deserialize("<red>That item could not be found."));
            }
        }
    }
}
