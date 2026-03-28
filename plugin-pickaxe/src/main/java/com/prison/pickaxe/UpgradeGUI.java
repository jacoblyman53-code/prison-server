package com.prison.pickaxe;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * UpgradeGUI — builds the pickaxe upgrade GUI and confirm dialog.
 *
 * Layout (54-slot, 6 rows):
 *   Row 0: border glass
 *   Row 1: [Tab: Custom | Tab: Vanilla] in slots 10-11; enchant items slots 19-24, 28-33, 37-42
 *   Row 5: border glass
 *
 * We use a simpler single-page layout:
 *   Slots 0-8:   top border
 *   Slots 9,17:  side borders
 *   Slots 45-53: bottom border
 *   Slot 4:      the pickaxe itself (display only)
 *   Slot 13:     Tab button (Custom / Vanilla)
 *   Slots 19-24: Custom enchant row 1
 *   Slots 28-33: Custom enchant row 2 (or Vanilla row 1)
 *   Slot 49:     "Switch tab" button
 */
public class UpgradeGUI {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    // Tab indicator
    public enum Tab { CUSTOM, VANILLA }

    // Enchant slot layout: 6 per row, 2 rows = 12 slots max
    private static final int[] ENCHANT_SLOTS = {
        19, 20, 21, 22, 23, 24,
        28, 29, 30, 31, 32, 33
    };

    private static final Material BORDER = Material.GRAY_STAINED_GLASS_PANE;

    private final PickaxeConfig config;

    public UpgradeGUI(PickaxeConfig config) {
        this.config = config;
    }

    /**
     * Open the upgrade GUI for a player.
     *
     * @param tab        which tab to show (CUSTOM or VANILLA)
     * @param item       the pickaxe being upgraded
     * @param prestige   the player's prestige level (for scaled costs)
     */
    public void open(Player player, Tab tab, ItemStack item, int prestige) {
        Inventory inv = Bukkit.createInventory(null, 54, MM.deserialize(config.getUpgradeTitle()));

        // Border
        ItemStack border = borderPane();
        for (int i = 0; i < 9; i++)  inv.setItem(i, border);       // top row
        for (int i = 45; i < 54; i++) inv.setItem(i, border);      // bottom row
        inv.setItem(9, border);  inv.setItem(17, border);
        inv.setItem(18, border); inv.setItem(26, border);
        inv.setItem(27, border); inv.setItem(35, border);
        inv.setItem(36, border); inv.setItem(44, border);

        // Pickaxe display in center top
        ItemStack display = item.clone();
        inv.setItem(4, display);

        // Tab buttons
        inv.setItem(10, makeTabButton(Tab.CUSTOM, tab));
        inv.setItem(11, makeTabButton(Tab.VANILLA, tab));

        // Enchant items
        List<EnchantDef> enchants = (tab == Tab.CUSTOM)
            ? config.getCustomEnchants()
            : config.getVanillaEnchants();

        Map<String, Integer> levels = PickaxeManager.getInstance().readAllLevels(item);

        for (int i = 0; i < ENCHANT_SLOTS.length && i < enchants.size(); i++) {
            EnchantDef def = enchants.get(i);
            int currentLevel = levels.getOrDefault(def.id(), 0);
            inv.setItem(ENCHANT_SLOTS[i], makeEnchantItem(def, currentLevel, prestige));
        }

        player.openInventory(inv);
    }

    /**
     * Open the confirm dialog for an upgrade purchase.
     *
     * @param enchantId   the enchant being upgraded
     * @param fromLevel   current level
     * @param cost        token cost (already scaled for prestige)
     * @param playerTokens the player's current token balance
     */
    public void openConfirm(Player player, String enchantId, int fromLevel, long cost, long playerTokens) {
        Inventory inv = Bukkit.createInventory(null, 27, MM.deserialize(config.getConfirmTitle()));

        ItemStack border = borderPane();
        for (int i = 0; i < 27; i++) inv.setItem(i, border);

        EnchantDef def = config.getEnchant(enchantId);
        if (def == null) return;

        int toLevel = fromLevel + 1;
        boolean canAfford = playerTokens >= cost;

        // Info item in center
        ItemStack info = new ItemStack(def.icon());
        ItemMeta meta = info.getItemMeta();
        meta.displayName(MM.deserialize("<gold>" + def.display() + " → " + toRoman(toLevel)));
        List<Component> lore = new ArrayList<>();
        lore.add(MM.deserialize("<gray>Cost: <gold>" + cost + " tokens"));
        lore.add(MM.deserialize("<gray>Your tokens: " + (canAfford ? "<green>" : "<red>") + playerTokens));
        lore.add(MM.deserialize(""));
        lore.add(MM.deserialize("<gray>" + def.description(toLevel)));
        meta.lore(lore);
        info.setItemMeta(meta);
        inv.setItem(13, info);

        // Confirm button (slot 11) — green wool if can afford, red if not
        ItemStack confirm = new ItemStack(canAfford ? Material.LIME_WOOL : Material.RED_WOOL);
        ItemMeta cm = confirm.getItemMeta();
        cm.displayName(MM.deserialize(canAfford ? "<green><bold>CONFIRM" : "<red><bold>CANNOT AFFORD"));
        confirm.setItemMeta(cm);
        inv.setItem(11, confirm);

        // Cancel button (slot 15)
        ItemStack cancel = new ItemStack(Material.RED_WOOL);
        ItemMeta xm = cancel.getItemMeta();
        xm.displayName(MM.deserialize("<red><bold>CANCEL"));
        cancel.setItemMeta(xm);
        inv.setItem(15, cancel);

        player.openInventory(inv);
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private ItemStack makeTabButton(Tab forTab, Tab currentTab) {
        boolean active = forTab == currentTab;
        Material mat = active ? Material.GOLDEN_PICKAXE : Material.STONE_PICKAXE;
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        String label = forTab == Tab.CUSTOM ? "Custom Enchants" : "Vanilla Enchants";
        meta.displayName(MM.deserialize(active
            ? "<gold><bold>" + label
            : "<gray>" + label));
        if (!active) {
            meta.lore(List.of(MM.deserialize("<yellow>Click to switch tab")));
        }
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeEnchantItem(EnchantDef def, int currentLevel, int prestige) {
        ItemStack item = new ItemStack(def.icon());
        ItemMeta meta = item.getItemMeta();

        boolean maxed = currentLevel >= def.maxLevel();
        String levelStr = currentLevel > 0 ? " " + toRoman(currentLevel) : "";
        meta.displayName(MM.deserialize(
            maxed ? "<green><bold>" + def.display() + levelStr
                  : "<aqua>" + def.display() + levelStr
        ));

        List<Component> lore = new ArrayList<>();
        lore.add(MM.deserialize("<gray>Level: <white>" + currentLevel + "<gray>/" + def.maxLevel()));

        if (currentLevel > 0) {
            lore.add(MM.deserialize("<gray>Current: <green>" + def.description(currentLevel)));
        }

        if (!maxed) {
            long cost = config.scaledCost(def, currentLevel, prestige);
            lore.add(MM.deserialize("<gray>Next: <yellow>" + def.description(currentLevel + 1)));
            lore.add(MM.deserialize("<gray>Upgrade cost: <gold>" + cost + " tokens"));
            if (prestige > 0) {
                lore.add(MM.deserialize("<dark_gray>(Prestige " + prestige + " cost scaling applied)"));
            }
            lore.add(MM.deserialize(""));
            lore.add(MM.deserialize("<yellow>Click to upgrade"));
        } else {
            lore.add(MM.deserialize(""));
            lore.add(MM.deserialize("<green><bold>MAXED OUT"));
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack borderPane() {
        ItemStack item = new ItemStack(BORDER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.empty());
        item.setItemMeta(meta);
        return item;
    }

    /** Returns the enchant ID for the enchant displayed at a given slot, or null. */
    public String getEnchantAtSlot(int slot, Tab tab) {
        for (int i = 0; i < ENCHANT_SLOTS.length; i++) {
            if (ENCHANT_SLOTS[i] == slot) {
                List<EnchantDef> enchants = (tab == Tab.CUSTOM)
                    ? config.getCustomEnchants()
                    : config.getVanillaEnchants();
                if (i < enchants.size()) return enchants.get(i).id();
                return null;
            }
        }
        return null;
    }

    /** Returns true if the slot is a tab button. */
    public boolean isTabSlot(int slot) { return slot == 10 || slot == 11; }

    /** Returns which tab the tab button at this slot represents. */
    public Tab getTabAtSlot(int slot) { return slot == 10 ? Tab.CUSTOM : Tab.VANILLA; }

    /** Returns true if the slot is the confirm button in the confirm dialog. */
    public boolean isConfirmSlot(int slot) { return slot == 11; }

    /** Returns true if the slot is the cancel button in the confirm dialog. */
    public boolean isCancelSlot(int slot)  { return slot == 15; }

    private static final String[] ROMAN = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
    private String toRoman(int n) {
        return (n >= 0 && n < ROMAN.length) ? ROMAN[n] : String.valueOf(n);
    }
}
