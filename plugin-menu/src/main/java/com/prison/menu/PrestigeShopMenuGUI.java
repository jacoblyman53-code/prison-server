package com.prison.menu;

import com.prison.menu.util.*;
import com.prison.permissions.PermissionEngine;
import com.prison.prestige.PrestigeManager;
import com.prison.prestige.PrestigeShopManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import java.util.*;

/**
 * PrestigeShopMenuGUI — 54-slot prestige upgrade shop.
 *
 * Top band  (slots 0-8)  : info band
 * Upgrades  (slots 10-16, skip 13) : up to 6 upgrade items
 * Slot 13   : points / bonus info (NETHER_STAR)
 * Slot 22   : Prestige Now shortcut
 * Slot 45   : back → MainMenuGUI
 * Slot 53   : close (filler, no action needed — standard close)
 */
public class PrestigeShopMenuGUI {

    public static final Component TITLE =
        MiniMessage.miniMessage().deserialize("<!italic>ASCENSION SHOP");

    private static final MiniMessage MM = MiniMessage.miniMessage();

    /** Upgrade display slots — 6 slots, skipping the centre info slot 13. */
    private static final int[] UPGRADE_SLOTS = { 10, 11, 12, 14, 15, 16 };

    private static final int SLOT_INFO        = 13;
    private static final int SLOT_PRESTIGE_NOW = 22;
    private static final int SLOT_BACK        = 45;
    private static final int SLOT_CLOSE       = 53;

    // ----------------------------------------------------------------
    // Public API
    // ----------------------------------------------------------------

    public static void open(Player player) {
        player.openInventory(build(player));
    }

    public static void handleClick(Player player, int slot, MenuPlugin plugin) {
        if (slot == 8 || slot == SLOT_BACK) {
            Sounds.nav(player);
            MainMenuGUI.open(player);
            return;
        }

        if (slot == SLOT_CLOSE) {
            Sounds.nav(player);
            player.closeInventory();
            return;
        }

        if (slot == SLOT_PRESTIGE_NOW) {
            handlePrestigeNow(player);
            return;
        }

        // Check upgrade slots
        for (int i = 0; i < UPGRADE_SLOTS.length; i++) {
            if (UPGRADE_SLOTS[i] == slot) {
                handleUpgradePurchase(player, i, plugin);
                return;
            }
        }
    }

    // ----------------------------------------------------------------
    // Build
    // ----------------------------------------------------------------

    private static Inventory build(Player player) {
        UUID uuid = player.getUniqueId();
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        TopBand.apply(inv, player);
        inv.setItem(8, Gui.back());

        // Slot 0: back
        inv.setItem(0, Gui.back());

        PrestigeManager     pm  = PrestigeManager.getInstance();
        PrestigeShopManager psm = PrestigeShopManager.getInstance();
        PermissionEngine    pe  = PermissionEngine.getInstance();

        int    prestigeLevel = pm  != null ? pm.getPrestigeLevel(uuid)  : 0;
        int    points        = psm != null ? psm.getPoints(uuid)        : 0;
        String rank          = pe  != null ? pe.getMineRank(uuid)       : "?";
        boolean canPrestige  = pm  != null && pm.canPrestige(uuid);

        List<PrestigeShopManager.UpgradeDef> upgrades =
            psm != null ? psm.getUpgrades() : List.of();

        // ── Compute total owned sell/token bonuses ──────────────────
        int totalSellBonus  = 0;
        int totalTokenBonus = 0;
        if (psm != null) {
            for (PrestigeShopManager.UpgradeDef def : upgrades) {
                if (psm.hasPurchased(uuid, def.id())) {
                    totalSellBonus  += def.sellBonus();
                    totalTokenBonus += def.tokenBonus();
                }
            }
        }

        // ── Slot 13: Info ───────────────────────────────────────────
        inv.setItem(SLOT_INFO, Gui.make(Material.NETHER_STAR,
            "<light_purple>Prestige Shop",
            "<gray>✦ Prestige points: <white>" + points,
            "<gray>✦ Prestige level:  <light_purple>✦" + prestigeLevel,
            "",
            "<gray>✦ Shop sell bonus:  <green>+" + totalSellBonus  + "%",
            "<gray>✦ Shop token bonus: <green>+" + totalTokenBonus + "%",
            "",
            "<gray>✦ Spend <aqua>prestige points<gray> on",
            "<gray>  permanent passive upgrades."));

        // ── Upgrade items ───────────────────────────────────────────
        for (int i = 0; i < UPGRADE_SLOTS.length; i++) {
            if (i >= upgrades.size()) break; // fewer upgrades than slots

            PrestigeShopManager.UpgradeDef def = upgrades.get(i);
            inv.setItem(UPGRADE_SLOTS[i], buildUpgradeItem(uuid, def, points, psm, upgrades));
        }

        // ── Slot 22: Prestige Now ───────────────────────────────────
        if (canPrestige) {
            inv.setItem(SLOT_PRESTIGE_NOW, Gui.make(Material.NETHER_STAR,
                "<light_purple>✦ Prestige Now",
                "<gray>✦ Current prestige level: <light_purple>✦" + prestigeLevel,
                "<gray>✦ Next prestige level:    <light_purple>✦" + (prestigeLevel + 1),
                "",
                "<green>→ Click to open the prestige confirmation!"));
        } else {
            inv.setItem(SLOT_PRESTIGE_NOW, Gui.make(Material.NETHER_STAR,
                "<gray>Prestige Locked",
                "<gray>✦ Reach rank <yellow>Z<gray> to prestige.",
                "<gray>✦ Current rank: <yellow>" + rank,
                "",
                "<red>✗ Requirements not met."));
        }

        // ── Slot 45: Back ───────────────────────────────────────────
        inv.setItem(SLOT_BACK, Gui.back());

        // ── Slot 53: Close ─────────────────────────────────────────
        inv.setItem(SLOT_CLOSE, Gui.close());

        return inv;
    }

    // ----------------------------------------------------------------
    // Item builders
    // ----------------------------------------------------------------

    private static org.bukkit.inventory.ItemStack buildUpgradeItem(
            UUID uuid,
            PrestigeShopManager.UpgradeDef def,
            int points,
            PrestigeShopManager psm,
            List<PrestigeShopManager.UpgradeDef> allUpgrades) {

        boolean owned = psm.hasPurchased(uuid, def.id());

        // Build bonus lore shared across states
        String bonusLine = buildBonusLine(def);

        if (owned) {
            return Gui.make(Material.LIME_CONCRETE,
                "<green>\u2714 " + def.display(),
                "<gray>✦ Status: <green>✓ Owned",
                bonusLine,
                "",
                "<gray>→ Already purchased — passive bonus active.");
        }

        // Check prerequisite
        String req = def.requiredUpgrade();
        if (req != null && !psm.hasPurchased(uuid, req)) {
            String reqDisplay = findDisplayById(req, allUpgrades);
            return Gui.make(Material.GRAY_STAINED_GLASS_PANE,
                "<gray>" + def.display(),
                "<gray>✦ Requires: <yellow>" + reqDisplay,
                bonusLine,
                "",
                "<red>✗ Prerequisite not met.");
        }

        // Check affordability
        boolean canAfford = points >= def.cost();
        if (canAfford) {
            return Gui.make(Material.YELLOW_STAINED_GLASS_PANE,
                "<yellow>" + def.display(),
                "<gray>✦ Cost: <gold>$ " + def.cost() + " prestige points",
                "<gray>✦ Your points: <white>" + points,
                bonusLine,
                "",
                "<green>→ Click to purchase this upgrade!");
        } else {
            return Gui.make(Material.RED_STAINED_GLASS_PANE,
                "<red>" + def.display(),
                "<gray>✦ Cost: <gold>$ " + def.cost() + " prestige points",
                "<gray>✦ Your points: <white>" + points,
                bonusLine,
                "",
                "<red>✗ Not enough prestige points.");
        }
    }

    private static String buildBonusLine(PrestigeShopManager.UpgradeDef def) {
        double sell  = def.sellBonus();
        double token = def.tokenBonus();
        int sellPct  = (int) Math.round((sell - 1.0) * 100);
        int tokenPct = (int) Math.round((token - 1.0) * 100);
        if (sellPct > 0 && tokenPct > 0) {
            return "<gray>✦ Bonus: <green>+" + sellPct + "% sell<gray>, <green>+" + tokenPct + "% tokens";
        } else if (sellPct > 0) {
            return "<gray>✦ Bonus: <green>+" + sellPct + "% sell income";
        } else if (tokenPct > 0) {
            return "<gray>✦ Bonus: <green>+" + tokenPct + "% token gain";
        }
        return "<gray>✦ Bonus: <red>✗ None";
    }

    private static String findDisplayById(String id, List<PrestigeShopManager.UpgradeDef> defs) {
        for (PrestigeShopManager.UpgradeDef d : defs) {
            if (d.id().equals(id)) return d.display();
        }
        return id; // fallback to raw id if not found
    }

    // ----------------------------------------------------------------
    // Click handlers
    // ----------------------------------------------------------------

    private static void handlePrestigeNow(Player player) {
        PrestigeManager pm = PrestigeManager.getInstance();
        if (pm != null && pm.canPrestige(player.getUniqueId())) {
            Sounds.nav(player);
            PrestigeConfirmGUI.open(player);
        } else {
            Sounds.deny(player);
            player.sendMessage(MM.deserialize("<red>You must reach rank <white>Z<red> before prestiging."));
        }
    }

    private static void handleUpgradePurchase(Player player, int upgradeIndex, MenuPlugin plugin) {
        PrestigeShopManager psm = PrestigeShopManager.getInstance();
        if (psm == null) {
            Sounds.deny(player);
            player.sendMessage(MM.deserialize("<red>Prestige shop is unavailable. Please try again."));
            return;
        }

        List<PrestigeShopManager.UpgradeDef> upgrades = psm.getUpgrades();
        if (upgradeIndex >= upgrades.size()) return;

        PrestigeShopManager.UpgradeDef def = upgrades.get(upgradeIndex);
        PrestigeShopManager.PurchaseResult result = psm.purchase(player.getUniqueId(), def.id());

        switch (result) {
            case OK -> {
                Sounds.buy(player);
                player.sendMessage(MM.deserialize(
                    "<green>Purchased <white>" + def.display() + "<green>!"));
                // Refresh the GUI so item states update
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline() && TITLE.equals(player.getOpenInventory().title())) {
                        player.openInventory(build(player));
                    }
                }, 1L);
            }
            case INSUFFICIENT_POINTS -> {
                Sounds.deny(player);
                player.sendMessage(MM.deserialize(
                    "<red>Not enough prestige points to purchase <white>" + def.display() + "<red>."));
            }
            case ALREADY_OWNED -> {
                Sounds.deny(player);
                player.sendMessage(MM.deserialize(
                    "<red>You already own <white>" + def.display() + "<red>."));
            }
            case PREREQUISITE_NOT_MET -> {
                Sounds.deny(player);
                String reqId      = def.requiredUpgrade();
                String reqDisplay = reqId != null
                    ? findDisplayById(reqId, upgrades) : "a required upgrade";
                player.sendMessage(MM.deserialize(
                    "<red>You must first purchase <white>" + reqDisplay
                    + "<red> before buying <white>" + def.display() + "<red>."));
            }
            case NOT_FOUND -> {
                Sounds.deny(player);
                player.sendMessage(MM.deserialize("<red>Upgrade not found. Please report this."));
            }
        }
    }
}
