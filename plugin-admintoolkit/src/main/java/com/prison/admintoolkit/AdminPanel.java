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

    private static final Component TITLE = MM.deserialize("<!italic>Admin Panel");

    // ----------------------------------------------------------------
    // Open
    // ----------------------------------------------------------------

    public static void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);

        // Slot 0: close button — spec: slot 0 always close/back
        inv.setItem(0, makeItem(Material.BARRIER,
            "<red>✗ Close",
            "<gray>Click to close this menu."));

        // Content slots
        inv.setItem(10, makeItem(Material.IRON_PICKAXE,
            "<aqua>Mine Editor",
            "<aqua>✦ <gray>Edit mine <green>block compositions<gray>,",
            "<gray>  percentages, and <green>sell prices<gray>.",
            "",
            "<green>→ <green>Click to open <yellow>Mine Editor<green>!"));

        inv.setItem(12, makeItem(Material.WRITABLE_BOOK,
            "<aqua>Rank Editor",
            "<aqua>✦ <gray>Edit mine rank <green>costs<gray>,",
            "<gray>  display names, and <green>prefixes<gray>.",
            "",
            "<green>→ <green>Click to open <yellow>Rank Editor<green>!"));

        inv.setItem(14, makeItem(Material.GOLD_INGOT,
            "<aqua>Economy Controls",
            "<aqua>✦ <gray>Give, take, or <green>set<gray> a player's",
            "<gray>  <green>IGC balance<gray> or <green>token balance<gray>.",
            "",
            "<gray>Use: <yellow>/ecotools <player>"));

        inv.setItem(16, makeItem(Material.PLAYER_HEAD,
            "<aqua>Player Manager",
            "<aqua>✦ <gray>View full <green>player profile<gray>,",
            "<gray>  apply <green>punishments<gray>, adjust ranks.",
            "",
            "<gray>Use: <yellow>/manage <player>"));

        inv.setItem(28, makeItem(Material.PAPER,
            "<aqua>Announcements",
            "<aqua>✦ <gray>Broadcast <green>chat<gray>, titles,",
            "<gray>  action bar, or <green>boss bar<gray> messages.",
            "",
            "<green>→ <green>Click to open <yellow>Announcements<green>!"));

        inv.setItem(30, makeItem(Material.REDSTONE,
            "<aqua>Server Controls",
            "<aqua>✦ <gray>Toggle <green>whitelist<gray>, maintenance,",
            "<gray>  global <green>PvP<gray>. Schedule restarts.",
            "",
            "<green>→ <green>Click to open <yellow>Server Controls<green>!"));

        inv.setItem(32, makeItem(Material.GOLDEN_AXE,
            "<aqua>Region Wand",
            "<aqua>✦ <gray>Gives you the <green>region selection wand<gray>.",
            "",
            "<gray>◆ Left-click block: <green>corner 1",
            "<gray>◆ Right-click block: <green>corner 2",
            "",
            "<green>→ <green>Click to receive the <yellow>Region Wand<green>!"));

        inv.setItem(34, makeItem(Material.PAPER,
            "<gray>Crates Editor",
            "<red>✗ <gray>Coming soon!",
            "<gray>  Plugin not yet installed."));

        // Close button (bottom-center)
        inv.setItem(49, makeItem(Material.BARRIER,
            "<red>✗ Close",
            "<gray>Click to close this menu."));

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
            case 0, 49 -> player.closeInventory();
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
