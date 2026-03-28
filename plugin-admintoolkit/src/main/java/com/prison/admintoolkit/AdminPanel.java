package com.prison.admintoolkit;

import com.prison.regions.RegionPlugin;
import com.prison.regions.RegionWand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * AdminPanel — the master hub GUI for admin tools.
 *
 * Opens a 54-slot inventory with navigation buttons for each admin sub-tool.
 */
public class AdminPanel {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private static final Component TITLE = MM.deserialize("<dark_red><bold>⚙ Admin Panel");

    // ----------------------------------------------------------------
    // Open
    // ----------------------------------------------------------------

    public static void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);

        // Fill border with gray stained glass panes
        ItemStack filler = makeItem(Material.GRAY_STAINED_GLASS_PANE, "<gray> ");
        for (int i = 0; i < 9; i++)  inv.setItem(i, filler);       // row 0
        for (int i = 45; i < 54; i++) inv.setItem(i, filler);      // row 5
        for (int i = 9; i < 45; i += 9) {
            inv.setItem(i, filler);      // column 0
            inv.setItem(i + 8, filler);  // column 8
        }

        // Content slots
        inv.setItem(10, makeItem(Material.IRON_PICKAXE,
            "<gold>Mine Editor",
            "<gray>Edit mine block compositions,",
            "<gray>percentages, and sell prices.",
            "<dark_gray>Click to open mine list."));

        inv.setItem(12, makeItem(Material.WRITABLE_BOOK,
            "<gold>Rank Editor",
            "<gray>Edit mine rank costs,",
            "<gray>display names, and prefixes.",
            "<dark_gray>Click to edit ranks."));

        inv.setItem(14, makeItem(Material.GOLD_INGOT,
            "<gold>Economy Controls",
            "<gray>Give, take, or set a player's",
            "<gray>IGC balance or token balance.",
            "<dark_gray>Use: /ecotools <player>"));

        inv.setItem(16, makeItem(Material.PLAYER_HEAD,
            "<gold>Player Manager",
            "<gray>View full player profile,",
            "<gray>apply punishments, adjust ranks.",
            "<dark_gray>Use: /manage <player>"));

        inv.setItem(28, makeItem(Material.PAPER,
            "<gold>Announcements",
            "<gray>Broadcast chat, titles,",
            "<gray>action bar, or boss bar messages.",
            "<dark_gray>Click to open."));

        inv.setItem(30, makeItem(Material.REDSTONE,
            "<gold>Server Controls",
            "<gray>Toggle whitelist, maintenance,",
            "<gray>global PvP. Schedule restarts.",
            "<dark_gray>Click to open."));

        inv.setItem(32, makeItem(Material.GOLDEN_AXE,
            "<gold>Region Wand",
            "<gray>Gives you the region selection wand.",
            "<gray>Left-click block: corner 1",
            "<gray>Right-click block: corner 2"));

        inv.setItem(34, makeItem(Material.GRAY_STAINED_GLASS_PANE,
            "<gray>Crates Editor",
            "<red>Coming soon!",
            "<dark_gray>Plugin not yet installed."));

        player.openInventory(inv);
    }

    // ----------------------------------------------------------------
    // Title matching
    // ----------------------------------------------------------------

    public static boolean isTitle(Component title) {
        return TITLE.equals(title);
    }

    // ----------------------------------------------------------------
    // Click handling
    // ----------------------------------------------------------------

    public static void handleClick(Player player, int slot) {
        switch (slot) {
            case 10 -> MineEditorGUI.openMineList(player);
            case 12 -> RankEditorGUI.open(player);
            case 14 -> {
                player.sendMessage(MM.deserialize("<yellow>Use <white>/ecotools <player></white> to open economy tools."));
                player.closeInventory();
            }
            case 16 -> {
                player.sendMessage(MM.deserialize("<yellow>Use <white>/manage <player></white> to open the player manager."));
                player.closeInventory();
            }
            case 28 -> AnnounceGUI.open(player);
            case 30 -> ServerToolsGUI.open(player);
            case 32 -> {
                RegionPlugin regionPlugin = (RegionPlugin) Bukkit.getPluginManager().getPlugin("PrisonRegions");
                if (regionPlugin == null) {
                    player.sendMessage(MM.deserialize("<red>Region plugin is not loaded."));
                    return;
                }
                RegionWand wand = regionPlugin.getWand();
                ItemStack wandItem = wand.createWand();
                player.getInventory().addItem(wandItem);
                player.closeInventory();
                player.sendMessage(MM.deserialize("<green>Region wand added to your inventory."));
            }
            case 34 -> player.sendMessage(MM.deserialize("<yellow>Crates plugin not yet installed."));
        }
    }

    // ----------------------------------------------------------------
    // Helper — create a named/lored item with hidden attributes
    // ----------------------------------------------------------------

    static ItemStack makeItem(Material mat, String name, String... lorelines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(MM.deserialize("<!italic>" + name));
        if (lorelines != null && lorelines.length > 0) {
            List<Component> lore = new ArrayList<>();
            for (String line : lorelines) {
                lore.add(MM.deserialize("<!italic>" + line));
            }
            meta.lore(lore);
        }
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }
}
