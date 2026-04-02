package com.prison.menu;

import com.prison.menu.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class LeaderboardSelectorGUI {

    public static final Component TITLE = MiniMessage.miniMessage().deserialize("<!italic>HALL OF LEGENDS");

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

        // Slot 0: back
        inv.setItem(0, Gui.back());

        inv.setItem(SLOT_RICHEST, Gui.make(Material.SUNFLOWER,
            "<aqua>Richest Players",
            "<gray>✦ Top 10 players by <aqua>balance<gray>.",
            "",
            "<green>→ Click to view this leaderboard!"));

        inv.setItem(SLOT_PRESTIGE, Gui.make(Material.NETHER_STAR,
            "<light_purple>Top Prestige",
            "<gray>✦ Top 10 players by <aqua>prestige level<gray>.",
            "",
            "<green>→ Click to view this leaderboard!"));

        inv.setItem(SLOT_BLOCKS, Gui.make(Material.DIAMOND_PICKAXE,
            "<aqua>Most Blocks Mined",
            "<gray>✦ Top 10 players by total <aqua>blocks mined<gray>.",
            "",
            "<green>→ Click to view this leaderboard!"));

        inv.setItem(SLOT_TOKENS, Gui.make(Material.EXPERIENCE_BOTTLE,
            "<aqua>Most Tokens",
            "<gray>✦ Top 10 players by <aqua>token balance<gray>.",
            "",
            "<green>→ Click to view this leaderboard!"));

        inv.setItem(SLOT_BACK, Gui.back());
        return inv;
    }
}
