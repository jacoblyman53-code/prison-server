package com.prison.menu;

import com.prison.menu.util.*;
import com.prison.quests.PlayerQuestData;
import com.prison.quests.QuestDefinition;
import com.prison.quests.QuestManager;
import com.prison.quests.QuestTier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class QuestsMenuGUI {

    public static final Component TITLE = MiniMessage.miniMessage().deserialize("<!italic><dark_gray>[ <aqua>Quests <dark_gray>]");
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private static final int SLOT_BACK = 45;

    // Content slots: rows 1-4, columns 1-7 (28 slots total)
    private static final int[] CONTENT_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43
    };

    public static void open(Player player) {
        player.openInventory(build(player));
        Sounds.nav(player);
    }

    public static void handleClick(Player player, int slot, MenuPlugin plugin) {
        if (slot == 8 || slot == SLOT_BACK) {
            Sounds.nav(player);
            MainMenuGUI.open(player);
        }
        // Quest items are read-only — no other click actions
    }

    // ----------------------------------------------------------------
    // Build
    // ----------------------------------------------------------------

    private static Inventory build(Player player) {
        UUID uuid = player.getUniqueId();
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        Gui.fillAll(inv);
        TopBand.apply(inv, player);
        inv.setItem(8, Gui.back());

        QuestManager qm;
        try {
            qm = QuestManager.getInstance();
        } catch (Exception e) {
            qm = null;
        }

        if (qm == null) {
            // System unavailable — show a single info item in the center
            inv.setItem(22, Gui.make(Material.BARRIER, "<gray>Quest System Unavailable",
                "<gray>Quest system not available."));
            inv.setItem(SLOT_BACK, Gui.back());
            return inv;
        }

        final QuestManager questMgr = qm;
        final UUID playerUuid = uuid;

        // ---- Summary header items ----
        try {
            List<QuestDefinition> daily = new ArrayList<>(questMgr.getDefinitionsByTier(QuestTier.DAILY));
            List<QuestDefinition> weekly = new ArrayList<>(questMgr.getDefinitionsByTier(QuestTier.WEEKLY));
            List<QuestDefinition> milestones = new ArrayList<>(questMgr.getDefinitionsByTier(QuestTier.MILESTONE));

            long dailyCompleted = daily.stream()
                .filter(d -> { PlayerQuestData pd = safeGetData(questMgr, playerUuid, d.getId()); return pd != null && pd.isCompleted(); })
                .count();
            long weeklyCompleted = weekly.stream()
                .filter(d -> { PlayerQuestData pd = safeGetData(questMgr, playerUuid, d.getId()); return pd != null && pd.isCompleted(); })
                .count();
            long milestoneCompleted = milestones.stream()
                .filter(d -> { PlayerQuestData pd = safeGetData(questMgr, playerUuid, d.getId()); return pd != null && pd.isCompleted(); })
                .count();

            // Slot 46: Daily summary
            inv.setItem(46, Gui.make(Material.SUNFLOWER, "<yellow>Daily Quests",
                "<gray>Total: <white>" + daily.size(),
                "<gray>Completed today: <green>" + dailyCompleted + "<gray>/<white>" + daily.size()));

            // Slot 47: Weekly summary
            inv.setItem(47, Gui.make(Material.GLOWSTONE, "<aqua>Weekly Quests",
                "<gray>Total: <white>" + weekly.size(),
                "<gray>Completed this week: <green>" + weeklyCompleted + "<gray>/<white>" + weekly.size()));

            // Slot 48: Milestones summary
            inv.setItem(48, Gui.make(Material.NETHER_STAR, "<light_purple>Milestones",
                "<gray>Total: <white>" + milestones.size(),
                "<gray>Completed: <green>" + milestoneCompleted + "<gray>/<white>" + milestones.size()));

            // Slot 50: Overall info
            Collection<QuestDefinition> all = questMgr.getAllDefinitions();
            long totalCompleted = all.stream()
                .filter(d -> { PlayerQuestData pd = safeGetData(questMgr, playerUuid, d.getId()); return pd != null && pd.isCompleted(); })
                .count();
            inv.setItem(50, Gui.make(Material.BOOK, "<white>Quest Overview",
                "<gray>Total quests: <white>" + all.size(),
                "<gray>Completed: <green>" + totalCompleted + "<gray>/<white>" + all.size(),
                "",
                "<dark_gray>Daily and weekly quests reset periodically.",
                "<dark_gray>Milestones are permanent achievements."));

            // ---- Quest items in content slots ----
            // Order: daily → weekly → milestones
            List<QuestDefinition> ordered = new ArrayList<>();
            ordered.addAll(daily);
            ordered.addAll(weekly);
            ordered.addAll(milestones);

            for (int i = 0; i < CONTENT_SLOTS.length && i < ordered.size(); i++) {
                QuestDefinition def = ordered.get(i);
                ItemStack item = buildQuestItem(def, uuid, qm);
                inv.setItem(CONTENT_SLOTS[i], item);
            }

        } catch (Exception e) {
            // Graceful degradation — leave header slots as filler
        }

        inv.setItem(SLOT_BACK, Gui.back());
        return inv;
    }

    // ----------------------------------------------------------------
    // Quest item builder
    // ----------------------------------------------------------------

    private static ItemStack buildQuestItem(QuestDefinition def, UUID uuid, QuestManager qm) {
        String color;
        switch (def.getTier()) {
            case WEEKLY    -> color = "<aqua>";
            case MILESTONE -> color = "<light_purple>";
            default        -> color = "<yellow>"; // DAILY
        }

        PlayerQuestData data = safeGetData(qm, uuid, def.getId());
        long progress = data != null ? data.getProgress() : 0L;
        long goal = def.getGoal();
        boolean completed = data != null && data.isCompleted();

        List<Component> lore = new ArrayList<>();

        // Description
        String desc = def.getDescription();
        if (desc != null && !desc.isBlank()) {
            lore.add(MM.deserialize("<!italic><gray>" + desc));
        }
        lore.add(Component.empty());

        // Progress bar
        String bar = progressBar(progress, goal, 10);
        lore.add(MM.deserialize("<!italic><gray>Progress: <white>" + progress + "<gray>/<white>" + goal));
        lore.add(MM.deserialize("<!italic><dark_gray>[" + bar + "<dark_gray>]"));

        // Status
        if (completed) {
            lore.add(MM.deserialize("<!italic><green>\u2714 Completed!"));
            // Reset countdown for time-limited tiers
            if (def.getTier() == QuestTier.DAILY || def.getTier() == QuestTier.WEEKLY) {
                try {
                    long secsUntilReset = qm.secondsUntilReset(uuid, def.getId());
                    lore.add(MM.deserialize("<!italic><gray>Resets in: <white>" + Fmt.duration(secsUntilReset * 1000L)));
                } catch (Exception ignored) {}
            }
        } else {
            lore.add(MM.deserialize("<!italic><yellow>In progress"));
        }

        // Rewards
        lore.add(Component.empty());
        long igcReward = def.getIgcReward();
        long tokenReward = def.getTokenReward();
        if (igcReward > 0) {
            lore.add(MM.deserialize("<!italic><gold>Reward: " + Fmt.number(igcReward) + "<gold> IGC"));
        }
        if (tokenReward > 0) {
            lore.add(MM.deserialize("<!italic><aqua>Reward: " + tokenReward + "<aqua> Tokens"));
        }

        return Gui.make(def.getIcon(), color + def.getTitle(), lore);
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private static PlayerQuestData safeGetData(QuestManager qm, UUID uuid, String questId) {
        try {
            return qm.getQuestData(uuid, questId);
        } catch (Exception e) {
            return null;
        }
    }

    private static String progressBar(long current, long goal, int length) {
        int filled = (int) Math.min(length, (current * length) / Math.max(1, goal));
        return "<green>" + "█".repeat(filled) + "<dark_gray>" + "█".repeat(length - filled);
    }
}
