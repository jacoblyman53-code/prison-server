package com.prison.menu;

import com.prison.economy.EconomyAPI;
import com.prison.kits.KitsManager;
import com.prison.kits.KitData;
import com.prison.permissions.PermissionEngine;
import com.prison.quests.QuestManager;
import com.prison.quests.QuestTier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.List;

/**
 * OnboardingListener — first-join welcome sequence and return-visit greeting.
 *
 * First join:
 *   1s  — Title "THE PHARAOH'S PRISON" + bell sound
 *   1s  — Chat message 1 (welcome)
 *   2s  — Chat message 2 (you are a Slave)
 *   3s  — Chat message 3 (mine/sell/earn) + auto-deliver starter kit
 *   4s  — Chat message 4 (/guide hint)
 *   8s  — Action bar objective pointer (persists 30s) + warp directions
 *
 * Return visit:
 *   1.5s — Greeting with rank + coin balance + daily quest reminder
 */
public class OnboardingListener implements Listener {

    private final MenuPlugin plugin;
    private static final MiniMessage MM = MiniMessage.miniMessage();

    public OnboardingListener(MenuPlugin plugin) {
        this.plugin = plugin;
    }

    // ────────────────────────────────────────────────────────────────
    // Join dispatch
    // ────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.NORMAL)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Capture before scheduling — hasPlayedBefore() becomes true once data is written
        boolean firstJoin = !player.hasPlayedBefore();

        if (firstJoin) {
            scheduleFirstJoin(player);
        } else {
            scheduleReturnVisit(player);
        }
    }

    // ────────────────────────────────────────────────────────────────
    // First-join sequence
    // ────────────────────────────────────────────────────────────────

    private void scheduleFirstJoin(Player player) {
        // 1s — title + bell sound + first chat message
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            player.showTitle(Title.title(
                MM.deserialize("<gold>THE PHARAOH'S PRISON"),
                MM.deserialize("<gray>Your sentence has begun. Prove your worth."),
                Title.Times.times(
                    Duration.ofMillis(1000),
                    Duration.ofMillis(3000),
                    Duration.ofMillis(1000)
                )
            ));
            player.playSound(player.getLocation(), Sound.BLOCK_BELL_USE, 0.8f, 0.8f);
            player.sendMessage(MM.deserialize(
                "<color:#E8C87A>⚱ Welcome to The Pharaoh's Prison, <white>" + player.getName() + "<color:#E8C87A>."));
        }, 20L);

        // 2s — second message
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            player.sendMessage(MM.deserialize("<gray>You are a Slave — the lowest of the condemned."));
        }, 40L);

        // 3s — third message + starter kit
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            player.sendMessage(MM.deserialize("<gray>Mine the tombs. Sell your haul. Earn the Pharaoh's favor."));
            deliverStarterKit(player);
        }, 60L);

        // 4s — fourth message
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            player.sendMessage(MM.deserialize(
                "<color:#E8C87A>⚱ Type <gold>/guide</gold> at any time for help."));
        }, 80L);

        // 8s — action bar + directions to Pit of Souls
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            player.sendMessage(MM.deserialize("<gray>Your first tomb awaits: <gold>/warp pit"));
            player.sendMessage(MM.deserialize("<gray>Mine blocks, then <gold>/sell all</gold> to earn Coins."));
            startObjectiveActionBar(player);
        }, 160L);
    }

    // ────────────────────────────────────────────────────────────────
    // Starter kit delivery
    // ────────────────────────────────────────────────────────────────

    private void deliverStarterKit(Player player) {
        try {
            KitsManager km = KitsManager.getInstance();
            if (km == null) return;

            KitData starter = km.getKit("starter");
            if (starter == null) return;

            KitsManager.ClaimResult result = km.claimKit(player, starter);
            if (result == KitsManager.ClaimResult.SUCCESS
                    || result == KitsManager.ClaimResult.INVENTORY_FULL) {
                giveScroll(player);
                player.sendMessage(MM.deserialize(
                    "<gold>⚱ The Pharaoh grants you a Condemned Soul's Kit."));
                player.sendMessage(MM.deserialize("<gray>Check your inventory."));
                player.playSound(player.getLocation(),
                    Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            }
        } catch (NoClassDefFoundError ignored) {
            // PrisonKits not loaded
        }
    }

    /** Give the Scroll of the Condemned paper item with lore (kit system doesn't support lore). */
    private void giveScroll(Player player) {
        ItemStack scroll = new ItemStack(Material.PAPER, 1);
        ItemMeta meta = scroll.getItemMeta();
        if (meta == null) return;

        meta.displayName(MM.deserialize("<!italic><gold>Scroll of the Condemned"));
        meta.lore(List.of(
            MM.deserialize("<!italic><color:#E8C87A>● Mine blocks in a Sacred Tomb"),
            MM.deserialize("<!italic><color:#E8C87A>● Use /sell to sell your haul"),
            MM.deserialize("<!italic><color:#E8C87A>● Use /ranks to see your path"),
            MM.deserialize("<!italic><color:#E8C87A>● Earn Coins → Rank Up → Better Tombs"),
            Component.empty(),
            MM.deserialize("<!italic><gray>The gods are watching.")
        ));
        scroll.setItemMeta(meta);
        player.getInventory().addItem(scroll);
    }

    // ────────────────────────────────────────────────────────────────
    // Action bar objective pointer
    // ────────────────────────────────────────────────────────────────

    /** Sends the objective action bar every second for 30 seconds. */
    private void startObjectiveActionBar(Player player) {
        new BukkitRunnable() {
            int iterations = 0;

            @Override
            public void run() {
                if (!player.isOnline() || iterations >= 30) {
                    cancel();
                    return;
                }
                player.sendActionBar(MM.deserialize(
                    "<gold>▸ Find the Pit of Souls and begin mining   <gray>[/warp pit]"));
                iterations++;
            }
        }.runTaskTimer(plugin, 0L, 20L); // every second × 30 = 30 seconds
    }

    // ────────────────────────────────────────────────────────────────
    // Return-visit greeting
    // ────────────────────────────────────────────────────────────────

    private void scheduleReturnVisit(Player player) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;

            String rank = getRank(player);
            String coins = getCoins(player);

            player.sendMessage(MM.deserialize(
                "<color:#E8C87A>⚱ Welcome back, <white>" + player.getName() + "<color:#E8C87A>."
                + "  <gray>Rank: <gold>[" + rank + "]"
                + "  <gray>Coins: <gold>✦ " + coins));

            sendQuestReminder(player);
        }, 30L); // 1.5s — let economy/permissions plugins load player data first
    }

    // ────────────────────────────────────────────────────────────────
    // Helpers
    // ────────────────────────────────────────────────────────────────

    private String getRank(Player player) {
        try {
            PermissionEngine pe = PermissionEngine.getInstance();
            if (pe != null) {
                String r = pe.getMineRank(player.getUniqueId());
                if (r != null) return r;
            }
        } catch (Exception ignored) {}
        return "?";
    }

    private String getCoins(Player player) {
        try {
            EconomyAPI eco = EconomyAPI.getInstance();
            if (eco != null) {
                long balance = eco.getBalance(player.getUniqueId());
                return String.format("%,d", balance);
            }
        } catch (Exception ignored) {}
        return "0";
    }

    private void sendQuestReminder(Player player) {
        try {
            QuestManager qm = QuestManager.getInstance();
            if (qm == null) return;

            boolean hasIncomplete = qm.getDefinitionsByTier(QuestTier.DAILY).stream()
                .anyMatch(def -> {
                    var data = qm.getQuestData(player.getUniqueId(), def.getId());
                    return data == null || !data.isCompleted();
                });

            if (hasIncomplete) {
                player.sendMessage(MM.deserialize(
                    "<gold>✦ New sacred quests are available. <gray>/quests"));
            }
        } catch (NoClassDefFoundError | Exception ignored) {
            // PrisonQuests not loaded or error — skip silently
        }
    }

    // ────────────────────────────────────────────────────────────────
    // /guide command output (called from MenuPlugin)
    // ────────────────────────────────────────────────────────────────

    public static void sendGuide(Player player) {
        player.sendMessage(MM.deserialize("<color:#E8C87A>⚱ ━━━━━━ THE PHARAOH'S GUIDE ━━━━━━"));
        player.sendMessage(MM.deserialize("<gray>"));
        player.sendMessage(MM.deserialize("<gold>✦ Getting Started"));
        player.sendMessage(MM.deserialize("<gray>  ◆ You start as a <gold>Slave<gray>. Mine to earn Coins."));
        player.sendMessage(MM.deserialize("<gray>  ◆ Sell blocks with <gold>/sell all<gray> after mining."));
        player.sendMessage(MM.deserialize("<gray>  ◆ Rank up with <gold>/rankup<gray> when you can afford it."));
        player.sendMessage(MM.deserialize("<gray>  ◆ Each rank unlocks better mines."));
        player.sendMessage(MM.deserialize("<gray>"));
        player.sendMessage(MM.deserialize("<gold>✦ Key Commands"));
        player.sendMessage(MM.deserialize("<gray>  ◆ <gold>/menu       <gray>— Open the main menu"));
        player.sendMessage(MM.deserialize("<gray>  ◆ <gold>/sell all   <gray>— Sell all your blocks"));
        player.sendMessage(MM.deserialize("<gray>  ◆ <gold>/rankup     <gray>— Rank up"));
        player.sendMessage(MM.deserialize("<gray>  ◆ <gold>/ranks      <gray>— See rank ladder & costs"));
        player.sendMessage(MM.deserialize("<gray>  ◆ <gold>/warp       <gray>— List warp locations"));
        player.sendMessage(MM.deserialize("<gray>  ◆ <gold>/kit        <gray>— Claim available kits"));
        player.sendMessage(MM.deserialize("<gray>  ◆ <gold>/prestige   <gray>— Ascend after reaching rank Z"));
        player.sendMessage(MM.deserialize("<gray>  ◆ <gold>/ah         <gray>— Player auction house"));
        player.sendMessage(MM.deserialize("<gray>  ◆ <gold>/quests     <gray>— Sacred Quests"));
        player.sendMessage(MM.deserialize("<gray>"));
        player.sendMessage(MM.deserialize("<gold>✦ Progression Path"));
        player.sendMessage(MM.deserialize("<gray>  ◆ Ranks A → Z   → Ascension (Prestige)"));
        player.sendMessage(MM.deserialize("<gray>  ◆ Better mines  → More Coins per block"));
        player.sendMessage(MM.deserialize("<gray>  ◆ Sell streaks  → Up to <gold>1.5×<gray> income bonus"));
        player.sendMessage(MM.deserialize("<gray>  ◆ Relics (Tokens) → Khopesh enchants"));
        player.sendMessage(MM.deserialize("<color:#E8C87A>⚱ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }
}
