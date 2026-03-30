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
    public static final String TITLE_STRING = "<dark_purple><bold>Prestige Confirmation";

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

        fillBorder(inv);

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
        meta.displayName(MM.deserialize("<red><bold>You Will LOSE:"));
        meta.lore(List.of(
            MM.deserialize("<red>✗ Mine rank resets to <white>A"),
            MM.deserialize("<red>✗ Balance wiped to <white>$0"),
            MM.deserialize(""),
            MM.deserialize("<gray>Token balance: <green>KEPT"),
            MM.deserialize("<gray>Donor rank: <green>KEPT"),
            MM.deserialize("<gray>Pickaxe enchants: <green>KEPT")
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildGainItem(int newPrestige, PrestigeConfig cfg) {
        ItemStack item = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta meta  = item.getItemMeta();
        meta.displayName(MM.deserialize("<green><bold>You Will GAIN:"));

        double bonus = cfg.getTotalTokenBonus(newPrestige) * 100;
        List<String> rewards = cfg.getRewardCommands(newPrestige);

        var lore = new java.util.ArrayList<net.kyori.adventure.text.Component>();
        lore.add(MM.deserialize("<green>✔ Prestige level: <white>P" + newPrestige));
        lore.add(MM.deserialize("<green>✔ Token earn bonus: <white>+" + String.format("%.0f", bonus) + "% total"));
        lore.add(MM.deserialize("<green>✔ Prestige prefix in chat"));
        if (!rewards.isEmpty()) {
            lore.add(MM.deserialize(""));
            lore.add(MM.deserialize("<gold>Tier reward included!"));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildConfirmItem(int newPrestige) {
        ItemStack item = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta meta  = item.getItemMeta();
        meta.displayName(MM.deserialize("<green><bold>CONFIRM PRESTIGE"));
        meta.lore(List.of(
            MM.deserialize("<gray>Click to prestige to <white>P" + newPrestige),
            MM.deserialize("<yellow>This cannot be undone!")
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildCancelItem() {
        ItemStack item = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta meta  = item.getItemMeta();
        meta.displayName(MM.deserialize("<red><bold>CANCEL"));
        meta.lore(List.of(MM.deserialize("<gray>Close without prestiging.")));
        item.setItemMeta(meta);
        return item;
    }

    private void fillBorder(Inventory inv) {
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta    = filler.getItemMeta();
        meta.displayName(MM.deserialize(" "));
        filler.setItemMeta(meta);
        for (int i = 0; i < 9; i++) inv.setItem(i, filler);
        inv.setItem(9,  filler); inv.setItem(17, filler);
        inv.setItem(18, filler); inv.setItem(26, filler);
        for (int i = 19; i < 27; i++) inv.setItem(i, filler);
    }
}
