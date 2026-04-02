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
            MM.deserialize("RANKS"));

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
        cm.displayName(MM.deserialize("<!italic><red>✗ Close"));
        cm.lore(List.of(MM.deserialize("<!italic><gray>Click to close this menu.")));
        close.setItemMeta(cm);
        inv.setItem(45, close);

        player.openInventory(inv);
    }

    private ItemStack buildRankItem(String letter, RankConfig.RankData data,
                                    int index, int currentIndex, boolean canAffordNext) {
        Material mat;

        if (index < currentIndex) {
            // Completed rank
            mat = Material.LIME_STAINED_GLASS_PANE;
        } else if (index == currentIndex) {
            // Current rank
            mat = Material.GOLD_BLOCK;
        } else if (index == currentIndex + 1 && canAffordNext) {
            // Next rank, affordable
            mat = Material.YELLOW_STAINED_GLASS_PANE;
        } else if (index == currentIndex + 1) {
            // Next rank, not yet affordable
            mat = Material.ORANGE_STAINED_GLASS_PANE;
        } else {
            // Locked future rank
            mat = Material.RED_STAINED_GLASS_PANE;
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta  = item.getItemMeta();
        meta.displayName(MM.deserialize("<!italic>" + data.prefix() + " <white>" + data.display()));

        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();

        if (index < currentIndex) {
            lore.add(MM.deserialize("<!italic><green>✓ <green>Completed"));
        } else if (index == currentIndex) {
            lore.add(MM.deserialize("<!italic><aqua>✦ <gray>Status: <green>✓ Current Rank"));
        } else if (index == currentIndex + 1 && canAffordNext) {
            lore.add(MM.deserialize("<!italic><aqua>✦ <gray>Status: <yellow>Next Rank"));
            lore.add(MM.deserialize("<!italic>"));
            lore.add(MM.deserialize("<!italic><gold>$ <gold>Cost: <white>$" + RankManager.formatNumber(data.cost())));
            lore.add(MM.deserialize("<!italic>"));
            lore.add(MM.deserialize("<!italic><green>→ <green>Click to <underlined>rank up</underlined> to <yellow>" + letter + "<green>!"));
        } else if (index == currentIndex + 1) {
            lore.add(MM.deserialize("<!italic><aqua>✦ <gray>Status: <yellow>Next Rank"));
            lore.add(MM.deserialize("<!italic>"));
            lore.add(MM.deserialize("<!italic><gold>$ <gold>Cost: <white>$" + RankManager.formatNumber(data.cost())));
            lore.add(MM.deserialize("<!italic>"));
            lore.add(MM.deserialize("<!italic><red>✗ <gray>Not enough funds to rank up yet."));
        } else {
            lore.add(MM.deserialize("<!italic><aqua>✦ <gray>Status: <red>✗ Locked"));
            if (data.cost() > 0) {
                lore.add(MM.deserialize("<!italic>"));
                lore.add(MM.deserialize("<!italic><gold>$ <gold>Cost: <white>$" + RankManager.formatNumber(data.cost())));
            }
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
        lore.add(MM.deserialize("<!italic><aqua>✦ <gray>Current Rank: <yellow>" + currentRank));
        if (nextRank != null) {
            RankConfig.RankData next = config.getRank(nextRank);
            lore.add(MM.deserialize("<!italic><aqua>✦ <gray>Next Rank: <yellow>" + nextRank));
            lore.add(MM.deserialize("<!italic>"));
            lore.add(MM.deserialize("<!italic><gold>$ <gold>Cost to Rank Up: <white>$" + RankManager.formatNumber(next.cost())));
            lore.add(MM.deserialize("<!italic>"));
            if (canAffordNext) {
                lore.add(MM.deserialize("<!italic><green>→ <green>Click the <underlined>highlighted rank</underlined> to rank up!"));
            } else {
                lore.add(MM.deserialize("<!italic><gray>Type <white>/rankup</white> to advance when ready."));
            }
        } else {
            lore.add(MM.deserialize("<!italic>"));
            lore.add(MM.deserialize("<!italic><green>✓ <green>Maximum mine rank reached!"));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
