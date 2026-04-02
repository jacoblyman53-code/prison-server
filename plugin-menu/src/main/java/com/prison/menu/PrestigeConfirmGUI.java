package com.prison.menu;

import com.prison.menu.util.*;
import com.prison.permissions.PermissionEngine;
import com.prison.prestige.PrestigeManager;
import com.prison.prestige.PrestigeShopManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import java.util.*;

/**
 * PrestigeConfirmGUI — 27-slot confirmation dialog for prestiging.
 *
 * Slot 11 : confirm (NETHER_STAR) or blocked (RED_STAINED_GLASS_PANE)
 * Slot 13 : info item showing current prestige / rank / sell bonus
 * Slot 15 : cancel (BARRIER)
 * Slot 18 : back → PrestigeShopMenuGUI
 */
public class PrestigeConfirmGUI {

    public static final Component TITLE =
        MiniMessage.miniMessage().deserialize("<!italic>Confirm Prestige");

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private static final int SLOT_CONFIRM = 11;
    private static final int SLOT_INFO    = 13;
    private static final int SLOT_CANCEL  = 15;
    private static final int SLOT_BACK    = 18;

    // ----------------------------------------------------------------
    // Public API
    // ----------------------------------------------------------------

    public static void open(Player player) {
        player.openInventory(build(player));
    }

    public static void handleClick(Player player, int slot, MenuPlugin plugin) {
        if (slot == SLOT_BACK) {
            Sounds.nav(player);
            PrestigeShopMenuGUI.open(player);
            return;
        }

        if (slot == SLOT_CANCEL) {
            Sounds.nav(player);
            MainMenuGUI.open(player);
            return;
        }

        if (slot == SLOT_CONFIRM) {
            handleConfirm(player);
            return;
        }
    }

    // ----------------------------------------------------------------
    // Build
    // ----------------------------------------------------------------

    private static Inventory build(Player player) {
        UUID uuid = player.getUniqueId();
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);

        PrestigeManager pm  = PrestigeManager.getInstance();
        PermissionEngine pe = PermissionEngine.getInstance();

        boolean canPrestige = pm != null && pm.canPrestige(uuid);
        int     prestige    = pm != null ? pm.getPrestigeLevel(uuid) : 0;
        String  rank        = pe != null ? pe.getMineRank(uuid) : "?";

        // --- Slot 11: Confirm or Blocked ---
        if (canPrestige) {
            inv.setItem(SLOT_CONFIRM, Gui.make(Material.NETHER_STAR,
                "<green>✓ Confirm Prestige",
                "<gray>✦ Your rank will reset to <yellow>A<gray>.",
                "<gray>✦ Your balance will be wiped.",
                "<gray>✦ Prestige level: <white>" + prestige + " → " + (prestige + 1),
                "",
                "<green>→ Click to confirm prestige!"));
        } else {
            inv.setItem(SLOT_CONFIRM, Gui.make(Material.RED_STAINED_GLASS_PANE,
                "<red>✗ Cannot Prestige Yet",
                "<gray>✦ You must reach rank <yellow>Z<gray> first.",
                "<gray>✦ Current rank: <yellow>" + rank));
        }

        // --- Slot 13: Info ---
        int sellBonus = prestige > 0 ? prestige * 5 : 0;
        inv.setItem(SLOT_INFO, Gui.make(Material.BOOK,
            "<aqua>✦ Prestige Info",
            "<gray>✦ Current prestige: <light_purple>✦" + prestige,
            "<gray>✦ Current mine rank: <yellow>" + rank,
            "<gray>✦ Sell bonus after: <green>+" + (sellBonus + 5) + "% <gray>(+5% per prestige)",
            "",
            "<gray>✦ Prestige <aqua>resets<gray> your rank to <yellow>A<gray>",
            "<gray>  but grants <aqua>permanent bonuses<gray>."));

        // --- Slot 15: Cancel ---
        inv.setItem(SLOT_CANCEL, Gui.make(Material.BARRIER,
            "<red>✗ Cancel",
            "<gray>✦ Click to cancel and return to menu."));

        // --- Slot 18: Back ---
        inv.setItem(SLOT_BACK, Gui.back());

        return inv;
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private static void handleConfirm(Player player) {
        PrestigeManager pm = PrestigeManager.getInstance();
        if (pm == null) {
            Sounds.deny(player);
            player.sendMessage(MM.deserialize("<red>Prestige system unavailable. Please try again."));
            return;
        }

        boolean canPrestige = pm.canPrestige(player.getUniqueId());
        if (!canPrestige) {
            Sounds.deny(player);
            player.sendMessage(MM.deserialize("<red>You cannot prestige yet. Reach rank Z first."));
            return;
        }

        int result = pm.executePrestige(player);
        if (result == -1) {
            Sounds.deny(player);
            player.sendMessage(MM.deserialize("<red>Prestige failed. Please try again."));
        } else {
            Sounds.reward(player);
            player.closeInventory();
            player.sendMessage(MM.deserialize(
                "<light_purple>\u2746 You have prestiged! Welcome to prestige <white>" + result + "<light_purple>."));
        }
    }
}
