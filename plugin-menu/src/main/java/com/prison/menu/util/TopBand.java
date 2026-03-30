package com.prison.menu.util;

import com.prison.economy.EconomyAPI;
import com.prison.events.EventsAPI;
import com.prison.gangs.GangAPI;
import com.prison.permissions.PermissionEngine;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * TopBand — builds the standardized info strip shown in every 54-slot GUI.
 *
 * Slot assignment (top row, slots 0-8):
 *   0 = Rank info
 *   1 = IGC balance
 *   2 = Token balance
 *   3 = Total sell multiplier
 *   4 = Sell streak
 *   5 = Active event (or filler)
 *   6 = Gang bonus (or filler)
 *   7 = filler
 *   8 = filler (reserved for close/back placed by each GUI)
 *
 * All items are read-only.
 */
public final class TopBand {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private TopBand() {}

    /**
     * Populate slots 0-7 of an inventory with the standard info band for this player.
     * Slot 8 is left to the calling GUI (close or back).
     */
    public static void apply(Inventory inv, Player player) {
        UUID uuid = player.getUniqueId();

        inv.setItem(0, rankItem(uuid));
        inv.setItem(1, balanceItem(uuid));
        inv.setItem(2, tokenItem(uuid));
        inv.setItem(3, multiplierItem(uuid));
        inv.setItem(4, streakItem(uuid));
        inv.setItem(5, eventItem());
        inv.setItem(6, gangItem(uuid));
        inv.setItem(7, Gui.filler());
    }

    // ----------------------------------------------------------------

    private static ItemStack rankItem(UUID uuid) {
        String rank = "A";
        int prestige = 0;
        try {
            PermissionEngine pe = PermissionEngine.getInstance();
            if (pe != null) {
                rank    = pe.getMineRank(uuid);
                prestige = pe.getPrestige(uuid);
            }
        } catch (Exception ignored) {}

        String donorPart = "";
        try {
            com.prison.donor.DonorAPI da = com.prison.donor.DonorAPI.getInstance();
            if (da != null) {
                String dr = da.getDonorRank(uuid);
                if (dr != null && !dr.isEmpty()) donorPart = "<gray> | <gold>" + dr;
            }
        } catch (Exception ignored) {}

        String prestigePart = prestige > 0 ? "<light_purple> ✦P" + prestige : "";

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(MM.deserialize("<!italic><gray>Mine Rank: <white>" + rank));
        if (prestige > 0) lore.add(MM.deserialize("<!italic><light_purple>Prestige: ✦" + prestige));
        lore.add(Component.empty());
        lore.add(MM.deserialize("<!italic><dark_gray>Your progression profile."));

        return Gui.make(Material.NETHER_STAR,
            "<white>Rank <aqua>" + rank + donorPart + prestigePart, lore);
    }

    private static ItemStack balanceItem(UUID uuid) {
        long bal = 0;
        try {
            EconomyAPI api = EconomyAPI.getInstance();
            if (api != null) bal = api.getBalance(uuid);
        } catch (Exception ignored) {}

        return Gui.make(Material.SUNFLOWER, "<yellow>Balance",
            "<gray>Bal: <gold>$" + Fmt.number(bal),
            "",
            "<dark_gray>Your prison currency.");
    }

    private static ItemStack tokenItem(UUID uuid) {
        long tokens = 0;
        try {
            EconomyAPI api = EconomyAPI.getInstance();
            if (api != null) tokens = api.getTokens(uuid);
        } catch (Exception ignored) {}

        return Gui.make(Material.RAW_GOLD, "<aqua>Tokens",
            "<gray>Tokens: <aqua>" + Fmt.number(tokens),
            "",
            "<dark_gray>Used to upgrade your pickaxe.");
    }

    private static ItemStack multiplierItem(UUID uuid) {
        double streak = 1.0, gang = 1.0, event = 1.0, prestige = 1.0;
        int streakCount = 0;
        try {
            EconomyAPI api = EconomyAPI.getInstance();
            if (api != null) {
                streakCount = api.getSellStreak(uuid);
                streak  = api.getStreakMultiplier(streakCount);
                gang    = api.getGangSellBonus(uuid);
                event   = api.getEventSellBonus(uuid);
                prestige = api.getPrestigeSellBonus(uuid);
            }
        } catch (Exception ignored) {}

        double total = streak * gang * event * prestige;

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(MM.deserialize("<!italic><gray>Total: <green>" + Fmt.multiplier(total)));
        lore.add(Component.empty());
        lore.add(MM.deserialize("<!italic><dark_gray>Streak:   <gray>" + Fmt.multiplier(streak)));
        lore.add(MM.deserialize("<!italic><dark_gray>Gang:     <gray>" + Fmt.multiplier(gang)));
        lore.add(MM.deserialize("<!italic><dark_gray>Event:    <gray>" + Fmt.multiplier(event)));
        lore.add(MM.deserialize("<!italic><dark_gray>Prestige: <gray>" + Fmt.multiplier(prestige)));

        return Gui.make(Material.BLAZE_POWDER, "<orange>Sell Multiplier", lore);
    }

    private static ItemStack streakItem(UUID uuid) {
        int streak = 0;
        try {
            EconomyAPI api = EconomyAPI.getInstance();
            if (api != null) streak = api.getSellStreak(uuid);
        } catch (Exception ignored) {}

        String next;
        if      (streak < 5)   next = "5 (1.05x)";
        else if (streak < 10)  next = "10 (1.10x)";
        else if (streak < 25)  next = "25 (1.20x)";
        else if (streak < 50)  next = "50 (1.35x)";
        else if (streak < 100) next = "100 (1.50x)";
        else                   next = "MAX";

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(MM.deserialize("<!italic><gray>Current streak: <yellow>x" + streak));
        lore.add(MM.deserialize("<!italic><gray>Next bonus at: <white>" + next));
        lore.add(Component.empty());
        lore.add(MM.deserialize("<!italic><dark_gray>Sell within 60s to keep your streak."));

        return Gui.make(Material.CLOCK, "<yellow>Sell Streak", lore);
    }

    private static ItemStack eventItem() {
        try {
            EventsAPI api = EventsAPI.getInstance();
            if (api != null && api.hasActiveEvent()) {
                List<com.prison.events.ActiveEvent> events = api.getActiveEvents();
                List<Component> lore = new ArrayList<>();
                lore.add(Component.empty());
                for (com.prison.events.ActiveEvent e : events) {
                    lore.add(MM.deserialize("<!italic><green>✔ <white>" + e.config().displayName()
                        + " <gray>(" + Fmt.multiplier(e.config().multiplier()) + ")"));
                }
                lore.add(Component.empty());
                lore.add(MM.deserialize("<!italic><dark_gray>Active server events."));
                return Gui.make(Material.BELL, "<green>Active Event", lore);
            }
        } catch (Exception ignored) {}
        return Gui.make(Material.BELL, "<gray>No Active Event", "<dark_gray>No events running right now.");
    }

    private static ItemStack gangItem(UUID uuid) {
        try {
            GangAPI api = GangAPI.getInstance();
            if (api != null) {
                String name = api.getGangName(uuid);
                if (name != null) {
                    int level = api.getGangLevel(uuid);
                    double sellBonus  = api.getSellBonus(uuid);
                    double tokenBonus = api.getTokenBonus(uuid);
                    List<Component> lore = new ArrayList<>();
                    lore.add(Component.empty());
                    lore.add(MM.deserialize("<!italic><gray>Gang: <white>" + name + " <dark_gray>(Lv." + level + ")"));
                    lore.add(MM.deserialize("<!italic><gray>Sell bonus:  <green>" + Fmt.multiplier(sellBonus)));
                    lore.add(MM.deserialize("<!italic><gray>Token bonus: <aqua>" + Fmt.multiplier(tokenBonus)));
                    lore.add(Component.empty());
                    lore.add(MM.deserialize("<!italic><yellow>Click to open Gangs menu."));
                    return Gui.make(Material.SHIELD, "<green>Gang Bonus", lore);
                }
            }
        } catch (Exception ignored) {}
        return Gui.make(Material.SHIELD, "<gray>No Gang",
            "<dark_gray>You are not in a gang.",
            "",
            "<yellow>Click to browse gangs.");
    }
}
