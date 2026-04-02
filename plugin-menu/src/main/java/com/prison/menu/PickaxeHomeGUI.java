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

    public static final Component TITLE = MiniMessage.miniMessage().deserialize("<!italic>Pickaxe");
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
        if (slot == 8 || slot == SLOT_BACK) {
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
        TopBand.apply(inv, player);
        inv.setItem(8, Gui.back());

        // Slot 0: back
        inv.setItem(0, Gui.back());

        PickaxeAPI papi = PickaxeAPI.getInstance();
        EconomyAPI eco  = EconomyAPI.getInstance();

        ItemStack held  = player.getInventory().getItemInMainHand();
        boolean hasPickaxe = papi != null && papi.isServerPickaxe(held);

        // --- Slot 40: Overview ---
        inv.setItem(40, Gui.make(Material.BOOK, "<aqua>✦ Server Pickaxe",
            "<gray>✦ Your server pickaxe is <aqua>bound<gray> to you.",
            "<gray>✦ Upgrade it with <aqua>tokens<gray> to mine faster",
            "<gray>  and earn more $.",
            "",
            "<gray>✦ Hold your pickaxe to see enchant levels."));

        // --- Slot 20: Blocks Mined ---
        inv.setItem(SLOT_BLOCKS, Gui.make(Material.ANVIL, "<aqua>Blocks Mined",
            "<gray>✦ Lifetime <aqua>blocks mined<gray> tracked by the server.",
            "<gray>✦ View on <white>/leaderboards<gray> for rankings."));

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
            pickaxeLore.add(MM.deserialize("<!italic><gray>✦ Current Enchants:"));
            if (speed > 0)      pickaxeLore.add(MM.deserialize("<!italic><gray>  ◆ Speed Mine: <white>Level " + speed));
            if (explosive > 0)  pickaxeLore.add(MM.deserialize("<!italic><gray>  ◆ Explosive:  <white>Level " + explosive));
            if (laser > 0)      pickaxeLore.add(MM.deserialize("<!italic><gray>  ◆ Laser:      <white>Level " + laser));
            if (fortune > 0)    pickaxeLore.add(MM.deserialize("<!italic><gray>  ◆ Fortune:    <white>Level " + fortune));
            if (efficiency > 0) pickaxeLore.add(MM.deserialize("<!italic><gray>  ◆ Efficiency: <white>Level " + efficiency));
            if (tokenBonus > 0) pickaxeLore.add(MM.deserialize("<!italic><gray>  ◆ Token Bonus:<white>Level " + tokenBonus));
            if (speed == 0 && explosive == 0 && laser == 0 && fortune == 0 && efficiency == 0 && tokenBonus == 0) {
                pickaxeLore.add(MM.deserialize("<!italic><red>✗ No enchants purchased yet."));
            }
            pickaxeLore.add(Component.empty());
            pickaxeLore.add(MM.deserialize("<!italic><gray>✦ Hold your pickaxe for live enchant data."));
            inv.setItem(SLOT_PICKAXE, Gui.make(Material.NETHERITE_PICKAXE, "<aqua>Your Pickaxe", pickaxeLore));

            // --- Slot 21: Token Bonus ---
            double tokMult = papi.getTokenatorMultiplier(held);
            long tokens = eco != null ? eco.getTokens(uuid) : 0L;
            inv.setItem(SLOT_TOKEN_BONUS, Gui.make(Material.GOLD_NUGGET, "<aqua>Token Bonus",
                "<gray>✦ Token multiplier: <green>" + Fmt.multiplier(tokMult),
                "<gray>✦ Your tokens: <aqua>" + Fmt.number(tokens),
                "",
                "<green>→ Open Enchants to upgrade Token Bonus!"));

            // --- Slot 22: Open Enchants ---
            inv.setItem(SLOT_ENCHANTS, Gui.make(Material.ENCHANTED_BOOK, "<aqua>Open Enchants",
                "<gray>✦ View and <aqua>upgrade<gray> your pickaxe enchants.",
                "",
                "<green>→ Click to open the Enchants menu!"));

            // --- Slot 23: Explosive ---
            inv.setItem(SLOT_EXPLOSIVE, Gui.make(Material.TNT,
                explosive > 0 ? "<aqua>Explosive " + explosive : "<gray>Explosive",
                explosive > 0 ? "<gray>✦ Mines in a <aqua>cube<gray> around your target." : "<red>✗ Not yet unlocked.",
                "",
                "<green>→ Open Enchants to upgrade!"));

            // --- Slot 24: Laser ---
            inv.setItem(SLOT_LASER, Gui.make(Material.END_ROD,
                laser > 0 ? "<aqua>Laser " + laser : "<gray>Laser",
                laser > 0 ? "<gray>✦ Mines in a <aqua>line<gray> forward." : "<red>✗ Not yet unlocked.",
                "",
                "<green>→ Open Enchants to upgrade!"));

            // --- Slot 29: Speed ---
            inv.setItem(SLOT_SPEED, Gui.make(Material.RABBIT_FOOT,
                speed > 0 ? "<aqua>Speed Mine " + speed : "<gray>Speed Mine",
                speed > 0 ? "<gray>✦ Applies <aqua>Haste<gray> effect while mining." : "<red>✗ Not yet unlocked.",
                "",
                "<green>→ Open Enchants to upgrade!"));

            // --- Slot 30: Jackpot ---
            double jackpotChance = papi.getJackpotChance(held);
            if (jackpotChance > 0) {
                inv.setItem(SLOT_JACKPOT, Gui.make(Material.TOTEM_OF_UNDYING,
                    "<aqua>Jackpot: " + String.format("%.1f%%", jackpotChance),
                    "<gray>✦ Each block has a <green>" + String.format("%.1f%%", jackpotChance) + "<gray> jackpot chance.",
                    "",
                    "<green>→ Open Enchants to increase Fortune!"));
            } else {
                inv.setItem(SLOT_JACKPOT, Gui.make(Material.TOTEM_OF_UNDYING, "<gray>Jackpot",
                    "<red>✗ No jackpot chance yet.",
                    "<gray>✦ Upgrade <aqua>Fortune<gray> to unlock jackpot.",
                    "",
                    "<green>→ Open Enchants to increase Fortune!"));
            }

            // --- Slot 31: Recommended Upgrade ---
            String recommend = speed == 0      ? "Speed Mine — applies Haste while mining." :
                               explosive == 0  ? "Explosive — AOE cube mining." :
                               laser == 0      ? "Laser — line forward mining." :
                                                 "Max your current enchants for greater power!";
            inv.setItem(SLOT_RECOMMEND, Gui.make(Material.COMPASS, "<aqua>Recommended Upgrade",
                "<gray>✦ " + recommend,
                "",
                "<green>→ Click to open Enchants!"));

        } else {
            // No server pickaxe held
            inv.setItem(SLOT_PICKAXE, Gui.make(Material.NETHERITE_PICKAXE, "<gray>No Server Pickaxe",
                "<red>✗ You don't have your server pickaxe equipped.",
                "",
                "<gray>✦ Hold your <aqua>prison pickaxe<gray> in your main hand",
                "<gray>  to see enchant data here.",
                "",
                "<gray>✦ Ask staff if you lost your pickaxe."));

            inv.setItem(SLOT_TOKEN_BONUS, Gui.make(Material.GOLD_NUGGET, "<gray>Token Bonus",
                "<gray>✦ Hold your pickaxe to view."));
            inv.setItem(SLOT_ENCHANTS, Gui.make(Material.ENCHANTED_BOOK, "<aqua>Open Enchants",
                "<gray>✦ View and <aqua>upgrade<gray> your pickaxe enchants.",
                "",
                "<green>→ Click to open the Enchants menu!"));
            inv.setItem(SLOT_EXPLOSIVE, Gui.make(Material.TNT, "<gray>Explosive",
                "<gray>✦ Hold your pickaxe to view."));
            inv.setItem(SLOT_LASER, Gui.make(Material.END_ROD, "<gray>Laser",
                "<gray>✦ Hold your pickaxe to view."));
            inv.setItem(SLOT_SPEED, Gui.make(Material.RABBIT_FOOT, "<gray>Speed Mine",
                "<gray>✦ Hold your pickaxe to view."));
            inv.setItem(SLOT_JACKPOT, Gui.make(Material.TOTEM_OF_UNDYING, "<gray>Jackpot",
                "<gray>✦ Hold your pickaxe to view."));
            inv.setItem(SLOT_RECOMMEND, Gui.make(Material.COMPASS, "<aqua>Open Enchants",
                "<green>→ Click to open the Enchants menu!"));
        }

        inv.setItem(SLOT_BACK, Gui.back());
        return inv;
    }
}
