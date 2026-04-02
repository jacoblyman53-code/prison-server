package com.prison.leaderboards;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 54-slot leaderboard GUI.
 *
 * <pre>
 * Row 0 (slots  0- 8): category selectors at 1/2/3/4; slot 0 = close button
 * Row 1 (slots  9-17): entries at 10-14
 * Row 2 (slots 18-26): entries at 19-23
 * Rows 3-5 (27-53):    slot 49 = Close button
 * </pre>
 */
public class LeaderboardGUI {

    // ----------------------------------------------------------------
    // Constants
    // ----------------------------------------------------------------

    private static final MiniMessage MM = MiniMessage.miniMessage();

    static final Component TITLE = MM.deserialize("<!italic>LEADERBOARDS");

    /** Slot for the close button. */
    private static final int SLOT_CLOSE = 49;

    /** Category selector slots (row 0). */
    private static final int SLOT_RICHEST  = 1;
    private static final int SLOT_PRESTIGE = 2;
    private static final int SLOT_BLOCKS   = 3;
    private static final int SLOT_TOKENS   = 4;

    /**
     * Entry display slots: two rows of 5, covering ranks 1-5 (row 1) and 6-10
     * (row 2).
     */
    private static final int[] ENTRY_SLOTS = {
        10, 11, 12, 13, 14,   // ranks 1-5
        19, 20, 21, 22, 23    // ranks 6-10
    };

    // ----------------------------------------------------------------
    // State
    // ----------------------------------------------------------------

    /** Active category per player (default: "richest"). */
    private static final Map<UUID, String> activeCategory = new ConcurrentHashMap<>();

    // ----------------------------------------------------------------
    // Public API
    // ----------------------------------------------------------------

    /**
     * Opens (or re-renders) the leaderboard GUI for {@code player} showing
     * the specified {@code category}.
     *
     * @param player   the player to show the GUI to
     * @param manager  the {@link LeaderboardManager} used to fetch cached data
     * @param category category id: "richest", "prestige", "blocks", or "tokens"
     */
    public static void open(Player player, LeaderboardManager manager, String category) {
        activeCategory.put(player.getUniqueId(), category);

        Inventory inv = Bukkit.createInventory(null, 54, TITLE);

        // --- Slot 0: Close button (spec: always top-left) ---
        inv.setItem(0, makeItem(Material.BARRIER,
            "<!italic><red>✗ Close",
            "<!italic><gray>Click to close this menu."));

        // --- Row 0: category selectors (slots 1-4) ---
        inv.setItem(SLOT_RICHEST,  makeCategoryItem(Material.GOLD_INGOT,
            "<!italic><gold>Richest",
            "<!italic><gray>View the <green>top balance<gray> players.",
            category.equals("richest")));

        inv.setItem(SLOT_PRESTIGE, makeCategoryItem(Material.NETHER_STAR,
            "<!italic><light_purple>Prestige",
            "<!italic><gray>View the <green>top prestige<gray> players.",
            category.equals("prestige")));

        inv.setItem(SLOT_BLOCKS,   makeCategoryItem(Material.DIAMOND_ORE,
            "<!italic><aqua>Blocks Mined",
            "<!italic><gray>View the <green>most blocks broken<gray>.",
            category.equals("blocks")));

        inv.setItem(SLOT_TOKENS,   makeCategoryItem(Material.EMERALD,
            "<!italic><green>Tokens",
            "<!italic><gray>View the <green>top token balance<gray> players.",
            category.equals("tokens")));

        // --- Rows 1-2: entry slots ---
        List<LeaderboardManager.LeaderboardEntry> entries = manager.getLeaderboard(category);
        String currencyLabel = categoryLabel(category);

        for (int i = 0; i < ENTRY_SLOTS.length; i++) {
            int slot = ENTRY_SLOTS[i];
            int rank = i + 1; // 1-based

            if (i < entries.size()) {
                LeaderboardManager.LeaderboardEntry entry = entries.get(i);
                inv.setItem(slot, makeEntryItem(rank, entry.name(), entry.value(), currencyLabel));
            } else {
                inv.setItem(slot, makeEmptyEntry());
            }
        }

        // --- Slot 49: Close button (functional — handleClick uses this slot) ---
        inv.setItem(SLOT_CLOSE, makeItem(Material.BARRIER,
            "<!italic><red>✗ Close",
            "<!italic><gray>Click to close this menu."));

        player.openInventory(inv);
    }

    /**
     * Handles an inventory click inside our GUI.
     * The event must already be cancelled before this is called.
     *
     * @param player the clicking player
     * @param slot   the raw slot that was clicked
     */
    public static void handleClick(Player player, int slot) {
        LeaderboardPlugin plugin = LeaderboardPlugin.getInstance();

        switch (slot) {
            case SLOT_RICHEST  -> { player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f); reopenWithCategory(player, plugin.getLeaderboardManager(), "richest");  }
            case SLOT_PRESTIGE -> { player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f); reopenWithCategory(player, plugin.getLeaderboardManager(), "prestige"); }
            case SLOT_BLOCKS   -> { player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f); reopenWithCategory(player, plugin.getLeaderboardManager(), "blocks");   }
            case SLOT_TOKENS   -> { player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f); reopenWithCategory(player, plugin.getLeaderboardManager(), "tokens");   }
            case SLOT_CLOSE    -> player.closeInventory();
            default            -> { /* do nothing — event already cancelled */ }
        }
    }

    /**
     * Returns {@code true} if the given inventory title belongs to this GUI.
     */
    public static boolean isOurInventory(Component title) {
        return TITLE.equals(title);
    }

    /**
     * Removes per-player GUI state when a player quits.
     */
    public static void cleanup(UUID uuid) {
        activeCategory.remove(uuid);
    }

    /**
     * Removes all per-player GUI state (called on plugin disable).
     */
    public static void cleanupAll() {
        activeCategory.clear();
    }

    // ----------------------------------------------------------------
    // Private helpers — category switching
    // ----------------------------------------------------------------

    private static void reopenWithCategory(Player player, LeaderboardManager manager, String category) {
        String current = activeCategory.getOrDefault(player.getUniqueId(), "");
        // Reopen regardless — clicking the active tab refreshes the view too.
        // Schedule one tick later to avoid the "modified open inventory" client issue.
        Bukkit.getScheduler().runTask(LeaderboardPlugin.getInstance(),
            () -> open(player, manager, category));
    }

    // ----------------------------------------------------------------
    // Private helpers — item construction
    // ----------------------------------------------------------------

    /**
     * Creates a category selector item.
     *
     * @param material    item material
     * @param displayName MiniMessage display name (should include {@code <!italic>})
     * @param loreLine    base lore line
     * @param active      whether this category is currently selected
     */
    private static ItemStack makeCategoryItem(Material material, String displayName,
                                              String loreLine, boolean active) {
        List<String> lore = new ArrayList<>();
        lore.add(loreLine);
        if (active) {
            lore.add("<!italic><green>✓ Active");
        }
        lore.add("<!italic>");
        lore.add("<!italic><green>→ Click to view " + extractCategoryName(displayName) + " leaderboard.");
        return makeItem(material, displayName, lore.toArray(new String[0]));
    }

    /**
     * Strips MiniMessage tags from a display name to get the plain category name.
     */
    private static String extractCategoryName(String displayName) {
        // Remove <!italic> and color tags for use in the CTA line
        return displayName.replaceAll("<[^>]+>", "").trim();
    }

    /**
     * Creates an entry item for a leaderboard rank.
     *
     * @param rank          1-based rank (1 = gold, 2 = iron, 3 = copper, 4-10 = gray pane)
     * @param playerName    display name of the player
     * @param value         numeric value to display
     * @param currencyLabel label appended after the formatted value (e.g. "IGC")
     */
    private static ItemStack makeEntryItem(int rank, String playerName, long value, String currencyLabel) {
        Material material = switch (rank) {
            case 1  -> Material.GOLD_BLOCK;
            case 2  -> Material.IRON_BLOCK;
            case 3  -> Material.COPPER_INGOT;
            default -> Material.GRAY_STAINED_GLASS_PANE;
        };

        String rankColor = switch (rank) {
            case 1  -> "<gold>";
            case 2  -> "<gray>";
            case 3  -> "<#CD7F32>";
            default -> "<white>";
        };

        String displayName = "<!italic>" + rankColor + "#" + rank + " <white>" + playerName;

        return makeItem(material, displayName,
            "<!italic><aqua>✦ <gray>Rank: <white>#" + rank,
            "<!italic><aqua>✦ <gray>Value: <white>" + formatNumber(value) + " " + currencyLabel.trim(),
            "<!italic>",
            "<!italic><green>→ Click to view this player's profile.");
    }

    /**
     * Creates a placeholder item for an empty leaderboard slot.
     */
    private static ItemStack makeEmptyEntry() {
        return makeItem(Material.GRAY_STAINED_GLASS_PANE,
            "<!italic><gray>—",
            "<!italic><gray>No player in this position yet.");
    }

    /**
     * Core item-factory used by all GUI item builders.
     * Applies {@link ItemFlag#HIDE_ATTRIBUTES} and {@link ItemFlag#HIDE_ENCHANTS},
     * and sets the display name and optional lore using MiniMessage.
     */
    private static ItemStack makeItem(Material material, String displayName, String... loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta  = item.getItemMeta();

        meta.displayName(MM.deserialize(displayName));

        if (loreLines != null && loreLines.length > 0) {
            List<Component> lore = new ArrayList<>(loreLines.length);
            for (String line : loreLines) {
                lore.add(MM.deserialize(line));
            }
            meta.lore(lore);
        }

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    // ----------------------------------------------------------------
    // Private helpers — misc
    // ----------------------------------------------------------------

    /** Returns the prefix/label for a given category's value display. */
    private static String categoryLabel(String category) {
        return switch (category) {
            case "richest"  -> "$";
            case "prestige" -> "Prestige";
            case "blocks"   -> "Blocks";
            case "tokens"   -> "Tokens";
            default         -> "";
        };
    }

    /** Formats a long with thousands separators: 1234567 → "1,234,567". */
    private static String formatNumber(long value) {
        return String.format("%,d", value);
    }

    /** Returns true if the given slot is one of the entry display slots. */
    private static boolean isEntrySlot(int slot) {
        for (int s : ENTRY_SLOTS) {
            if (s == slot) return true;
        }
        return false;
    }

    /** Returns true if the given slot is a category selector slot. */
    private static boolean isCategorySlot(int slot) {
        return slot == SLOT_RICHEST || slot == SLOT_PRESTIGE
            || slot == SLOT_BLOCKS  || slot == SLOT_TOKENS;
    }
}
