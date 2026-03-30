package com.prison.menu;

import com.prison.menu.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.UUID;

public class SettingsGUI {

    public static final Component TITLE = MiniMessage.miniMessage().deserialize("<!italic><dark_gray>[ <gray>Settings <dark_gray>]");

    private static final int SLOT_SOUNDS        = 10;
    private static final int SLOT_NOTIFICATIONS = 11;
    private static final int SLOT_AUTOSELL      = 12;
    private static final int SLOT_RANKUP_TP     = 13;
    private static final int SLOT_DETAIL_MODE   = 14;
    private static final int SLOT_BACK          = 18;

    // ----------------------------------------------------------------

    public static void open(Player player) {
        player.openInventory(build(player));
    }

    public static void handleClick(Player player, int slot, MenuPlugin plugin) {
        if (slot == SLOT_BACK) {
            Sounds.nav(player);
            MainMenuGUI.open(player);
            return;
        }

        PlayerSettingsManager psm = PlayerSettingsManager.getInstance();
        if (psm == null) return;

        UUID uuid = player.getUniqueId();
        PlayerSettingsManager.PlayerSettings settings = psm.get(uuid);

        switch (slot) {
            case SLOT_SOUNDS -> settings.setSounds(!settings.isSounds());
            case SLOT_NOTIFICATIONS -> settings.setNotifications(!settings.isNotifications());
            case SLOT_AUTOSELL -> settings.setAutosellDefault(!settings.isAutosellDefault());
            case SLOT_RANKUP_TP -> settings.setRankupAutoteleport(!settings.isRankupAutoteleport());
            case SLOT_DETAIL_MODE -> settings.setUiDetailMode(!settings.isUiDetailMode());
            default -> { return; }
        }

        psm.save(uuid);
        Sounds.nav(player);
        player.openInventory(build(player));
    }

    // ----------------------------------------------------------------

    private static Inventory build(Player player) {
        UUID uuid = player.getUniqueId();
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);
        Gui.fillAll(inv);

        PlayerSettingsManager psm = PlayerSettingsManager.getInstance();
        PlayerSettingsManager.PlayerSettings settings = psm != null
            ? psm.get(uuid)
            : new PlayerSettingsManager.PlayerSettings();

        // ---- Sounds ----
        boolean sounds = settings.isSounds();
        if (sounds) {
            inv.setItem(SLOT_SOUNDS, Gui.make(Material.NOTE_BLOCK,
                "<yellow>Sounds: <green>ON",
                "<gray>Play sound effects for GUI interactions.",
                "",
                "<green>Currently enabled. Click to disable."));
        } else {
            inv.setItem(SLOT_SOUNDS, Gui.make(Material.NOTE_BLOCK,
                "<yellow>Sounds: <red>OFF",
                "<gray>Play sound effects for GUI interactions.",
                "",
                "<red>Currently disabled. Click to enable."));
        }

        // ---- Notifications ----
        boolean notifications = settings.isNotifications();
        if (notifications) {
            inv.setItem(SLOT_NOTIFICATIONS, Gui.make(Material.BELL,
                "<yellow>Notifications: <green>ON",
                "<gray>Receive in-chat alerts for rankups,",
                "<gray>quest completions, and server events.",
                "",
                "<green>Currently enabled. Click to disable."));
        } else {
            inv.setItem(SLOT_NOTIFICATIONS, Gui.make(Material.BELL,
                "<yellow>Notifications: <red>OFF",
                "<gray>Receive in-chat alerts for rankups,",
                "<gray>quest completions, and server events.",
                "",
                "<red>Currently disabled. Click to enable."));
        }

        // ---- Auto-Sell Default ----
        boolean autosell = settings.isAutosellDefault();
        if (autosell) {
            inv.setItem(SLOT_AUTOSELL, Gui.make(Material.HOPPER,
                "<yellow>Auto-Sell: <green>ON",
                "<gray>Automatically enable auto-sell when",
                "<gray>you log in or enter a mine.",
                "",
                "<green>Currently enabled. Click to disable."));
        } else {
            inv.setItem(SLOT_AUTOSELL, Gui.make(Material.HOPPER,
                "<yellow>Auto-Sell: <red>OFF",
                "<gray>Automatically enable auto-sell when",
                "<gray>you log in or enter a mine.",
                "",
                "<red>Currently disabled. Click to enable."));
        }

        // ---- Rankup Auto-Teleport ----
        boolean rankupTp = settings.isRankupAutoteleport();
        if (rankupTp) {
            inv.setItem(SLOT_RANKUP_TP, Gui.make(Material.COMPASS,
                "<yellow>Rankup Teleport: <green>ON",
                "<gray>Automatically teleport to your new mine",
                "<gray>after purchasing a rank.",
                "",
                "<green>Currently enabled. Click to disable."));
        } else {
            inv.setItem(SLOT_RANKUP_TP, Gui.make(Material.COMPASS,
                "<yellow>Rankup Teleport: <red>OFF",
                "<gray>Automatically teleport to your new mine",
                "<gray>after purchasing a rank.",
                "",
                "<red>Currently disabled. Click to enable."));
        }

        // ---- UI Detail Mode ----
        boolean detail = settings.isUiDetailMode();
        if (detail) {
            inv.setItem(SLOT_DETAIL_MODE, Gui.make(Material.BOOK,
                "<yellow>Detail Mode: <green>ON",
                "<gray>Show extra tooltip info in menus.",
                "<dark_gray>Includes stat breakdowns, raw values,",
                "<dark_gray>and extended descriptions.",
                "",
                "<green>Currently enabled. Click to disable."));
        } else {
            inv.setItem(SLOT_DETAIL_MODE, Gui.make(Material.BOOK,
                "<yellow>Detail Mode: <red>OFF",
                "<gray>Show extra tooltip info in menus.",
                "<dark_gray>Includes stat breakdowns, raw values,",
                "<dark_gray>and extended descriptions.",
                "",
                "<red>Currently disabled. Click to enable."));
        }

        inv.setItem(SLOT_BACK, Gui.back());
        return inv;
    }
}
