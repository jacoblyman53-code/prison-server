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
 * MineBrowserGUI — 27-slot (3x9) mine warp and info panel.
 *
 * Layout:
 *   Slot  0      : Red X — close
 *   Slots 1-8    : Mines A–H  (row 0, right of X)
 *   Slots 9-17   : Mines I–Q  (row 1)
 *   Slots 18-26  : Mines R–Z  (row 2)
 */
public class MineBrowserGUI {

    public static final Component TITLE = MiniMessage.miniMessage()
        .deserialize("<!italic>MINES");
    private static final MiniMessage MM = MiniMessage.miniMessage();

    // Slot 0 = back/close, mines start at slot 1
    private static final int SLOT_BACK = 0;

    // 26 mine slots: slot 0 is back, mines A-Z fill slots 1-26
    private static final int[] MINE_SLOTS;
    static {
        MINE_SLOTS = new int[26];
        for (int i = 0; i < 26; i++) MINE_SLOTS[i] = i + 1;
    }

    private static final String[] MINE_IDS = RankConfig.RANK_ORDER; // A-Z

    // ~28 full stacks per inventory run
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
        int playerIdx = rankIndex(getPlayerRank(player));
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
                "<red>Mine <white>" + mineId + "<red> does not have a warp set yet."));
            return;
        }

        Sounds.nav(player);
        player.closeInventory();
        player.teleport(loc);
        player.sendMessage(MM.deserialize(
            "<green>Teleported to <white>" + mineId + " Mine<green>."));
    }

    // ----------------------------------------------------------------
    // Build
    // ----------------------------------------------------------------

    private static Inventory build(Player player) {
        // 27 slots = 3 rows × 9: slot 0 = barrier, slots 1-26 = mines A-Z, no empty rows
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);

        // Slot 0: close/back button (BARRIER) per spec
        inv.setItem(SLOT_BACK, Gui.make(Material.BARRIER, "<red>✗ Close",
            "<gray>Click to close this menu."));

        String playerRank    = getPlayerRank(player);
        int    playerRankIdx = rankIndex(playerRank);

        MinesAPI   api = MinesAPI.getInstance();
        EconomyAPI eco = EconomyAPI.getInstance();

        for (int i = 0; i < MINE_IDS.length; i++) {
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
        boolean isUnlocked = (mineIdx <= playerRankIdx);
        boolean isCurrent  = (mineIdx == playerRankIdx);

        Material icon = isUnlocked ? Material.EMERALD : Material.DIAMOND;

        // Name: "Mine A" through "Mine Z"
        String nameColor = isCurrent ? "<green>" : (isUnlocked ? "<aqua>" : "<gray>");
        String itemName  = nameColor + id + " Mine";

        // ── Lore ────────────────────────────────────────────────────
        List<Component> lore = new ArrayList<>();

        if (isCurrent) {
            lore.add(MM.deserialize("<!italic><green>✓ Current mine."));
        } else if (isUnlocked) {
            lore.add(MM.deserialize("<!italic><gray>✦ Unlocked mine."));
        } else {
            lore.add(MM.deserialize("<!italic><red>✗ Requires rank <yellow>" + id + "<red> to unlock."));
        }

        lore.add(Component.empty());

        // Average inventory value
        long avgValue = calcAvgInventoryValue(player, data, eco);
        lore.add(MM.deserialize("<!italic><gray>✦ Avg. Inventory Value: <gold>$" + Fmt.number(avgValue)));

        lore.add(Component.empty());

        // Block composition
        lore.add(MM.deserialize("<!italic><aqua>✦ Blocks:"));
        if (data != null && data.composition() != null && !data.composition().isEmpty()) {
            for (Map.Entry<Material, Double> entry : data.composition().entrySet()) {
                String blockName = Fmt.mat(entry.getKey().name());
                int    pct       = (int) Math.round(entry.getValue());
                lore.add(MM.deserialize(
                    "<!italic><gray>  ◆ <white>" + blockName + " <gray>(" + pct + "%)"));
            }
        } else {
            lore.add(MM.deserialize("<!italic><gray>  ◆ Not configured yet."));
        }

        lore.add(Component.empty());
        if (isUnlocked) {
            lore.add(MM.deserialize("<!italic><green>→ Click to warp to this mine!"));
        } else {
            lore.add(MM.deserialize("<!italic><gray>→ Reach rank <yellow>" + id + "<gray> to unlock."));
        }

        return Gui.make(icon, itemName, lore);
    }

    // ----------------------------------------------------------------
    // Average inventory value calculation
    // ----------------------------------------------------------------

    private static long calcAvgInventoryValue(Player player, MineData data, EconomyAPI eco) {
        if (data == null) return 0L;

        // Use spec target value if configured
        if (data.targetAvgInventory() > 0) return data.targetAvgInventory();

        if (data.composition() == null || data.composition().isEmpty()) return 0L;
        if (eco == null) return 0L;

        double total = data.composition().values().stream().mapToDouble(Double::doubleValue).sum();
        if (total <= 0) return 0L;

        double avgPerBlock = 0.0;
        for (Map.Entry<Material, Double> entry : data.composition().entrySet()) {
            double weight = entry.getValue() / total;
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
