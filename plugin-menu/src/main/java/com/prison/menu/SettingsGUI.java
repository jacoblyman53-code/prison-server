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

    public static final Component TITLE = MiniMessage.miniMessage().deserialize("<!italic>SETTINGS");

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

        PlayerSettingsManager psm = PlayerSettingsManager.getInstance();
        PlayerSettingsManager.PlayerSettings settings = psm != null
            ? psm.get(uuid)
            : new PlayerSettingsManager.PlayerSettings();

        // Slot 0: back
        inv.setItem(0, Gui.back());

        // ---- Sounds ----
        boolean sounds = settings.isSounds();
        if (sounds) {
            inv.setItem(SLOT_SOUNDS, Gui.make(Material.NOTE_BLOCK,
                "<aqua>Sounds",
                "<gray>✦ Status: <green>✓ Enabled",
                "<gray>✦ Play <aqua>sound effects<gray> for GUI interactions.",
                "",
                "<green>→ Click to toggle this setting!"));
        } else {
            inv.setItem(SLOT_SOUNDS, Gui.make(Material.NOTE_BLOCK,
                "<aqua>Sounds",
                "<gray>✦ Status: <red>✗ Disabled",
                "<gray>✦ Play <aqua>sound effects<gray> for GUI interactions.",
                "",
                "<green>→ Click to toggle this setting!"));
        }

        // ---- Notifications ----
        boolean notifications = settings.isNotifications();
        if (notifications) {
            inv.setItem(SLOT_NOTIFICATIONS, Gui.make(Material.BELL,
                "<aqua>Notifications",
                "<gray>✦ Status: <green>✓ Enabled",
                "<gray>✦ Receive in-chat alerts for <aqua>rankups<gray>,",
                "<gray>  quest completions, and server events.",
                "",
                "<green>→ Click to toggle this setting!"));
        } else {
            inv.setItem(SLOT_NOTIFICATIONS, Gui.make(Material.BELL,
                "<aqua>Notifications",
                "<gray>✦ Status: <red>✗ Disabled",
                "<gray>✦ Receive in-chat alerts for <aqua>rankups<gray>,",
                "<gray>  quest completions, and server events.",
                "",
                "<green>→ Click to toggle this setting!"));
        }

        // ---- Auto-Sell Default ----
        boolean autosell = settings.isAutosellDefault();
        if (autosell) {
            inv.setItem(SLOT_AUTOSELL, Gui.make(Material.HOPPER,
                "<aqua>Auto-Sell",
                "<gray>✦ Status: <green>✓ Enabled",
                "<gray>✦ Automatically enable <aqua>auto-sell<gray> when",
                "<gray>  you log in or enter a mine.",
                "",
                "<green>→ Click to toggle this setting!"));
        } else {
            inv.setItem(SLOT_AUTOSELL, Gui.make(Material.HOPPER,
                "<aqua>Auto-Sell",
                "<gray>✦ Status: <red>✗ Disabled",
                "<gray>✦ Automatically enable <aqua>auto-sell<gray> when",
                "<gray>  you log in or enter a mine.",
                "",
                "<green>→ Click to toggle this setting!"));
        }

        // ---- Rankup Auto-Teleport ----
        boolean rankupTp = settings.isRankupAutoteleport();
        if (rankupTp) {
            inv.setItem(SLOT_RANKUP_TP, Gui.make(Material.COMPASS,
                "<aqua>Rankup Teleport",
                "<gray>✦ Status: <green>✓ Enabled",
                "<gray>✦ Automatically <aqua>teleport<gray> to your new mine",
                "<gray>  after purchasing a rank.",
                "",
                "<green>→ Click to toggle this setting!"));
        } else {
            inv.setItem(SLOT_RANKUP_TP, Gui.make(Material.COMPASS,
                "<aqua>Rankup Teleport",
                "<gray>✦ Status: <red>✗ Disabled",
                "<gray>✦ Automatically <aqua>teleport<gray> to your new mine",
                "<gray>  after purchasing a rank.",
                "",
                "<green>→ Click to toggle this setting!"));
        }

        // ---- UI Detail Mode ----
        boolean detail = settings.isUiDetailMode();
        if (detail) {
            inv.setItem(SLOT_DETAIL_MODE, Gui.make(Material.BOOK,
                "<aqua>Detail Mode",
                "<gray>✦ Status: <green>✓ Enabled",
                "<gray>✦ Show extra <aqua>tooltip info<gray> in menus.",
                "<gray>  ◆ Stat breakdowns, raw values,",
                "<gray>  ◆ Extended descriptions.",
                "",
                "<green>→ Click to toggle this setting!"));
        } else {
            inv.setItem(SLOT_DETAIL_MODE, Gui.make(Material.BOOK,
                "<aqua>Detail Mode",
                "<gray>✦ Status: <red>✗ Disabled",
                "<gray>✦ Show extra <aqua>tooltip info<gray> in menus.",
                "<gray>  ◆ Stat breakdowns, raw values,",
                "<gray>  ◆ Extended descriptions.",
                "",
                "<green>→ Click to toggle this setting!"));
        }

        inv.setItem(SLOT_BACK, Gui.back());
        return inv;
    }
}
