package com.prison.events;

import com.prison.permissions.PermissionEngine;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;

/**
 * Main entry point for the PrisonEvents plugin.
 *
 * <p>Initialises {@link EventManager}, registers the {@code /event} command,
 * exposes {@link EventsAPI}, and handles GUI inventory events.
 */
public class EventPlugin extends JavaPlugin implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private EventManager eventManager;

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    public void onEnable() {
        saveDefaultConfig();

        eventManager = new EventManager(this);
        eventManager.load();

        EventsAPI.init(eventManager);

        // Register event multiplier providers with EconomyAPI (soft-dep)
        org.bukkit.plugin.Plugin ecoPlugin =
            getServer().getPluginManager().getPlugin("PrisonEconomy");
        if (ecoPlugin != null && ecoPlugin.isEnabled()) {
            try {
                com.prison.economy.EconomyAPI eco = com.prison.economy.EconomyAPI.getInstance();
                if (eco != null) {
                    eco.setEventSellBonusProvider(uuid -> {
                        EventsAPI api = EventsAPI.getInstance();
                        return api != null ? api.getSellMultiplier() : 1.0;
                    });
                    eco.setEventTokenBonusProvider(uuid -> {
                        EventsAPI api = EventsAPI.getInstance();
                        return api != null ? api.getTokenMultiplier() : 1.0;
                    });
                    getLogger().info("[Events] Registered sell + token bonus providers with EconomyAPI.");
                }
            } catch (Exception e) {
                getLogger().warning("[Events] Could not register EconomyAPI providers: " + e.getMessage());
            }
        }

        getServer().getPluginManager().registerEvents(this, this);

        getLogger().info("PrisonEvents enabled.");
    }

    @Override
    public void onDisable() {
        if (eventManager != null) {
            eventManager.shutdown();
        }
        EventsAPI.shutdown();
        getLogger().info("PrisonEvents disabled.");
    }

    // -------------------------------------------------------------------------
    // Commands — /event
    // -------------------------------------------------------------------------

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("event")) return false;

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {

            case "start" -> {
                if (!hasAdminPermission(sender)) return true;
                if (args.length < 2) {
                    sender.sendMessage(MM.deserialize("<red>Usage: /event start <eventId>"));
                    return true;
                }
                String id = args[1].toLowerCase();
                if (!eventManager.getConfiguredEvents().containsKey(id)) {
                    sender.sendMessage(MM.deserialize(
                            "<red>Unknown event id: <yellow>" + id +
                            "<red>. Use <yellow>/event list<red> to see available events."));
                    return true;
                }
                eventManager.startEvent(id);
                sender.sendMessage(MM.deserialize("<green>Event <yellow>" + id + "<green> started."));
            }

            case "stop" -> {
                if (!hasAdminPermission(sender)) return true;
                if (args.length < 2) {
                    sender.sendMessage(MM.deserialize("<red>Usage: /event stop <eventId>"));
                    return true;
                }
                String id = args[1].toLowerCase();
                if (!eventManager.getActiveEvents().stream()
                        .anyMatch(e -> e.config().id().equals(id))) {
                    sender.sendMessage(MM.deserialize(
                            "<red>No active event found with id: <yellow>" + id));
                    return true;
                }
                eventManager.stopEvent(id);
                sender.sendMessage(MM.deserialize("<green>Event <yellow>" + id + "<green> stopped."));
            }

            case "list" -> handleList(sender);

            case "info" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("This sub-command is player-only.");
                    return true;
                }
                EventInfoGUI.open(player, eventManager);
            }

            default -> sendHelp(sender);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!command.getName().equalsIgnoreCase("event")) return List.of();

        if (args.length == 1) {
            return List.of("start", "stop", "list", "info").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("start")) {
                return eventManager.getConfiguredEvents().keySet().stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .toList();
            }
            if (sub.equals("stop")) {
                return eventManager.getActiveEvents().stream()
                        .map(e -> e.config().id())
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .toList();
            }
        }
        return List.of();
    }

    // -------------------------------------------------------------------------
    // Command helpers
    // -------------------------------------------------------------------------

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(MM.deserialize("<gold><bold>— Events Help —"));
        sender.sendMessage(MM.deserialize("<yellow>/event list<gray> — List all configured and active events"));
        sender.sendMessage(MM.deserialize("<yellow>/event info<gray> — View active events GUI (players only)"));
        if (hasAdminPermission(sender, false)) {
            sender.sendMessage(MM.deserialize("<yellow>/event start <eventId><gray> — Start a configured event"));
            sender.sendMessage(MM.deserialize("<yellow>/event stop <eventId><gray> — Force-stop a running event"));
        }
    }

    private void handleList(CommandSender sender) {
        Map<String, EventConfig> configured = eventManager.getConfiguredEvents();

        sender.sendMessage(MM.deserialize("<gold><bold>— Configured Events —"));
        if (configured.isEmpty()) {
            sender.sendMessage(MM.deserialize("<gray>  No events configured."));
        } else {
            for (Map.Entry<String, EventConfig> entry : configured.entrySet()) {
                EventConfig cfg = entry.getValue();
                boolean isActive = eventManager.getActiveEvents().stream()
                        .anyMatch(e -> e.config().id().equals(entry.getKey()));
                String status = isActive ? "<green>[ACTIVE]" : "<dark_gray>[INACTIVE]";
                sender.sendMessage(MM.deserialize(
                        status + " <gray>" + entry.getKey() +
                        " <dark_gray>(" + cfg.type().name() + ", x" + cfg.multiplier() +
                        ", " + cfg.durationMinutes() + "m)"
                ));
            }
        }

        sender.sendMessage(MM.deserialize("<gold><bold>— Active Events —"));
        var active = eventManager.getActiveEvents();
        if (active.isEmpty()) {
            sender.sendMessage(MM.deserialize("<gray>  No events currently running."));
        } else {
            for (ActiveEvent event : active) {
                sender.sendMessage(MM.deserialize(
                        "<green>• " + event.config().displayName() +
                        " <gray>— " + event.remainingMinutes() + "m remaining"
                ));
            }
        }
    }

    private boolean hasAdminPermission(CommandSender sender) {
        return hasAdminPermission(sender, true);
    }

    private boolean hasAdminPermission(CommandSender sender, boolean sendMessage) {
        boolean has;
        if (sender instanceof Player player) {
            has = PermissionEngine.getInstance().hasPermission(player, "prison.admin.events");
        } else {
            // Console always has admin access
            has = true;
        }
        if (!has && sendMessage) {
            sender.sendMessage(MM.deserialize("<red>You don't have permission to use this command."));
        }
        return has;
    }

    // -------------------------------------------------------------------------
    // GUI inventory events
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        var openTitle = player.getOpenInventory().title();
        if (EventInfoGUI.isTitle(openTitle)) {
            EventInfoGUI.handleClick(player, event);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        var openTitle = player.getOpenInventory().title();
        if (EventInfoGUI.isTitle(openTitle)) {
            event.setCancelled(true);
        }
    }
}
