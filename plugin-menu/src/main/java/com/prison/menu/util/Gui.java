package com.prison.menu.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;

import java.util.Arrays;
import java.util.List;

/**
 * Gui — shared GUI construction utilities used by every menu in plugin-menu.
 */
public final class Gui {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private Gui() {}

    /** Standard filler item: black stained glass pane, empty display name, no lore. */
    public static ItemStack filler() {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.empty());
        meta.lore(List.of());
        item.setItemMeta(meta);
        return item;
    }

    /** Fill every slot in the given inventory array with filler. */
    public static void fillAll(org.bukkit.inventory.Inventory inv) {
        ItemStack f = filler();
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, f);
    }

    /**
     * Build an ItemStack with a given material, display name (MiniMessage), and lore lines (MiniMessage).
     * Display name automatically gets <!italic> prefix.
     */
    public static ItemStack make(Material mat, String name, String... loreMM) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(MM.deserialize("<!italic>" + name));
        if (loreMM.length > 0) {
            meta.lore(Arrays.stream(loreMM)
                .map(l -> l.isEmpty() ? Component.empty() : MM.deserialize("<!italic>" + l))
                .toList());
        }
        item.setItemMeta(meta);
        return item;
    }

    /** Make an item with a Component list as lore. */
    public static ItemStack make(Material mat, String name, List<Component> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(MM.deserialize("<!italic>" + name));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /** Standard back button — BARRIER at slot 0 per spec. */
    public static ItemStack back() {
        return make(Material.BARRIER, "<red>← Back", "<gray>Return to previous menu.");
    }

    /** Standard close button — BARRIER at slot 0 per spec. */
    public static ItemStack close() {
        return make(Material.BARRIER, "<red>✗ Close", "<gray>Click to close this menu.");
    }

    /** Previous page button — gray dye, slot 45. */
    public static ItemStack prevPage(int currentPage, int totalPages) {
        return make(Material.GRAY_DYE, "<gray>← Previous Page",
            "<gray>Page <white>" + currentPage + " <gray>← <white>" + (currentPage - 1));
    }

    /** Next page button — lime dye, slot 53. */
    public static ItemStack nextPage(int currentPage, int totalPages) {
        return make(Material.LIME_DYE, "<green>→ Next Page",
            "<gray>Page <white>" + currentPage + " <gray>→ <white>" + (currentPage + 1));
    }

    /** Locked slot indicator (RED_STAINED_GLASS_PANE). */
    public static ItemStack locked(String reason) {
        return make(Material.RED_STAINED_GLASS_PANE, "<red>Locked", reason);
    }

    /** Info item (BOOK). */
    public static ItemStack info(String name, String... lore) {
        return make(Material.BOOK, "<yellow>" + name, lore);
    }

    /** Converts "DIAMOND_PICKAXE" → "Diamond Pickaxe". */
    public static String formatMat(String name) {
        String[] parts = name.toLowerCase().replace("_", " ").split(" ");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return sb.toString();
    }

    /** MiniMessage instance available for GUI classes. */
    public static MiniMessage mm() { return MM; }

    /** Checks if the given inventory title matches a MiniMessage string. */
    public static boolean titleMatches(Component title, String mmString) {
        return title.equals(MM.deserialize(mmString));
    }
}
