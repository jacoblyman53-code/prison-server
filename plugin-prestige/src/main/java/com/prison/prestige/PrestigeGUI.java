package com.prison.prestige;

import com.prison.permissions.PermissionEngine;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.UUID;

/**
 * PrestigeGUI — confirmation dialog shown before a prestige executes.
 *
 * Layout (27 slots, 3 rows):
 *
 *   [border] [border] [border] [border] [border] [border] [border] [border] [border]
 *   [border] [  LOSE  ] [      ] [      ] [INFO  ] [      ] [      ] [GAIN ] [border]
 *   [border] [border] [CANCEL ] [border] [CONFIRM] [border] [CANCEL ] [border] [border]
 *
 * Red item shows what is LOST (rank resets to A, IGC wiped).
 * Green item shows what is GAINED (prestige level, token bonus, rewards).
 * Confirm = lime green wool. Cancel = red wool.
 *
 * The GUI is tracked by PrestigePlugin which handles click events.
 */
public class PrestigeGUI {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    public static final String TITLE_STRING = "Confirm Prestige";

    public static final int SLOT_CONFIRM = 13;
    public static final int SLOT_CANCEL  = 11;
    public static final int SLOT_CANCEL2 = 15;

    private final PrestigeManager manager;

    public PrestigeGUI(PrestigeManager manager) {
        this.manager = manager;
    }

    public void openConfirm(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27,
            MM.deserialize(TITLE_STRING));

        UUID uuid        = player.getUniqueId();
        int  currentP    = manager.getPrestigeLevel(uuid);
        int  newP        = currentP + 1;
        PrestigeConfig cfg = manager.getConfig();

        // LOSE item — slot 3
        inv.setItem(3, buildLoseItem());

        // GAIN item — slot 5
        inv.setItem(5, buildGainItem(newP, cfg));

        // CONFIRM — center
        inv.setItem(SLOT_CONFIRM, buildConfirmItem(newP));

        // CANCEL — both sides
        inv.setItem(SLOT_CANCEL,  buildCancelItem());
        inv.setItem(SLOT_CANCEL2, buildCancelItem());

        player.openInventory(inv);
    }

    private ItemStack buildLoseItem() {
        ItemStack item = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta  = item.getItemMeta();
        meta.displayName(MM.deserialize("<!italic><red>You Will Lose"));
        meta.lore(List.of(
            MM.deserialize("<!italic><aqua>✦ <gray>Losses on prestige:"),
            MM.deserialize("<!italic><dark_aqua>  ◆ <red>✗ <gray>Mine rank resets to <yellow>A"),
            MM.deserialize("<!italic><dark_aqua>  ◆ <red>✗ <gray>Balance wiped to <white>$0"),
            MM.deserialize("<!italic>"),
            MM.deserialize("<!italic><aqua>✦ <gray>Kept on prestige:"),
            MM.deserialize("<!italic><dark_aqua>  ◆ <green>✓ <gray>Token balance"),
            MM.deserialize("<!italic><dark_aqua>  ◆ <green>✓ <gray>Donor rank"),
            MM.deserialize("<!italic><dark_aqua>  ◆ <green>✓ <gray>Pickaxe enchants")
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildGainItem(int newPrestige, PrestigeConfig cfg) {
        ItemStack item = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta meta  = item.getItemMeta();
        meta.displayName(MM.deserialize("<!italic><green>You Will Gain"));

        double bonus = cfg.getTotalTokenBonus(newPrestige) * 100;
        List<String> rewards = cfg.getRewardCommands(newPrestige);

        var lore = new java.util.ArrayList<net.kyori.adventure.text.Component>();
        lore.add(MM.deserialize("<!italic><aqua>✦ <gray>Rewards at <yellow>P" + newPrestige + "<gray>:"));
        lore.add(MM.deserialize("<!italic><dark_aqua>  ◆ <green>+ <gray>Prestige level: <yellow>P" + newPrestige));
        lore.add(MM.deserialize("<!italic><dark_aqua>  ◆ <green>+ <gray>Token earn bonus: <green>+" + String.format("%.0f", bonus) + "% <gray>total"));
        lore.add(MM.deserialize("<!italic><dark_aqua>  ◆ <green>+ <gray>Prestige prefix in chat"));
        if (!rewards.isEmpty()) {
            lore.add(MM.deserialize("<!italic>"));
            lore.add(MM.deserialize("<!italic><green>+ <green>Tier reward included!"));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildConfirmItem(int newPrestige) {
        ItemStack item = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta meta  = item.getItemMeta();
        meta.displayName(MM.deserialize("<!italic><green>✓ Confirm"));
        meta.lore(List.of(
            MM.deserialize("<!italic><gray>Click to confirm <green>prestige<gray> to <yellow>P" + newPrestige + "<gray>."),
            MM.deserialize("<!italic>"),
            MM.deserialize("<!italic><red>✗ <gray>This cannot be undone!")
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildCancelItem() {
        ItemStack item = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta meta  = item.getItemMeta();
        meta.displayName(MM.deserialize("<!italic><red>✗ Cancel"));
        meta.lore(List.of(MM.deserialize("<!italic><gray>Click to cancel and return.")));
        item.setItemMeta(meta);
        return item;
    }
}
