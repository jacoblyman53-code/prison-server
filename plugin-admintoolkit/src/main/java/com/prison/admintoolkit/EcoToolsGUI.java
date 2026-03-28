package com.prison.admintoolkit;

import com.prison.database.DatabaseManager;
import com.prison.economy.EconomyAPI;
import com.prison.economy.TransactionType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * EcoToolsGUI — GUI for giving, taking, and setting a player's IGC and token balances.
 *
 * Balance operations require the target to be online (EconomyAPI operates on
 * loaded player state). Reading works for offline players via direct DB query.
 */
public class EcoToolsGUI {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private static final Component TITLE = MM.deserialize("<gold><bold>Economy Tools");

    // Admin UUID → target player name
    private static final Map<UUID, String> targetPlayerNames = new ConcurrentHashMap<>();

    // ----------------------------------------------------------------
    // Open (async data load)
    // ----------------------------------------------------------------

    public static void open(Player admin, String targetName) {
        targetPlayerNames.put(admin.getUniqueId(), targetName);

        // Try to find online player first
        Player onlineTarget = findOnlinePlayer(targetName);
        if (onlineTarget != null) {
            long igc    = EconomyAPI.getInstance().getBalance(onlineTarget.getUniqueId());
            long tokens = EconomyAPI.getInstance().getTokens(onlineTarget.getUniqueId());
            renderEcoToolsGUI(admin, targetName, igc, tokens);
            return;
        }

        // Offline player — query DB
        CompletableFuture.supplyAsync(() -> {
            try {
                return DatabaseManager.getInstance().query(
                    "SELECT igc_balance, token_balance FROM players WHERE username = ?",
                    rs -> {
                        if (rs.next()) {
                            return new long[]{ rs.getLong("igc_balance"), rs.getLong("token_balance") };
                        }
                        return new long[]{ 0L, 0L };
                    },
                    targetName
                );
            } catch (Exception e) {
                return new long[]{ 0L, 0L };
            }
        }).thenAccept(balances -> {
            long igc    = balances != null ? balances[0] : 0L;
            long tokens = balances != null ? balances[1] : 0L;
            Bukkit.getScheduler().runTask(AdminToolkitPlugin.getInstance(),
                () -> renderEcoToolsGUI(admin, targetName, igc, tokens));
        });
    }

    // ----------------------------------------------------------------
    // Render
    // ----------------------------------------------------------------

    private static void renderEcoToolsGUI(Player admin, String targetName, long igc, long tokens) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);

        // Fill border
        ItemStack filler = AdminPanel.makeItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++)   inv.setItem(i, filler);
        for (int i = 45; i < 54; i++) inv.setItem(i, filler);
        for (int i = 9; i < 45; i += 9) {
            inv.setItem(i, filler);
            inv.setItem(i + 8, filler);
        }

        // Player head at slot 4
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
        skullMeta.displayName(MM.deserialize("<!italic><gold>" + targetName));
        skullMeta.lore(java.util.List.of(
            MM.deserialize("<!italic><gray>IGC Balance: <white>" + igc),
            MM.deserialize("<!italic><gray>Token Balance: <white>" + tokens)
        ));
        @SuppressWarnings("deprecation")
        OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
        skullMeta.setOwningPlayer(offlineTarget);
        skullMeta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
        head.setItemMeta(skullMeta);
        inv.setItem(4, head);

        // IGC controls
        inv.setItem(18, AdminPanel.makeItem(Material.LIME_STAINED_GLASS_PANE,
            "<green>Give IGC",
            "<gray>Add IGC to " + targetName + "'s balance.",
            "<dark_gray>Click to enter amount."));
        inv.setItem(19, AdminPanel.makeItem(Material.RED_STAINED_GLASS_PANE,
            "<red>Take IGC",
            "<gray>Remove IGC from balance.",
            "<dark_gray>Click to enter amount."));
        inv.setItem(20, AdminPanel.makeItem(Material.YELLOW_STAINED_GLASS_PANE,
            "<yellow>Set IGC",
            "<gray>Force-set IGC balance.",
            "<dark_gray>Click to enter amount."));

        // Token controls
        inv.setItem(24, AdminPanel.makeItem(Material.LIME_STAINED_GLASS_PANE,
            "<green>Give Tokens",
            "<gray>Add tokens to " + targetName + "'s balance."));
        inv.setItem(25, AdminPanel.makeItem(Material.RED_STAINED_GLASS_PANE,
            "<red>Take Tokens",
            "<gray>Remove tokens from balance."));
        inv.setItem(26, AdminPanel.makeItem(Material.YELLOW_STAINED_GLASS_PANE,
            "<yellow>Set Tokens",
            "<gray>Force-set token balance."));

        // Transaction log
        inv.setItem(36, AdminPanel.makeItem(Material.BOOK,
            "<gold>Transaction Log",
            "<gray>Shows last 10 transactions in chat.",
            "<dark_gray>Click to view."));

        // Close
        inv.setItem(45, AdminPanel.makeItem(Material.BARRIER, "<red>Close", "<gray>Close economy tools."));

        admin.openInventory(inv);
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

    public static void handleClick(Player admin, int slot) {
        String targetName = targetPlayerNames.get(admin.getUniqueId());
        if (targetName == null) return;

        Player onlineTarget = findOnlinePlayer(targetName);

        switch (slot) {
            case 18 -> {
                // Give IGC
                requireOnline(admin, onlineTarget, targetName, () ->
                    AnvilInputGUI.open(admin, "0", text -> {
                        try {
                            long amount = Long.parseLong(text.trim());
                            // TODO: add ADMIN_GIVE transaction type
                            EconomyAPI.getInstance().addBalance(onlineTarget.getUniqueId(), amount, TransactionType.MINE_SELL);
                            admin.sendMessage(MM.deserialize("<green>Gave " + amount + " IGC to " + targetName + "."));
                        } catch (NumberFormatException e) {
                            admin.sendMessage(MM.deserialize("<red>Invalid amount: " + text));
                        }
                        refreshGui(admin, targetName);
                    })
                );
            }
            case 19 -> {
                // Take IGC
                requireOnline(admin, onlineTarget, targetName, () ->
                    AnvilInputGUI.open(admin, "0", text -> {
                        try {
                            long amount = Long.parseLong(text.trim());
                            // TODO: add ADMIN_TAKE transaction type
                            EconomyAPI.getInstance().deductBalance(onlineTarget.getUniqueId(), amount, TransactionType.MINE_SELL);
                            admin.sendMessage(MM.deserialize("<green>Removed " + amount + " IGC from " + targetName + "."));
                        } catch (NumberFormatException e) {
                            admin.sendMessage(MM.deserialize("<red>Invalid amount: " + text));
                        }
                        refreshGui(admin, targetName);
                    })
                );
            }
            case 20 -> {
                // Set IGC
                requireOnline(admin, onlineTarget, targetName, () ->
                    AnvilInputGUI.open(admin, "0", text -> {
                        try {
                            long amount = Long.parseLong(text.trim());
                            // TODO: add ADMIN_SET transaction type
                            EconomyAPI.getInstance().setBalance(onlineTarget.getUniqueId(), amount, TransactionType.MINE_SELL);
                            admin.sendMessage(MM.deserialize("<green>Set " + targetName + "'s IGC to " + amount + "."));
                        } catch (NumberFormatException e) {
                            admin.sendMessage(MM.deserialize("<red>Invalid amount: " + text));
                        }
                        refreshGui(admin, targetName);
                    })
                );
            }
            case 24 -> {
                // Give Tokens
                requireOnline(admin, onlineTarget, targetName, () ->
                    AnvilInputGUI.open(admin, "0", text -> {
                        try {
                            long amount = Long.parseLong(text.trim());
                            // TODO: add ADMIN_GIVE transaction type
                            EconomyAPI.getInstance().addTokens(onlineTarget.getUniqueId(), amount, TransactionType.MINE_SELL);
                            admin.sendMessage(MM.deserialize("<green>Gave " + amount + " tokens to " + targetName + "."));
                        } catch (NumberFormatException e) {
                            admin.sendMessage(MM.deserialize("<red>Invalid amount: " + text));
                        }
                        refreshGui(admin, targetName);
                    })
                );
            }
            case 25 -> {
                // Take Tokens
                requireOnline(admin, onlineTarget, targetName, () ->
                    AnvilInputGUI.open(admin, "0", text -> {
                        try {
                            long amount = Long.parseLong(text.trim());
                            // TODO: add ADMIN_TAKE transaction type
                            EconomyAPI.getInstance().deductTokens(onlineTarget.getUniqueId(), amount, TransactionType.MINE_SELL);
                            admin.sendMessage(MM.deserialize("<green>Removed " + amount + " tokens from " + targetName + "."));
                        } catch (NumberFormatException e) {
                            admin.sendMessage(MM.deserialize("<red>Invalid amount: " + text));
                        }
                        refreshGui(admin, targetName);
                    })
                );
            }
            case 26 -> {
                // Set Tokens
                requireOnline(admin, onlineTarget, targetName, () ->
                    AnvilInputGUI.open(admin, "0", text -> {
                        try {
                            long amount = Long.parseLong(text.trim());
                            // TODO: add ADMIN_SET transaction type
                            EconomyAPI.getInstance().setTokens(onlineTarget.getUniqueId(), amount, TransactionType.MINE_SELL);
                            admin.sendMessage(MM.deserialize("<green>Set " + targetName + "'s tokens to " + amount + "."));
                        } catch (NumberFormatException e) {
                            admin.sendMessage(MM.deserialize("<red>Invalid amount: " + text));
                        }
                        refreshGui(admin, targetName);
                    })
                );
            }
            case 36 -> {
                // Transaction log
                admin.closeInventory();
                showTransactionLog(admin, targetName);
            }
            case 45 -> {
                targetPlayerNames.remove(admin.getUniqueId());
                admin.closeInventory();
            }
        }
    }

    // ----------------------------------------------------------------
    // Transaction log
    // ----------------------------------------------------------------

    private static void showTransactionLog(Player admin, String targetName) {
        CompletableFuture.supplyAsync(() -> {
            try {
                return EconomyAPI.getInstance().getRecentTransactions(
                    getUUIDForName(targetName), "IGC", 10);
            } catch (Exception e) {
                return java.util.Collections.emptyList();
            }
        }).thenAccept(transactions -> Bukkit.getScheduler().runTask(AdminToolkitPlugin.getInstance(), () -> {
            admin.sendMessage(MM.deserialize("<gold>=== Last 10 IGC Transactions for " + targetName + " ==="));
            if (transactions.isEmpty()) {
                admin.sendMessage(MM.deserialize("<gray>No transactions found."));
                return;
            }
            for (Object tx : transactions) {
                admin.sendMessage(MM.deserialize("<gray>" + tx.toString()));
            }
        }));
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    static Player findOnlinePlayer(String name) {
        Collection<? extends Player> online = Bukkit.getOnlinePlayers();
        for (Player p : online) {
            if (p.getName().equalsIgnoreCase(name)) return p;
        }
        return null;
    }

    private static void requireOnline(Player admin, Player target, String targetName, Runnable action) {
        if (target == null) {
            admin.sendMessage(MM.deserialize("<red>" + targetName + " must be online to adjust balance."));
            return;
        }
        action.run();
    }

    private static void refreshGui(Player admin, String targetName) {
        Bukkit.getScheduler().runTask(AdminToolkitPlugin.getInstance(), () -> open(admin, targetName));
    }

    @SuppressWarnings("deprecation")
    private static UUID getUUIDForName(String name) {
        Player online = findOnlinePlayer(name);
        if (online != null) return online.getUniqueId();
        return Bukkit.getOfflinePlayer(name).getUniqueId();
    }
}
