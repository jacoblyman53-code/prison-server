package com.prison.menu;

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

public class MineBrowserGUI {

    public static final Component TITLE = MiniMessage.miniMessage().deserialize("<!italic><dark_gray>[ <green>Mine Browser <dark_gray>]");
    private static final MiniMessage MM = MiniMessage.miniMessage();

    // Slot positions for mines A-Z (26 total)
    private static final int[] MINE_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,   // A-G  (row 1)
        19, 20, 21, 22, 23, 24, 25,   // H-N  (row 2)
        28, 29, 30, 31, 32, 33, 34,   // O-U  (row 3)
        37, 38, 39, 40, 41            // V-Z  (row 4)
    };

    // Reuse the same ordered array defined in RankConfig
    private static final String[] MINE_IDS = RankConfig.RANK_ORDER;

    private static final int SLOT_INFO  = 4;
    private static final int SLOT_BACK  = 49;
    private static final int SLOT_CLOSE = 53;

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
        if (slot == SLOT_CLOSE) {
            Sounds.close(player);
            player.closeInventory();
            return;
        }

        // Check if a mine slot was clicked
        for (int i = 0; i < MINE_SLOTS.length; i++) {
            if (MINE_SLOTS[i] == slot) {
                handleMineClick(player, MINE_IDS[i], plugin);
                return;
            }
        }
        // All other slots are filler — no action
    }

    // ----------------------------------------------------------------
    // Click handling
    // ----------------------------------------------------------------

    private static void handleMineClick(Player player, String mineId, MenuPlugin plugin) {
        String playerRank = getPlayerRank(player);
        int playerIdx = rankIndex(playerRank);
        int mineIdx   = rankIndex(mineId);

        if (mineIdx > playerIdx) {
            Sounds.deny(player);
            player.sendMessage(MM.deserialize("<red>You need rank <white>" + mineId + "<red> to access this mine."));
            return;
        }

        MinesAPI api = MinesAPI.getInstance();
        if (api == null) {
            player.sendMessage(MM.deserialize("<red>Mines are not available right now."));
            return;
        }

        Location loc = api.getSpawnLocation(mineId);
        if (loc == null) {
            player.sendMessage(MM.deserialize("<red>Mine <white>" + mineId + "<red> does not have a teleport configured."));
            return;
        }

        Sounds.nav(player);
        player.closeInventory();
        player.teleport(loc);
        player.sendMessage(MM.deserialize("<green>Teleported to Mine <white>" + mineId + "<green>."));
    }

    // ----------------------------------------------------------------
    // Build
    // ----------------------------------------------------------------

    private static Inventory build(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        Gui.fillAll(inv);

        String playerRank    = getPlayerRank(player);
        int    playerRankIdx = rankIndex(playerRank);

        // Info item (slot 4)
        inv.setItem(SLOT_INFO, Gui.make(Material.BOOK, "<yellow>Mine Guide",
            "<gray>Green  = your current mine.",
            "<gray>White  = unlocked — click to teleport.",
            "<gray>Red    = locked — need higher rank.",
            "",
            "<gray>Your rank: <white>" + playerRank));

        // Mine items
        MinesAPI api = MinesAPI.getInstance();
        for (int i = 0; i < MINE_IDS.length && i < MINE_SLOTS.length; i++) {
            String   id      = MINE_IDS[i];
            MineData data    = (api != null) ? api.getMine(id) : null;
            inv.setItem(MINE_SLOTS[i], buildMineItem(id, i, playerRankIdx, data));
        }

        inv.setItem(SLOT_BACK,  Gui.back());
        inv.setItem(SLOT_CLOSE, Gui.close());
        return inv;
    }

    // ----------------------------------------------------------------
    // Item factory
    // ----------------------------------------------------------------

    private static ItemStack buildMineItem(String id, int mineIdx, int playerRankIdx, MineData data) {
        boolean isCurrent  = (mineIdx == playerRankIdx);
        boolean isUnlocked = (mineIdx <= playerRankIdx);

        String displayName = (data != null && data.display() != null && !data.display().isEmpty())
            ? data.display()
            : "Mine " + id;

        String resetLine = (data != null && data.resetTimerMins() > 0)
            ? "<gray>Resets every: <white>" + data.resetTimerMins() + "m"
            : "<gray>Reset: <dark_gray>timer disabled";

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(MM.deserialize("<!italic><gray>Required rank: <white>" + id));
        lore.add(MM.deserialize("<!italic>" + resetLine));
        lore.add(Component.empty());

        Material mat;
        String nameColor;

        if (isCurrent) {
            mat = resolveCompositionMaterial(data, Material.LIME_CONCRETE);
            nameColor = "<green>";
            lore.add(MM.deserialize("<!italic><green><bold>\u25b6 YOU ARE HERE"));
            lore.add(Component.empty());
            lore.add(MM.deserialize("<!italic><green>Click to teleport to this mine."));
        } else if (isUnlocked) {
            mat = resolveCompositionMaterial(data, Material.STONE);
            nameColor = "<white>";
            lore.add(MM.deserialize("<!italic><aqua>Unlocked"));
            lore.add(Component.empty());
            lore.add(MM.deserialize("<!italic><green>Click to teleport here."));
        } else {
            mat = Material.RED_STAINED_GLASS_PANE;
            nameColor = "<red>";
            lore.add(MM.deserialize("<!italic><red>Locked"));
            lore.add(Component.empty());
            lore.add(MM.deserialize("<!italic><red>Requires mine rank <white>" + id + "<red> to access."));
        }

        // Strip any MiniMessage tags from the display name so it reads cleanly inside the composite title
        String plainDisplay = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
            .plainText().serialize(MM.deserialize(displayName));

        return Gui.make(mat, nameColor + "Mine " + id + " \u2014 " + plainDisplay, lore);
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    /**
     * Returns the first composition material from a MineData, or the given
     * fallback if the data is null or the composition map is empty.
     */
    private static Material resolveCompositionMaterial(MineData data, Material fallback) {
        if (data != null && data.composition() != null && !data.composition().isEmpty()) {
            return data.composition().keySet().iterator().next();
        }
        return fallback;
    }

    /**
     * Looks up the 0-based index of a rank letter inside the static
     * {@link RankConfig#RANK_ORDER} array. Returns 0 if not found.
     */
    private static int rankIndex(String rank) {
        String upper = (rank != null) ? rank.toUpperCase() : "";
        for (int i = 0; i < RankConfig.RANK_ORDER.length; i++) {
            if (RankConfig.RANK_ORDER[i].equals(upper)) return i;
        }
        return 0;
    }

    /** Reads the player's current mine rank from PermissionEngine, defaulting to "A". */
    private static String getPlayerRank(Player player) {
        try {
            PermissionEngine pe = PermissionEngine.getInstance();
            if (pe != null) return pe.getMineRank(player.getUniqueId());
        } catch (Exception ignored) {}
        return "A";
    }
}
