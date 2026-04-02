package com.prison.menu;

import com.prison.economy.EconomyAPI;
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
import java.util.UUID;

public class SellCenterGUI {

    public static final Component TITLE = MiniMessage.miniMessage().deserialize("<!italic>MERCHANT SELL");
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private static final int SLOT_INV_VALUE  = 10;
    private static final int SLOT_STREAK     = 11;
    private static final int SLOT_MULTIPLIER = 12;
    private static final int SLOT_AUTOSELL   = 13;
    private static final int SLOT_WHAT_SELLS = 14;
    private static final int SLOT_SELL_ALL   = 22;
    private static final int SLOT_BACK       = 18;

    public static void open(Player player) {
        player.openInventory(build(player));
    }

    public static void handleClick(Player player, int slot, MenuPlugin plugin) {
        EconomyAPI eco = EconomyAPI.getInstance();

        if (slot == SLOT_BACK) {
            Sounds.nav(player);
            MainMenuGUI.open(player);
            return;
        }

        if (slot == SLOT_SELL_ALL) {
            Sounds.sell(player);
            player.closeInventory();
            Bukkit.dispatchCommand(player, "sellall");
            return;
        }

        if (slot == SLOT_AUTOSELL && eco != null) {
            boolean current = eco.hasAutoSell(player.getUniqueId());
            eco.setAutoSell(player.getUniqueId(), !current);
            Sounds.nav(player);
            player.openInventory(build(player));
            return;
        }
    }

    // ----------------------------------------------------------------

    private static Inventory build(Player player) {
        UUID uuid = player.getUniqueId();
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);

        // Slot 0: back
        inv.setItem(0, Gui.back());

        EconomyAPI eco = EconomyAPI.getInstance();

        // --- Inventory value estimate ---
        long estimatedValue = 0;
        Map<Material, Long> sellables = new HashMap<>();
        if (eco != null) {
            for (ItemStack item : player.getInventory().getContents()) {
                if (item == null || item.getType() == Material.AIR) continue;
                long price = eco.getSellPrice(item.getType(), player);
                if (price > 0) {
                    estimatedValue += price * item.getAmount();
                    sellables.merge(item.getType(), (long) item.getAmount(), Long::sum);
                }
            }
        }
        inv.setItem(SLOT_INV_VALUE, Gui.make(Material.SUNFLOWER, "<aqua>Inventory Value",
            "<gray>✦ Estimated sell value: <gold>$" + Fmt.number(estimatedValue),
            "<gray>✦ Based on current <aqua>mine prices<gray> and multiplier.",
            "",
            "<gray>✦ Does not include live streak/event bonus."));

        // --- Streak info ---
        int streak = eco != null ? eco.getSellStreak(uuid) : 0;
        double streakMult = eco != null ? eco.getStreakMultiplier(streak) : 1.0;
        String nextBreak;
        if      (streak < 5)   nextBreak = "5 (1.05x)";
        else if (streak < 10)  nextBreak = "10 (1.10x)";
        else if (streak < 25)  nextBreak = "25 (1.20x)";
        else if (streak < 50)  nextBreak = "50 (1.35x)";
        else if (streak < 100) nextBreak = "100 (1.50x)";
        else                   nextBreak = "MAX (1.50x)";

        inv.setItem(SLOT_STREAK, Gui.make(Material.BLAZE_POWDER, "<aqua>Sell Streak",
            "<gray>✦ Current streak: <yellow>x" + streak,
            "<gray>✦ Streak bonus: <green>" + Fmt.multiplier(streakMult),
            "<gray>✦ Next bonus at: <white>" + nextBreak,
            "",
            "<gray>✦ Sell within <aqua>60 seconds<gray> to keep streak."));

        // --- Multiplier breakdown ---
        double gang  = eco != null ? eco.getGangSellBonus(uuid)     : 1.0;
        double event = eco != null ? eco.getEventSellBonus(uuid)    : 1.0;
        double prest = eco != null ? eco.getPrestigeSellBonus(uuid) : 1.0;
        double boost = eco != null ? eco.getBoostSellBonus(uuid)    : 1.0;
        double total = streakMult * gang * event * prest * boost;

        List<Component> multLore = new ArrayList<>();
        multLore.add(Component.empty());
        multLore.add(MM.deserialize("<!italic><gray>✦ Total: <green>" + Fmt.multiplier(total)));
        multLore.add(Component.empty());
        multLore.add(MM.deserialize("<!italic><gray>  ◆ Streak:   <white>" + Fmt.multiplier(streakMult)));
        multLore.add(MM.deserialize("<!italic><gray>  ◆ Gang:     <white>" + Fmt.multiplier(gang)));
        multLore.add(MM.deserialize("<!italic><gray>  ◆ Event:    <white>" + Fmt.multiplier(event)));
        multLore.add(MM.deserialize("<!italic><gray>  ◆ Prestige: <white>" + Fmt.multiplier(prest)));
        multLore.add(MM.deserialize("<!italic><gray>  ◆ Boost:    <white>" + Fmt.multiplier(boost)));
        inv.setItem(SLOT_MULTIPLIER, Gui.make(Material.GLOWSTONE_DUST, "<aqua>Multiplier Breakdown", multLore));

        // --- Auto-sell toggle ---
        boolean autoSell = eco != null && eco.hasAutoSell(uuid);
        if (autoSell) {
            inv.setItem(SLOT_AUTOSELL, Gui.make(Material.HOPPER, "<aqua>Auto Sell",
                "<gray>✦ Status: <green>✓ Enabled",
                "<gray>✦ Automatically <aqua>sell blocks<gray> as you mine.",
                "",
                "<green>→ Click to toggle this setting!"));
        } else {
            inv.setItem(SLOT_AUTOSELL, Gui.make(Material.HOPPER, "<aqua>Auto Sell",
                "<gray>✦ Status: <red>✗ Disabled",
                "<gray>✦ Automatically <aqua>sell blocks<gray> as you mine.",
                "",
                "<green>→ Click to toggle this setting!"));
        }

        // --- What will sell ---
        List<Component> whatSellsLore = new ArrayList<>();
        whatSellsLore.add(Component.empty());
        if (sellables.isEmpty()) {
            whatSellsLore.add(MM.deserialize("<!italic><red>✗ No sellable blocks in inventory."));
        } else {
            whatSellsLore.add(MM.deserialize("<!italic><gray>✦ Sellable items in your inventory:"));
            int shown = 0;
            for (Map.Entry<Material, Long> e : sellables.entrySet()) {
                if (shown >= 6) {
                    whatSellsLore.add(MM.deserialize("<!italic><gray>  ◆ ... and more"));
                    break;
                }
                whatSellsLore.add(MM.deserialize("<!italic><gray>  ◆ " + e.getValue() + "x <white>" + Fmt.mat(e.getKey().name())));
                shown++;
            }
        }
        inv.setItem(SLOT_WHAT_SELLS, Gui.make(Material.CHEST, "<aqua>What Will Sell", whatSellsLore));

        // --- Main sell button ---
        if (estimatedValue > 0) {
            inv.setItem(SLOT_SELL_ALL, Gui.make(Material.EMERALD_BLOCK, "<green>Sell All",
                "<gray>✦ Estimated total: <gold>$" + Fmt.number(estimatedValue),
                "<gray>✦ Multiplier applied at time of sale.",
                "",
                "<green>→ Click to sell all blocks!"));
        } else {
            inv.setItem(SLOT_SELL_ALL, Gui.make(Material.EMERALD_BLOCK, "<gray>Sell All",
                "<red>✗ No sellable blocks in your inventory.",
                "",
                "<gray>✦ Mine blocks to earn $."));
        }

        inv.setItem(SLOT_BACK, Gui.back());
        return inv;
    }
}
