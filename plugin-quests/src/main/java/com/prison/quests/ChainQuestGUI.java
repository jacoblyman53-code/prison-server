package com.prison.quests;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * ChainQuestGUI — 45-slot GUI showing the 5-stage chain questline.
 *
 * Layout (45 slots, 5 rows):
 *   Row 0 (0-8):   empty
 *   Row 1 (9-17):  empty, S1, empty, S2, empty, S3, empty, empty, empty
 *   Row 2 (18-26): empty (spacer row)
 *   Row 3 (27-35): empty, empty, empty, S4, empty, S5, empty, empty, empty
 *   Row 4 (36-44): empty x4, info, empty x3, empty, close
 *
 * Stage slots: 10, 12, 14, 30, 32
 * Info slot: 40   Close slot: 44
 */
public class ChainQuestGUI {

    public static final String TITLE_STRING = "Chain Quests";

    private static final int[] STAGE_SLOTS = {10, 12, 14, 30, 32};

    private static final int SLOT_INFO  = 40;
    private static final int SLOT_CLOSE = 44;

    private static final MiniMessage MM    = MiniMessage.miniMessage();

    // Stage icons
    private static final Material[] STAGE_ICONS = {
        Material.STONE,           // Stage 1 — mine
        Material.EMERALD,         // Stage 2 — sell
        Material.IRON_PICKAXE,    // Stage 3 — mine
        Material.NETHER_STAR,     // Stage 4 — rankup
        Material.DIAMOND_PICKAXE, // Stage 5 — mine
    };

    // ----------------------------------------------------------------
    // Open
    // ----------------------------------------------------------------

    public static void open(Player player) {
        player.openInventory(build(player));
    }

    public static boolean isTitle(Component title) {
        return title.equals(MM.deserialize(TITLE_STRING));
    }

    public static void handleClick(Player player, int slot) {
        if (slot == SLOT_CLOSE) {
            player.closeInventory();
        }
        // All other clicks are informational only — no action needed.
    }

    // ----------------------------------------------------------------
    // Builder
    // ----------------------------------------------------------------

    private static Inventory build(Player player) {
        UUID uuid = player.getUniqueId();
        ChainQuestManager cqm = ChainQuestManager.getInstance();

        Inventory inv = Bukkit.createInventory(null, 45, MM.deserialize(TITLE_STRING));

        // Place stage items
        for (int i = 0; i < ChainQuestManager.TOTAL_STAGES; i++) {
            int stageNum = i + 1;
            inv.setItem(STAGE_SLOTS[i], makeStageItem(uuid, stageNum, cqm));
        }

        inv.setItem(SLOT_INFO,  makeInfoItem(uuid, cqm));
        inv.setItem(SLOT_CLOSE, makeCloseItem());

        return inv;
    }

    // ----------------------------------------------------------------
    // Item builders
    // ----------------------------------------------------------------

    private static ItemStack makeStageItem(UUID uuid, int stageNum, ChainQuestManager cqm) {
        ChainQuestManager.ChainStage def = ChainQuestManager.getStageDefinition(stageNum);
        if (def == null) return new ItemStack(Material.AIR);

        int  currentStage = cqm.getCurrentStage(uuid);
        long progress     = (currentStage == stageNum) ? cqm.getCurrentProgress(uuid) : 0L;
        boolean done      = currentStage > stageNum;
        boolean active    = currentStage == stageNum;
        boolean locked    = currentStage < stageNum;

        Material icon;
        if (done) {
            icon = Material.LIME_DYE;
        } else if (locked) {
            icon = Material.GRAY_DYE;
        } else {
            icon = STAGE_ICONS[stageNum - 1];
        }

        ItemStack item = new ItemStack(icon);
        ItemMeta meta  = item.getItemMeta();

        String titleColor = done ? "<green>" : (active ? "<yellow>" : "<dark_gray>");
        String prefix     = done ? "✓ " : (active ? "▶ " : "✗ ");
        meta.displayName(MM.deserialize("<!italic>" + titleColor + "<bold>" + prefix + "Stage " + stageNum + ": " + def.title()));

        List<Component> lore = new ArrayList<>();

        // Description
        lore.add(MM.deserialize("<!italic><aqua>✦ <gray>" + def.desc()));

        lore.add(Component.empty());

        // Progress / status
        if (done) {
            lore.add(MM.deserialize("<!italic><aqua>✦ <green>✓ Completed!"));
        } else if (active) {
            long pct = (progress * 100) / def.goal();
            lore.add(MM.deserialize("<!italic><aqua>✦ <gray>Progress: <white>" + fmt(progress) + " <dark_gray>/ <white>" + fmt(def.goal())));
            lore.add(MM.deserialize("<!italic>" + progressBar(progress, def.goal()) + " <yellow>" + pct + "%"));
        } else {
            lore.add(MM.deserialize("<!italic><aqua>✦ <red>✗ <gray>Complete Stage " + (stageNum - 1) + " first"));
        }

        lore.add(Component.empty());

        // Reward
        lore.add(MM.deserialize("<!italic><aqua>✦ <gray>Reward:"));
        lore.add(MM.deserialize("<!italic><dark_aqua>  ◆ <gold>$" + fmt(def.igcReward()) + " <gray>IGC"));
        lore.add(MM.deserialize("<!italic><dark_aqua>  ◆ <gray>" + def.tokenReward() + " <green>Tokens"));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack makeInfoItem(UUID uuid, ChainQuestManager cqm) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta  = item.getItemMeta();
        meta.displayName(MM.deserialize("<!italic><aqua>✦ Chain Quest Progress"));

        List<Component> lore = new ArrayList<>();
        if (cqm.isComplete(uuid)) {
            lore.add(MM.deserialize("<!italic><aqua>✦ <green>✓ All stages complete!"));
        } else {
            int stage = cqm.getCurrentStage(uuid);
            lore.add(MM.deserialize("<!italic><aqua>✦ <gray>Current Stage: <white>" + stage + " <dark_gray>/ " + ChainQuestManager.TOTAL_STAGES));
            lore.add(MM.deserialize("<!italic><aqua>✦ <gray>Complete each stage in <green>order</green>"));
            lore.add(MM.deserialize("<!italic><gray>  to unlock the next one."));
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack makeCloseItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta  = item.getItemMeta();
        meta.displayName(MM.deserialize("<!italic><red>✗ Close"));
        meta.lore(List.of(MM.deserialize("<!italic><gray>Click to close this menu.")));
        item.setItemMeta(meta);
        return item;
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    /** 10-segment progress bar using filled/empty chars. */
    private static String progressBar(long progress, long goal) {
        int filled = (int) Math.min(10, (progress * 10) / goal);
        StringBuilder sb = new StringBuilder("<dark_gray>[");
        for (int i = 0; i < 10; i++) {
            sb.append(i < filled ? "<green>█" : "<dark_gray>█");
        }
        sb.append("<dark_gray>]");
        return sb.toString();
    }

    private static String fmt(long n) { return String.format("%,d", n); }
}
