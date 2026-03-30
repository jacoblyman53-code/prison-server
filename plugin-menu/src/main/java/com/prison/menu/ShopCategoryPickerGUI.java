package com.prison.menu;

import com.prison.menu.util.*;
import com.prison.shop.ShopCategory;
import com.prison.shop.ShopManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;

public class ShopCategoryPickerGUI {

    public static final Component TITLE = MiniMessage.miniMessage().deserialize("<!italic><dark_gray>[ <yellow>Shop <dark_gray>]");
    private static final MiniMessage MM = MiniMessage.miniMessage();

    // Three rows of 7 content slots
    private static final int[] CONTENT_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,  // row 1: categories 0-6
        19, 20, 21, 22, 23, 24, 25,  // row 2: categories 7-13
        28, 29, 30, 31, 32, 33, 34   // row 3: categories 14-20
    };
    private static final int SLOT_BACK = 45;

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

        ShopManager sm = ShopManager.getInstance();
        if (sm == null) return;

        List<ShopCategory> categories = sm.getCategories();

        for (int i = 0; i < CONTENT_SLOTS.length && i < categories.size(); i++) {
            if (CONTENT_SLOTS[i] == slot) {
                Sounds.nav(player);
                ShopCategoryPageGUI.open(player, categories.get(i).id(), 0);
                return;
            }
        }
    }

    // ----------------------------------------------------------------

    private static Inventory build(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        Gui.fillAll(inv);

        ShopManager sm = ShopManager.getInstance();
        if (sm != null) {
            List<ShopCategory> categories = sm.getCategories();

            for (int i = 0; i < CONTENT_SLOTS.length && i < categories.size(); i++) {
                ShopCategory cat = categories.get(i);

                List<Component> lore = new ArrayList<>();
                lore.add(MM.deserialize("<!italic><gray>" + cat.items().size() + "<gray> items"));
                lore.add(Component.empty());
                lore.add(MM.deserialize("<!italic><green>Click to browse."));

                inv.setItem(CONTENT_SLOTS[i], Gui.make(cat.icon(), cat.displayName(), lore));
            }
        }

        inv.setItem(SLOT_BACK, Gui.back());
        return inv;
    }
}
