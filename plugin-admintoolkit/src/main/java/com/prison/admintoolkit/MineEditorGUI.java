package com.prison.admintoolkit;

import com.prison.mines.MineData;
import com.prison.mines.MinesAPI;
import com.prison.mines.MinesPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MineEditorGUI — three-view GUI for editing mine compositions.
 *
 * Views:
 *   1. Mine list      — select which mine to edit
 *   2. Composition editor — edit block percentages for a mine
 *   3. Material picker    — pick a new block type to add
 */
public class MineEditorGUI {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private static final Component TITLE_LIST   = MM.deserialize("MINE EDITOR");
    private static final Component TITLE_PICKER = MM.deserialize("ADD BLOCK");

    // Composition title is dynamic: "Mine: {id} — Composition"
    // We identify it with a plain-text prefix check.

    // ----------------------------------------------------------------
    // State
    // ----------------------------------------------------------------

    /** Mutable state for a player's active mine edit session. */
    static class MineEditorState {
        private final String mineId;
        private final LinkedHashMap<Material, Double> pendingComposition;

        MineEditorState(String mineId, LinkedHashMap<Material, Double> pendingComposition) {
            this.mineId = mineId;
            this.pendingComposition = pendingComposition;
        }

        String mineId() { return mineId; }
        LinkedHashMap<Material, Double> pendingComposition() { return pendingComposition; }
    }

    private static final Map<UUID, MineEditorState> states = new ConcurrentHashMap<>();

    // Track which material is in each slot for the composition editor
    // (slot index → Material). Rebuilt on each renderCompositionEditor call.
    private static final Map<UUID, Map<Integer, Material>> slotMaterials = new ConcurrentHashMap<>();

    // ----------------------------------------------------------------
    // Common block materials for the picker
    // ----------------------------------------------------------------

    private static final Material[] PICKER_MATERIALS = {
        Material.STONE, Material.COBBLESTONE, Material.GRAVEL, Material.SAND,
        Material.GRANITE, Material.DIORITE, Material.ANDESITE,
        Material.COAL_ORE, Material.COPPER_ORE, Material.IRON_ORE,
        Material.GOLD_ORE, Material.REDSTONE_ORE, Material.LAPIS_ORE,
        Material.DIAMOND_ORE, Material.EMERALD_ORE,
        Material.DEEPSLATE_COAL_ORE, Material.DEEPSLATE_COPPER_ORE,
        Material.DEEPSLATE_IRON_ORE, Material.DEEPSLATE_GOLD_ORE,
        Material.DEEPSLATE_REDSTONE_ORE, Material.DEEPSLATE_LAPIS_ORE,
        Material.DEEPSLATE_DIAMOND_ORE, Material.DEEPSLATE_EMERALD_ORE,
        Material.NETHER_QUARTZ_ORE, Material.NETHER_GOLD_ORE,
        Material.ANCIENT_DEBRIS, Material.OBSIDIAN,
        Material.NETHERRACK, Material.BLACKSTONE,
        Material.SANDSTONE, Material.DIRT
    };

    // ----------------------------------------------------------------
    // View 1 — Mine List
    // ----------------------------------------------------------------

    public static void openMineList(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_LIST);

        List<MineData> mines = new ArrayList<>(MinesAPI.getInstance().getAllMines().values());
        mines.sort(Comparator.comparing(MineData::id));

        for (int i = 0; i < Math.min(mines.size(), 45); i++) {
            MineData mine = mines.get(i);
            int blockCount = mine.totalBlocks();
            int compEntries = mine.composition().size();

            ItemStack item = AdminPanel.makeItem(Material.DIAMOND_PICKAXE,
                "<aqua>" + mine.display(),
                "<aqua>✦ <gray>World: <yellow>" + mine.world(),
                "<aqua>✦ <gray>Blocks: <white>" + blockCount,
                "<aqua>✦ <gray>Composition entries: <white>" + compEntries,
                "",
                "<green>→ <green>Click to <green><underlined>edit</underlined> this mine!");
            inv.setItem(i, item);
        }

        inv.setItem(49, AdminPanel.makeItem(Material.BARRIER, "<red>✗ Close", "<gray>Click to close this menu."));

        player.openInventory(inv);
    }

    // ----------------------------------------------------------------
    // View 2 — Composition Editor
    // ----------------------------------------------------------------

    public static void openMineEditor(Player player, String mineId) {
        MineData mine = MinesAPI.getInstance().getMine(mineId);
        if (mine == null) {
            player.sendMessage(MM.deserialize("<red>Mine '" + mineId + "' not found or not enabled."));
            return;
        }

        MineEditorState state = new MineEditorState(mineId, new LinkedHashMap<>(mine.composition()));
        states.put(player.getUniqueId(), state);
        renderCompositionEditor(player, state);
    }

    static void renderCompositionEditor(Player player, MineEditorState state) {
        Component title = MM.deserialize("Mine: " + state.mineId() + " — Composition");
        Inventory inv = Bukkit.createInventory(null, 54, title);

        // Composition slots (slots 10–43, skipping borders at col 0 and 8)
        // We use inner slots: 10-16, 19-25, 28-34, 37-43 (4 rows × 7 cols = 28 max)
        // But spec says slots 9-35, so let's use 9+1 to 35 skipping borders.
        // Use explicit inner slots:
        int[] innerSlots = buildInnerSlots();
        Map<Integer, Material> slotMap = new LinkedHashMap<>();

        List<Map.Entry<Material, Double>> entries = new ArrayList<>(state.pendingComposition().entrySet());
        int max = Math.min(entries.size(), innerSlots.length);

        for (int i = 0; i < max; i++) {
            Material mat = entries.get(i).getKey();
            double pct = entries.get(i).getValue();
            int slotIdx = innerSlots[i];

            String matName = formatMaterialName(mat);
            ItemStack item = AdminPanel.makeItem(mat,
                "<aqua>" + matName,
                "<aqua>✦ <gray>Percentage: <white>" + pct + "%",
                "",
                "<green>→ <green>Click to <green><underlined>change</underlined> percentage.",
                "<green>→ <green>Shift-click to <green><underlined>remove</underlined> this block.");
            inv.setItem(slotIdx, item);
            slotMap.put(slotIdx, mat);
        }

        if (entries.size() > innerSlots.length) {
            player.sendMessage(MM.deserialize("<yellow>Warning: only showing first " + innerSlots.length + " of " + entries.size() + " composition entries."));
        }

        slotMaterials.put(player.getUniqueId(), slotMap);

        // Calculate total
        double total = state.pendingComposition().values().stream().mapToDouble(Double::doubleValue).sum();
        String totalLine = Math.abs(total - 100.0) < 0.01
            ? "<green>✓ Total: 100%"
            : "<red>✗ Warning: total is " + String.format("%.1f", total) + "%, should be 100%";

        // Control row (slot 45-53)
        inv.setItem(45, AdminPanel.makeItem(Material.ARROW,
            "<red>← Back to MINE EDITOR",
            "<gray>Return to Mine Editor.",
            "",
            "<green>→ <green>Click to <green><underlined>go back</underlined>."));
        inv.setItem(46, AdminPanel.makeItem(Material.LIME_DYE,
            "<aqua>Add Block",
            "<gray>Add a <green>new block type<gray> to this mine.",
            "",
            "<green>→ <green>Click to <green><underlined>add</underlined> a block!"));
        inv.setItem(47, AdminPanel.makeItem(Material.ORANGE_DYE,
            "<aqua>✦ Current Total",
            totalLine));
        inv.setItem(48, AdminPanel.makeItem(Material.EMERALD,
            "<aqua>Save & Reset Mine",
            "<gray>Saves the <green>composition<gray> and immediately",
            "<gray>resets the mine with new blocks.",
            "",
            "<red>✗ This will clear the mine!",
            "",
            "<green>→ <green>Click to <green><underlined>save</underlined> and reset!"));
        inv.setItem(49, AdminPanel.makeItem(Material.BARRIER,
            "<red>✗ Discard Changes",
            "<gray>Discard all pending changes",
            "<gray>and return to mine list.",
            "",
            "<green>→ <green>Click to <green><underlined>discard</underlined> changes."));

        player.openInventory(inv);
    }

    // ----------------------------------------------------------------
    // View 3 — Material Picker
    // ----------------------------------------------------------------

    static void openMaterialPicker(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_PICKER);

        for (int i = 0; i < Math.min(PICKER_MATERIALS.length, 45); i++) {
            Material mat = PICKER_MATERIALS[i];
            String name = formatMaterialName(mat);
            inv.setItem(i, AdminPanel.makeItem(mat,
                "<aqua>" + name,
                "",
                "<green>→ <green>Click to <green><underlined>select</underlined> this block!"));
        }

        inv.setItem(45, AdminPanel.makeItem(Material.ARROW,
            "<red>← Back to Editor",
            "<gray>Return to Mine Editor.",
            "",
            "<green>→ <green>Click to <green><underlined>go back</underlined>."));

        player.openInventory(inv);
    }

    // ----------------------------------------------------------------
    // Title matching
    // ----------------------------------------------------------------

    public static boolean isTitle(Component title) {
        String plain = PlainTextComponentSerializer.plainText().serialize(title);
        return plain.startsWith("Mine Editor") || plain.startsWith("Mine: ") || plain.startsWith("Add Block") || plain.equals("MINE EDITOR") || plain.equals("ADD BLOCK");
    }

    // ----------------------------------------------------------------
    // Click handling
    // ----------------------------------------------------------------

    public static void handleClick(Player player, int slot, ClickType click) {
        var viewTitle = player.getOpenInventory().title();
        String plain = PlainTextComponentSerializer.plainText().serialize(viewTitle);

        if (plain.startsWith("Mine Editor") || plain.equals("MINE EDITOR")) {
            // Mine list view
            handleMineListClick(player, slot);
        } else if (plain.startsWith("Mine: ")) {
            // Composition editor view
            handleCompositionEditorClick(player, slot, click);
        } else if (plain.startsWith("Add Block") || plain.equals("ADD BLOCK")) {
            // Material picker view
            handleMaterialPickerClick(player, slot);
        }
    }

    private static void handleMineListClick(Player player, int slot) {
        if (slot == 49) {
            player.closeInventory();
            return;
        }
        // Any non-empty slot in slots 0-44 = open that mine's editor
        if (slot >= 0 && slot < 45) {
            Inventory inv = player.getOpenInventory().getTopInventory();
            ItemStack item = inv.getItem(slot);
            if (item == null || item.getType() == Material.AIR) return;

            // Get mine at this index
            List<MineData> mines = new ArrayList<>(MinesAPI.getInstance().getAllMines().values());
            mines.sort(Comparator.comparing(MineData::id));
            if (slot < mines.size()) {
                openMineEditor(player, mines.get(slot).id());
            }
        }
    }

    private static void handleCompositionEditorClick(Player player, int slot, ClickType click) {
        MineEditorState state = states.get(player.getUniqueId());
        if (state == null) return;

        switch (slot) {
            case 45 -> {
                // Back
                states.remove(player.getUniqueId());
                slotMaterials.remove(player.getUniqueId());
                openMineList(player);
                return;
            }
            case 46 -> {
                // Add block — open picker
                openMaterialPicker(player);
                return;
            }
            case 48 -> {
                // Save
                saveAndReset(player);
                return;
            }
            case 49 -> {
                // Discard
                states.remove(player.getUniqueId());
                slotMaterials.remove(player.getUniqueId());
                openMineList(player);
                return;
            }
        }

        // Check if slot is a composition slot
        Map<Integer, Material> slotMap = slotMaterials.get(player.getUniqueId());
        if (slotMap == null || !slotMap.containsKey(slot)) return;

        Material mat = slotMap.get(slot);

        if (click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT) {
            // Remove this material
            state.pendingComposition().remove(mat);
            renderCompositionEditor(player, state);
        } else {
            // Open anvil to change percentage
            double currentPct = state.pendingComposition().getOrDefault(mat, 0.0);
            AnvilInputGUI.open(player, String.valueOf(currentPct), text -> {
                try {
                    double newPct = Double.parseDouble(text.trim());
                    if (newPct <= 0) {
                        state.pendingComposition().remove(mat);
                    } else {
                        state.pendingComposition().put(mat, newPct);
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage(MM.deserialize("<red>Invalid number: " + text));
                }
                Bukkit.getScheduler().runTask(AdminToolkitPlugin.getInstance(),
                    () -> renderCompositionEditor(player, state));
            });
        }
    }

    private static void handleMaterialPickerClick(Player player, int slot) {
        if (slot == 45) {
            // Back to editor
            MineEditorState state = states.get(player.getUniqueId());
            if (state != null) {
                renderCompositionEditor(player, state);
            } else {
                openMineList(player);
            }
            return;
        }

        if (slot < 0 || slot >= PICKER_MATERIALS.length) return;

        Material mat = PICKER_MATERIALS[slot];
        MineEditorState state = states.get(player.getUniqueId());
        if (state == null) return;

        // Open anvil to enter percentage
        AnvilInputGUI.open(player, "0", text -> {
            try {
                double pct = Double.parseDouble(text.trim());
                if (pct > 0) {
                    state.pendingComposition().put(mat, pct);
                }
            } catch (NumberFormatException e) {
                player.sendMessage(MM.deserialize("<red>Invalid number: " + text));
            }
            Bukkit.getScheduler().runTask(AdminToolkitPlugin.getInstance(),
                () -> renderCompositionEditor(player, state));
        });
    }

    // ----------------------------------------------------------------
    // Save & Reset
    // ----------------------------------------------------------------

    private static void saveAndReset(Player player) {
        MineEditorState state = states.get(player.getUniqueId());
        if (state == null) return;

        double total = state.pendingComposition().values().stream().mapToDouble(Double::doubleValue).sum();
        if (Math.abs(total - 100.0) > 0.01) {
            player.sendMessage(MM.deserialize("<yellow>Warning: composition totals " + String.format("%.1f", total) + "% (not 100%). Saving anyway."));
        }

        MinesPlugin minesPlugin = (MinesPlugin) Bukkit.getPluginManager().getPlugin("PrisonMines");
        if (minesPlugin == null) {
            player.sendMessage(MM.deserialize("<red>Could not find PrisonMines plugin!"));
            return;
        }

        String mineId = state.mineId();
        minesPlugin.adminSetComposition(mineId, new LinkedHashMap<>(state.pendingComposition()));

        states.remove(player.getUniqueId());
        slotMaterials.remove(player.getUniqueId());
        player.closeInventory();
        player.sendMessage(MM.deserialize("<green>Mine " + mineId + " composition saved and reset triggered."));
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    /** Returns the inner slot indices for the composition editor (skipping borders). */
    private static int[] buildInnerSlots() {
        List<Integer> slots = new ArrayList<>();
        // Rows 1-4 (indices 1-4), inner columns 1-7
        for (int row = 1; row <= 4; row++) {
            for (int col = 1; col <= 7; col++) {
                slots.add(row * 9 + col);
            }
        }
        return slots.stream().mapToInt(Integer::intValue).toArray();
    }

    static String formatMaterialName(Material mat) {
        String[] words = mat.name().toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return sb.toString();
    }

    public static void cleanup(UUID uuid) {
        states.remove(uuid);
        slotMaterials.remove(uuid);
    }
}
