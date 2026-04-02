package com.prison.admintoolkit;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * ServerToolsGUI — toggle whitelist, maintenance mode, global PvP, and schedule restarts.
 */
public class ServerToolsGUI {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private static final Component TITLE = MM.deserialize("SERVER TOOLS");

    // ----------------------------------------------------------------
    // Open
    // ----------------------------------------------------------------

    public static void open(Player player) {
        AdminToolkitPlugin plugin = AdminToolkitPlugin.getInstance();
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);

        // Slot 10 — Whitelist
        boolean whitelistOn = Bukkit.hasWhitelist();
        ItemStack whitelistItem = AdminPanel.makeItem(
            whitelistOn ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE,
            "<aqua>Whitelist",
            "<aqua>✦ <gray>Status: " + (whitelistOn ? "<green>✓ ON" : "<red>✗ OFF"),
            "",
            "<green>→ <green>Click to <green><underlined>toggle</underlined> whitelist!");
        inv.setItem(10, whitelistItem);

        // Slot 12 — Maintenance Mode
        boolean maintOn = plugin.isMaintenanceMode();
        ItemStack maintItem = AdminPanel.makeItem(
            maintOn ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE,
            "<aqua>Maintenance Mode",
            "<aqua>✦ <gray>Status: " + (maintOn ? "<green>✓ ON" : "<red>✗ OFF"),
            "<gray>Only <green>admins<gray> can join when ON.",
            "",
            "<green>→ <green>Click to <green><underlined>toggle</underlined> maintenance mode!");
        inv.setItem(12, maintItem);

        // Slot 14 — Global PvP
        boolean pvpOn = plugin.isGlobalPvpEnabled();
        ItemStack pvpItem = AdminPanel.makeItem(
            pvpOn ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE,
            "<aqua>Global PvP",
            "<aqua>✦ <gray>Status: " + (pvpOn ? "<green>✓ ON" : "<red>✗ OFF"),
            "<gray>When <red>OFF<gray>, no player vs player combat.",
            "",
            "<green>→ <green>Click to <green><underlined>toggle</underlined> global PvP!");
        inv.setItem(14, pvpItem);

        // Slot 16 — Schedule Restart
        inv.setItem(16, AdminPanel.makeItem(Material.CLOCK,
            "<aqua>Schedule Restart",
            "<gray>Broadcast <green>restart warnings<gray> and restart server.",
            "",
            "<green>→ <green>Click to <green><underlined>schedule</underlined> a restart! <gray>(e.g. 30m)"));

        // Slot 26 — Close
        inv.setItem(26, AdminPanel.makeItem(Material.BARRIER, "<red>✗ Close", "<gray>Click to close this menu."));

        player.openInventory(inv);
    }

    // ----------------------------------------------------------------
    // Title matching
    // ----------------------------------------------------------------

    public static boolean isTitle(Component title) {
        return TITLE.equals(title);
    }

    // ----------------------------------------------------------------
    // Click handling
    // ----------------------------------------------------------------

    public static void handleClick(Player player, int slot) {
        AdminToolkitPlugin plugin = AdminToolkitPlugin.getInstance();

        switch (slot) {
            case 10 -> {
                // Toggle whitelist
                if (Bukkit.hasWhitelist()) {
                    Bukkit.setWhitelist(false);
                    player.sendMessage(MM.deserialize("<red>Whitelist disabled."));
                } else {
                    Bukkit.setWhitelist(true);
                    player.sendMessage(MM.deserialize("<green>Whitelist enabled."));
                }
                // Refresh GUI
                Bukkit.getScheduler().runTask(plugin, () -> open(player));
            }
            case 12 -> {
                // Toggle maintenance
                boolean newState = !plugin.isMaintenanceMode();
                plugin.setMaintenanceMode(newState);
                player.sendMessage(MM.deserialize(newState
                    ? "<green>Maintenance mode enabled. Non-admins have been kicked."
                    : "<red>Maintenance mode disabled."));
                Bukkit.getScheduler().runTask(plugin, () -> open(player));
            }
            case 14 -> {
                // Toggle global PvP
                boolean newState = !plugin.isGlobalPvpEnabled();
                plugin.setGlobalPvp(newState);
                player.sendMessage(MM.deserialize(newState
                    ? "<green>Global PvP enabled."
                    : "<red>Global PvP disabled."));
                Bukkit.getScheduler().runTask(plugin, () -> open(player));
            }
            case 16 -> {
                // Schedule restart
                AnvilInputGUI.open(player, "30m", text -> {
                    long seconds = parseDuration(text.trim());
                    if (seconds <= 0) {
                        player.sendMessage(MM.deserialize("<red>Invalid duration. Use e.g. 30m, 1h, 2h30m."));
                        return;
                    }
                    long ticks = seconds * 20L;
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        // Broadcast warning
                        Bukkit.broadcast(MM.deserialize("<red>[Server] <yellow>Server restart in " + text.trim() + "!"));
                        // Schedule actual shutdown
                        Bukkit.getScheduler().runTaskLater(plugin, Bukkit::shutdown, ticks);
                        player.sendMessage(MM.deserialize("<green>Server restart scheduled in " + text.trim() + "."));
                        player.closeInventory();
                    });
                });
            }
            case 26 -> player.closeInventory();
        }
    }

    // ----------------------------------------------------------------
    // Duration parser — supports 30m, 1h, 2h30m, 90s, etc.
    // ----------------------------------------------------------------

    static long parseDuration(String input) {
        if (input == null || input.isBlank()) return -1;
        input = input.toLowerCase().trim();

        long total = 0;
        StringBuilder num = new StringBuilder();

        for (char c : input.toCharArray()) {
            if (Character.isDigit(c)) {
                num.append(c);
            } else if (c == 'h' && !num.isEmpty()) {
                total += Long.parseLong(num.toString()) * 3600;
                num.setLength(0);
            } else if (c == 'm' && !num.isEmpty()) {
                total += Long.parseLong(num.toString()) * 60;
                num.setLength(0);
            } else if (c == 's' && !num.isEmpty()) {
                total += Long.parseLong(num.toString());
                num.setLength(0);
            }
        }

        // Leftover digits with no unit — treat as minutes
        if (!num.isEmpty()) {
            total += Long.parseLong(num.toString()) * 60;
        }

        return total;
    }
}
