package com.prison.prestige;

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
import java.util.UUID;

/**
 * PrestigeShopGUI — 27-slot GUI for spending prestige points on permanent upgrades.
 *
 * Layout:
 *   Row 0 (0-8):   borders
 *   Row 1 (9-17):  [border x2] [Mine Profit I] [border] [Mine Profit II] [border] [Mine Profit III] [border x2]
 *   Row 2 (18-26): [border x2] [Token Mastery I] [border] [Token Mastery II] [border] [Token Mastery III] [Points] [Close]
 */
public class PrestigeShopGUI {

    public static final String TITLE_STRING = "<!italic><dark_purple>[ <light_purple>Prestige Shop</light_purple> ]";

    // Upgrade slots (row 1 and 2)
    private static final int SLOT_MINE_PROFIT_1   = 11;
    private static final int SLOT_MINE_PROFIT_2   = 13;
    private static final int SLOT_MINE_PROFIT_3   = 15;
    private static final int SLOT_TOKEN_MASTERY_1 = 20;
    private static final int SLOT_TOKEN_MASTERY_2 = 22;
    private static final int SLOT_TOKEN_MASTERY_3 = 24;
    private static final int SLOT_POINTS_INFO     = 4;
    private static final int SLOT_CLOSE           = 26;

    /** Maps upgrade id → inventory slot */
    private static final java.util.Map<String, Integer> UPGRADE_SLOTS = new java.util.LinkedHashMap<>();
    static {
        UPGRADE_SLOTS.put("mine_profit_1",   SLOT_MINE_PROFIT_1);
        UPGRADE_SLOTS.put("mine_profit_2",   SLOT_MINE_PROFIT_2);
        UPGRADE_SLOTS.put("mine_profit_3",   SLOT_MINE_PROFIT_3);
        UPGRADE_SLOTS.put("token_mastery_1", SLOT_TOKEN_MASTERY_1);
        UPGRADE_SLOTS.put("token_mastery_2", SLOT_TOKEN_MASTERY_2);
        UPGRADE_SLOTS.put("token_mastery_3", SLOT_TOKEN_MASTERY_3);
    }

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final Material BORDER = Material.GRAY_STAINED_GLASS_PANE;

    private final PrestigeShopManager manager;

    public PrestigeShopGUI(PrestigeShopManager manager) {
        this.manager = manager;
    }

    public void open(Player player) {
        UUID uuid = player.getUniqueId();
        Inventory inv = Bukkit.createInventory(null, 27, MM.deserialize(TITLE_STRING));

        // Fill borders
        ItemStack border = borderPane();
        for (int i = 0; i < 27; i++) inv.setItem(i, border);

        // Points info item (center top)
        inv.setItem(SLOT_POINTS_INFO, buildPointsItem(uuid));

        // Upgrade items
        for (PrestigeShopManager.UpgradeDef def : manager.getUpgrades()) {
            Integer slot = UPGRADE_SLOTS.get(def.id());
            if (slot != null) {
                inv.setItem(slot, buildUpgradeItem(def, uuid));
            }
        }

        // Close button
        ItemStack closeItem = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeItem.getItemMeta();
        closeMeta.displayName(MM.deserialize("<!italic><red>Close"));
        closeItem.setItemMeta(closeMeta);
        inv.setItem(SLOT_CLOSE, closeItem);

        player.openInventory(inv);
    }

    /** Handle a click in the GUI. Returns the upgrade id if an upgrade was clicked, else null. */
    public String getUpgradeIdAtSlot(int slot) {
        for (java.util.Map.Entry<String, Integer> e : UPGRADE_SLOTS.entrySet()) {
            if (e.getValue() == slot) return e.getKey();
        }
        return null;
    }

    public boolean isCloseSlot(int slot) { return slot == SLOT_CLOSE; }

    // ----------------------------------------------------------------
    // Item builders
    // ----------------------------------------------------------------

    private ItemStack buildPointsItem(UUID uuid) {
        int pts = manager.getPoints(uuid);
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(MM.deserialize("<!italic><light_purple>Prestige Points"));
        List<Component> lore = new ArrayList<>();
        lore.add(MM.deserialize("<!italic><gray>Available: <white><bold>" + pts + " pts"));
        lore.add(MM.deserialize("<!italic><dark_gray>Earned by prestiging."));
        lore.add(MM.deserialize("<!italic><dark_gray>+" + manager.getPointsPerPrestige() + " pts per prestige."));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildUpgradeItem(PrestigeShopManager.UpgradeDef def, UUID uuid) {
        boolean owned     = manager.hasPurchased(uuid, def.id());
        boolean prereqMet = def.requiredUpgrade() == null || manager.hasPurchased(uuid, def.requiredUpgrade());
        int pts           = manager.getPoints(uuid);
        boolean canAfford = pts >= def.cost();

        Material icon;
        if (def.sellBonus() > 1.0) {
            icon = owned ? Material.EMERALD : (prereqMet ? Material.EMERALD_ORE : Material.COAL_ORE);
        } else {
            icon = owned ? Material.NETHER_STAR : (prereqMet ? Material.QUARTZ : Material.COAL_BLOCK);
        }

        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();

        String statusColor = owned ? "<green>" : (prereqMet ? "<aqua>" : "<dark_gray>");
        meta.displayName(MM.deserialize("<!italic>" + statusColor + def.display()));

        List<Component> lore = new ArrayList<>();

        if (def.sellBonus() > 1.0) {
            lore.add(MM.deserialize("<!italic><gray>Sell bonus: <white>+" + pct(def.sellBonus()) + "%"));
        }
        if (def.tokenBonus() > 1.0) {
            lore.add(MM.deserialize("<!italic><gray>Token bonus: <white>+" + pct(def.tokenBonus()) + "%"));
        }

        lore.add(MM.deserialize("<!italic><gray>Cost: <light_purple>" + def.cost() + " pts"));

        if (owned) {
            lore.add(MM.deserialize("<!italic>"));
            lore.add(MM.deserialize("<!italic><green><bold>PURCHASED"));
        } else if (!prereqMet) {
            String reqDisplay = manager.getUpgrade(def.requiredUpgrade()) != null
                ? manager.getUpgrade(def.requiredUpgrade()).display() : def.requiredUpgrade();
            lore.add(MM.deserialize("<!italic>"));
            lore.add(MM.deserialize("<!italic><red>Requires: " + reqDisplay));
        } else if (!canAfford) {
            lore.add(MM.deserialize("<!italic>"));
            lore.add(MM.deserialize("<!italic><red>Not enough points <dark_gray>(" + pts + "/" + def.cost() + ")"));
        } else {
            lore.add(MM.deserialize("<!italic>"));
            lore.add(MM.deserialize("<!italic><yellow>Click to purchase"));
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

    /** Convert a multiplier like 1.05 → 5 (percent bonus above 1.0) */
    private int pct(double mult) {
        return (int) Math.round((mult - 1.0) * 100);
    }
}
