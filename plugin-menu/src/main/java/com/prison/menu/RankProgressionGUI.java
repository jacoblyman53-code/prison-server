package com.prison.menu;

import com.prison.economy.EconomyAPI;
import com.prison.menu.util.*;
import com.prison.permissions.PermissionEngine;
import com.prison.ranks.RankConfig;
import com.prison.ranks.RankManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RankProgressionGUI {

    public static final Component TITLE = MiniMessage.miniMessage().deserialize("<!italic><dark_gray>[ <yellow>Rank Progression <dark_gray>]");
    private static final MiniMessage MM = MiniMessage.miniMessage();

    // Rank slots A-Z in order, avoiding slots 4, 13, 22, 31, 49
    // Row 1 (9-17):  10,11,12, 14,15,16,17  (7 slots, skip 13)
    // Row 2 (18-26): 19,20,21, 23,24,25,26  (7 slots, skip 22)
    // Row 3 (27-35): 28,29,30, 32,33,34,35  (7 slots, skip 31)
    // Row 4 (36-44): 37,38,39,40,41         (5 slots)
    private static final int[] RANK_SLOTS = {
        10, 11, 12,     14, 15, 16, 17,   // A-G
        19, 20, 21,     23, 24, 25, 26,   // H-N
        28, 29, 30,     32, 33, 34, 35,   // O-U
        37, 38, 39, 40, 41                // V-Z
    };

    private static final int SLOT_CURRENT_RANK = 4;
    private static final int SLOT_RANKUP        = 13;
    private static final int SLOT_RANKUP_MAX    = 22;
    private static final int SLOT_AUTOTELEPORT  = 31;
    private static final int SLOT_BACK          = 49;

    public static void open(Player player) {
        player.openInventory(build(player));
    }

    public static void handleClick(Player player, int slot, MenuPlugin plugin) {
        UUID uuid = player.getUniqueId();

        if (slot == 8 || slot == SLOT_BACK) {
            Sounds.nav(player);
            MainMenuGUI.open(player);
            return;
        }

        if (slot == SLOT_RANKUP) {
            handleRankup(player, plugin);
            return;
        }

        if (slot == SLOT_RANKUP_MAX) {
            handleRankupMax(player, plugin);
            return;
        }

        if (slot == SLOT_AUTOTELEPORT) {
            handleToggleAutoteleport(player, plugin);
            return;
        }

        // Rank slot click — only the next rank slot is actionable
        for (int i = 0; i < RANK_SLOTS.length; i++) {
            if (RANK_SLOTS[i] == slot) {
                String playerRank = getPlayerRank(uuid);
                int playerIdx = rankIndex(playerRank);
                if (i == playerIdx + 1) {
                    handleRankup(player, plugin);
                }
                return;
            }
        }
    }

    private static void handleRankup(Player player, MenuPlugin plugin) {
        RankManager rm = RankManager.getInstance();
        if (rm == null) {
            Sounds.deny(player);
            player.sendMessage(MM.deserialize("<red>Rank system unavailable."));
            return;
        }
        Bukkit.dispatchCommand(player, "rankup");
        Sounds.upgrade(player);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && TITLE.equals(player.getOpenInventory().title())) {
                player.openInventory(build(player));
            }
        }, 2L);
    }

    private static void handleRankupMax(Player player, MenuPlugin plugin) {
        Bukkit.dispatchCommand(player, "rankup max");
        Sounds.upgrade(player);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && TITLE.equals(player.getOpenInventory().title())) {
                player.openInventory(build(player));
            }
        }, 2L);
    }

    private static void handleToggleAutoteleport(Player player, MenuPlugin plugin) {
        RankManager rm = RankManager.getInstance();
        if (rm != null) {
            rm.toggleAutoteleport(player.getUniqueId());
        }
        Sounds.nav(player);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && TITLE.equals(player.getOpenInventory().title())) {
                player.openInventory(build(player));
            }
        }, 2L);
    }

    // ----------------------------------------------------------------
    // Build
    // ----------------------------------------------------------------

    private static Inventory build(Player player) {
        UUID uuid = player.getUniqueId();
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        Gui.fillAll(inv);
        TopBand.apply(inv, player);
        inv.setItem(8, Gui.back());

        RankManager rm  = RankManager.getInstance();
        RankConfig  rc  = rm != null ? rm.getConfig() : null;
        EconomyAPI  eco = EconomyAPI.getInstance();

        String playerRank = getPlayerRank(uuid);
        int    playerIdx  = rankIndex(playerRank);
        long   balance    = eco != null ? eco.getBalance(uuid) : 0L;

        // --- Slot 13: Rank Up CTA ---
        if (playerIdx < 25) {
            String nextRank = RankConfig.RANK_ORDER[playerIdx + 1];
            long   nextCost = rc != null && rc.getRank(nextRank) != null ? rc.getRank(nextRank).cost() : 0L;
            String nextDisplay = rc != null && rc.getRank(nextRank) != null ? rc.getRank(nextRank).display() : "Rank " + nextRank;
            boolean canAfford  = balance >= nextCost;

            if (canAfford) {
                inv.setItem(SLOT_RANKUP, Gui.make(Material.TOTEM_OF_UNDYING, "<green>Rank Up \u2192 " + nextRank,
                    "<gray>Next rank: " + nextDisplay,
                    "<gray>Cost: <gold>$" + Fmt.number(nextCost),
                    "<gray>Your balance: <gold>$" + Fmt.number(balance),
                    "",
                    "<green>Click to rank up!"));
            } else {
                long shortfall = nextCost - balance;
                inv.setItem(SLOT_RANKUP, Gui.make(Material.TOTEM_OF_UNDYING, "<red>Rank Up \u2192 " + nextRank,
                    "<gray>Next rank: " + nextDisplay,
                    "<gray>Cost: <gold>" + Fmt.number(nextCost) + " IGC",
                    "<red>Need <gold>$" + Fmt.number(shortfall) + "<red> more.",
                    "",
                    "<red>Cannot afford yet."));
            }
        } else {
            inv.setItem(SLOT_RANKUP, Gui.make(Material.NETHER_STAR, "<gold>Maximum Rank!",
                "<gray>You have reached rank Z.",
                "",
                "<light_purple>Consider prestiging for permanent bonuses!"));
        }

        // --- Slot 22: Rank Up Max CTA ---
        if (playerIdx < 25) {
            long totalCost = 0;
            int  finalIdx  = playerIdx;
            if (rc != null) {
                for (int i = playerIdx + 1; i < RankConfig.RANK_ORDER.length; i++) {
                    RankConfig.RankData data = rc.getRank(RankConfig.RANK_ORDER[i]);
                    long c = data != null ? data.cost() : 0L;
                    if (balance >= totalCost + c) {
                        totalCost += c;
                        finalIdx = i;
                    } else {
                        break;
                    }
                }
            }
            boolean anyPurchasable = finalIdx > playerIdx;
            if (anyPurchasable) {
                String resultRank = RankConfig.RANK_ORDER[finalIdx];
                inv.setItem(SLOT_RANKUP_MAX, Gui.make(Material.NETHER_STAR, "<green>Rank Up Max",
                    "<gray>Bulk purchase all affordable ranks.",
                    "<gray>Result rank: <white>" + resultRank,
                    "<gray>Total cost: <gold>$" + Fmt.number(totalCost),
                    "",
                    "<green>Click to rank up as far as possible."));
            } else {
                inv.setItem(SLOT_RANKUP_MAX, Gui.make(Material.NETHER_STAR, "<gray>Rank Up Max",
                    "<gray>No additional ranks affordable.",
                    "<dark_gray>Earn more $ to rank up."));
            }
        } else {
            inv.setItem(SLOT_RANKUP_MAX, Gui.make(Material.NETHER_STAR, "<gold>Maximum Rank!",
                "<gray>You have already reached rank Z."));
        }

        // --- Slot 31: Auto-Teleport Toggle ---
        boolean autoTp = rm != null && rm.getAutoteleport(uuid);
        if (autoTp) {
            inv.setItem(SLOT_AUTOTELEPORT, Gui.make(Material.COMPASS, "<green>Auto Teleport: ON",
                "<gray>Automatically teleport to your new mine",
                "<gray>when you rank up.",
                "",
                "<green>Currently enabled. Click to disable."));
        } else {
            inv.setItem(SLOT_AUTOTELEPORT, Gui.make(Material.COMPASS, "<red>Auto Teleport: OFF",
                "<gray>Automatically teleport to your new mine",
                "<gray>when you rank up.",
                "",
                "<red>Currently disabled. Click to enable."));
        }

        // --- Rank Ladder Items ---
        for (int i = 0; i < RANK_SLOTS.length && i < RankConfig.RANK_ORDER.length; i++) {
            String rankId = RankConfig.RANK_ORDER[i];
            RankConfig.RankData data = rc != null ? rc.getRank(rankId) : null;
            long   cost    = data != null ? data.cost() : 0L;
            String display = data != null ? data.display() : "Rank " + rankId;
            String prefix  = data != null ? data.prefix() : rankId;

            Material mat;
            String nameTag;
            List<Component> lore = new ArrayList<>();

            if (i < playerIdx) {
                // Past rank — purchased
                mat     = Material.GREEN_STAINED_GLASS_PANE;
                nameTag = "<green>\u2714 Rank " + rankId;
                lore.add(MM.deserialize("<!italic><green>Rank purchased."));
            } else if (i == playerIdx) {
                // Current rank
                mat     = Material.GOLD_BLOCK;
                nameTag = "<gold><bold>\u25b6 Rank " + rankId + " (CURRENT)";
                lore.add(MM.deserialize("<!italic><gold><bold>CURRENT RANK"));
                lore.add(Component.empty());
                lore.add(MM.deserialize("<!italic><gray>Display: " + display));
                lore.add(MM.deserialize("<!italic><gray>Prefix: " + prefix));
            } else if (i == playerIdx + 1) {
                // Next rank — may or may not be affordable
                boolean affordable = balance >= cost;
                mat     = affordable ? Material.YELLOW_STAINED_GLASS_PANE : Material.ORANGE_STAINED_GLASS_PANE;
                nameTag = affordable ? "<yellow>Rank " + rankId : "<gold>Rank " + rankId;
                lore.add(MM.deserialize("<!italic><gray>Cost: <gold>$" + Fmt.number(cost)));
                if (affordable) {
                    lore.add(MM.deserialize("<!italic><green>Click to purchase next rank!"));
                } else {
                    lore.add(MM.deserialize("<!italic><red>Need <gold>$" + Fmt.number(cost - balance) + "<red> more."));
                }
            } else {
                // Future locked rank
                mat     = Material.RED_STAINED_GLASS_PANE;
                nameTag = "<red>Rank " + rankId;
                lore.add(MM.deserialize("<!italic><gray>Cost: <gold>$" + Fmt.number(cost)));
                lore.add(MM.deserialize("<!italic><red>Locked \u2014 rank up progressively."));
            }

            inv.setItem(RANK_SLOTS[i], Gui.make(mat, nameTag, lore));
        }

        inv.setItem(SLOT_BACK, Gui.back());
        return inv;
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private static String getPlayerRank(UUID uuid) {
        try {
            PermissionEngine pe = PermissionEngine.getInstance();
            if (pe != null) return pe.getMineRank(uuid);
        } catch (Exception ignored) {}
        return "A";
    }

    private static int rankIndex(String rank) {
        String upper = rank.toUpperCase();
        for (int i = 0; i < RankConfig.RANK_ORDER.length; i++) {
            if (RankConfig.RANK_ORDER[i].equals(upper)) return i;
        }
        return 0;
    }
}
