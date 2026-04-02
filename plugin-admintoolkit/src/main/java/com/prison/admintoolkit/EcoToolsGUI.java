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
 * All balance operations work for both online and offline players.
 * Online players use the in-memory wallet; offline players use direct DB queries.
 */
public class EcoToolsGUI {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private static final Component TITLE = MM.deserialize("ECO TOOLS");

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
            renderEcoToolsGUI(admin, targetName, igc, tokens, false);
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
                () -> renderEcoToolsGUI(admin, targetName, igc, tokens, true));
        });
    }

    // ----------------------------------------------------------------
    // Render
    // ----------------------------------------------------------------

    private static void renderEcoToolsGUI(Player admin, String targetName, long igc, long tokens, boolean offline) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);

        // Player head at slot 4
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
        skullMeta.displayName(MM.deserialize("<!italic><yellow>" + targetName + (offline ? " <red>(offline)" : "")));
        skullMeta.lore(java.util.List.of(
            MM.deserialize("<!italic><aqua>✦ <gray>IGC Balance: <white>" + igc),
            MM.deserialize("<!italic><aqua>✦ <gray>Token Balance: <white>" + tokens),
            MM.deserialize("<!italic>"),
            MM.deserialize("<!italic><aqua>✦ <gray>Status: " + (offline ? "<red>✗ Offline" : "<green>✓ Online"))
        ));
        @SuppressWarnings("deprecation")
        OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
        skullMeta.setOwningPlayer(offlineTarget);
        skullMeta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
        head.setItemMeta(skullMeta);
        inv.setItem(4, head);

        // Offline warning — slot 22 (center of content area)
        if (offline) {
            inv.setItem(22, AdminPanel.makeItem(Material.ORANGE_STAINED_GLASS_PANE,
                "<gold>✦ Offline Player",
                "<aqua>✦ <yellow>" + targetName + " <gray>is not currently online.",
                "",
                "<gray>Balance changes write directly to the <green>database<gray>.",
                "<gray>The new value takes effect on their next <green>login<gray>.",
                "",
                "<red>✗ DB value shown above may be cached; reload to refresh."));
        }

        // Offline note appended to each button's lore
        String offlineNote = offline ? "<gold>$ Writes directly to DB — takes effect on login." : null;

        // IGC controls
        inv.setItem(18, offlineNote != null
            ? AdminPanel.makeItem(Material.LIME_DYE,
                "<aqua>Give IGC",
                "<gray>Add <green>IGC<gray> to <yellow>" + targetName + "<gray>'s balance.",
                "",
                offlineNote,
                "",
                "<green>→ <green>Click to <green><underlined>give</underlined> IGC!")
            : AdminPanel.makeItem(Material.LIME_DYE,
                "<aqua>Give IGC",
                "<gray>Add <green>IGC<gray> to <yellow>" + targetName + "<gray>'s balance.",
                "",
                "<green>→ <green>Click to <green><underlined>give</underlined> IGC!"));
        inv.setItem(19, offlineNote != null
            ? AdminPanel.makeItem(Material.RED_DYE,
                "<aqua>Take IGC",
                "<gray>Remove <green>IGC<gray> from <yellow>" + targetName + "<gray>'s balance.",
                "",
                offlineNote,
                "",
                "<green>→ <green>Click to <green><underlined>take</underlined> IGC!")
            : AdminPanel.makeItem(Material.RED_DYE,
                "<aqua>Take IGC",
                "<gray>Remove <green>IGC<gray> from <yellow>" + targetName + "<gray>'s balance.",
                "<red>✗ Fails if balance is insufficient.",
                "",
                "<green>→ <green>Click to <green><underlined>take</underlined> IGC!"));
        inv.setItem(20, offlineNote != null
            ? AdminPanel.makeItem(Material.YELLOW_DYE,
                "<aqua>Set IGC",
                "<gray>Force-set <yellow>" + targetName + "<gray>'s <green>IGC balance<gray>.",
                "",
                offlineNote,
                "",
                "<green>→ <green>Click to <green><underlined>set</underlined> IGC!")
            : AdminPanel.makeItem(Material.YELLOW_DYE,
                "<aqua>Set IGC",
                "<gray>Force-set <yellow>" + targetName + "<gray>'s <green>IGC balance<gray>.",
                "<gray>Overwrites current value.",
                "",
                "<green>→ <green>Click to <green><underlined>set</underlined> IGC!"));

        // Token controls
        inv.setItem(24, offlineNote != null
            ? AdminPanel.makeItem(Material.LIME_DYE,
                "<aqua>Give Tokens",
                "<gray>Add <green>tokens<gray> to <yellow>" + targetName + "<gray>'s balance.",
                "",
                offlineNote,
                "",
                "<green>→ <green>Click to <green><underlined>give</underlined> tokens!")
            : AdminPanel.makeItem(Material.LIME_DYE,
                "<aqua>Give Tokens",
                "<gray>Add <green>tokens<gray> to <yellow>" + targetName + "<gray>'s balance.",
                "",
                "<green>→ <green>Click to <green><underlined>give</underlined> tokens!"));
        inv.setItem(25, offlineNote != null
            ? AdminPanel.makeItem(Material.RED_DYE,
                "<aqua>Take Tokens",
                "<gray>Remove <green>tokens<gray> from <yellow>" + targetName + "<gray>'s balance.",
                "",
                offlineNote,
                "",
                "<green>→ <green>Click to <green><underlined>take</underlined> tokens!")
            : AdminPanel.makeItem(Material.RED_DYE,
                "<aqua>Take Tokens",
                "<gray>Remove <green>tokens<gray> from <yellow>" + targetName + "<gray>'s balance.",
                "<red>✗ Fails if balance is insufficient.",
                "",
                "<green>→ <green>Click to <green><underlined>take</underlined> tokens!"));
        inv.setItem(26, offlineNote != null
            ? AdminPanel.makeItem(Material.YELLOW_DYE,
                "<aqua>Set Tokens",
                "<gray>Force-set <yellow>" + targetName + "<gray>'s <green>token balance<gray>.",
                "",
                offlineNote,
                "",
                "<green>→ <green>Click to <green><underlined>set</underlined> tokens!")
            : AdminPanel.makeItem(Material.YELLOW_DYE,
                "<aqua>Set Tokens",
                "<gray>Force-set <yellow>" + targetName + "<gray>'s <green>token balance<gray>.",
                "<gray>Overwrites current value.",
                "",
                "<green>→ <green>Click to <green><underlined>set</underlined> tokens!"));

        // Transaction log
        inv.setItem(36, AdminPanel.makeItem(Material.BOOK,
            "<aqua>Transaction Log",
            "<gray>Shows last <green>10 transactions<gray> in chat.",
            "",
            "<green>→ <green>Click to <green><underlined>view</underlined> transaction log!"));

        // Close
        inv.setItem(45, AdminPanel.makeItem(Material.BARRIER, "<red>✗ Close", "<gray>Click to close this menu."));

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

        UUID targetUuid = getUUIDForName(targetName);

        boolean offline = findOnlinePlayer(targetName) == null;

        switch (slot) {
            case 18 -> {
                // Give IGC — works for online and offline players
                AnvilInputGUI.open(admin, "0", text -> {
                    try {
                        long amount = Long.parseLong(text.trim());
                        if (amount <= 0) { admin.sendMessage(MM.deserialize("<red>Amount must be positive.")); return; }
                        if (offline) admin.sendMessage(MM.deserialize("<gold>⚠ " + targetName + " is offline — change writes to DB and takes effect on login."));
                        EconomyAPI.getInstance().addBalanceAsync(targetUuid, amount, TransactionType.ADMIN_ADD)
                            .thenAccept(newBal -> Bukkit.getScheduler().runTask(AdminToolkitPlugin.getInstance(), () -> {
                                admin.sendMessage(MM.deserialize("<green>Gave <white>" + amount + " IGC</white> to " + targetName + "."));
                                logStaffAction(admin.getUniqueId(), targetUuid, "IGC", "give", amount, newBal);
                                refreshGui(admin, targetName);
                            }));
                    } catch (NumberFormatException e) {
                        admin.sendMessage(MM.deserialize("<red>Invalid amount: " + text));
                    }
                });
            }
            case 19 -> {
                // Take IGC
                AnvilInputGUI.open(admin, "0", text -> {
                    try {
                        long amount = Long.parseLong(text.trim());
                        if (amount <= 0) { admin.sendMessage(MM.deserialize("<red>Amount must be positive.")); return; }
                        if (offline) admin.sendMessage(MM.deserialize("<gold>⚠ " + targetName + " is offline — change writes to DB and takes effect on login."));
                        EconomyAPI.getInstance().deductBalanceAsync(targetUuid, amount, TransactionType.ADMIN_REMOVE)
                            .thenAccept(newBal -> Bukkit.getScheduler().runTask(AdminToolkitPlugin.getInstance(), () -> {
                                if (newBal < 0) {
                                    admin.sendMessage(MM.deserialize("<red>" + targetName + " has insufficient funds."));
                                } else {
                                    admin.sendMessage(MM.deserialize("<green>Removed <white>" + amount + " IGC</white> from " + targetName + "."));
                                    logStaffAction(admin.getUniqueId(), targetUuid, "IGC", "take", amount, newBal);
                                }
                                refreshGui(admin, targetName);
                            }));
                    } catch (NumberFormatException e) {
                        admin.sendMessage(MM.deserialize("<red>Invalid amount: " + text));
                    }
                });
            }
            case 20 -> {
                // Set IGC
                AnvilInputGUI.open(admin, "0", text -> {
                    try {
                        long amount = Long.parseLong(text.trim());
                        if (amount < 0) { admin.sendMessage(MM.deserialize("<red>Amount cannot be negative.")); return; }
                        if (offline) admin.sendMessage(MM.deserialize("<gold>⚠ " + targetName + " is offline — change writes to DB and takes effect on login."));
                        EconomyAPI.getInstance().setBalanceAsync(targetUuid, amount, TransactionType.ADMIN_SET)
                            .thenRun(() -> Bukkit.getScheduler().runTask(AdminToolkitPlugin.getInstance(), () -> {
                                admin.sendMessage(MM.deserialize("<green>Set " + targetName + "'s IGC to <white>" + amount + "</white>."));
                                logStaffAction(admin.getUniqueId(), targetUuid, "IGC", "set", amount, amount);
                                refreshGui(admin, targetName);
                            }));
                    } catch (NumberFormatException e) {
                        admin.sendMessage(MM.deserialize("<red>Invalid amount: " + text));
                    }
                });
            }
            case 24 -> {
                // Give Tokens
                AnvilInputGUI.open(admin, "0", text -> {
                    try {
                        long amount = Long.parseLong(text.trim());
                        if (amount <= 0) { admin.sendMessage(MM.deserialize("<red>Amount must be positive.")); return; }
                        if (offline) admin.sendMessage(MM.deserialize("<gold>⚠ " + targetName + " is offline — change writes to DB and takes effect on login."));
                        EconomyAPI.getInstance().addTokensAsync(targetUuid, amount, TransactionType.ADMIN_ADD)
                            .thenAccept(newBal -> Bukkit.getScheduler().runTask(AdminToolkitPlugin.getInstance(), () -> {
                                admin.sendMessage(MM.deserialize("<green>Gave <white>" + amount + " tokens</white> to " + targetName + "."));
                                logStaffAction(admin.getUniqueId(), targetUuid, "TOKEN", "give", amount, newBal);
                                refreshGui(admin, targetName);
                            }));
                    } catch (NumberFormatException e) {
                        admin.sendMessage(MM.deserialize("<red>Invalid amount: " + text));
                    }
                });
            }
            case 25 -> {
                // Take Tokens
                AnvilInputGUI.open(admin, "0", text -> {
                    try {
                        long amount = Long.parseLong(text.trim());
                        if (amount <= 0) { admin.sendMessage(MM.deserialize("<red>Amount must be positive.")); return; }
                        if (offline) admin.sendMessage(MM.deserialize("<gold>⚠ " + targetName + " is offline — change writes to DB and takes effect on login."));
                        EconomyAPI.getInstance().deductTokensAsync(targetUuid, amount, TransactionType.ADMIN_REMOVE)
                            .thenAccept(newBal -> Bukkit.getScheduler().runTask(AdminToolkitPlugin.getInstance(), () -> {
                                if (newBal < 0) {
                                    admin.sendMessage(MM.deserialize("<red>" + targetName + " has insufficient tokens."));
                                } else {
                                    admin.sendMessage(MM.deserialize("<green>Removed <white>" + amount + " tokens</white> from " + targetName + "."));
                                    logStaffAction(admin.getUniqueId(), targetUuid, "TOKEN", "take", amount, newBal);
                                }
                                refreshGui(admin, targetName);
                            }));
                    } catch (NumberFormatException e) {
                        admin.sendMessage(MM.deserialize("<red>Invalid amount: " + text));
                    }
                });
            }
            case 26 -> {
                // Set Tokens
                AnvilInputGUI.open(admin, "0", text -> {
                    try {
                        long amount = Long.parseLong(text.trim());
                        if (amount < 0) { admin.sendMessage(MM.deserialize("<red>Amount cannot be negative.")); return; }
                        if (offline) admin.sendMessage(MM.deserialize("<gold>⚠ " + targetName + " is offline — change writes to DB and takes effect on login."));
                        EconomyAPI.getInstance().setTokensAsync(targetUuid, amount, TransactionType.ADMIN_SET)
                            .thenRun(() -> Bukkit.getScheduler().runTask(AdminToolkitPlugin.getInstance(), () -> {
                                admin.sendMessage(MM.deserialize("<green>Set " + targetName + "'s tokens to <white>" + amount + "</white>."));
                                logStaffAction(admin.getUniqueId(), targetUuid, "TOKEN", "set", amount, amount);
                                refreshGui(admin, targetName);
                            }));
                    } catch (NumberFormatException e) {
                        admin.sendMessage(MM.deserialize("<red>Invalid amount: " + text));
                    }
                });
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

    private static void refreshGui(Player admin, String targetName) {
        Bukkit.getScheduler().runTask(AdminToolkitPlugin.getInstance(), () -> open(admin, targetName));
    }

    @SuppressWarnings("deprecation")
    private static UUID getUUIDForName(String name) {
        Player online = findOnlinePlayer(name);
        if (online != null) return online.getUniqueId();
        return Bukkit.getOfflinePlayer(name).getUniqueId();
    }

    // ----------------------------------------------------------------
    // Staff action logging
    // ----------------------------------------------------------------

    /**
     * Async-write a row to staff_actions for every eco adjustment made via this GUI.
     * JSON format: {"currency":"IGC","action":"give","amount":1000,"balance_after":5000}
     */
    private static void logStaffAction(UUID adminUuid, UUID targetUuid,
                                       String currency, String action, long amount, long balanceAfter) {
        String details = String.format(
            "{\"currency\":\"%s\",\"action\":\"%s\",\"amount\":%d,\"balance_after\":%d}",
            currency, action, amount, balanceAfter);
        DatabaseManager.getInstance().queueWrite(
            "INSERT INTO staff_actions (actor_uuid, target_uuid, action_type, details) VALUES (?,?,?,?)",
            adminUuid.toString(), targetUuid.toString(), "economy", details
        );
    }
}
