package com.prison.menu;

import com.prison.menu.util.*;
import com.prison.warps.WarpAPI;
import com.prison.warps.WarpData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WarpsMenuGUI {

    public static final Component TITLE = MiniMessage.miniMessage().deserialize("<!italic><dark_gray>[ <white>Warps <dark_gray>]");
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private static final int[] CONTENT_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43
    };
    private static final int WARPS_PER_PAGE = CONTENT_SLOTS.length; // 28

    private static final int SLOT_BACK     = 45;
    private static final int SLOT_PREV     = 46;
    private static final int SLOT_INFO     = 49;
    private static final int SLOT_NEXT     = 52;

    /** Tracks current page per player. */
    private static final ConcurrentHashMap<UUID, Integer> pageMap = new ConcurrentHashMap<>();

    public static void open(Player player) {
        open(player, 0);
    }

    public static void open(Player player, int page) {
        pageMap.put(player.getUniqueId(), page);
        player.openInventory(build(player, page));
        Sounds.nav(player);
    }

    public static void handleClick(Player player, int slot, MenuPlugin plugin) {
        UUID uuid = player.getUniqueId();
        int page = pageMap.getOrDefault(uuid, 0);

        List<WarpData> warps = getWarps(player);
        int totalPages = Math.max(1, (int) Math.ceil((double) warps.size() / WARPS_PER_PAGE));

        if (slot == 8 || slot == SLOT_BACK) {
            Sounds.nav(player);
            pageMap.remove(uuid);
            MainMenuGUI.open(player);
            return;
        }

        if (slot == SLOT_PREV && page > 0) {
            Sounds.nav(player);
            open(player, page - 1);
            return;
        }

        if (slot == SLOT_NEXT && page < totalPages - 1) {
            Sounds.nav(player);
            open(player, page + 1);
            return;
        }

        // Check if a warp item was clicked
        int startIndex = page * WARPS_PER_PAGE;
        for (int i = 0; i < CONTENT_SLOTS.length; i++) {
            if (CONTENT_SLOTS[i] == slot) {
                int warpIndex = startIndex + i;
                if (warpIndex < warps.size()) {
                    WarpData warp = warps.get(warpIndex);
                    Sounds.nav(player);
                    player.closeInventory();
                    try {
                        Bukkit.dispatchCommand(player, "warp " + warp.name());
                    } catch (Exception ignored) {}
                }
                return;
            }
        }
    }

    // ----------------------------------------------------------------
    // Build
    // ----------------------------------------------------------------

    private static Inventory build(Player player, int page) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        Gui.fillAll(inv);
        TopBand.apply(inv, player);
        inv.setItem(8, Gui.back());

        List<WarpData> warps = getWarps(player);
        int totalPages = Math.max(1, (int) Math.ceil((double) warps.size() / WARPS_PER_PAGE));
        page = Math.max(0, Math.min(page, totalPages - 1));

        int startIndex = page * WARPS_PER_PAGE;

        for (int i = 0; i < CONTENT_SLOTS.length; i++) {
            int warpIndex = startIndex + i;
            if (warpIndex >= warps.size()) break;

            WarpData warp = warps.get(warpIndex);
            ItemStack item = buildWarpItem(warp);
            inv.setItem(CONTENT_SLOTS[i], item);
        }

        // Back
        inv.setItem(SLOT_BACK, Gui.back());

        // Prev page
        if (page > 0) {
            inv.setItem(SLOT_PREV, Gui.prevPage(page + 1, totalPages));
        }

        // Next page
        if (page < totalPages - 1) {
            inv.setItem(SLOT_NEXT, Gui.nextPage(page + 1, totalPages));
        }

        // Info item
        inv.setItem(SLOT_INFO, Gui.make(Material.COMPASS, "<white>Warps",
            "<gray>Accessible warps: <white>" + warps.size(),
            "<gray>Page <white>" + (page + 1) + "<gray>/" + totalPages));

        return inv;
    }

    private static ItemStack buildWarpItem(WarpData warp) {
        List<Component> lore = new ArrayList<>();
        lore.add(MM.deserialize("<!italic><gray>World: <white>" + warp.world()));
        lore.add(MM.deserialize("<!italic><gray>Location: <white>"
            + (int) warp.x() + ", " + (int) warp.y() + ", " + (int) warp.z()));
        if (warp.permissionNode() != null) {
            lore.add(MM.deserialize("<!italic><dark_gray>Requires: " + warp.permissionNode()));
        }
        lore.add(Component.empty());
        lore.add(MM.deserialize("<!italic><green>Click to teleport!"));
        return Gui.make(Material.COMPASS, "<white>" + Fmt.mat(warp.name()), lore);
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private static List<WarpData> getWarps(Player player) {
        try {
            WarpAPI api = WarpAPI.getInstance();
            if (api != null) {
                List<WarpData> warps = api.getAccessibleWarps(player);
                return warps != null ? warps : new ArrayList<>();
            }
        } catch (Exception ignored) {}
        return new ArrayList<>();
    }
}
