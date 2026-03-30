package com.prison.ranks;

import com.prison.permissions.PermissionEngine;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * RanksGUI — displays the rank progression ladder in a chest GUI.
 *
 * Layout: 54-slot inventory (6 rows).
 * Each rank (A-Z) gets its own slot with a colored stained glass pane.
 *
 * Colors:
 *   - Completed ranks (below current):  green glass pane
 *   - Current rank:                     gold block
 *   - Next rank (affordable):           yellow glass pane
 *   - Locked ranks (too expensive):     red glass pane
 *
 * Lore shows:
 *   - Display name
 *   - Cost to reach this rank
 *   - "CURRENT RANK" / "UNLOCKED" / "Cost: X IGC" status
 *
 * This is a read-only display — clicking does nothing.
 * Actual rankup happens via /rankup command.
 */
public class RanksGUI {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final RankManager manager;

    // Slot layout: ranks A-Z fill slots 1-26 in the 54-slot grid (skip edges)
    // package-private so RankPlugin can route click events
    static final int[] RANK_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,   // row 2: A-G
        19, 20, 21, 22, 23, 24, 25,   // row 3: H-N
        28, 29, 30, 31, 32, 33, 34,   // row 4: O-U
        38, 39, 40, 41, 42            // row 5: V-Z
    };

    public RanksGUI(RankManager manager) {
        this.manager = manager;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54,
            MM.deserialize("<gold><bold>Mine Rank Progression"));

        fillBorder(inv);

        RankConfig config    = manager.getConfig();
        String currentRank   = PermissionEngine.getInstance().getMineRank(player.getUniqueId());
        int    currentIndex  = config.rankIndex(currentRank);
        boolean canAffordNext = manager.canRankUp(player.getUniqueId());

        String[] ranks = RankConfig.RANK_ORDER;
        for (int i = 0; i < ranks.length && i < RANK_SLOTS.length; i++) {
            String letter = ranks[i];
            RankConfig.RankData data = config.getRank(letter);
            if (data == null) continue;

            int slot = RANK_SLOTS[i];
            inv.setItem(slot, buildRankItem(letter, data, i, currentIndex, canAffordNext));
        }

        // Info item in bottom center
        inv.setItem(49, buildInfoItem(player, currentRank, config, canAffordNext));

        // Close button
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta cm = close.getItemMeta();
        cm.displayName(MM.deserialize("<!italic><red>Close"));
        close.setItemMeta(cm);
        inv.setItem(45, close);

        player.openInventory(inv);
    }

    private ItemStack buildRankItem(String letter, RankConfig.RankData data,
                                    int index, int currentIndex, boolean canAffordNext) {
        Material mat;
        String statusLine;

        if (index < currentIndex) {
            // Completed rank
            mat = Material.LIME_STAINED_GLASS_PANE;
            statusLine = "<green>✔ Unlocked";
        } else if (index == currentIndex) {
            // Current rank
            mat = Material.GOLD_BLOCK;
            statusLine = "<gold>★ Current Rank";
        } else if (index == currentIndex + 1 && canAffordNext) {
            // Next rank, affordable
            mat = Material.YELLOW_STAINED_GLASS_PANE;
            statusLine = "<yellow>★ Click to rank up! Cost: <white>$" + RankManager.formatNumber(data.cost());
        } else if (index == currentIndex + 1) {
            // Next rank, not yet affordable
            mat = Material.ORANGE_STAINED_GLASS_PANE;
            statusLine = "<yellow>Next | Cost: <white>$" + RankManager.formatNumber(data.cost());
        } else {
            // Locked future rank
            mat = Material.RED_STAINED_GLASS_PANE;
            statusLine = "<red>Locked | Cost: <white>$" + RankManager.formatNumber(data.cost());
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta  = item.getItemMeta();
        meta.displayName(MM.deserialize(data.prefix() + " <white>" + data.display()));

        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        lore.add(MM.deserialize(statusLine));
        if (index > 0 && data.cost() > 0) {
            lore.add(MM.deserialize("<dark_gray>Rankup cost: <gray>$" + RankManager.formatNumber(data.cost())));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildInfoItem(Player player, String currentRank, RankConfig config,
                                     boolean canAffordNext) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta  = item.getItemMeta();
        meta.displayName(MM.deserialize("<!italic><aqua>Your Progress"));

        String nextRank = config.nextRank(currentRank);
        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        lore.add(MM.deserialize("<!italic><gray>Current rank: <white>" + currentRank));
        if (nextRank != null) {
            RankConfig.RankData next = config.getRank(nextRank);
            lore.add(MM.deserialize("<!italic><gray>Next rank: <white>" + nextRank
                + " <dark_gray>(costs $" + RankManager.formatNumber(next.cost()) + ")"));
            lore.add(MM.deserialize("<!italic>"));
            if (canAffordNext) {
                lore.add(MM.deserialize("<!italic><green><bold>Click the highlighted rank to rank up!"));
            } else {
                lore.add(MM.deserialize("<!italic><yellow>Type <white>/rankup</white> to advance!"));
            }
        } else {
            lore.add(MM.deserialize("<!italic><gold>You are at the maximum mine rank!"));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private void fillBorder(Inventory inv) {
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta    = filler.getItemMeta();
        meta.displayName(MM.deserialize(" "));
        filler.setItemMeta(meta);

        for (int i = 0; i < 9; i++)  inv.setItem(i, filler);       // top row
        for (int i = 45; i < 54; i++) inv.setItem(i, filler);      // bottom row
        for (int i = 1; i < 5; i++) {
            inv.setItem(i * 9, filler);      // left column
            inv.setItem(i * 9 + 8, filler);  // right column
        }
    }
}
