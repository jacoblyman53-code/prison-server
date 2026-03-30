package com.prison.menu;

import com.prison.crates.CrateKey;
import com.prison.crates.CrateManager;
import com.prison.crates.CrateTier;
import com.prison.menu.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CratesHubGUI {

    public static final Component TITLE = MiniMessage.miniMessage().deserialize("<!italic><dark_gray>[ <gold>Crates Hub <dark_gray>]");
    private static final MiniMessage MM = MiniMessage.miniMessage();

    // Content slots: rows 1-3, columns 1-7 (up to 21 tiers max across 3 rows)
    private static final int[][] ROW_STARTS = { {10}, {19}, {28} };
    private static final int SLOT_BACK = 45;
    private static final int SLOT_INFO = 49;

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

        // Determine if a tier slot was clicked
        CrateManager cm = CrateManager.getInstance();
        if (cm == null) return;

        List<CrateTier> tiers = new ArrayList<>(cm.getTiers().values());
        int[] contentSlots = buildContentSlots(tiers.size());

        for (int i = 0; i < contentSlots.length; i++) {
            if (contentSlots[i] == slot) {
                CrateTier tier = tiers.get(i);
                int keyCount = countKeys(player, tier.id());
                if (keyCount > 0) {
                    player.closeInventory();
                    Bukkit.dispatchCommand(player, "crate open " + tier.id());
                } else {
                    Sounds.deny(player);
                    player.sendMessage(MM.deserialize(
                        "<red>You don't have any <white>" + tier.displayName() + " Keys<red>."));
                }
                return;
            }
        }
    }

    // ----------------------------------------------------------------

    private static Inventory build(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        Gui.fillAll(inv);

        CrateManager cm = CrateManager.getInstance();
        if (cm != null) {
            List<CrateTier> tiers = new ArrayList<>(cm.getTiers().values());
            int[] contentSlots = buildContentSlots(tiers.size());

            for (int i = 0; i < contentSlots.length && i < tiers.size(); i++) {
                CrateTier tier = tiers.get(i);
                int keyCount = countKeys(player, tier.id());

                List<Component> lore = new ArrayList<>();
                lore.add(MM.deserialize("<!italic><gray>Keys in inventory: <white>" + keyCount));
                lore.add(MM.deserialize("<!italic><gray>Possible rewards: <white>" + tier.rewards().size()));
                lore.add(Component.empty());
                if (keyCount > 0) {
                    lore.add(MM.deserialize("<!italic><green>Click to open a crate!"));
                } else {
                    lore.add(MM.deserialize("<!italic><gray>No keys in inventory."));
                }

                ItemStack item = Gui.make(tier.keyMaterial(), tier.keyColor() + tier.displayName(), lore);
                inv.setItem(contentSlots[i], item);
            }
        }

        // Back button
        inv.setItem(SLOT_BACK, Gui.back());

        // Info item
        inv.setItem(SLOT_INFO, Gui.make(Material.TRIPWIRE_HOOK, "<gold>Crates Hub",
            "<gray>Right-click crate blocks to open.",
            "<gray>Keys can be earned from ranks, quests, and more."));

        return inv;
    }

    /**
     * Returns an array of inventory slots for up to tiers.size() tiers,
     * laid out 7 per row starting at slots 10, 19, 28.
     */
    private static int[] buildContentSlots(int tierCount) {
        int count = Math.min(tierCount, 21);
        int[] slots = new int[count];
        int[] rowStarts = {10, 19, 28};
        for (int i = 0; i < count; i++) {
            int row = i / 7;
            int col = i % 7;
            slots[i] = rowStarts[row] + col;
        }
        return slots;
    }

    /**
     * Counts how many keys for the given tier id the player has in their inventory.
     */
    private static int countKeys(Player player, String tierId) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() == Material.AIR) continue;
            String id = CrateKey.getTierIdFromKey(item);
            if (tierId.equals(id)) {
                count += item.getAmount();
            }
        }
        return count;
    }
}
