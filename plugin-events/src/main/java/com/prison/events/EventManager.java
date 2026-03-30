package com.prison.events;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Core logic for loading event configs, running active events, managing
 * per-event expiry tasks, the action-bar ticker, and the daily schedule.
 */
public class EventManager {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final JavaPlugin plugin;
    private final Logger log;

    /** eventId -> EventConfig loaded from config.yml */
    private final Map<String, EventConfig> configuredEvents = new LinkedHashMap<>();

    /** eventId -> ActiveEvent (at most one active run per eventId at a time) */
    private final Map<String, ActiveEvent> activeEvents = new ConcurrentHashMap<>();

    /** eventId -> BukkitTask that will fire when the event expires */
    private final Map<String, BukkitTask> expiryTasks = new HashMap<>();

    /** eventId -> BukkitTask that fires the 5-minute warning */
    private final Map<String, BukkitTask> warnTasks = new HashMap<>();

    /** Repeating task that refreshes every player's action bar each minute */
    private BukkitTask actionBarTask;

    /** Repeating task that fires once per minute to check the daily schedule */
    private BukkitTask scheduleTask;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public EventManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /** Load configs from plugin config, start background tasks. */
    public void load() {
        loadConfiguredEvents();
        startActionBarTask();
        if (plugin.getConfig().getBoolean("schedule.enabled", false)) {
            startScheduleTask();
        }
    }

    /** Cancel all tasks and clear state on plugin disable. */
    public void shutdown() {
        cancelTask(actionBarTask);
        cancelTask(scheduleTask);
        expiryTasks.values().forEach(this::cancelTask);
        warnTasks.values().forEach(this::cancelTask);
        expiryTasks.clear();
        warnTasks.clear();
        activeEvents.clear();
    }

    // -------------------------------------------------------------------------
    // Config loading
    // -------------------------------------------------------------------------

    private void loadConfiguredEvents() {
        configuredEvents.clear();
        var eventsSection = plugin.getConfig().getConfigurationSection("events");
        if (eventsSection == null) {
            log.warning("No 'events' section found in config.yml — no events configured.");
            return;
        }

        for (String key : eventsSection.getKeys(false)) {
            var sec = eventsSection.getConfigurationSection(key);
            if (sec == null) continue;

            String typeStr = sec.getString("type", "SELL_BOOST").toUpperCase(Locale.ROOT);
            EventType type;
            try {
                type = EventType.valueOf(typeStr);
            } catch (IllegalArgumentException e) {
                log.warning("Unknown event type '" + typeStr + "' for event '" + key + "' — skipping.");
                continue;
            }

            EventConfig cfg = new EventConfig(
                    key,
                    type,
                    sec.getDouble("multiplier", 1.0),
                    sec.getInt("duration-minutes", 30),
                    sec.getString("display", key),
                    sec.getString("announcement", "")
            );
            configuredEvents.put(key, cfg);
        }
        log.info("Loaded " + configuredEvents.size() + " configured events.");
    }

    // -------------------------------------------------------------------------
    // Public event control
    // -------------------------------------------------------------------------

    /**
     * Start the event with the given config id. If the event is already running,
     * logs a warning and does nothing.
     */
    public void startEvent(String eventId) {
        EventConfig cfg = configuredEvents.get(eventId);
        if (cfg == null) {
            log.warning("Tried to start unknown event id: " + eventId);
            return;
        }
        if (activeEvents.containsKey(eventId)) {
            log.warning("Event '" + eventId + "' is already running — ignoring start request.");
            return;
        }

        String instanceId = UUID.randomUUID().toString();
        ActiveEvent active = new ActiveEvent(instanceId, cfg, System.currentTimeMillis());
        activeEvents.put(eventId, active);

        broadcastStart(active);
        scheduleTasks(active);

        log.info("Event started: " + eventId + " (x" + cfg.multiplier() + ", " + cfg.durationMinutes() + "m)");
    }

    /**
     * Force-stop the active event with the given config id.
     */
    public void stopEvent(String eventId) {
        ActiveEvent active = activeEvents.remove(eventId);
        if (active == null) return;

        cancelTask(expiryTasks.remove(eventId));
        cancelTask(warnTasks.remove(eventId));

        broadcastEnd(active);
        log.info("Event stopped: " + eventId);
    }

    /** Returns an unmodifiable view of all currently active events. */
    public Collection<ActiveEvent> getActiveEvents() {
        return Collections.unmodifiableCollection(activeEvents.values());
    }

    /** Returns all configured events (loaded from config.yml). */
    public Map<String, EventConfig> getConfiguredEvents() {
        return Collections.unmodifiableMap(configuredEvents);
    }

    // -------------------------------------------------------------------------
    // Broadcasts and titles
    // -------------------------------------------------------------------------

    private void broadcastStart(ActiveEvent active) {
        EventConfig cfg = active.config();

        // Server-wide chat broadcast
        if (!cfg.announcement().isEmpty()) {
            Bukkit.broadcast(MM.deserialize(cfg.announcement()));
        }

        // Title + subtitle to all online players
        String titleStr    = "<bold>" + cfg.displayName();
        String subtitleStr = "<yellow>Active for " + cfg.durationMinutes() + " minutes!";
        Title title = Title.title(
                MM.deserialize(titleStr),
                MM.deserialize(subtitleStr)
        );
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.showTitle(title);
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
        }
    }

    private void broadcastEnd(ActiveEvent active) {
        String msg = "<gray>⚡ " + stripMiniMessageTags(active.config().displayName()) + " event has ended.";
        Bukkit.broadcast(MM.deserialize(msg));
    }

    private void broadcastWarning(ActiveEvent active) {
        // Check the event is still active before warning
        if (!activeEvents.containsKey(active.config().id())) return;
        String msg = "<red>⚡ " + stripMiniMessageTags(active.config().displayName()) + " ends in 5 minutes!";
        Bukkit.broadcast(MM.deserialize(msg));
    }

    /**
     * Strip basic MiniMessage color/format tags for use in plain-ish messages.
     * We just re-serialize via MiniMessage to a plain string by extracting
     * the content of the parsed component — simple approach using legacy stripping.
     */
    private String stripMiniMessageTags(String miniMsg) {
        // Parse then convert back to a plain string
        Component comp = MM.deserialize(miniMsg);
        return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(comp);
    }

    // -------------------------------------------------------------------------
    // Per-event scheduler tasks
    // -------------------------------------------------------------------------

    private void scheduleTasks(ActiveEvent active) {
        String eventId = active.config().id();
        long durationTicks = (long) active.config().durationMinutes() * 20L * 60L;
        long warnTicks     = durationTicks - (5L * 20L * 60L); // 5 minutes before end

        // Expiry task
        BukkitTask expiry = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (activeEvents.containsKey(eventId)) {
                stopEvent(eventId);
            }
        }, durationTicks);
        expiryTasks.put(eventId, expiry);

        // 5-minute warning (only if duration > 5 minutes)
        if (warnTicks > 0) {
            BukkitTask warn = Bukkit.getScheduler().runTaskLater(plugin, () -> broadcastWarning(active), warnTicks);
            warnTasks.put(eventId, warn);
        }
    }

    // -------------------------------------------------------------------------
    // Action-bar ticker (runs every 60 seconds)
    // -------------------------------------------------------------------------

    private void startActionBarTask() {
        // 1200 ticks = 60 seconds; initial delay of 1200 so it shows up after first minute
        actionBarTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (activeEvents.isEmpty()) return;
            Component bar = buildActionBar();
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendActionBar(bar);
            }
        }, 1200L, 1200L);
    }

    /**
     * Build a single action-bar component showing all active events with
     * remaining time, separated by " | ".
     * Example: {@code <gold>⚡ 2x Sell Boost <gray>(14m) <dark_gray>| <aqua>⚡ Token Storm <gray>(8m)}
     */
    Component buildActionBar() {
        if (activeEvents.isEmpty()) return Component.empty();

        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (ActiveEvent active : activeEvents.values()) {
            if (!first) sb.append(" <dark_gray>| ");
            first = false;

            int mins = active.remainingMinutes();
            sb.append(active.config().displayName())
              .append(" <gray>(")
              .append(mins)
              .append("m)");
        }
        return MM.deserialize(sb.toString());
    }

    // -------------------------------------------------------------------------
    // Daily schedule checker (runs every 60 seconds)
    // -------------------------------------------------------------------------

    private void startScheduleTask() {
        // Track which schedule entries have already fired today so we don't double-fire
        Set<String> firedToday = new HashSet<>();
        String[] lastCheckedDay = {""};

        scheduleTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            var schedEntries = plugin.getConfig().getList("schedule.entries");
            if (schedEntries == null || schedEntries.isEmpty()) return;

            LocalTime now = LocalTime.now();
            String dayKey = java.time.LocalDate.now().toString();

            // Reset fired-today tracker at midnight
            if (!dayKey.equals(lastCheckedDay[0])) {
                firedToday.clear();
                lastCheckedDay[0] = dayKey;
            }

            // Current HH:MM
            String nowStr = now.format(TIME_FMT);

            for (Object raw : schedEntries) {
                if (!(raw instanceof Map<?, ?> entry)) continue;
                String timeStr  = String.valueOf(entry.get("time"));
                String eventId  = String.valueOf(entry.get("event"));

                if (timeStr == null || eventId == null) continue;

                // Parse and compare — allow a 1-minute window
                LocalTime scheduled;
                try {
                    scheduled = LocalTime.parse(timeStr, TIME_FMT);
                } catch (DateTimeParseException e) {
                    log.warning("Invalid schedule time format: '" + timeStr + "' — expected HH:MM");
                    continue;
                }

                String fireKey = dayKey + ":" + timeStr + ":" + eventId;
                if (!firedToday.contains(fireKey) && nowStr.equals(scheduled.format(TIME_FMT))) {
                    firedToday.add(fireKey);
                    log.info("Schedule firing event: " + eventId + " at " + timeStr);
                    startEvent(eventId);
                }
            }
        }, 1200L, 1200L); // check every 60 seconds
    }

    // -------------------------------------------------------------------------
    // Jackpot Hour particle effect (called by EventPlugin sell handler)
    // -------------------------------------------------------------------------

    /**
     * Spawn confetti particles at the given player's location.
     * Called when a player sells during a JACKPOT_HOUR event.
     */
    public void spawnConfetti(Player player) {
        Location loc = player.getLocation().add(0, 1, 0);
        Random rng = new Random();
        // Spawn 30 colored dust particles in a small burst around the player
        for (int i = 0; i < 30; i++) {
            double offsetX = (rng.nextDouble() - 0.5) * 2.0;
            double offsetY = rng.nextDouble() * 1.5;
            double offsetZ = (rng.nextDouble() - 0.5) * 2.0;
            Location spawnLoc = loc.clone().add(offsetX, offsetY, offsetZ);

            // Pick a random bright color
            float r = rng.nextFloat();
            float g = rng.nextFloat();
            float b = rng.nextFloat();
            player.getWorld().spawnParticle(
                    Particle.DUST,
                    spawnLoc,
                    1,
                    0, 0, 0,
                    0,
                    new Particle.DustOptions(org.bukkit.Color.fromRGB(
                            (int)(r * 255), (int)(g * 255), (int)(b * 255)
                    ), 1.5f)
            );
        }
    }

    /**
     * Returns {@code true} if any JACKPOT_HOUR event is currently active.
     */
    public boolean isJackpotActive() {
        for (ActiveEvent e : activeEvents.values()) {
            if (e.config().type() == EventType.JACKPOT_HOUR) return true;
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void cancelTask(BukkitTask task) {
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
    }
}
