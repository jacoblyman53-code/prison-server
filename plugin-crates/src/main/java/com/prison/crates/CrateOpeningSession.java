package com.prison.crates;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * CrateOpeningSession — manages the animated chest-spin GUI for a single player.
 *
 * Layout (27 slots, 3 rows):
 *   Row 0 (0-8):   Decorative border — gray glass panes
 *   Row 1 (9-17):  The spinning reel — 9 reward icons cycle left-to-right
 *   Row 2 (18-26): Decorative border — gray glass panes, center shows crate name
 *
 * The center slot of the reel (slot 13) is framed by lime glass panes at slots 12 and 14.
 * When the spin settles, the winner is placed at slot 13 and highlighted.
 */
public class CrateOpeningSession {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    /** Tracks all players currently in an opening animation. */
    static final Map<UUID, CrateOpeningSession> activeSessions = new ConcurrentHashMap<>();

    // ----------------------------------------------------------------
    // Config
    // ----------------------------------------------------------------

    /** How many extra display items are shown in the reel before/after the winner. */
    private static final int REEL_SIZE = 9;
    private static final int CENTER_SLOT = 13; // slot 13 in middle row

    // ----------------------------------------------------------------
    // State
    // ----------------------------------------------------------------

    private final Player player;
    private final CrateTier tier;
    private final CrateReward winner;
    private final Inventory inv;
    private final Consumer<CrateReward> onComplete;
    private final CratePlugin plugin;

    /** Cyclic list of display items for the reel. */
    private final List<ItemStack> reelItems;
    /** Current offset into reelItems for the leftmost reel slot. */
    private int reelOffset = 0;

    /** Tick intervals for each animation step — slowing down. */
    private final int[] spinSchedule;
    private int spinStep = 0;
    private int totalTicksElapsed = 0;
    private boolean finished = false;

    private BukkitRunnable task;

    CrateOpeningSession(Player player, CrateTier tier, CrateReward winner,
                        Consumer<CrateReward> onComplete, CratePlugin plugin) {
        this.player     = player;
        this.tier       = tier;
        this.winner     = winner;
        this.onComplete = onComplete;
        this.plugin     = plugin;

        // Build inventory
        Component title = MM.deserialize(tier.keyColor() + tier.displayName() + " Opening...");
        this.inv = Bukkit.createInventory(null, 27, title);

        // Build reel items: winner + shuffled pool (minimum REEL_SIZE * 2 items)
        this.reelItems = buildReelItems(tier, winner);

        // Build spin schedule: fast early, slow late
        // e.g. update every 2 ticks × 10 steps, then every 4 × 5, then every 7 × 3, then 10 × 2
        this.spinSchedule = buildSpinSchedule(
            plugin.getConfig().getInt("animation-start-speed", 2),
            plugin.getConfig().getInt("animation-end-speed", 8),
            plugin.getConfig().getInt("animation-spin-ticks", 60)
        );

        populate();
    }

    // ----------------------------------------------------------------
    // Start
    // ----------------------------------------------------------------

    void start() {
        activeSessions.put(player.getUniqueId(), this);
        player.openInventory(inv);

        task = new BukkitRunnable() {
            @Override
            public void run() {
                if (finished || !player.isOnline()) {
                    cancel();
                    activeSessions.remove(player.getUniqueId());
                    return;
                }
                tick();
            }
        };
        task.runTaskTimer(plugin, 1L, 1L);
    }

    // ----------------------------------------------------------------
    // Animation
    // ----------------------------------------------------------------

    private void tick() {
        if (spinStep >= spinSchedule.length) {
            // All spin steps done — reveal winner
            revealWinner();
            return;
        }

        int nextTrigger = spinSchedule[spinStep];
        totalTicksElapsed++;

        if (totalTicksElapsed >= nextTrigger) {
            totalTicksElapsed = 0;
            spinStep++;
            advanceReel();
        }
    }

    private void advanceReel() {
        reelOffset = (reelOffset + 1) % reelItems.size();
        drawReel();
        // Click sound
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f + (spinStep * 0.02f));
    }

    private void revealWinner() {
        if (finished) return;
        finished = true;
        task.cancel();

        // Draw the winner at center slot
        inv.setItem(CENTER_SLOT, buildWinnerDisplay(winner));
        // Frame the center slot with lime glass panes
        inv.setItem(12, makePane(Material.LIME_STAINED_GLASS_PANE, " "));
        inv.setItem(14, makePane(Material.LIME_STAINED_GLASS_PANE, " "));

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);

        // Close and deliver after reveal-ticks
        int revealTicks = plugin.getConfig().getInt("animation-reveal-ticks", 60);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            activeSessions.remove(player.getUniqueId());
            if (player.isOnline()) player.closeInventory();
            onComplete.accept(winner);
        }, revealTicks);
    }

    // ----------------------------------------------------------------
    // GUI population
    // ----------------------------------------------------------------

    private void populate() {
        ItemStack gray = makePane(Material.GRAY_STAINED_GLASS_PANE, " ");
        // Top row
        for (int i = 0; i < 9; i++) inv.setItem(i, gray);
        // Bottom row
        for (int i = 18; i < 27; i++) inv.setItem(i, gray);
        // Bottom center: crate name
        inv.setItem(22, makeCrateDisplay());
        // Frame slots beside center
        inv.setItem(12, makePane(Material.GRAY_STAINED_GLASS_PANE, " "));
        inv.setItem(14, makePane(Material.GRAY_STAINED_GLASS_PANE, " "));
        // Draw reel
        drawReel();
    }

    private void drawReel() {
        for (int i = 0; i < REEL_SIZE; i++) {
            int slot = 9 + i;
            ItemStack item = reelItems.get((reelOffset + i) % reelItems.size());
            inv.setItem(slot, item);
        }
    }

    private ItemStack buildWinnerDisplay(CrateReward reward) {
        ItemStack display = reward.buildDisplayItem();
        ItemMeta meta = display.getItemMeta();
        if (meta != null) {
            List<Component> lore = new ArrayList<>(meta.lore() != null ? meta.lore() : List.of());
            lore.add(0, MM.deserialize("<green><bold>YOU WON THIS!"));
            meta.lore(lore);
            display.setItemMeta(meta);
        }
        return display;
    }

    private ItemStack makeCrateDisplay() {
        ItemStack item = new ItemStack(tier.keyMaterial());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(MM.deserialize(tier.keyColor() + "<bold>" + tier.displayName()));
        item.setItemMeta(meta);
        return item;
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private static List<ItemStack> buildReelItems(CrateTier tier, CrateReward winner) {
        List<CrateReward> pool = tier.rewards();
        List<ItemStack> items = new ArrayList<>();

        // Add every reward from pool multiple times to ensure good variety
        int repeats = Math.max(2, (REEL_SIZE * 3) / Math.max(1, pool.size()));
        for (int r = 0; r < repeats; r++) {
            for (CrateReward reward : pool) {
                items.add(reward.buildDisplayItem());
            }
        }

        // Ensure we have at least REEL_SIZE * 2 items
        while (items.size() < REEL_SIZE * 2) {
            items.add(winner.buildDisplayItem());
        }

        Collections.shuffle(items);

        // Force the winner into the position that will land at CENTER_SLOT
        // After all spin steps the reel offset will be spinSchedule.length.
        // The center slot (index 4 in the 9-slot reel) will show reelItems[(offset + 4) % size].
        // We don't need to pre-plant since revealWinner() overrides slot 13 directly.

        return items;
    }

    private static int[] buildSpinSchedule(int startSpeed, int endSpeed, int totalSpinTicks) {
        // Produce a schedule of cumulative tick thresholds.
        // startSpeed = ticks between early frames, endSpeed = ticks between late frames.
        // We linearly interpolate across totalSpinTicks' worth of spin steps.
        List<Integer> schedule = new ArrayList<>();
        int elapsed = 0;
        int step = 0;
        int totalSteps = (totalSpinTicks / startSpeed) + 1;
        while (elapsed < totalSpinTicks) {
            float t = (float) step / totalSteps;
            int interval = (int) (startSpeed + t * (endSpeed - startSpeed));
            interval = Math.max(startSpeed, Math.min(endSpeed, interval));
            elapsed += interval;
            schedule.add(interval);
            step++;
        }
        return schedule.stream().mapToInt(Integer::intValue).toArray();
    }

    static ItemStack makePane(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(MiniMessage.miniMessage().deserialize(name));
        item.setItemMeta(meta);
        return item;
    }

    // ----------------------------------------------------------------
    // Cancel (e.g., player disconnects)
    // ----------------------------------------------------------------

    void cancel(boolean deliver) {
        if (finished) return;
        finished = true;
        if (task != null) task.cancel();
        activeSessions.remove(player.getUniqueId());
        if (deliver) onComplete.accept(winner);
    }

    /** True if this player is currently in an opening animation. */
    static boolean isOpening(UUID uuid) {
        return activeSessions.containsKey(uuid);
    }
}
