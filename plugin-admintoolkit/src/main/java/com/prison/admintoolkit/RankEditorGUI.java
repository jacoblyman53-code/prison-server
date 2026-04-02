package com.prison.admintoolkit;

import com.prison.ranks.RankConfig;
import com.prison.ranks.RankConfig.RankData;
import com.prison.ranks.RankManager;
import com.prison.ranks.RankPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RankEditorGUI — two-view GUI for editing mine rank costs, displays, and prefixes.
 *
 * Views:
 *   1. Rank list   — shows all 26 ranks A-Z
 *   2. Rank editor — edit a single rank's cost, display, prefix
 */
public class RankEditorGUI {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private static final Component TITLE_LIST = MM.deserialize("RANK EDITOR");

    // ----------------------------------------------------------------
    // Mutable state for a rank edit session
    // ----------------------------------------------------------------

    static class RankEditorState {
        private final String rankLetter;
        private long pendingCost;
        private String pendingDisplay;
        private String pendingPrefix;

        RankEditorState(String rankLetter, long pendingCost, String pendingDisplay, String pendingPrefix) {
            this.rankLetter    = rankLetter;
            this.pendingCost   = pendingCost;
            this.pendingDisplay = pendingDisplay;
            this.pendingPrefix  = pendingPrefix;
        }

        String rankLetter()    { return rankLetter; }
        long pendingCost()     { return pendingCost; }
        String pendingDisplay(){ return pendingDisplay; }
        String pendingPrefix() { return pendingPrefix; }

        void setPendingCost(long cost)        { this.pendingCost = cost; }
        void setPendingDisplay(String display){ this.pendingDisplay = display; }
        void setPendingPrefix(String prefix)  { this.pendingPrefix = prefix; }
    }

    private static final Map<UUID, RankEditorState> states = new ConcurrentHashMap<>();

    // ----------------------------------------------------------------
    // View 1 — Rank List
    // ----------------------------------------------------------------

    public static void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_LIST);

        RankConfig config = RankManager.getInstance().getConfig();
        String[] order = RankConfig.RANK_ORDER;

        for (int i = 0; i < Math.min(order.length, 26); i++) {
            String letter = order[i];
            RankData data = config.getRank(letter);
            if (data == null) continue;

            ItemStack item = AdminPanel.makeItem(Material.PAPER,
                "<aqua>Rank <yellow>" + letter,
                "<aqua>✦ <gray>Letter: <yellow>" + letter,
                "<aqua>✦ <gray>Cost: <white>" + data.cost() + " <gold>IGC",
                "<aqua>✦ <gray>Prefix: <white>" + data.prefix(),
                "",
                "<green>→ <green>Click to <green><underlined>edit</underlined> this rank!");
            // Override the display with parsed MiniMessage for proper rendering
            var meta = item.getItemMeta();
            meta.displayName(MM.deserialize("<!italic>" + data.display()));
            item.setItemMeta(meta);
            inv.setItem(i, item);
        }

        inv.setItem(45, AdminPanel.makeItem(Material.BARRIER, "<red>✗ Close", "<gray>Click to close this menu."));

        player.openInventory(inv);
    }

    // ----------------------------------------------------------------
    // View 2 — Rank Editor
    // ----------------------------------------------------------------

    static void openRankEditor(Player player, String rankLetter) {
        RankData data = RankManager.getInstance().getConfig().getRank(rankLetter);
        if (data == null) {
            player.sendMessage(MM.deserialize("<red>Rank '" + rankLetter + "' not found."));
            return;
        }

        RankEditorState state = new RankEditorState(rankLetter, data.cost(), data.display(), data.prefix());
        states.put(player.getUniqueId(), state);
        renderRankEditor(player, state);
    }

    static void renderRankEditor(Player player, RankEditorState state) {
        Component title = MM.deserialize("Edit Rank: " + state.rankLetter());
        Inventory inv = Bukkit.createInventory(null, 54, title);

        // Slot 10 — Cost
        inv.setItem(10, AdminPanel.makeItem(Material.GOLD_NUGGET,
            "<aqua>Rankup Cost",
            "<aqua>✦ <gray>Current cost: <white>" + state.pendingCost() + " <gold>IGC",
            "",
            "<green>→ <green>Click to <green><underlined>change</underlined> this cost!"));

        // Slot 12 — Display Name
        inv.setItem(12, AdminPanel.makeItem(Material.NAME_TAG,
            "<aqua>Display Name",
            "<aqua>✦ <gray>Current display:",
            "<!italic>" + state.pendingDisplay(),
            "",
            "<green>→ <green>Click to <green><underlined>edit</underlined> display name! <gray>(MiniMessage format)"));

        // Slot 14 — Prefix
        inv.setItem(14, AdminPanel.makeItem(Material.PAPER,
            "<aqua>Chat Prefix",
            "<aqua>✦ <gray>Current prefix: <white>" + state.pendingPrefix(),
            "",
            "<green>→ <green>Click to <green><underlined>edit</underlined> prefix! <gray>(MiniMessage format)"));

        // Slot 22 — Save
        inv.setItem(22, AdminPanel.makeItem(Material.EMERALD,
            "<aqua>Save Changes",
            "<gray>Saves <green>cost<gray>, <green>display<gray>, and <green>prefix<gray>.",
            "",
            "<red>✗ Changes apply immediately — no restart.",
            "",
            "<green>→ <green>Click to <green><underlined>save</underlined> this rank!"));

        // Slot 45 — Back
        inv.setItem(45, AdminPanel.makeItem(Material.ARROW,
            "<red>← Back to RANK EDITOR",
            "<gray>Return to Rank Editor.",
            "",
            "<green>→ <green>Click to <green><underlined>go back</underlined>."));

        player.openInventory(inv);
    }

    // ----------------------------------------------------------------
    // Title matching
    // ----------------------------------------------------------------

    public static boolean isTitle(Component title) {
        String plain = PlainTextComponentSerializer.plainText().serialize(title);
        return plain.equals("Rank Editor — All Ranks") || plain.equals("RANK EDITOR") || plain.startsWith("Edit Rank:");
    }

    // ----------------------------------------------------------------
    // Click handling
    // ----------------------------------------------------------------

    public static void handleClick(Player player, int slot, Component viewTitle) {
        String plain = PlainTextComponentSerializer.plainText().serialize(viewTitle);

        if (plain.equals("Rank Editor — All Ranks") || plain.equals("RANK EDITOR")) {
            handleRankListClick(player, slot);
        } else if (plain.startsWith("Edit Rank:")) {
            handleRankEditorClick(player, slot);
        }
    }

    private static void handleRankListClick(Player player, int slot) {
        if (slot == 45) {
            player.closeInventory();
            return;
        }
        if (slot >= 0 && slot < 26) {
            String letter = RankConfig.RANK_ORDER[slot];
            openRankEditor(player, letter);
        }
    }

    private static void handleRankEditorClick(Player player, int slot) {
        RankEditorState state = states.get(player.getUniqueId());
        if (state == null) return;

        switch (slot) {
            case 10 -> {
                // Edit cost
                AnvilInputGUI.open(player, String.valueOf(state.pendingCost()), text -> {
                    try {
                        long newCost = Long.parseLong(text.trim());
                        state.setPendingCost(newCost);
                    } catch (NumberFormatException e) {
                        player.sendMessage(MM.deserialize("<red>Invalid number: " + text));
                    }
                    Bukkit.getScheduler().runTask(AdminToolkitPlugin.getInstance(),
                        () -> renderRankEditor(player, state));
                });
            }
            case 12 -> {
                // Edit display
                AnvilInputGUI.open(player, state.pendingDisplay(), text -> {
                    state.setPendingDisplay(text);
                    Bukkit.getScheduler().runTask(AdminToolkitPlugin.getInstance(),
                        () -> renderRankEditor(player, state));
                });
            }
            case 14 -> {
                // Edit prefix
                AnvilInputGUI.open(player, state.pendingPrefix(), text -> {
                    state.setPendingPrefix(text);
                    Bukkit.getScheduler().runTask(AdminToolkitPlugin.getInstance(),
                        () -> renderRankEditor(player, state));
                });
            }
            case 22 -> saveRank(player);
            case 45 -> {
                states.remove(player.getUniqueId());
                open(player);
            }
        }
    }

    // ----------------------------------------------------------------
    // Save
    // ----------------------------------------------------------------

    private static void saveRank(Player player) {
        RankEditorState state = states.get(player.getUniqueId());
        if (state == null) return;

        RankPlugin rankPlugin = (RankPlugin) Bukkit.getPluginManager().getPlugin("PrisonRanks");
        if (rankPlugin == null) {
            player.sendMessage(MM.deserialize("<red>Could not find PrisonRanks plugin!"));
            return;
        }

        rankPlugin.adminUpdateRank(state.rankLetter(), state.pendingCost(), state.pendingDisplay(), state.pendingPrefix());

        String letter = state.rankLetter();
        long cost = state.pendingCost();
        states.remove(player.getUniqueId());

        player.closeInventory();
        player.sendMessage(MM.deserialize("<green>Rank " + letter + " updated. Cost: " + cost + " IGC."));

        // Re-open rank list after 1 tick
        Bukkit.getScheduler().runTaskLater(AdminToolkitPlugin.getInstance(), () -> open(player), 1L);
    }

    // ----------------------------------------------------------------
    // Cleanup
    // ----------------------------------------------------------------

    public static void cleanup(UUID uuid) {
        states.remove(uuid);
    }
}
