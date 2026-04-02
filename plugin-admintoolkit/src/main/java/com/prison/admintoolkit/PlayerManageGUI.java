package com.prison.admintoolkit;

import com.prison.database.DatabaseManager;
import com.prison.economy.EconomyAPI;
import com.prison.economy.TransactionType;
import com.prison.permissions.PermissionEngine;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PlayerManageGUI — full player profile and management panel.
 *
 * Shows stats, punishment history, and provides action buttons for
 * banning, muting, kicking, teleporting, balance adjustments, and rank changes.
 */
public class PlayerManageGUI {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private static final Component TITLE = MM.deserialize("PLAYER MANAGEMENT");

    // Admin UUID → target player name
    private static final Map<UUID, String> managedPlayerNames = new ConcurrentHashMap<>();

    // ----------------------------------------------------------------
    // Data holder
    // ----------------------------------------------------------------

    record PlayerData(
        String uuid,
        String username,
        String mineRank,
        String donorRank,
        String staffRank,
        int prestige,
        long igcBalance,
        long tokenBalance,
        long blocksMined,
        long playtime,
        String firstJoin,
        String lastSeen,
        List<String> recentPunishments,
        List<String> usernameHistory
    ) {}

    // ----------------------------------------------------------------
    // Open (async data load)
    // ----------------------------------------------------------------

    public static void open(Player admin, String targetName) {
        managedPlayerNames.put(admin.getUniqueId(), targetName);

        CompletableFuture.supplyAsync(() -> {
            try {
                // Main player data
                PlayerData data = DatabaseManager.getInstance().query(
                    "SELECT uuid, username, mine_rank, donor_rank, staff_rank, prestige, " +
                    "igc_balance, token_balance, blocks_mined, playtime, first_join, last_seen " +
                    "FROM players WHERE username = ?",
                    rs -> {
                        if (!rs.next()) return null;
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                        String fj = rs.getTimestamp("first_join") != null
                            ? sdf.format(rs.getTimestamp("first_join")) : "Unknown";
                        String ls = rs.getTimestamp("last_seen") != null
                            ? sdf.format(rs.getTimestamp("last_seen")) : "Unknown";
                        return new PlayerData(
                            rs.getString("uuid"),
                            rs.getString("username"),
                            rs.getString("mine_rank"),
                            rs.getString("donor_rank"),
                            rs.getString("staff_rank"),
                            rs.getInt("prestige"),
                            rs.getLong("igc_balance"),
                            rs.getLong("token_balance"),
                            rs.getLong("blocks_mined"),
                            rs.getLong("playtime"),
                            fj, ls,
                            new ArrayList<>(), new ArrayList<>()
                        );
                    },
                    targetName
                );

                if (data == null) return null;

                // Recent punishments
                List<String> punishments = DatabaseManager.getInstance().query(
                    "SELECT type, reason, issued_at, expires_at, active FROM punishments " +
                    "WHERE player_uuid = ? ORDER BY issued_at DESC LIMIT 3",
                    rs -> {
                        List<String> list = new ArrayList<>();
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                        while (rs.next()) {
                            String type    = rs.getString("type");
                            String reason  = rs.getString("reason");
                            String issued  = rs.getTimestamp("issued_at") != null
                                ? sdf.format(rs.getTimestamp("issued_at")) : "?";
                            boolean active = rs.getBoolean("active");
                            list.add("[" + type + "] " + reason + " (" + issued + ")" + (active ? " [ACTIVE]" : ""));
                        }
                        return list;
                    },
                    data.uuid()
                );

                // Username history
                List<String> history = DatabaseManager.getInstance().query(
                    "SELECT username, recorded_at FROM username_history " +
                    "WHERE player_uuid = ? ORDER BY recorded_at DESC LIMIT 3",
                    rs -> {
                        List<String> list = new ArrayList<>();
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                        while (rs.next()) {
                            String name = rs.getString("username");
                            String date = rs.getTimestamp("recorded_at") != null
                                ? sdf.format(rs.getTimestamp("recorded_at")) : "?";
                            list.add(name + " (" + date + ")");
                        }
                        return list;
                    },
                    data.uuid()
                );

                List<String> safeP = punishments != null ? punishments : new ArrayList<>();
                List<String> safeH = history != null ? history : new ArrayList<>();

                return new PlayerData(
                    data.uuid(), data.username(), data.mineRank(), data.donorRank(),
                    data.staffRank(), data.prestige(), data.igcBalance(), data.tokenBalance(),
                    data.blocksMined(), data.playtime(), data.firstJoin(), data.lastSeen(),
                    safeP, safeH
                );

            } catch (Exception e) {
                AdminToolkitPlugin.getInstance().getLogger()
                    .warning("[PlayerManageGUI] DB error for " + targetName + ": " + e.getMessage());
                return null;
            }
        }).thenAccept(data -> Bukkit.getScheduler().runTask(AdminToolkitPlugin.getInstance(), () -> {
            if (data == null) {
                admin.sendMessage(MM.deserialize("<red>Player '" + targetName + "' not found in database."));
                return;
            }
            renderPlayerManageGUI(admin, data);
        }));
    }

    // ----------------------------------------------------------------
    // Render
    // ----------------------------------------------------------------

    private static void renderPlayerManageGUI(Player admin, PlayerData data) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);

        // Player head (slot 4)
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
        skullMeta.displayName(MM.deserialize("<!italic><yellow>" + data.username()));
        List<Component> headLore = new ArrayList<>();
        headLore.add(MM.deserialize("<!italic><aqua>✦ <gray>Mine Rank: <yellow>" + nvl(data.mineRank(), "<red>None")));
        headLore.add(MM.deserialize("<!italic><aqua>✦ <gray>Prestige: <white>" + data.prestige()));
        headLore.add(MM.deserialize("<!italic><aqua>✦ <gray>Donor: <yellow>" + nvl(data.donorRank(), "<red>None")));
        headLore.add(MM.deserialize("<!italic><aqua>✦ <gray>Staff: <yellow>" + nvl(data.staffRank(), "<red>None")));
        headLore.add(MM.deserialize("<!italic>"));
        headLore.add(MM.deserialize("<!italic><aqua>✦ <gray>IGC Balance: <white>" + data.igcBalance()));
        headLore.add(MM.deserialize("<!italic><aqua>✦ <gray>Tokens: <white>" + data.tokenBalance()));
        headLore.add(MM.deserialize("<!italic><aqua>✦ <gray>Blocks Mined: <white>" + data.blocksMined()));
        headLore.add(MM.deserialize("<!italic><aqua>✦ <gray>Playtime: <white>" + formatPlaytime(data.playtime())));
        headLore.add(MM.deserialize("<!italic>"));
        headLore.add(MM.deserialize("<!italic><aqua>✦ <gray>First Join: <white>" + data.firstJoin()));
        headLore.add(MM.deserialize("<!italic><aqua>✦ <gray>Last Seen: <white>" + data.lastSeen()));
        skullMeta.lore(headLore);
        skullMeta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
        @SuppressWarnings("deprecation")
        OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(data.username());
        skullMeta.setOwningPlayer(offlineTarget);
        head.setItemMeta(skullMeta);
        inv.setItem(4, head);

        // Action buttons row 1 (18-26)
        inv.setItem(18, AdminPanel.makeItem(Material.RED_BANNER,
            "<red>Ban",
            "<gray>Permanently <red>ban<gray> this player.",
            "",
            "<red>✗ Requires confirmation.",
            "",
            "<green>→ <green>Click to <green><underlined>ban</underlined> this player!"));
        inv.setItem(19, AdminPanel.makeItem(Material.ORANGE_BANNER,
            "<gold>Temp Ban",
            "<gray>Temporarily <red>ban<gray> this player.",
            "<gray>You will enter <green>duration<gray> + <green>reason<gray>.",
            "",
            "<red>✗ Requires confirmation.",
            "",
            "<green>→ <green>Click to <green><underlined>temp ban</underlined> this player!"));
        inv.setItem(20, AdminPanel.makeItem(Material.YELLOW_BANNER,
            "<yellow>Mute",
            "<gray>Permanently <red>mute<gray> this player.",
            "",
            "<red>✗ Requires confirmation.",
            "",
            "<green>→ <green>Click to <green><underlined>mute</underlined> this player!"));
        inv.setItem(21, AdminPanel.makeItem(Material.YELLOW_STAINED_GLASS_PANE,
            "<yellow>Kick",
            "<gray><green>Kick<gray> this player from the server.",
            "",
            "<red>✗ Requires confirmation.",
            "",
            "<green>→ <green>Click to <green><underlined>kick</underlined> this player!"));
        inv.setItem(22, AdminPanel.makeItem(Material.ICE,
            "<aqua>Freeze / Unfreeze",
            "<gray>Freeze or <green>unfreeze<gray> the player.",
            "",
            "<green>→ <green>Click to <green><underlined>toggle</underlined> freeze!"));
        inv.setItem(23, AdminPanel.makeItem(Material.ENDER_PEARL,
            "<aqua>Teleport To",
            "<gray>Teleport to the player's <green>location<gray>.",
            "<gray>Player must be <green>online<gray>.",
            "",
            "<green>→ <green>Click to <green><underlined>teleport</underlined> to this player!"));
        inv.setItem(24, AdminPanel.makeItem(Material.ENDER_EYE,
            "<aqua>Bring Here",
            "<gray>Teleport player to your <green>location<gray>.",
            "<gray>Player must be <green>online<gray>.",
            "",
            "<green>→ <green>Click to <green><underlined>bring</underlined> this player here!"));
        inv.setItem(25, AdminPanel.makeItem(Material.CHEST,
            "<aqua>View Inventory",
            "<gray>Open the player's <green>inventory<gray>.",
            "<gray>Player must be <green>online<gray>.",
            "",
            "<green>→ <green>Click to <green><underlined>view</underlined> inventory!"));
        inv.setItem(26, AdminPanel.makeItem(Material.BOOK,
            "<aqua>History",
            "<gray>Show <green>punishments<gray> and <green>usernames<gray> in chat.",
            "",
            "<green>→ <green>Click to <green><underlined>view</underlined> history!"));

        // Action buttons row 2 (27-35)
        inv.setItem(27, AdminPanel.makeItem(Material.GOLD_INGOT,
            "<gold>Give IGC",
            "<gray>Add <green>IGC<gray> to player's balance.",
            "",
            "<green>→ <green>Click to <green><underlined>give</underlined> IGC!"));
        inv.setItem(28, AdminPanel.makeItem(Material.NETHER_STAR,
            "<aqua>Give Tokens",
            "<gray>Add <green>tokens<gray> to player's balance.",
            "",
            "<green>→ <green>Click to <green><underlined>give</underlined> tokens!"));
        inv.setItem(29, AdminPanel.makeItem(Material.COMPASS,
            "<aqua>Set Mine Rank",
            "<gray>Force-set player's <green>mine rank<gray> (A-Z).",
            "",
            "<green>→ <green>Click to <green><underlined>set</underlined> mine rank!"));
        inv.setItem(30, AdminPanel.makeItem(Material.EMERALD,
            "<aqua>Set Donor Rank",
            "<gray>Set <green>donor rank<gray> (donor/donorplus/elite/eliteplus).",
            "",
            "<green>→ <green>Click to <green><underlined>set</underlined> donor rank!"));
        inv.setItem(31, AdminPanel.makeItem(Material.IRON_SWORD,
            "<aqua>Set Staff Rank",
            "<gray>Set <green>staff rank<gray> or <red>'none'<gray> to remove.",
            "",
            "<green>→ <green>Click to <green><underlined>set</underlined> staff rank!"));

        // Back
        inv.setItem(45, AdminPanel.makeItem(Material.ARROW,
            "<red>← Back",
            "<gray>Click to close this menu.",
            "",
            "<green>→ <green>Click to <green><underlined>close</underlined> player manager."));

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
        String targetName = managedPlayerNames.get(admin.getUniqueId());
        if (targetName == null) return;

        Player onlineTarget = EcoToolsGUI.findOnlinePlayer(targetName);

        switch (slot) {
            case 18 -> ConfirmDestructiveGUI.open(admin, targetName, ConfirmDestructiveGUI.ActionType.BAN);
            case 19 -> ConfirmDestructiveGUI.open(admin, targetName, ConfirmDestructiveGUI.ActionType.TEMP_BAN);
            case 20 -> ConfirmDestructiveGUI.open(admin, targetName, ConfirmDestructiveGUI.ActionType.MUTE);
            case 21 -> ConfirmDestructiveGUI.open(admin, targetName, ConfirmDestructiveGUI.ActionType.KICK);
            case 22 -> {
                // Freeze toggle
                Bukkit.getScheduler().runTask(AdminToolkitPlugin.getInstance(), () ->
                    Bukkit.dispatchCommand(admin, "freeze " + targetName));
            }
            case 23 -> {
                // Teleport to
                if (onlineTarget == null) {
                    admin.sendMessage(MM.deserialize("<red>" + targetName + " is not online."));
                    return;
                }
                admin.closeInventory();
                Bukkit.getScheduler().runTask(AdminToolkitPlugin.getInstance(), () ->
                    admin.teleport(onlineTarget.getLocation()));
            }
            case 24 -> {
                // Bring here
                if (onlineTarget == null) {
                    admin.sendMessage(MM.deserialize("<red>" + targetName + " is not online."));
                    return;
                }
                admin.closeInventory();
                Bukkit.getScheduler().runTask(AdminToolkitPlugin.getInstance(), () ->
                    onlineTarget.teleport(admin.getLocation()));
            }
            case 25 -> {
                // View inventory
                Bukkit.getScheduler().runTask(AdminToolkitPlugin.getInstance(), () ->
                    Bukkit.dispatchCommand(admin, "invsee " + targetName));
            }
            case 26 -> {
                // History — show in chat
                String storedTarget = targetName;
                admin.closeInventory();
                Bukkit.getScheduler().runTask(AdminToolkitPlugin.getInstance(), () ->
                    showHistory(admin, storedTarget));
            }
            case 27 -> {
                // Give IGC
                AnvilInputGUI.open(admin, "0", text -> {
                    try {
                        long amount = Long.parseLong(text.trim());
                        UUID targetUuid = resolveUUID(targetName);
                        if (targetUuid != null) {
                            EconomyAPI.getInstance().addBalance(targetUuid, amount, TransactionType.ADMIN_ADD);
                            admin.sendMessage(MM.deserialize("<green>Gave " + amount + " IGC to " + targetName + "."));
                        } else {
                            admin.sendMessage(MM.deserialize("<red>Could not resolve UUID for " + targetName + "."));
                        }
                    } catch (NumberFormatException e) {
                        admin.sendMessage(MM.deserialize("<red>Invalid amount: " + text));
                    }
                });
            }
            case 28 -> {
                // Give Tokens
                AnvilInputGUI.open(admin, "0", text -> {
                    try {
                        long amount = Long.parseLong(text.trim());
                        UUID targetUuid = resolveUUID(targetName);
                        if (targetUuid != null) {
                            EconomyAPI.getInstance().addTokens(targetUuid, amount, TransactionType.ADMIN_ADD);
                            admin.sendMessage(MM.deserialize("<green>Gave " + amount + " tokens to " + targetName + "."));
                        } else {
                            admin.sendMessage(MM.deserialize("<red>Could not resolve UUID for " + targetName + "."));
                        }
                    } catch (NumberFormatException e) {
                        admin.sendMessage(MM.deserialize("<red>Invalid amount: " + text));
                    }
                });
            }
            case 29 -> {
                // Set mine rank
                AnvilInputGUI.open(admin, "A", text -> {
                    String rankLetter = text.trim().toUpperCase();
                    UUID targetUuid = resolveUUID(targetName);
                    if (targetUuid != null) {
                        PermissionEngine.getInstance().setMineRank(targetUuid, rankLetter);
                        admin.sendMessage(MM.deserialize("<green>Set " + targetName + "'s mine rank to " + rankLetter + "."));
                    } else {
                        admin.sendMessage(MM.deserialize("<red>Could not resolve UUID for " + targetName + "."));
                    }
                });
            }
            case 30 -> {
                // Set donor rank
                AnvilInputGUI.open(admin, "donor", text -> {
                    UUID targetUuid = resolveUUID(targetName);
                    if (targetUuid != null) {
                        PermissionEngine.getInstance().setDonorRank(targetUuid, text.trim().toLowerCase());
                        admin.sendMessage(MM.deserialize("<green>Set " + targetName + "'s donor rank to " + text.trim() + "."));
                    } else {
                        admin.sendMessage(MM.deserialize("<red>Could not resolve UUID for " + targetName + "."));
                    }
                });
            }
            case 31 -> {
                // Set staff rank
                AnvilInputGUI.open(admin, "admin", text -> {
                    UUID targetUuid = resolveUUID(targetName);
                    if (targetUuid != null) {
                        String rank = text.trim().equalsIgnoreCase("none") ? null : text.trim().toLowerCase();
                        PermissionEngine.getInstance().setStaffRank(targetUuid, rank);
                        admin.sendMessage(MM.deserialize("<green>Set " + targetName + "'s staff rank to " + (rank != null ? rank : "none") + "."));
                    } else {
                        admin.sendMessage(MM.deserialize("<red>Could not resolve UUID for " + targetName + "."));
                    }
                });
            }
            case 45 -> {
                managedPlayerNames.remove(admin.getUniqueId());
                admin.closeInventory();
            }
        }
    }

    // ----------------------------------------------------------------
    // Show history in chat
    // ----------------------------------------------------------------

    private static void showHistory(Player admin, String targetName) {
        CompletableFuture.supplyAsync(() -> {
            try {
                UUID targetUuid = resolveUUID(targetName);
                if (targetUuid == null) return null;

                List<String> punishments = DatabaseManager.getInstance().query(
                    "SELECT type, reason, issued_at, active FROM punishments " +
                    "WHERE player_uuid = ? ORDER BY issued_at DESC LIMIT 5",
                    rs -> {
                        List<String> list = new ArrayList<>();
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                        while (rs.next()) {
                            String type   = rs.getString("type");
                            String reason = rs.getString("reason");
                            String issued = rs.getTimestamp("issued_at") != null
                                ? sdf.format(rs.getTimestamp("issued_at")) : "?";
                            boolean active = rs.getBoolean("active");
                            list.add("[" + type + "] " + reason + " (" + issued + ")" + (active ? " [ACTIVE]" : ""));
                        }
                        return list;
                    },
                    targetUuid.toString()
                );

                List<String> usernames = DatabaseManager.getInstance().query(
                    "SELECT username, recorded_at FROM username_history " +
                    "WHERE player_uuid = ? ORDER BY recorded_at DESC LIMIT 5",
                    rs -> {
                        List<String> list = new ArrayList<>();
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                        while (rs.next()) {
                            String name = rs.getString("username");
                            String date = rs.getTimestamp("recorded_at") != null
                                ? sdf.format(rs.getTimestamp("recorded_at")) : "?";
                            list.add(name + " (" + date + ")");
                        }
                        return list;
                    },
                    targetUuid.toString()
                );

                return new Object[]{ punishments, usernames };
            } catch (Exception e) {
                return null;
            }
        }).thenAccept(result -> Bukkit.getScheduler().runTask(AdminToolkitPlugin.getInstance(), () -> {
            if (result == null) {
                admin.sendMessage(MM.deserialize("<red>Could not load history for " + targetName + "."));
                return;
            }
            @SuppressWarnings("unchecked")
            List<String> punishments = (List<String>) result[0];
            @SuppressWarnings("unchecked")
            List<String> usernames   = (List<String>) result[1];

            admin.sendMessage(MM.deserialize("<gold>=== History for " + targetName + " ==="));
            admin.sendMessage(MM.deserialize("<yellow>Punishments:"));
            if (punishments == null || punishments.isEmpty()) {
                admin.sendMessage(MM.deserialize("<gray>  None"));
            } else {
                for (String p : punishments) admin.sendMessage(MM.deserialize("<gray>  " + p));
            }
            admin.sendMessage(MM.deserialize("<yellow>Username History:"));
            if (usernames == null || usernames.isEmpty()) {
                admin.sendMessage(MM.deserialize("<gray>  None"));
            } else {
                for (String u : usernames) admin.sendMessage(MM.deserialize("<gray>  " + u));
            }
        }));
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    static String formatPlaytime(long seconds) {
        long days  = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long mins  = (seconds % 3600) / 60;
        StringBuilder sb = new StringBuilder();
        if (days > 0)  sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        sb.append(mins).append("m");
        return sb.toString().trim();
    }

    private static String nvl(String val, String fallback) {
        return (val == null || val.isBlank()) ? fallback : val;
    }

    @SuppressWarnings("deprecation")
    private static UUID resolveUUID(String name) {
        Player online = EcoToolsGUI.findOnlinePlayer(name);
        if (online != null) return online.getUniqueId();
        OfflinePlayer off = Bukkit.getOfflinePlayer(name);
        return off.hasPlayedBefore() ? off.getUniqueId() : null;
    }

    public static void cleanup(UUID uuid) {
        managedPlayerNames.remove(uuid);
    }
}
