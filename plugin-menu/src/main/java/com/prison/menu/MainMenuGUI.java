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
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class MainMenuGUI {

    public static final Component TITLE = MiniMessage.miniMessage().deserialize("<!italic>Main Menu");
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private static final String[] TIPS = {
        "<gray>Use <white>/sellall<gray> or open <white>Sell Center<gray> to cash in your blocks.",
        "<gray>Upgrade your pickaxe with <white>tokens<gray> to mine faster.",
        "<gray>Join a gang for passive sell and token bonuses.",
        "<gray>Prestige at rank <yellow>Z<gray> to earn permanent bonuses.",
        "<gray>Check <white>Black Market<gray> daily for limited-time deals.",
        "<gray>Complete quests daily to earn extra $ and tokens.",
        "<gray>Use <white>/coinflip<gray> to wager $ against other players.",
        "<gray>Crate keys drop from milestones and are available in the store.",
    };

    public static void open(Player player) {
        player.openInventory(build(player));
        Sounds.nav(player);
    }

    public static void handleClick(Player player, int slot, MenuPlugin plugin) {
        switch (slot) {
            case 6  -> { Sounds.nav(player);   GangHomeGUI.open(player); }
            case 8  -> { Sounds.close(player);  player.closeInventory(); }
            case 10 -> { Sounds.nav(player);   MineBrowserGUI.open(player); }
            case 11 -> { Sounds.nav(player);   PickaxeHomeGUI.open(player); }
            case 12 -> { Sounds.nav(player);   PickaxeEnchantsGUI.open(player); }
            case 13 -> { Sounds.nav(player);   RankProgressionGUI.open(player); }
            case 14 -> { Sounds.nav(player);   handlePrestigeClick(player); }
            case 19 -> { Sounds.nav(player);   ShopCategoryPickerGUI.open(player); }
            case 20 -> { Sounds.nav(player);   SellCenterGUI.open(player); }
            case 21 -> { Sounds.nav(player);   CratesHubGUI.open(player); }
            case 22 -> { Sounds.nav(player);   openCoinflip(player); }
            case 23 -> { Sounds.nav(player);   QuestsMenuGUI.open(player); }
            case 24 -> { Sounds.nav(player);   GangHomeGUI.open(player); }
            case 25 -> { Sounds.nav(player);   CosmeticsMenuGUI.open(player); }
            case 26 -> { Sounds.nav(player);   SettingsGUI.open(player); }
            case 28 -> { Sounds.nav(player);   openAuctionHouse(player); }
            case 29 -> { Sounds.nav(player);   WarpsMenuGUI.open(player); }
            case 30 -> { Sounds.nav(player);   KitsMenuGUI.open(player); }
            case 31 -> { Sounds.nav(player);   LeaderboardSelectorGUI.open(player); }
            case 32 -> { Sounds.nav(player);   BlackMarketMenuGUI.open(player); }
            case 33 -> { Sounds.nav(player);   BoostsDetailGUI.open(player); }
            default -> {} // filler — no action
        }
    }

    // ----------------------------------------------------------------
    // Soft-depend GUI launchers
    // ----------------------------------------------------------------

    private static void openCoinflip(Player player) {
        try {
            Class.forName("com.prison.coinflip.CoinflipBrowserGUI")
                 .getMethod("open", Player.class)
                 .invoke(null, player);
        } catch (Exception | NoClassDefFoundError e) {
            player.sendMessage(MM.deserialize("<red>Coinflip is not available right now."));
        }
    }

    private static void openAuctionHouse(Player player) {
        try {
            com.prison.auctionhouse.AuctionGUI.open(player, 0, null);
        } catch (NoClassDefFoundError | Exception e) {
            player.sendMessage(MM.deserialize("<red>Auction House is not available right now."));
        }
    }

    private static void handlePrestigeClick(Player player) {
        String rank = "";
        try {
            PermissionEngine pe = PermissionEngine.getInstance();
            if (pe != null) rank = pe.getMineRank(player.getUniqueId());
        } catch (Exception ignored) {}
        if ("Z".equalsIgnoreCase(rank)) {
            PrestigeConfirmGUI.open(player);
        } else {
            PrestigeShopMenuGUI.open(player);
        }
    }

    // ----------------------------------------------------------------
    // Build
    // ----------------------------------------------------------------

    private static Inventory build(Player player) {
        UUID uuid = player.getUniqueId();
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);

        // TopBand fills slots 0-7 with profile/balance/token/multiplier/streak/event/gang info
        TopBand.apply(inv, player);

        // Slot 0: close (overrides TopBand slot 0) — spec: slot 0 always close/back
        inv.setItem(0, Gui.close());

        // Slot 7: rotating tip
        String tip = TIPS[(int) (System.currentTimeMillis() / 30000) % TIPS.length];
        inv.setItem(7, Gui.make(Material.BOOK, "<aqua>Server Tip",
            tip,
            "",
            "<green>→ Tip rotates every 30 seconds."));

        // Slot 8: close
        inv.setItem(8, Gui.make(Material.BARRIER, "<red>✗ Close",
            "<gray>Click to close this menu."));

        // ---- Row 1: core features (slots 10-14) ----
        inv.setItem(10, Gui.make(Material.IRON_PICKAXE, "<aqua>Mines",
            "<gray>✦ Browse all <aqua>26 prison mines<gray>.",
            "<gray>✦ Teleport to your <aqua>current mine<gray>.",
            "",
            "<green>→ Click to open Mine Browser!"));

        inv.setItem(11, Gui.make(Material.NETHERITE_PICKAXE, "<aqua>Your Pickaxe",
            "<gray>✦ View <aqua>stats<gray> and <aqua>enchant details<gray>.",
            "",
            "<green>→ Click to open Pickaxe Home!"));

        inv.setItem(12, Gui.make(Material.ENCHANTED_BOOK, "<aqua>Enchants",
            "<gray>✦ <aqua>Upgrade<gray> your pickaxe enchants.",
            "",
            "<green>→ Click to open Pickaxe Enchants!"));

        inv.setItem(13, Gui.make(Material.TOTEM_OF_UNDYING, "<aqua>Ranks",
            "<gray>✦ View the full <aqua>rank ladder<gray>.",
            "<gray>✦ Purchase your <aqua>next rank<gray>.",
            "",
            "<green>→ Click to open Rank Progression!"));

        // Prestige — live rank + prestige data
        int prestige = 0;
        String rank = "A";
        try {
            PermissionEngine pe = PermissionEngine.getInstance();
            if (pe != null) {
                prestige = pe.getPrestige(uuid);
                rank = pe.getMineRank(uuid);
            }
        } catch (Exception ignored) {}
        String prestigeSubtitle = prestige > 0
            ? "<aqua>✦ <gray>Prestige Level: <white>" + prestige
            : "<aqua>✦ <gray>Prestige at rank <yellow>Z<gray> for <green>permanent bonuses<gray>.";
        String prestigeCta = "Z".equalsIgnoreCase(rank)
            ? "<green>✓ You are eligible to prestige!"
            : "<red>✗ Reach rank <yellow>Z<red> to prestige.";
        inv.setItem(14, Gui.make(Material.NETHER_STAR, "<light_purple>Prestige",
            prestigeSubtitle,
            "",
            prestigeCta,
            "",
            "<green>→ Click to open Prestige!"));

        // ---- Row 2: economy & social (slots 19-26) ----
        inv.setItem(19, Gui.make(Material.CHEST, "<aqua>Shop",
            "<gray>✦ Browse <aqua>categories<gray> and buy items.",
            "",
            "<green>→ Click to open Shop!"));

        inv.setItem(20, Gui.make(Material.EMERALD_BLOCK, "<aqua>Sell Center",
            "<gray>✦ <aqua>Sell<gray> your blocks and track your streak.",
            "",
            "<green>→ Click to open Sell Center!"));

        inv.setItem(21, Gui.make(Material.TRIPWIRE_HOOK, "<aqua>Crates",
            "<gray>✦ Preview tiers and open your <aqua>crate keys<gray>.",
            "",
            "<green>→ Click to open Crates Hub!"));

        inv.setItem(22, Gui.make(Material.GOLD_INGOT, "<aqua>Coinflip",
            "<gray>✦ <green>Wager<gray> $ against other players.",
            "",
            "<green>→ Click to open Coinflip!"));

        inv.setItem(23, Gui.make(Material.WRITABLE_BOOK, "<aqua>Quests",
            "<gray>✦ Daily, weekly, and milestone <aqua>objectives<gray>.",
            "",
            "<green>→ Click to open Quests!"));

        // Gang — live gang name + level
        String gangName = null;
        int gangLevel = -1;
        try {
            GangAPI gapi = GangAPI.getInstance();
            if (gapi != null) {
                gangName = gapi.getGangName(uuid);
                gangLevel = gapi.getGangLevel(uuid);
            }
        } catch (Exception ignored) {}
        ItemStack gangItem;
        if (gangName != null) {
            gangItem = Gui.make(Material.SHIELD, "<aqua>" + gangName,
                "<gray>✦ Level: <yellow>" + gangLevel,
                "<gray>✦ Your active gang.",
                "",
                "<green>→ Click to open Gang Home!");
        } else {
            gangItem = Gui.make(Material.SHIELD, "<gray>No Gang",
                "<gray>✦ Status: <red>✗ None",
                "<gray>✦ You are not in a gang.",
                "",
                "<green>→ Click to create or join a gang!");
        }
        inv.setItem(24, gangItem);

        inv.setItem(25, Gui.make(Material.ARMOR_STAND, "<light_purple>Cosmetics",
            "<gray>✦ <aqua>Chat tags<gray>, effects, and visual upgrades.",
            "",
            "<green>→ Click to open Cosmetics!"));

        inv.setItem(26, Gui.make(Material.COMPARATOR, "<aqua>Settings",
            "<gray>✦ Sounds, notifications, and <aqua>display preferences<gray>.",
            "",
            "<green>→ Click to open Settings!"));

        // ---- Row 3: extras (slots 28-33) ----
        inv.setItem(28, Gui.make(Material.ENDER_CHEST, "<aqua>Auction House",
            "<gray>✦ Browse and list items on the <aqua>player market<gray>.",
            "",
            "<green>→ Click to open Auction House!"));

        inv.setItem(29, Gui.make(Material.COMPASS, "<aqua>Warps",
            "<gray>✦ Teleport to <aqua>spawn<gray>, shop, crates, and more.",
            "",
            "<green>→ Click to open Warps!"));

        inv.setItem(30, Gui.make(Material.CHEST_MINECART, "<aqua>Kits",
            "<gray>✦ Claim available <aqua>starter<gray> and rank kits.",
            "",
            "<green>→ Click to open Kits!"));

        inv.setItem(31, Gui.make(Material.GLOW_ITEM_FRAME, "<aqua>Leaderboards",
            "<gray>✦ Top players by <aqua>balance<gray>, prestige, blocks, and more.",
            "",
            "<green>→ Click to open Leaderboards!"));

        inv.setItem(32, Gui.make(Material.AMETHYST_SHARD, "<light_purple>Black Market",
            "<gray>✦ Rotating <aqua>premium offers<gray> — limited stock.",
            "",
            "<green>→ Click to open Black Market!"));

        // Boosts — live total sell multiplier
        double totalSell = 1.0;
        try {
            EconomyAPI eco = EconomyAPI.getInstance();
            if (eco != null) {
                totalSell = eco.getExternalSellMultiplier(uuid)
                    * eco.getStreakMultiplier(eco.getSellStreak(uuid));
            }
        } catch (Exception ignored) {}
        inv.setItem(33, Gui.make(Material.FIREWORK_STAR, "<aqua>Active Boosts",
            "<gray>✦ Total sell multiplier: <green>" + Fmt.multiplier(totalSell),
            "",
            "<green>→ Click to see full boost breakdown!"));

        return inv;
    }
}
