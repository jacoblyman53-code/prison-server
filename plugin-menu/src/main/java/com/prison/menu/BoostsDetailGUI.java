package com.prison.menu;

import com.prison.economy.EconomyAPI;
import com.prison.events.EventsAPI;
import com.prison.gangs.GangAPI;
import com.prison.menu.util.*;
import com.prison.permissions.PermissionEngine;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BoostsDetailGUI {

    public static final Component TITLE = MiniMessage.miniMessage().deserialize("<!italic><dark_gray>[ <green>Active Boosts <dark_gray>]");
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private static final int SLOT_STREAK   = 10;
    private static final int SLOT_GANG     = 11;
    private static final int SLOT_PRESTIGE = 12;
    private static final int SLOT_EVENT    = 13;
    private static final int SLOT_DONOR    = 14;
    private static final int SLOT_TOTAL    = 22;
    private static final int SLOT_BACK     = 18;

    // ----------------------------------------------------------------

    public static void open(Player player) {
        player.openInventory(build(player));
    }

    public static void handleClick(Player player, int slot, MenuPlugin plugin) {
        if (slot == SLOT_BACK) {
            Sounds.nav(player);
            MainMenuGUI.open(player);
        }
        // All other slots are info-only — no action needed
    }

    // ----------------------------------------------------------------

    private static Inventory build(Player player) {
        UUID uuid = player.getUniqueId();
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);
        Gui.fillAll(inv);

        // ---- Gather data safely ----

        EconomyAPI eco = null;
        try { eco = EconomyAPI.getInstance(); } catch (Exception ignored) {}

        GangAPI gangApi = null;
        try { gangApi = GangAPI.getInstance(); } catch (Exception ignored) {}

        EventsAPI eventsApi = null;
        try { eventsApi = EventsAPI.getInstance(); } catch (Exception ignored) {}

        PermissionEngine pe = null;
        try { pe = PermissionEngine.getInstance(); } catch (Exception ignored) {}

        // Sell streak
        int streak = 0;
        double streakMult = 1.0;
        try {
            if (eco != null) {
                streak = eco.getSellStreak(uuid);
                streakMult = eco.getStreakMultiplier(streak);
            }
        } catch (Exception ignored) {}

        // Gang bonuses
        String gangName = null;
        double gangSell  = 1.0;
        double gangToken = 1.0;
        try {
            if (gangApi != null) {
                gangName  = gangApi.getGangName(uuid);
                gangSell  = gangApi.getSellBonus(uuid);
                gangToken = gangApi.getTokenBonus(uuid);
            }
        } catch (Exception ignored) {}
        // Fallback to EconomyAPI gang sell if GangAPI unavailable
        if (gangName == null) {
            try {
                if (eco != null) gangSell = eco.getGangSellBonus(uuid);
            } catch (Exception ignored) {}
        }

        // Prestige bonus
        int prestige = 0;
        double prestigeSell = 1.0;
        try {
            if (pe != null) prestige = pe.getPrestige(uuid);
            if (eco != null) prestigeSell = eco.getPrestigeSellBonus(uuid);
        } catch (Exception ignored) {}

        // Event bonus
        boolean hasActiveEvent = false;
        int activeEventCount   = 0;
        double eventSell       = 1.0;
        double eventToken      = 1.0;
        try {
            if (eventsApi != null) {
                hasActiveEvent   = eventsApi.hasActiveEvent();
                activeEventCount = eventsApi.getActiveEvents() != null ? eventsApi.getActiveEvents().size() : 0;
                eventSell        = eventsApi.getSellMultiplier();
                eventToken       = eventsApi.getTokenMultiplier();
            }
            if (eco != null) eventSell = eco.getEventSellBonus(uuid);
        } catch (Exception ignored) {}

        // Donor / boost bonus
        double boostSell = 1.0;
        try {
            if (eco != null) boostSell = eco.getBoostSellBonus(uuid);
        } catch (Exception ignored) {}

        // Total sell multiplier
        double totalSell = streakMult * gangSell * eventSell * prestigeSell * boostSell;

        // ================================================================
        // SLOT 10 — Sell Streak
        // ================================================================
        {
            String nextBreak;
            if      (streak < 5)   nextBreak = "5 (1.05x)";
            else if (streak < 10)  nextBreak = "10 (1.10x)";
            else if (streak < 25)  nextBreak = "25 (1.20x)";
            else if (streak < 50)  nextBreak = "50 (1.35x)";
            else if (streak < 100) nextBreak = "100 (1.50x)";
            else                   nextBreak = "MAX (1.50x)";

            inv.setItem(SLOT_STREAK, Gui.make(Material.BLAZE_POWDER,
                "<yellow>Sell Streak",
                "<gray>Current streak: <yellow>" + streak + " sells",
                "<gray>Streak bonus: <green>" + Fmt.multiplier(streakMult),
                "",
                "<dark_gray>Next threshold: <white>" + nextBreak,
                "<dark_gray>Sell within 60s to maintain streak."));
        }

        // ================================================================
        // SLOT 11 — Gang Bonus
        // ================================================================
        {
            if (gangName != null) {
                inv.setItem(SLOT_GANG, Gui.make(Material.SHIELD,
                    "<green>Gang Bonus",
                    "<gray>Gang: <white>" + gangName,
                    "",
                    "<gray>Sell bonus:  <green>" + Fmt.multiplier(gangSell),
                    "<gray>Token bonus: <green>" + Fmt.multiplier(gangToken)));
            } else {
                inv.setItem(SLOT_GANG, Gui.make(Material.SHIELD,
                    "<green>Gang Bonus",
                    "<gray>You are not in a gang.",
                    "",
                    "<dark_gray>Join a gang for passive sell",
                    "<dark_gray>and token bonuses."));
            }
        }

        // ================================================================
        // SLOT 12 — Prestige Bonus
        // ================================================================
        {
            String prestigeLabel = prestige > 0
                ? "<light_purple>\u2746 Prestige " + prestige
                : "<gray>No prestige yet";
            inv.setItem(SLOT_PRESTIGE, Gui.make(Material.FIREWORK_STAR,
                "<light_purple>Prestige Bonus",
                prestigeLabel,
                "",
                "<gray>Sell bonus: <green>" + Fmt.multiplier(prestigeSell),
                "",
                "<dark_gray>Prestige at rank Z to earn more."));
        }

        // ================================================================
        // SLOT 13 — Event Bonus
        // ================================================================
        {
            if (hasActiveEvent && activeEventCount > 0) {
                inv.setItem(SLOT_EVENT, Gui.make(Material.TOTEM_OF_UNDYING,
                    "<aqua>Event Bonus",
                    "<green>" + activeEventCount + " event(s) currently active.",
                    "",
                    "<gray>Sell multiplier:  <green>" + Fmt.multiplier(eventSell),
                    "<gray>Token multiplier: <green>" + Fmt.multiplier(eventToken)));
            } else {
                inv.setItem(SLOT_EVENT, Gui.make(Material.TOTEM_OF_UNDYING,
                    "<aqua>Event Bonus",
                    "<gray>No active event.",
                    "",
                    "<dark_gray>Events are announced in chat",
                    "<dark_gray>and grant temporary multipliers."));
            }
        }

        // ================================================================
        // SLOT 14 — Donor Boost
        // ================================================================
        {
            if (boostSell > 1.0) {
                inv.setItem(SLOT_DONOR, Gui.make(Material.DIAMOND,
                    "<gold>Donor Boost",
                    "<green>Active boost detected.",
                    "",
                    "<gray>Sell bonus: <green>" + Fmt.multiplier(boostSell),
                    "",
                    "<dark_gray>Boosts can be purchased from the store."));
            } else {
                inv.setItem(SLOT_DONOR, Gui.make(Material.DIAMOND,
                    "<gold>Donor Boost",
                    "<gray>No active donor boost.",
                    "",
                    "<dark_gray>Purchase boosts from the store",
                    "<dark_gray>for a temporary sell bonus."));
            }
        }

        // ================================================================
        // SLOT 22 — Total Sell Multiplier
        // ================================================================
        {
            List<Component> totalLore = new ArrayList<>();
            totalLore.add(Component.empty());
            totalLore.add(MM.deserialize("<!italic><dark_gray>Breakdown:"));
            totalLore.add(MM.deserialize("<!italic><dark_gray>  Streak:   <gray>" + Fmt.multiplier(streakMult)));
            totalLore.add(MM.deserialize("<!italic><dark_gray>  Gang:     <gray>" + Fmt.multiplier(gangSell)));
            totalLore.add(MM.deserialize("<!italic><dark_gray>  Event:    <gray>" + Fmt.multiplier(eventSell)));
            totalLore.add(MM.deserialize("<!italic><dark_gray>  Prestige: <gray>" + Fmt.multiplier(prestigeSell)));
            totalLore.add(MM.deserialize("<!italic><dark_gray>  Boost:    <gray>" + Fmt.multiplier(boostSell)));
            totalLore.add(Component.empty());
            totalLore.add(MM.deserialize("<!italic><white>Total: <green>" + Fmt.multiplier(totalSell)));
            inv.setItem(SLOT_TOTAL, Gui.make(Material.SUNFLOWER, "<gold>Total Sell Multiplier", totalLore));
        }

        inv.setItem(SLOT_BACK, Gui.back());
        return inv;
    }
}
