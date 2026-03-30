package com.prison.menu;

import com.prison.economy.EconomyAPI;
import com.prison.menu.util.*;
import com.prison.pickaxe.PickaxeAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PickaxeEnchantsGUI {

    public static final Component TITLE = MiniMessage.miniMessage().deserialize("<!italic><dark_gray>[ <aqua>Pickaxe Enchants <dark_gray>]");
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private static final int SLOT_SPEED       = 10;
    private static final int SLOT_EXPLOSIVE   = 11;
    private static final int SLOT_LASER       = 12;
    private static final int SLOT_FORTUNE     = 13;
    private static final int SLOT_EFFICIENCY  = 14;
    private static final int SLOT_TOKEN_BONUS = 15;
    private static final int SLOT_PREV        = 45;
    private static final int SLOT_BACK        = 49;
    private static final int SLOT_MY_TOKENS   = 50;
    private static final int SLOT_NEXT        = 53;

    // Enchant IDs parallel to slots above
    private static final String[] ENCHANT_IDS   = {"speed", "explosive", "laser", "fortune", "efficiency", "token_bonus"};
    private static final int[]    ENCHANT_SLOTS  = {SLOT_SPEED, SLOT_EXPLOSIVE, SLOT_LASER, SLOT_FORTUNE, SLOT_EFFICIENCY, SLOT_TOKEN_BONUS};

    public static void open(Player player) {
        player.openInventory(build(player));
    }

    public static void handleClick(Player player, int slot, ClickType click, MenuPlugin plugin) {
        if (slot == SLOT_BACK) {
            Sounds.nav(player);
            PickaxeHomeGUI.open(player);
            return;
        }

        // Pagination slots are no-ops on page 0 (only one page currently)
        if (slot == SLOT_PREV || slot == SLOT_NEXT) {
            return;
        }

        for (int i = 0; i < ENCHANT_SLOTS.length; i++) {
            if (ENCHANT_SLOTS[i] == slot) {
                handleEnchantClick(player, ENCHANT_IDS[i], click, plugin);
                return;
            }
        }
    }

    private static void handleEnchantClick(Player player, String enchantId, ClickType click, MenuPlugin plugin) {
        PickaxeAPI papi = PickaxeAPI.getInstance();
        ItemStack held  = player.getInventory().getItemInMainHand();

        if (papi == null || !papi.isServerPickaxe(held)) {
            Sounds.deny(player);
            player.sendMessage(MM.deserialize("<red>Hold your server pickaxe to upgrade enchants."));
            return;
        }

        boolean isBuyMax = click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT;
        String cmd = isBuyMax
            ? "pickaxe upgrade " + enchantId + " max"
            : "pickaxe upgrade " + enchantId;

        Bukkit.dispatchCommand(player, cmd);
        Sounds.upgrade(player);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && TITLE.equals(player.getOpenInventory().title())) {
                player.openInventory(build(player));
            }
        }, 2L);
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

        long tokens = eco != null ? eco.getTokens(uuid) : 0L;

        // --- Token balance display ---
        inv.setItem(SLOT_MY_TOKENS, Gui.make(Material.RAW_GOLD, "<aqua>Your Tokens",
            "<gray>Balance: <aqua>" + Fmt.number(tokens) + " tokens",
            "",
            "<dark_gray>Spend tokens to upgrade enchants."));

        // No pickaxe held — show error and bail
        if (!hasPickaxe) {
            inv.setItem(13, Gui.make(Material.BARRIER, "<red>No Server Pickaxe",
                "<gray>Hold your server pickaxe to upgrade enchants.",
                "",
                "<yellow>Enchants are locked to your pickaxe."));
            inv.setItem(SLOT_BACK, Gui.back());
            return inv;
        }

        // Read current levels
        int lvlSpeed      = papi.getEnchantLevel(held, "speed");
        int lvlExplosive  = papi.getEnchantLevel(held, "explosive");
        int lvlLaser      = papi.getEnchantLevel(held, "laser");
        int lvlFortune    = papi.getEnchantLevel(held, "fortune");
        int lvlEfficiency = papi.getEnchantLevel(held, "efficiency");
        int lvlTokenBonus = papi.getEnchantLevel(held, "token_bonus");

        // --- Speed Mine ---
        inv.setItem(SLOT_SPEED, buildEnchantItem("Speed Mine", Material.SUGAR, "speed",
            lvlSpeed, 10,
            "Applies Haste while mining.",
            new String[]{"Haste I","Haste II","Haste III","Haste IV","Haste V",
                         "Haste VI","Haste VII","Haste VIII","Haste IX","Haste X"}));

        // --- Explosive ---
        inv.setItem(SLOT_EXPLOSIVE, buildEnchantItem("Explosive", Material.TNT, "explosive",
            lvlExplosive, 5,
            "Mines in a cube around your target.",
            new String[]{"3\u00d73","5\u00d75","7\u00d77","9\u00d79","11\u00d711"}));

        // --- Laser ---
        inv.setItem(SLOT_LASER, buildEnchantItem("Laser", Material.END_ROD, "laser",
            lvlLaser, 5,
            "Mines in a line forward.",
            new String[]{"3 blocks","5 blocks","7 blocks","9 blocks","11 blocks"}));

        // --- Fortune ---
        inv.setItem(SLOT_FORTUNE, buildEnchantItem("Fortune", Material.GOLD_INGOT, "fortune",
            lvlFortune, 5,
            "Increases token drops and jackpot chance.",
            new String[]{"Fortune I","Fortune II","Fortune III","Fortune IV","Fortune V"}));

        // --- Efficiency ---
        inv.setItem(SLOT_EFFICIENCY, buildEnchantItem("Efficiency", Material.DIAMOND_PICKAXE, "efficiency",
            lvlEfficiency, 5,
            "Increases mining speed (vanilla enchant).",
            new String[]{"Efficiency I","Efficiency II","Efficiency III","Efficiency IV","Efficiency V"}));

        // --- Token Bonus ---
        inv.setItem(SLOT_TOKEN_BONUS, buildEnchantItem("Token Bonus", Material.EMERALD, "token_bonus",
            lvlTokenBonus, 5,
            "Increases token drops from mining.",
            new String[]{"+10% tokens","+20% tokens","+30% tokens","+40% tokens","+50% tokens"}));

        inv.setItem(SLOT_BACK, Gui.back());
        return inv;
    }

    private static ItemStack buildEnchantItem(String name, Material mat, String id,
                                               int currentLevel, int maxLevel,
                                               String description, String[] levelNames) {
        boolean isMaxed      = currentLevel >= maxLevel;
        String currentEffect = currentLevel > 0 && currentLevel <= levelNames.length
            ? levelNames[currentLevel - 1] : "Not purchased";
        String nextEffect    = !isMaxed && currentLevel < levelNames.length
            ? levelNames[currentLevel] : "MAXED";

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(MM.deserialize("<!italic><gray>" + description));
        lore.add(Component.empty());
        lore.add(MM.deserialize("<!italic><gray>Level: <white>" + currentLevel + "<gray>/<white>" + maxLevel));
        lore.add(MM.deserialize("<!italic><gray>Current: <green>" + currentEffect));
        if (!isMaxed) {
            lore.add(MM.deserialize("<!italic><gray>Next:    <yellow>" + nextEffect));
            lore.add(MM.deserialize("<!italic><gray>Cost:    <aqua>see /pickaxe upgrade " + id));
        }
        lore.add(Component.empty());
        if (isMaxed) {
            lore.add(MM.deserialize("<!italic><green><bold>MAXED OUT"));
        } else {
            lore.add(MM.deserialize("<!italic><green>Left-click to upgrade (\u00d71)."));
            lore.add(MM.deserialize("<!italic><yellow>Shift-click to upgrade (max)."));
        }

        String nameTag = isMaxed
            ? "<green>\u2714 " + name + " (MAXED)"
            : "<aqua>" + name + " Lv." + currentLevel;
        return Gui.make(mat, nameTag, lore);
    }
}
