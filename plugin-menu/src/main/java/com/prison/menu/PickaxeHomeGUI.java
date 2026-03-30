package com.prison.menu;

import com.prison.economy.EconomyAPI;
import com.prison.menu.util.*;
import com.prison.pickaxe.PickaxeAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PickaxeHomeGUI {

    public static final Component TITLE = MiniMessage.miniMessage().deserialize("<!italic><dark_gray>[ <gold>Pickaxe <dark_gray>]");
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private static final int SLOT_OVERVIEW    = 4;
    private static final int SLOT_PICKAXE     = 13;
    private static final int SLOT_BLOCKS      = 20;
    private static final int SLOT_TOKEN_BONUS = 21;
    private static final int SLOT_ENCHANTS    = 22;
    private static final int SLOT_EXPLOSIVE   = 23;
    private static final int SLOT_LASER       = 24;
    private static final int SLOT_SPEED       = 29;
    private static final int SLOT_JACKPOT     = 30;
    private static final int SLOT_RECOMMEND   = 31;
    private static final int SLOT_BACK        = 49;

    public static void open(Player player) {
        player.openInventory(build(player));
    }

    public static void handleClick(Player player, int slot, MenuPlugin plugin) {
        if (slot == SLOT_BACK) {
            Sounds.nav(player);
            MainMenuGUI.open(player);
            return;
        }
        if (slot == SLOT_ENCHANTS || slot == SLOT_RECOMMEND) {
            Sounds.nav(player);
            PickaxeEnchantsGUI.open(player);
            return;
        }
    }

    // ----------------------------------------------------------------

    private static Inventory build(Player player) {
        UUID uuid = player.getUniqueId();
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        Gui.fillAll(inv);

        PickaxeAPI papi = PickaxeAPI.getInstance();
        EconomyAPI eco  = EconomyAPI.getInstance();

        ItemStack held  = player.getInventory().getItemInMainHand();
        boolean hasPickaxe = papi != null && papi.isServerPickaxe(held);

        // --- Slot 4: Overview ---
        inv.setItem(SLOT_OVERVIEW, Gui.make(Material.BOOK, "<yellow>Server Pickaxe",
            "<gray>Your server pickaxe is bound to you.",
            "<gray>Upgrade it with tokens to mine faster",
            "<gray>and earn more IGC.",
            "",
            "<dark_gray>Hold your pickaxe to see enchant levels."));

        // --- Slot 20: Blocks Mined ---
        inv.setItem(SLOT_BLOCKS, Gui.make(Material.ANVIL, "<white>Blocks Mined",
            "<gray>Lifetime blocks mined tracked by the server.",
            "<dark_gray>View on /leaderboards for rankings."));

        if (hasPickaxe) {
            int explosive  = papi.getEnchantLevel(held, "explosive");
            int laser      = papi.getEnchantLevel(held, "laser");
            int speed      = papi.getEnchantLevel(held, "speed");
            int fortune    = papi.getEnchantLevel(held, "fortune");
            int efficiency = papi.getEnchantLevel(held, "efficiency");
            int tokenBonus = papi.getEnchantLevel(held, "token_bonus");

            // --- Slot 13: Pickaxe Summary ---
            List<Component> pickaxeLore = new ArrayList<>();
            pickaxeLore.add(Component.empty());
            if (speed > 0)      pickaxeLore.add(MM.deserialize("<!italic><gray>Speed Mine: <white>Level " + speed));
            if (explosive > 0)  pickaxeLore.add(MM.deserialize("<!italic><gray>Explosive:  <white>Level " + explosive));
            if (laser > 0)      pickaxeLore.add(MM.deserialize("<!italic><gray>Laser:      <white>Level " + laser));
            if (fortune > 0)    pickaxeLore.add(MM.deserialize("<!italic><gray>Fortune:    <white>Level " + fortune));
            if (efficiency > 0) pickaxeLore.add(MM.deserialize("<!italic><gray>Efficiency: <white>Level " + efficiency));
            if (tokenBonus > 0) pickaxeLore.add(MM.deserialize("<!italic><gray>Token Bonus:<white>Level " + tokenBonus));
            if (speed == 0 && explosive == 0 && laser == 0 && fortune == 0 && efficiency == 0 && tokenBonus == 0) {
                pickaxeLore.add(MM.deserialize("<!italic><gray>No enchants purchased yet."));
            }
            pickaxeLore.add(Component.empty());
            pickaxeLore.add(MM.deserialize("<!italic><dark_gray>Hold your pickaxe for live enchant data."));
            inv.setItem(SLOT_PICKAXE, Gui.make(Material.NETHERITE_PICKAXE, "<gold>Your Pickaxe", pickaxeLore));

            // --- Slot 21: Token Bonus ---
            double tokMult = papi.getTokenatorMultiplier(held);
            long tokens = eco != null ? eco.getTokens(uuid) : 0L;
            inv.setItem(SLOT_TOKEN_BONUS, Gui.make(Material.GOLD_NUGGET, "<aqua>Token Bonus",
                "<gray>Token multiplier: <aqua>" + Fmt.multiplier(tokMult),
                "<gray>Your tokens: <aqua>" + Fmt.number(tokens),
                "",
                "<yellow>Open Enchants to upgrade Token Bonus."));

            // --- Slot 22: Open Enchants ---
            inv.setItem(SLOT_ENCHANTS, Gui.make(Material.ENCHANTED_BOOK, "<aqua>Open Enchants",
                "<gray>View and upgrade your pickaxe enchants.",
                "",
                "<green>Click to open the Enchants menu."));

            // --- Slot 23: Explosive ---
            inv.setItem(SLOT_EXPLOSIVE, Gui.make(Material.TNT,
                explosive > 0 ? "<orange>Explosive Lv." + explosive : "<gray>Explosive: Not Purchased",
                explosive > 0 ? "<gray>Mines in a cube around your target." : "<gray>Not yet unlocked.",
                "",
                "<yellow>Open Enchants to upgrade."));

            // --- Slot 24: Laser ---
            inv.setItem(SLOT_LASER, Gui.make(Material.END_ROD,
                laser > 0 ? "<aqua>Laser Lv." + laser : "<gray>Laser: Not Purchased",
                laser > 0 ? "<gray>Mines in a line forward." : "<gray>Not yet unlocked.",
                "",
                "<yellow>Open Enchants to upgrade."));

            // --- Slot 29: Speed ---
            inv.setItem(SLOT_SPEED, Gui.make(Material.RABBIT_FOOT,
                speed > 0 ? "<yellow>Speed Mine Lv." + speed : "<gray>Speed Mine: Not Purchased",
                speed > 0 ? "<gray>Applies Haste effect while mining." : "<gray>Not yet unlocked.",
                "",
                "<yellow>Open Enchants to upgrade."));

            // --- Slot 30: Jackpot ---
            double jackpotChance = papi.getJackpotChance(held);
            if (jackpotChance > 0) {
                inv.setItem(SLOT_JACKPOT, Gui.make(Material.TOTEM_OF_UNDYING,
                    "<green>Jackpot: " + String.format("%.1f%%", jackpotChance),
                    "<gray>Each block has a " + String.format("%.1f%%", jackpotChance) + " jackpot chance.",
                    "",
                    "<yellow>Open Enchants to increase Fortune."));
            } else {
                inv.setItem(SLOT_JACKPOT, Gui.make(Material.TOTEM_OF_UNDYING, "<gray>Jackpot: None",
                    "<gray>Upgrade Fortune to unlock jackpot.",
                    "",
                    "<yellow>Open Enchants to increase Fortune."));
            }

            // --- Slot 31: Recommended Upgrade ---
            String recommend = speed == 0      ? "Speed Mine \u2014 applies Haste while mining." :
                               explosive == 0  ? "Explosive \u2014 AOE cube mining." :
                               laser == 0      ? "Laser \u2014 line forward mining." :
                                                 "Max your current enchants for greater power!";
            inv.setItem(SLOT_RECOMMEND, Gui.make(Material.COMPASS, "<yellow>Recommended Upgrade",
                "<gray>" + recommend,
                "",
                "<green>Click to open Enchants."));

        } else {
            // No server pickaxe held
            inv.setItem(SLOT_PICKAXE, Gui.make(Material.NETHERITE_PICKAXE, "<gray>No Server Pickaxe",
                "<gray>You don't have your server pickaxe equipped.",
                "",
                "<yellow>Hold your prison pickaxe in your main hand",
                "<yellow>to see enchant data here.",
                "",
                "<dark_gray>Ask staff if you lost your pickaxe."));

            inv.setItem(SLOT_TOKEN_BONUS, Gui.make(Material.GOLD_NUGGET, "<gray>Token Bonus",
                "<gray>Hold your pickaxe to view."));
            inv.setItem(SLOT_ENCHANTS, Gui.make(Material.ENCHANTED_BOOK, "<aqua>Open Enchants",
                "<gray>View and upgrade your pickaxe enchants.",
                "",
                "<green>Click to open the Enchants menu."));
            inv.setItem(SLOT_EXPLOSIVE, Gui.make(Material.TNT, "<gray>Explosive",
                "<gray>Hold your pickaxe to view."));
            inv.setItem(SLOT_LASER, Gui.make(Material.END_ROD, "<gray>Laser",
                "<gray>Hold your pickaxe to view."));
            inv.setItem(SLOT_SPEED, Gui.make(Material.RABBIT_FOOT, "<gray>Speed Mine",
                "<gray>Hold your pickaxe to view."));
            inv.setItem(SLOT_JACKPOT, Gui.make(Material.TOTEM_OF_UNDYING, "<gray>Jackpot",
                "<gray>Hold your pickaxe to view."));
            inv.setItem(SLOT_RECOMMEND, Gui.make(Material.COMPASS, "<yellow>Open Enchants",
                "<green>Click to open the Enchants menu."));
        }

        inv.setItem(SLOT_BACK, Gui.back());
        return inv;
    }
}
