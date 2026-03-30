package com.prison.menu;

import com.prison.menu.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class LeaderboardSelectorGUI {

    public static final Component TITLE = MiniMessage.miniMessage().deserialize("<!italic><dark_gray>[ <aqua>Leaderboards <dark_gray>]");

    private static final int SLOT_RICHEST  = 10;
    private static final int SLOT_PRESTIGE = 11;
    private static final int SLOT_BLOCKS   = 12;
    private static final int SLOT_TOKENS   = 13;
    private static final int SLOT_BACK     = 18;

    // ----------------------------------------------------------------

    public static void open(Player player) {
        player.openInventory(build(player));
    }

    public static void handleClick(Player player, int slot, MenuPlugin plugin) {
        switch (slot) {
            case SLOT_BACK -> {
                Sounds.nav(player);
                MainMenuGUI.open(player);
            }
            case SLOT_RICHEST, SLOT_PRESTIGE, SLOT_BLOCKS, SLOT_TOKENS -> {
                Sounds.nav(player);
                player.closeInventory();
                Bukkit.dispatchCommand(player, "leaderboard");
            }
            default -> {} // filler
        }
    }

    // ----------------------------------------------------------------

    private static Inventory build(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);
        Gui.fillAll(inv);

        inv.setItem(SLOT_RICHEST, Gui.make(Material.SUNFLOWER,
            "<gold>Richest Players",
            "<gray>Top 10 players by balance.",
            "",
            "<green>Click to view!"));

        inv.setItem(SLOT_PRESTIGE, Gui.make(Material.NETHER_STAR,
            "<light_purple>Top Prestige",
            "<gray>Top 10 players by prestige level.",
            "",
            "<green>Click to view!"));

        inv.setItem(SLOT_BLOCKS, Gui.make(Material.DIAMOND_PICKAXE,
            "<aqua>Most Blocks Mined",
            "<gray>Top 10 players by total blocks mined.",
            "",
            "<green>Click to view!"));

        inv.setItem(SLOT_TOKENS, Gui.make(Material.EXPERIENCE_BOTTLE,
            "<green>Most Tokens",
            "<gray>Top 10 players by token balance.",
            "",
            "<green>Click to view!"));

        inv.setItem(SLOT_BACK, Gui.back());
        return inv;
    }
}
