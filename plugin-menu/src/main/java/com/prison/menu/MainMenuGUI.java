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

    public static final Component TITLE = MiniMessage.miniMessage().deserialize("<!italic><dark_gray>[ <gold>Prison Menu <dark_gray>]");
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private static final String[] TIPS = {
        "<gray>Use <white>/sellall <gray>or open <white>Sell Center <gray>to cash in your blocks.",
        "<gray>Upgrade your pickaxe with <white>tokens <gray>to mine faster.",
        "<gray>Join a gang for passive sell and token bonuses.",
        "<gray>Prestige at rank Z to earn permanent bonuses.",
        "<gray>Check <white>Black Market <gray>daily for limited-time deals.",
        "<gray>Complete quests daily to earn extra IGC and tokens.",
        "<gray>Use <white>/coinflip <gray>to wager IGC against other players.",
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
        Gui.fillAll(inv);

        // TopBand fills slots 0-7 with profile/balance/token/multiplier/streak/event/gang/tips info
        TopBand.apply(inv, player);

        // Slot 7: rotating tip (overwrites whatever TopBand placed there)
        String tip = TIPS[(int) (System.currentTimeMillis() / 30000) % TIPS.length];
        inv.setItem(7, Gui.make(Material.BOOK, "<yellow>Server Tip", tip));

        // Slot 8: close
        inv.setItem(8, Gui.close());

        // ---- Row 1: core features (slots 10-14) ----
        inv.setItem(10, Gui.make(Material.IRON_PICKAXE, "<green>Mines",
            "<gray>Browse all 26 prison mines.",
            "<gray>Teleport to your current mine.",
            "",
            "<green>Click to open Mine Browser."));

        inv.setItem(11, Gui.make(Material.NETHERITE_PICKAXE, "<gold>Your Pickaxe",
            "<gray>View stats and enchant details.",
            "",
            "<green>Click to open Pickaxe Home."));

        inv.setItem(12, Gui.make(Material.ENCHANTED_BOOK, "<aqua>Enchants",
            "<gray>Upgrade your pickaxe enchants.",
            "",
            "<green>Click to open Pickaxe Enchants."));

        inv.setItem(13, Gui.make(Material.TOTEM_OF_UNDYING, "<yellow>Ranks",
            "<gray>View the full rank ladder.",
            "<gray>Purchase your next rank.",
            "",
            "<green>Click to open Rank Progression."));

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
            ? "<light_purple>Prestige Level: \u2746" + prestige
            : "<gray>Prestige at rank Z for permanent bonuses.";
        String prestigeCta = "Z".equalsIgnoreCase(rank)
            ? "<green>You are eligible to prestige!"
            : "<gray>Reach rank Z to prestige.";
        inv.setItem(14, Gui.make(Material.NETHER_STAR, "<light_purple>Prestige",
            prestigeSubtitle,
            "",
            prestigeCta,
            "",
            "<green>Click to open Prestige."));

        // ---- Row 2: economy & social (slots 19-26) ----
        inv.setItem(19, Gui.make(Material.CHEST, "<yellow>Shop",
            "<gray>Browse categories and buy items with IGC.",
            "",
            "<green>Click to open Shop."));

        inv.setItem(20, Gui.make(Material.EMERALD_BLOCK, "<green>Sell Center",
            "<gray>Sell your blocks and track your streak.",
            "",
            "<green>Click to open Sell Center."));

        inv.setItem(21, Gui.make(Material.TRIPWIRE_HOOK, "<gold>Crates",
            "<gray>Preview tiers and open your crate keys.",
            "",
            "<green>Click to open Crates Hub."));

        inv.setItem(22, Gui.make(Material.GOLD_INGOT, "<yellow>Coinflip",
            "<gray>Wager IGC against other players.",
            "",
            "<green>Click to open Coinflip."));

        inv.setItem(23, Gui.make(Material.WRITABLE_BOOK, "<aqua>Quests",
            "<gray>Daily, weekly, and milestone objectives.",
            "",
            "<green>Click to open Quests."));

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
            gangItem = Gui.make(Material.SHIELD, "<green>" + gangName + " <dark_gray>(Lv." + gangLevel + ")",
                "<gray>Your gang.",
                "",
                "<green>Click to open Gang Home.");
        } else {
            gangItem = Gui.make(Material.SHIELD, "<gray>No Gang",
                "<dark_gray>You are not in a gang.",
                "",
                "<yellow>Click to create or join one.");
        }
        inv.setItem(24, gangItem);

        inv.setItem(25, Gui.make(Material.ARMOR_STAND, "<light_purple>Cosmetics",
            "<gray>Chat tags, effects, and visual upgrades.",
            "",
            "<green>Click to open Cosmetics."));

        inv.setItem(26, Gui.make(Material.COMPARATOR, "<gray>Settings",
            "<gray>Sounds, notifications, and display preferences.",
            "",
            "<green>Click to open Settings."));

        // ---- Row 3: extras (slots 28-33) ----
        inv.setItem(28, Gui.make(Material.ENDER_CHEST, "<yellow>Auction House",
            "<gray>Browse and list items on the player market.",
            "",
            "<green>Click to open Auction House."));

        inv.setItem(29, Gui.make(Material.COMPASS, "<white>Warps",
            "<gray>Teleport to spawn, shop, crates, and more.",
            "",
            "<green>Click to open Warps."));

        inv.setItem(30, Gui.make(Material.CHEST_MINECART, "<green>Kits",
            "<gray>Claim available starter and rank kits.",
            "",
            "<green>Click to open Kits."));

        inv.setItem(31, Gui.make(Material.GLOW_ITEM_FRAME, "<aqua>Leaderboards",
            "<gray>Top players by balance, prestige, blocks, and more.",
            "",
            "<green>Click to open Leaderboards."));

        inv.setItem(32, Gui.make(Material.AMETHYST_SHARD, "<light_purple>Black Market",
            "<gray>Rotating premium offers — limited stock.",
            "",
            "<green>Click to open Black Market."));

        // Boosts — live total sell multiplier
        double totalSell = 1.0;
        try {
            EconomyAPI eco = EconomyAPI.getInstance();
            if (eco != null) {
                totalSell = eco.getExternalSellMultiplier(uuid)
                    * eco.getStreakMultiplier(eco.getSellStreak(uuid));
            }
        } catch (Exception ignored) {}
        inv.setItem(33, Gui.make(Material.FIREWORK_STAR, "<green>Active Boosts",
            "<gray>Total sell multiplier: <green>" + Fmt.multiplier(totalSell),
            "",
            "<green>Click to see full boost breakdown."));

        return inv;
    }
}
