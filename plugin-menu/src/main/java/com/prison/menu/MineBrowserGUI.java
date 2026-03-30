package com.prison.menu;

import com.prison.economy.EconomyAPI;
import com.prison.menu.util.*;
import com.prison.mines.MineData;
import com.prison.mines.MinesAPI;
import com.prison.permissions.PermissionEngine;
import com.prison.ranks.RankConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * MineBrowserGUI — shows all 26 A-Z mines in a compact grid.
 *
 * Layout (36 slots, 4 rows):
 *   Row 0 (0-8):   TopBand, back at slot 8
 *   Row 1 (9-17):  Mines A-I
 *   Row 2 (18-26): Mines J-R
 *   Row 3 (27-35): Mines S-Z + 1 filler at slot 35
 *
 * No border columns — mines fill every slot in rows 1-3.
 */
public class MineBrowserGUI {

    public static final Component TITLE = MiniMessage.miniMessage()
        .deserialize("<!italic><dark_gray>[ <green>Mines <dark_gray>]");
    private static final MiniMessage MM = MiniMessage.miniMessage();

    // 26 consecutive slots — rows 1-3, all 9 columns, no border gaps
    private static final int[] MINE_SLOTS = {
         9, 10, 11, 12, 13, 14, 15, 16, 17,  // A-I
        18, 19, 20, 21, 22, 23, 24, 25, 26,  // J-R
        27, 28, 29, 30, 31, 32, 33, 34       // S-Z
    };

    private static final String[] MINE_IDS = RankConfig.RANK_ORDER; // A-Z

    private static final int SLOT_BACK = 8;

    // Icons: locked = red glass, unlocked/current = emerald, future = diamond
    private static final Material ICON_LOCKED   = Material.RED_STAINED_GLASS_PANE;
    private static final Material ICON_UNLOCKED = Material.EMERALD;
    private static final Material ICON_LOCKED_MINE = Material.DIAMOND;

    // ── Tier display colors (matches mine display tags in config) ─────────────
    private static final String[] MINE_TIER_COLORS = {
        "<gray>",         // A
        "<gray>",         // B
        "<gray>",         // C
        "<gray>",         // D
        "<gray>",         // E
        "<yellow>",       // F
        "<yellow>",       // G
        "<yellow>",       // H
        "<yellow>",       // I
        "<yellow>",       // J
        "<gold>",         // K
        "<gold>",         // L
        "<gold>",         // M
        "<gold>",         // N
        "<gold>",         // O
        "<aqua>",         // P
        "<aqua>",         // Q
        "<aqua>",         // R
        "<aqua>",         // S
        "<aqua>",         // T
        "<light_purple>", // U
        "<light_purple>", // V
        "<light_purple>", // W
        "<dark_purple>",  // X
        "<dark_purple>",  // Y
        "<dark_purple>",  // Z
    };

    // Assumed block count for "average full inventory" value calculation
    // ~28 full stacks of 64 = 1,792 blocks (typical mine run)
    private static final int AVG_INVENTORY_BLOCKS = 1792;

    // ----------------------------------------------------------------
    // Public API
    // ----------------------------------------------------------------

    public static void open(Player player) {
        player.openInventory(build(player));
        Sounds.nav(player);
    }

    public static void handleClick(Player player, int slot, MenuPlugin plugin) {
        if (slot == SLOT_BACK) {
            Sounds.nav(player);
            MainMenuGUI.open(player);
            return;
        }

        for (int i = 0; i < MINE_SLOTS.length; i++) {
            if (MINE_SLOTS[i] == slot) {
                handleMineClick(player, MINE_IDS[i]);
                return;
            }
        }
    }

    // ----------------------------------------------------------------
    // Click handling
    // ----------------------------------------------------------------

    private static void handleMineClick(Player player, String mineId) {
        String playerRank = getPlayerRank(player);
        int playerIdx = rankIndex(playerRank);
        int mineIdx   = rankIndex(mineId);

        if (mineIdx > playerIdx) {
            Sounds.deny(player);
            player.sendMessage(MM.deserialize(
                "<red>You need rank <white>" + mineId + "<red> to access this mine."));
            return;
        }

        MinesAPI api = MinesAPI.getInstance();
        if (api == null) {
            player.sendMessage(MM.deserialize("<red>Mines are not available right now."));
            return;
        }

        Location loc = api.getSpawnLocation(mineId);
        if (loc == null) {
            player.sendMessage(MM.deserialize(
                "<red>Mine <white>" + mineId + "<red> does not have a warp configured yet."));
            return;
        }

        Sounds.nav(player);
        player.closeInventory();
        player.teleport(loc);
        player.sendMessage(MM.deserialize(
            "<green>Teleported to <white>Mine " + mineId + "<green>."));
    }

    // ----------------------------------------------------------------
    // Build
    // ----------------------------------------------------------------

    private static Inventory build(Player player) {
        Inventory inv = Bukkit.createInventory(null, 36, TITLE);
        Gui.fillAll(inv);
        TopBand.apply(inv, player);
        inv.setItem(SLOT_BACK, Gui.back());

        String playerRank    = getPlayerRank(player);
        int    playerRankIdx = rankIndex(playerRank);

        MinesAPI api = MinesAPI.getInstance();
        EconomyAPI eco = EconomyAPI.getInstance();

        for (int i = 0; i < MINE_IDS.length && i < MINE_SLOTS.length; i++) {
            String   id   = MINE_IDS[i];
            MineData data = (api != null) ? api.getMine(id) : null;
            inv.setItem(MINE_SLOTS[i], buildMineItem(player, id, i, playerRankIdx, data, eco));
        }

        return inv;
    }

    // ----------------------------------------------------------------
    // Mine item builder
    // ----------------------------------------------------------------

    private static ItemStack buildMineItem(Player player, String id, int mineIdx,
                                            int playerRankIdx, MineData data, EconomyAPI eco) {
        boolean isCurrent  = (mineIdx == playerRankIdx);
        boolean isUnlocked = (mineIdx <= playerRankIdx);

        String tierColor = mineIdx < MINE_TIER_COLORS.length ? MINE_TIER_COLORS[mineIdx] : "<white>";

        // ── Average inventory value ─────────────────────────────────
        long avgValue = calcAvgInventoryValue(player, data, eco);

        // ── Build lore ──────────────────────────────────────────────
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());

        // Avg inventory value (cyan, prominent)
        lore.add(MM.deserialize("<!italic><gray>Avg. Inventory: <aqua>$" + Fmt.number(avgValue)));

        // Composition block list
        if (data != null && data.composition() != null && !data.composition().isEmpty()) {
            lore.add(Component.empty());
            lore.add(MM.deserialize("<!italic><dark_gray>Block Composition:"));
            for (Map.Entry<Material, Double> entry : data.composition().entrySet()) {
                String blockName = Fmt.mat(entry.getKey().name());
                int    pct       = (int) Math.round(entry.getValue());
                lore.add(MM.deserialize(
                    "<!italic><green>+ <white>" + blockName + " <gray>(" + pct + "%)"));
            }
        }

        lore.add(Component.empty());

        // Reset timer
        if (data != null && data.resetTimerMins() > 0) {
            lore.add(MM.deserialize(
                "<!italic><gray>Resets every: <white>" + data.resetTimerMins() + "m"));
        }

        lore.add(Component.empty());

        // Status line + click instruction
        Material icon;
        String   namePrefix;

        if (!isUnlocked) {
            icon       = ICON_LOCKED_MINE;
            namePrefix = "<dark_gray>";
            lore.add(MM.deserialize("<!italic><red>Locked"));
            lore.add(MM.deserialize(
                "<!italic><dark_gray>Requires rank <gray>" + id + "<dark_gray> to access."));
        } else if (isCurrent) {
            icon       = ICON_UNLOCKED;
            namePrefix = "<green>";
            lore.add(MM.deserialize("<!italic><green><bold>▶ YOUR MINE"));
            lore.add(Component.empty());
            lore.add(MM.deserialize("<!italic><green>Click to warp to the " + id + " Mine."));
        } else {
            icon       = ICON_UNLOCKED;
            namePrefix = tierColor;
            lore.add(MM.deserialize("<!italic><aqua>Unlocked"));
            lore.add(Component.empty());
            lore.add(MM.deserialize("<!italic><green>Click to warp to the " + id + " Mine."));
        }

        return Gui.make(icon, namePrefix + "Mine " + id, lore);
    }

    // ----------------------------------------------------------------
    // Average inventory value calculation
    // ----------------------------------------------------------------

    /**
     * Calculates the average value of a full inventory if mined from this mine.
     * Formula: sum(weight% × sell_price_per_block) × AVG_INVENTORY_BLOCKS
     */
    private static long calcAvgInventoryValue(Player player, MineData data, EconomyAPI eco) {
        if (data == null || data.composition() == null || data.composition().isEmpty()) {
            return 0L;
        }
        if (eco == null) {
            return 0L;
        }

        double totalWeight  = data.composition().values().stream().mapToDouble(Double::doubleValue).sum();
        if (totalWeight <= 0) return 0L;

        double avgPerBlock = 0.0;
        for (Map.Entry<Material, Double> entry : data.composition().entrySet()) {
            double weight = entry.getValue() / totalWeight; // normalise to 0-1
            long   price  = eco.getSellPrice(entry.getKey(), player);
            avgPerBlock  += weight * price;
        }

        return Math.round(avgPerBlock * AVG_INVENTORY_BLOCKS);
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private static int rankIndex(String rank) {
        String upper = (rank != null) ? rank.toUpperCase() : "";
        for (int i = 0; i < RankConfig.RANK_ORDER.length; i++) {
            if (RankConfig.RANK_ORDER[i].equals(upper)) return i;
        }
        return 0;
    }

    private static String getPlayerRank(Player player) {
        try {
            PermissionEngine pe = PermissionEngine.getInstance();
            if (pe != null) return pe.getMineRank(player.getUniqueId());
        } catch (Exception ignored) {}
        return "A";
    }
}
