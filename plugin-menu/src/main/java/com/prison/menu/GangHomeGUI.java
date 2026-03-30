package com.prison.menu;

import com.prison.gangs.GangAPI;
import com.prison.gangs.GangData;
import com.prison.gangs.GangManager;
import com.prison.gangs.GangRole;
import com.prison.menu.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class GangHomeGUI {

    public static final Component TITLE = MiniMessage.miniMessage().deserialize("<!italic><dark_gray>[ <green>Gang Home <dark_gray>]");
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private static final int SLOT_BACK = 45;

    public static void open(Player player) {
        player.openInventory(build(player));
        Sounds.nav(player);
    }

    public static void handleClick(Player player, int slot, MenuPlugin plugin) {
        switch (slot) {
            case SLOT_BACK -> {
                Sounds.nav(player);
                MainMenuGUI.open(player);
            }
            case 13 -> {
                // "No Gang" info item — close and hint at /gang commands
                boolean inGang = false;
                try {
                    GangAPI gapi = GangAPI.getInstance();
                    if (gapi != null && gapi.getGangName(player.getUniqueId()) != null) inGang = true;
                } catch (Exception ignored) {}
                if (!inGang) {
                    Sounds.nav(player);
                    player.closeInventory();
                }
            }
            case 24 -> {
                // Gang commands book — close inventory
                Sounds.nav(player);
                player.closeInventory();
            }
            case 20 -> {
                // Gang management (owner only) — close inventory
                Sounds.nav(player);
                player.closeInventory();
            }
            default -> {} // filler — no action
        }
    }

    // ----------------------------------------------------------------
    // Build
    // ----------------------------------------------------------------

    private static Inventory build(Player player) {
        UUID uuid = player.getUniqueId();
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        Gui.fillAll(inv);

        // Resolve gang state
        String gangName = null;
        String gangTag = null;
        int gangLevel = -1;
        GangRole role = null;
        double sellBonus = 1.0;
        double tokenBonus = 1.0;
        GangData gangData = null;

        try {
            GangAPI gapi = GangAPI.getInstance();
            if (gapi != null) {
                gangName  = gapi.getGangName(uuid);
                gangTag   = gapi.getGangTag(uuid);
                gangLevel = gapi.getGangLevel(uuid);
                role      = gapi.getGangRole(uuid);
                sellBonus = gapi.getSellBonus(uuid);
                tokenBonus = gapi.getTokenBonus(uuid);
            }
        } catch (Exception ignored) {}

        try {
            GangManager gm = GangManager.getInstance();
            if (gm != null) {
                gangData = gm.getGangOf(uuid);
            }
        } catch (Exception ignored) {}

        boolean inGang = gangName != null;

        // ---- Slot 13: Main gang info ----
        if (!inGang) {
            inv.setItem(13, Gui.make(Material.SHIELD, "<gray>No Gang",
                "<gray>You are not in a gang.",
                "<dark_gray>Use <white>/gang create <name> <tag> <dark_gray>to create one.",
                "",
                "<yellow>Click to open gang commands..."));
        } else {
            String tag     = gangTag  != null ? gangTag  : "";
            String bankStr = "<gray>Bank: <gold>N/A";
            String createdStr = "";
            if (gangData != null) {
                bankStr = "<gray>Bank: <gold>" + Fmt.number((long) gangData.bankBalance()) + " IGC";
                if (gangData.createdAt() != null) {
                    createdStr = "<gray>Founded: <white>" +
                        gangData.createdAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                }
            }
            inv.setItem(13, Gui.make(Material.SHIELD, "<green>" + gangName,
                "<gray>Tag: <white>[" + tag + "]",
                "<gray>Level: <white>" + gangLevel,
                bankStr,
                createdStr,
                ""));
        }

        // ---- Slot 11: Sell Bonus ----
        inv.setItem(11, Gui.make(Material.SUNFLOWER, "<yellow>Sell Bonus",
            "<gray>Gang sell bonus: <green>" + Fmt.multiplier(sellBonus)));

        // ---- Slot 15: Token Bonus ----
        inv.setItem(15, Gui.make(Material.EXPERIENCE_BOTTLE, "<aqua>Token Bonus",
            "<gray>Gang token bonus: <aqua>" + Fmt.multiplier(tokenBonus)));

        if (inGang) {
            // ---- Slot 22: Your Role ----
            String roleName = role != null ? role.name() : "MEMBER";
            inv.setItem(22, Gui.make(Material.PLAYER_HEAD, "<green>Your Role: <white>" + roleName,
                "<gray>Role: <white>" + roleName));

            // ---- Slot 24: Gang Commands ----
            inv.setItem(24, Gui.make(Material.WRITABLE_BOOK, "<gray>Gang Commands",
                "<dark_gray>/gang help",
                "<dark_gray>/gang invite <player>",
                "<dark_gray>/gang leave",
                "<dark_gray>/gang bank deposit <amt>",
                "",
                "<yellow>Click to close menu"));

            // ---- Slot 20: Gang Management (owner only) ----
            boolean isOwner = role == GangRole.LEADER;
            if (isOwner) {
                inv.setItem(20, Gui.make(Material.ANVIL, "<red>Gang Management",
                    "<dark_gray>/gang disband",
                    "<dark_gray>/gang kick <player>",
                    "",
                    "<yellow>Click to close menu"));
            }
        }

        inv.setItem(SLOT_BACK, Gui.back());
        return inv;
    }
}
