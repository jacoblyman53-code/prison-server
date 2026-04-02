package com.prison.admintoolkit;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ConfirmDestructiveGUI — confirmation step before destructive admin actions.
 *
 * Intercepts Ban / TempBan / Mute / Kick from PlayerManageGUI and requires the
 * admin to click CONFIRM before being prompted for a reason. Clicking CANCEL
 * returns to PlayerManageGUI for the same target.
 *
 * Layout (27 slots, 3 rows):
 *   Row 0 (0-8):  filler
 *   Row 1 (9-17): filler | filler | CONFIRM(11) | filler | HEAD(13) | filler | CANCEL(15) | filler | filler
 *   Row 2 (18-26): filler | filler | ACTION-INFO centered at 22
 */
public class ConfirmDestructiveGUI {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final Component TITLE = MM.deserialize("CONFIRM");

    public enum ActionType { BAN, TEMP_BAN, MUTE, KICK }

    private record PendingAction(String targetName, ActionType actionType) {}

    private static final Map<UUID, PendingAction> pending = new ConcurrentHashMap<>();

    // ----------------------------------------------------------------

    public static void open(Player admin, String targetName, ActionType actionType) {
        pending.put(admin.getUniqueId(), new PendingAction(targetName, actionType));

        Inventory inv = Bukkit.createInventory(null, 27, TITLE);

        // Action description — centered in top row (slot 4)
        String actionLabel = switch (actionType) {
            case BAN      -> "Permanently Ban";
            case TEMP_BAN -> "Temporarily Ban";
            case MUTE     -> "Permanently Mute";
            case KICK     -> "Kick";
        };
        String actionColor = (actionType == ActionType.KICK) ? "<yellow>" : "<red>";
        String hint = switch (actionType) {
            case BAN      -> "<gray>This will <red>permanently ban <gray><yellow>" + targetName + "<gray>.";
            case TEMP_BAN -> "<gray>You will enter a <green>duration<gray> and <green>reason<gray> next.";
            case MUTE     -> "<gray>This will <red>permanently mute <gray><yellow>" + targetName + "<gray>.";
            case KICK     -> "<gray>This will <green>kick <yellow>" + targetName + " <gray>from the server.";
        };
        inv.setItem(4, AdminPanel.makeItem(Material.PAPER,
            actionColor + "✦ " + actionLabel + ": <yellow>" + targetName,
            hint,
            "",
            "<green>→ <green>Click <green><underlined>CONFIRM</underlined><green> to proceed or <red>CANCEL<green> to abort."));

        // CONFIRM — slot 11
        inv.setItem(11, AdminPanel.makeItem(Material.LIME_WOOL,
            "<green>✓ Confirm",
            "<gray>Click to confirm <green>" + actionLabel + "<gray>.",
            "",
            "<green>→ <green>Click to <green><underlined>confirm</underlined> this action!"));

        // Target player head — slot 13
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skull = (SkullMeta) head.getItemMeta();
        skull.displayName(MM.deserialize("<!italic><yellow>" + targetName));
        skull.lore(java.util.List.of(
            MM.deserialize("<!italic><aqua>✦ <gray>Target player"),
            MM.deserialize("<!italic><aqua>✦ <gray>Action: <white>" + actionLabel)
        ));
        @SuppressWarnings("deprecation")
        OfflinePlayer op = Bukkit.getOfflinePlayer(targetName);
        skull.setOwningPlayer(op);
        skull.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
        head.setItemMeta(skull);
        inv.setItem(13, head);

        // CANCEL — slot 15
        inv.setItem(15, AdminPanel.makeItem(Material.RED_WOOL,
            "<red>✗ Cancel",
            "<gray>Click to cancel and return.",
            "",
            "<green>→ <green>Click to <green><underlined>cancel</underlined> this action."));

        admin.openInventory(inv);
    }

    public static boolean isTitle(Component title) {
        return TITLE.equals(title);
    }

    // ----------------------------------------------------------------
    // Click handling
    // ----------------------------------------------------------------

    public static void handleClick(Player admin, int slot) {
        PendingAction action = pending.get(admin.getUniqueId());
        if (action == null) { admin.closeInventory(); return; }

        String targetName = action.targetName();

        if (slot == 11) {
            // CONFIRM — open AnvilInputGUI for reason/input
            pending.remove(admin.getUniqueId());
            switch (action.actionType()) {
                case BAN -> AnvilInputGUI.open(admin, "Reason...", reason ->
                    Bukkit.getScheduler().runTask(AdminToolkitPlugin.getInstance(), () ->
                        Bukkit.dispatchCommand(admin, "ban " + targetName + " " + reason)));

                case TEMP_BAN -> AnvilInputGUI.open(admin, "30d Cheating", input -> {
                    String[] parts = input.trim().split(" ", 2);
                    String duration = parts[0];
                    String reason   = parts.length > 1 ? parts[1] : "No reason given";
                    Bukkit.getScheduler().runTask(AdminToolkitPlugin.getInstance(), () ->
                        Bukkit.dispatchCommand(admin, "tempban " + targetName + " " + duration + " " + reason));
                });

                case MUTE -> AnvilInputGUI.open(admin, "Reason...", reason ->
                    Bukkit.getScheduler().runTask(AdminToolkitPlugin.getInstance(), () ->
                        Bukkit.dispatchCommand(admin, "mute " + targetName + " " + reason)));

                case KICK -> AnvilInputGUI.open(admin, "Reason...", reason ->
                    Bukkit.getScheduler().runTask(AdminToolkitPlugin.getInstance(), () ->
                        Bukkit.dispatchCommand(admin, "kick " + targetName + " " + reason)));
            }
        } else if (slot == 15) {
            // CANCEL — go back to player manager
            pending.remove(admin.getUniqueId());
            Bukkit.getScheduler().runTask(AdminToolkitPlugin.getInstance(), () ->
                PlayerManageGUI.open(admin, targetName));
        }
        // Other slots: no action
    }

    public static void cleanup(UUID uuid) {
        pending.remove(uuid);
    }
}
