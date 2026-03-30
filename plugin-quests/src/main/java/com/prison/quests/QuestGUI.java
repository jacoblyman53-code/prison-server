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
import java.util.concurrent.TimeUnit;

/**
 * QuestGUI — 54-slot inventory GUI for the /quests command.
 *
 * Layout:
 *   Row 0 (slots  0– 8): Tab row — Daily | Weekly | Milestones + filler
 *   Rows 1–4 (slots 9–44): Quest cards for the active tab (up to 4×9 = 36 slots)
 *   Row 5 (slots 45–53): Bottom bar — Close button at slot 49
 *
 * Slot constants are public so QuestPlugin can check clicks without importing
 * the full GUI class internals.
 */
public class QuestGUI {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    // ── Tab slots ──────────────────────────────────────────────────
    public static final int SLOT_TAB_DAILY     = 1;
    public static final int SLOT_TAB_WEEKLY    = 2;
    public static final int SLOT_TAB_MILESTONE = 3;

    // ── Bottom row ─────────────────────────────────────────────────
    public static final int SLOT_CLOSE = 49;

    // ── GUI dimensions ─────────────────────────────────────────────
    private static final int SIZE          = 54;
    private static final int CONTENT_START = 9;
    private static final int CONTENT_END   = 44; // inclusive
    private static final int BOTTOM_START  = 45;

    public static final String TITLE_STRING = "<gold><bold>⚑ Quests";

    private final QuestManager manager;

    public QuestGUI(QuestManager manager) {
        this.manager = manager;
    }

    // ----------------------------------------------------------------
    // Open
    // ----------------------------------------------------------------

    /**
     * Build and open the quest GUI for the player, defaulting to the DAILY tab.
     */
    public void open(Player player) {
        open(player, QuestTier.DAILY);
    }

    /**
     * Build and open the quest GUI with a specific active tab.
     */
    public void open(Player player, QuestTier activeTab) {
        Inventory inv = Bukkit.createInventory(null, SIZE, MM.deserialize(TITLE_STRING));

        fillGray(inv);         // background
        buildTabRow(inv, activeTab, player.getUniqueId());
        buildContentArea(inv, activeTab, player.getUniqueId());
        buildBottomRow(inv);

        player.openInventory(inv);
    }

    // ----------------------------------------------------------------
    // Tab Row (slots 0–8)
    // ----------------------------------------------------------------

    private void buildTabRow(Inventory inv, QuestTier activeTab, UUID uuid) {
        inv.setItem(SLOT_TAB_DAILY,     buildTabItem(QuestTier.DAILY,     activeTab, uuid));
        inv.setItem(SLOT_TAB_WEEKLY,    buildTabItem(QuestTier.WEEKLY,    activeTab, uuid));
        inv.setItem(SLOT_TAB_MILESTONE, buildTabItem(QuestTier.MILESTONE, activeTab, uuid));
    }

    private ItemStack buildTabItem(QuestTier tier, QuestTier activeTab, UUID uuid) {
        boolean active = tier == activeTab;

        Material mat;
        String nameColor;
        String tierLabel;

        switch (tier) {
            case DAILY -> {
                mat       = Material.PAPER;
                nameColor = "<yellow>";
                tierLabel = "Daily";
            }
            case WEEKLY -> {
                mat       = Material.BOOK;
                nameColor = "<green>";
                tierLabel = "Weekly";
            }
            default -> {  // MILESTONE
                mat       = Material.NETHER_STAR;
                nameColor = "<light_purple>";
                tierLabel = "Milestones";
            }
        }

        ItemStack item = new ItemStack(active ? Material.ENCHANTED_BOOK : mat);
        ItemMeta meta  = item.getItemMeta();

        String prefix = active ? "► " : "";
        meta.displayName(MM.deserialize("<!italic>" + nameColor + "<bold>" + prefix + tierLabel));

        List<Component> lore = new ArrayList<>();
        if (active) {
            lore.add(MM.deserialize("<!italic><yellow>Currently viewing"));
        } else {
            lore.add(MM.deserialize("<!italic><gray>Click to view " + tierLabel.toLowerCase() + " quests"));
        }

        // Add reset countdown for resettable tiers
        if (tier.isResettable()) {
            // Find the smallest seconds-until-reset across this tier's quests for this player
            List<QuestDefinition> defs = manager.getDefinitionsByTier(tier);
            long minReset = Long.MAX_VALUE;
            for (QuestDefinition def : defs) {
                long secs = manager.secondsUntilReset(uuid, def.getId());
                if (secs < minReset) minReset = secs;
            }
            if (minReset == Long.MAX_VALUE || minReset == 0) {
                lore.add(MM.deserialize("<!italic><gray>Resets: <red>Now"));
            } else {
                lore.add(MM.deserialize("<!italic><gray>Resets in: <white>" + formatDuration(minReset)));
            }
        } else {
            lore.add(MM.deserialize("<!italic><gray>Permanent challenges"));
        }

        // Quest count summary
        int total     = manager.getDefinitionsByTier(tier).size();
        int completed = countCompleted(uuid, tier);
        lore.add(MM.deserialize("<!italic><gray>Progress: <white>" + completed + "/" + total + " complete"));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private int countCompleted(UUID uuid, QuestTier tier) {
        int count = 0;
        for (QuestDefinition def : manager.getDefinitionsByTier(tier)) {
            PlayerQuestData data = manager.getQuestData(uuid, def.getId());
            if (data != null && data.isCompleted()) count++;
        }
        return count;
    }

    // ----------------------------------------------------------------
    // Content Area (slots 9–44)
    // ----------------------------------------------------------------

    private void buildContentArea(Inventory inv, QuestTier tier, UUID uuid) {
        List<QuestDefinition> defs = manager.getDefinitionsByTier(tier);
        int slot = CONTENT_START;

        for (QuestDefinition def : defs) {
            if (slot > CONTENT_END) break;  // more quests than visible slots
            PlayerQuestData data = manager.getQuestData(uuid, def.getId());
            inv.setItem(slot, buildQuestItem(def, data, uuid));
            slot++;
        }
        // Remaining content slots stay as gray glass (filled in fillGray)
    }

    private ItemStack buildQuestItem(QuestDefinition def, PlayerQuestData data, UUID uuid) {
        boolean completed = data != null && data.isCompleted();
        long    progress  = data != null ? data.getProgress() : 0L;
        long    goal      = def.getGoal();

        Material mat;
        String   titleColor;

        if (completed) {
            mat        = Material.LIME_STAINED_GLASS_PANE;
            titleColor = "<green>";
        } else {
            mat        = def.getIcon();
            titleColor = "<yellow>";
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta  meta = item.getItemMeta();

        // Display name
        String checkmark = completed ? "✔ " : "";
        meta.displayName(MM.deserialize("<!italic>" + titleColor + "<bold>" + checkmark + def.getTitle()));

        // Lore
        List<Component> lore = new ArrayList<>();

        // Description
        if (!def.getDescription().isEmpty()) {
            lore.add(MM.deserialize("<!italic><gray>" + def.getDescription()));
        }

        lore.add(Component.empty());

        // Progress bar
        if (!completed) {
            long capped = Math.min(progress, goal);
            lore.add(MM.deserialize("<!italic>" + buildProgressBar(capped, goal)));
        } else {
            lore.add(MM.deserialize("<!italic><green>✔ Completed!"));
        }

        lore.add(Component.empty());

        // Rewards
        lore.add(MM.deserialize("<!italic><gold>Rewards:"));
        if (def.hasIgcReward()) {
            lore.add(MM.deserialize("<!italic>  <gold>" + QuestManager.formatAmount(def.getIgcReward()) + " IGC"));
        }
        if (def.hasTokenReward()) {
            lore.add(MM.deserialize("<!italic>  <aqua>" + QuestManager.formatAmount(def.getTokenReward()) + " Tokens"));
        }
        if (!def.hasIgcReward() && !def.hasTokenReward()) {
            lore.add(MM.deserialize("<!italic>  <gray>No rewards configured"));
        }

        // Reset info (only for resettable tiers)
        if (def.getTier().isResettable() && !completed) {
            long secsLeft = manager.secondsUntilReset(uuid, def.getId());
            if (secsLeft > 0) {
                lore.add(Component.empty());
                lore.add(MM.deserialize("<!italic><gray>Resets in: <white>" + formatDuration(secsLeft)));
            }
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Build a 10-character ASCII progress bar.
     * Example: {@code <gray>[<green>████░░░░░░<gray>] <white>400/1000}
     */
    private String buildProgressBar(long current, long goal) {
        int bars  = 10;
        int filled = (goal > 0) ? (int) Math.min(bars, (current * bars) / goal) : 0;
        int empty  = bars - filled;

        StringBuilder sb = new StringBuilder("<gray>[");
        sb.append("<green>");
        sb.append("█".repeat(filled));
        sb.append("<gray>");
        sb.append("░".repeat(empty));
        sb.append("<gray>] <white>");
        sb.append(QuestManager.formatAmount(current)).append("/").append(QuestManager.formatAmount(goal));
        return sb.toString();
    }

    // ----------------------------------------------------------------
    // Bottom Row (slots 45–53)
    // ----------------------------------------------------------------

    private void buildBottomRow(Inventory inv) {
        // Close button
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta  meta  = close.getItemMeta();
        meta.displayName(MM.deserialize("<!italic><red><bold>Close"));
        meta.lore(List.of(MM.deserialize("<!italic><gray>Click to close the quest menu")));
        close.setItemMeta(meta);
        inv.setItem(SLOT_CLOSE, close);
    }

    // ----------------------------------------------------------------
    // Background Fill
    // ----------------------------------------------------------------

    /**
     * Fill all 54 slots with a gray glass pane filler.
     * Individual slots are then overwritten by their real items.
     */
    private void fillGray(Inventory inv) {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta  meta   = filler.getItemMeta();
        meta.displayName(MM.deserialize("<!italic> "));
        filler.setItemMeta(meta);
        for (int i = 0; i < SIZE; i++) {
            inv.setItem(i, filler);
        }
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    /**
     * Format a seconds value into a human-readable string like "23h 14m" or "6d 2h".
     */
    static String formatDuration(long seconds) {
        if (seconds <= 0) return "0s";
        long days    = TimeUnit.SECONDS.toDays(seconds);
        long hours   = TimeUnit.SECONDS.toHours(seconds)   % 24;
        long minutes = TimeUnit.SECONDS.toMinutes(seconds) % 60;
        long secs    = seconds % 60;

        if (days > 0)    return days + "d " + hours + "h";
        if (hours > 0)   return hours + "h " + minutes + "m";
        if (minutes > 0) return minutes + "m " + secs + "s";
        return secs + "s";
    }
}
