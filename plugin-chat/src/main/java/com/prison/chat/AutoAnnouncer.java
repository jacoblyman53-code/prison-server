package com.prison.chat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;

/**
 * AutoAnnouncer — broadcasts rotating tip/announcement messages to all players.
 *
 * Messages are defined in config.yml under `announcer.messages`. Each message
 * is sent in sequence; after the last message the index wraps back to zero.
 * All messages are parsed with MiniMessage so full gradient/color markup works.
 *
 * The task is started via {@link #start()} and cancelled via {@link #stop()}.
 * Both methods are idempotent — safe to call multiple times.
 */
public class AutoAnnouncer {

    private final ChatPlugin plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    private BukkitTask task;
    private int index = 0;

    public AutoAnnouncer(ChatPlugin plugin) {
        this.plugin = plugin;
    }

    // ----------------------------------------------------------------
    // Lifecycle
    // ----------------------------------------------------------------

    /**
     * Schedule the repeating broadcast task.
     * If a task is already running it is cancelled and restarted (safe reload path).
     */
    public void start() {
        stop(); // Cancel any previous task

        ChatConfig cfg     = plugin.getChatConfig();
        List<String> msgs  = cfg.announcerMessages();

        if (msgs.isEmpty()) {
            plugin.getLogger().info("[AutoAnnouncer] No messages configured — announcer idle.");
            return;
        }

        long intervalTicks = cfg.announcerIntervalSeconds() * 20L;
        if (intervalTicks <= 0) intervalTicks = 20L * 120; // safety fallback: 120 s

        // Reset index to 0 on (re)start so cycles are predictable
        index = 0;

        task = Bukkit.getScheduler().runTaskTimer(plugin, this::broadcast, intervalTicks, intervalTicks);
        plugin.getLogger().info(String.format(
            "[AutoAnnouncer] Started — %d message(s), interval %ds.",
            msgs.size(), cfg.announcerIntervalSeconds()
        ));
    }

    /**
     * Cancel the broadcast task. Safe to call when no task is running.
     */
    public void stop() {
        if (task != null && !task.isCancelled()) {
            task.cancel();
            task = null;
        }
    }

    // ----------------------------------------------------------------
    // Broadcast Logic
    // ----------------------------------------------------------------

    private void broadcast() {
        List<String> msgs = plugin.getChatConfig().announcerMessages();
        if (msgs.isEmpty()) return;

        // Clamp index in case config was reloaded with fewer messages
        if (index >= msgs.size()) index = 0;

        String raw       = msgs.get(index);
        String prefix    = plugin.getChatConfig().announcerPrefix();
        String full      = prefix + raw;
        Component msg    = mm.deserialize(full);

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(msg);
        }

        // Advance and wrap
        index = (index + 1) % msgs.size();
    }
}
