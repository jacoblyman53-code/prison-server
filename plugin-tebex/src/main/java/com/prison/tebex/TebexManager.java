package com.prison.tebex;

import com.prison.database.DatabaseManager;
import com.prison.donor.DonorAPI;
import com.prison.permissions.PermissionEngine;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * TebexManager — core delivery logic.
 *
 * Flow for a new purchase:
 *  1. Check tebex_transaction_id for duplicate → skip if found.
 *  2. Insert into tebex_deliveries (delivered=0 initially).
 *  3. If player is online → deliver immediately, mark delivered=1, notify.
 *  4. If player is offline → leave delivered=0 for login pickup.
 *
 * Flow on player login:
 *  1. Query tebex_deliveries WHERE player_uuid=? AND delivered=0.
 *  2. For each row → deliver + mark delivered=1 + notify.
 */
public class TebexManager {

    private static TebexManager instance;
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final TebexPlugin plugin;
    private final Logger logger;

    private TebexManager(TebexPlugin plugin, Logger logger) {
        this.plugin = plugin;
        this.logger = logger;
        instance = this;
    }

    public static TebexManager initialize(TebexPlugin plugin, Logger logger) {
        if (instance != null) throw new IllegalStateException("TebexManager already initialized");
        return new TebexManager(plugin, logger);
    }

    public static TebexManager getInstance() { return instance; }

    public static void reset() { instance = null; }

    // ----------------------------------------------------------------
    // Schema migration — adds 'delivered' and 'extra_args' columns
    // ----------------------------------------------------------------

    public void runMigration() {
        // Add delivered column (1=done, 0=pending)
        silentAlter("ALTER TABLE tebex_deliveries ADD COLUMN IF NOT EXISTS delivered TINYINT(1) NOT NULL DEFAULT 1");
        // Add extra_args for storing rank id / crate tier etc.
        silentAlter("ALTER TABLE tebex_deliveries ADD COLUMN IF NOT EXISTS extra_args VARCHAR(255) DEFAULT NULL");
    }

    private void silentAlter(String sql) {
        try {
            DatabaseManager.getInstance().execute(sql);
        } catch (SQLException e) {
            // Column already exists on older MySQL without IF NOT EXISTS support — ignore.
            logger.fine("[Tebex] Migration note: " + e.getMessage());
        }
    }

    // ----------------------------------------------------------------
    // Receive a new delivery from the /tebexdeliver command
    // ----------------------------------------------------------------

    /**
     * Process a purchase delivery. Safe to call from any thread.
     *
     * @param username      Minecraft username from Tebex
     * @param transactionId Tebex transaction ID (idempotency key)
     * @param productId     Tebex product/package ID
     * @param type          Parsed delivery type
     * @param extraArgs     Type-specific args (rank id, crate tier+count, etc.)
     * @param onResult      Callback fired on main thread: true=success, false=duplicate/error
     */
    public void receiveDelivery(String username, String transactionId, String productId,
                                DeliveryType type, String[] extraArgs,
                                java.util.function.Consumer<Boolean> onResult) {

        DatabaseManager db = DatabaseManager.getInstance();

        // Run DB work off the main thread
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // 1. Idempotency check
                boolean duplicate = db.query(
                    "SELECT 1 FROM tebex_deliveries WHERE tebex_transaction_id = ?",
                    rs -> rs.next(),
                    transactionId
                );
                if (duplicate) {
                    logger.info("[Tebex] Duplicate transaction skipped: " + transactionId);
                    Bukkit.getScheduler().runTask(plugin, () -> onResult.accept(false));
                    return;
                }

                // 2. Look up the player (may be offline)
                Player online = Bukkit.getPlayerExact(username);
                UUID uuid;
                if (online != null) {
                    uuid = online.getUniqueId();
                } else {
                    @SuppressWarnings("deprecation")
                    org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(username);
                    uuid = op.getUniqueId();
                }

                String argsJson = String.join(",", extraArgs); // simple CSV stored in extra_args

                // 3. Insert delivery row (delivered=0 until we confirm it landed)
                long rowId = db.executeAndGetId(
                    "INSERT INTO tebex_deliveries " +
                    "(player_uuid, player_username, product_id, product_type, " +
                    " tebex_transaction_id, delivered, extra_args) " +
                    "VALUES (?, ?, ?, ?, ?, 0, ?)",
                    uuid.toString(), username, productId, type.key, transactionId, argsJson
                );

                // 4. Deliver (or leave for login pickup)
                if (online != null) {
                    final Player finalOnline = online;
                    final long finalRowId = rowId;
                    final UUID finalUuid = uuid;
                    Bukkit.getScheduler().runTask(plugin, () ->
                        deliverNow(finalOnline, finalUuid, finalRowId, type, extraArgs, true));
                } else {
                    logger.info("[Tebex] Player " + username + " offline — queued delivery #" + rowId);
                }

                Bukkit.getScheduler().runTask(plugin, () -> onResult.accept(true));

            } catch (SQLException e) {
                logger.log(Level.SEVERE, "[Tebex] Failed to process delivery for " + username, e);
                Bukkit.getScheduler().runTask(plugin, () -> onResult.accept(false));
            }
        });
    }

    // ----------------------------------------------------------------
    // Deliver pending rows on player login
    // ----------------------------------------------------------------

    public void deliverPending(Player player) {
        UUID uuid = player.getUniqueId();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                List<PendingDelivery> pending = DatabaseManager.getInstance().query(
                    "SELECT id, player_uuid, player_username, product_id, product_type, " +
                    "tebex_transaction_id, extra_args " +
                    "FROM tebex_deliveries WHERE player_uuid = ? AND delivered = 0",
                    rs -> {
                        List<PendingDelivery> list = new ArrayList<>();
                        while (rs.next()) {
                            list.add(new PendingDelivery(
                                rs.getLong("id"),
                                rs.getString("player_uuid"),
                                rs.getString("player_username"),
                                rs.getString("product_id"),
                                rs.getString("product_type"),
                                rs.getString("tebex_transaction_id"),
                                rs.getString("extra_args")
                            ));
                        }
                        return list;
                    },
                    uuid.toString()
                );

                if (pending.isEmpty()) return;

                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!player.isOnline()) return;
                    for (PendingDelivery d : pending) {
                        DeliveryType type = DeliveryType.fromKey(d.productType());
                        if (type == null) {
                            logger.warning("[Tebex] Unknown product type in pending: " + d.productType());
                            continue;
                        }
                        String[] args = d.extraArgs() != null ? d.extraArgs().split(",") : new String[0];
                        deliverNow(player, uuid, d.id(), type, args, false);
                    }
                });

            } catch (SQLException e) {
                logger.log(Level.SEVERE, "[Tebex] Failed to load pending for " + player.getName(), e);
            }
        });
    }

    // ----------------------------------------------------------------
    // Core delivery — must be called on the main thread
    // ----------------------------------------------------------------

    /**
     * Apply a delivery immediately to an online player and mark it delivered in DB.
     *
     * @param isNew  true if this is a fresh purchase (use delivery-message),
     *               false if this is a pending pickup on login (use pending-message).
     */
    private void deliverNow(Player player, UUID uuid, long rowId,
                            DeliveryType type, String[] args, boolean isNew) {
        String productDesc;
        boolean success = false;

        switch (type) {
            case DONOR_RANK -> {
                if (args.length < 1) {
                    logger.warning("[Tebex] DONOR_RANK delivery missing rank arg (row " + rowId + ")");
                    break;
                }
                String rankId = args[0].toLowerCase();
                productDesc = "Donor Rank: " + capitalize(rankId);

                // Grant via PermissionEngine (handles cache invalidation)
                PermissionEngine.getInstance().setDonorRank(uuid, rankId).thenRun(() ->
                    logger.info("[Tebex] Granted donor rank " + rankId + " to " + player.getName())
                );

                notifyPlayer(player, productDesc, isNew);
                success = true;
            }
            case CRATE_KEY -> {
                if (args.length < 1) {
                    logger.warning("[Tebex] CRATE_KEY delivery missing tier arg (row " + rowId + ")");
                    break;
                }
                String tier = args[0].toLowerCase();
                int amount  = args.length >= 2 ? parseIntSafe(args[1], 1) : 1;
                productDesc = amount + "x " + capitalize(tier) + " Crate Key" + (amount > 1 ? "s" : "");

                // Prefer CrateManager if loaded; fall back to direct DB write
                com.prison.crates.CrateManager crateManager = com.prison.crates.CrateManager.getInstance();
                if (crateManager != null) {
                    crateManager.giveKeys(uuid, tier, amount);
                } else {
                    giveCrateKeysDirect(uuid, tier, amount);
                }

                notifyPlayer(player, productDesc, isNew);
                success = true;
            }
            default -> {
                logger.warning("[Tebex] Unhandled delivery type: " + type + " (row " + rowId + ")");
                productDesc = type.key;
            }
        }

        // Mark delivered in DB (async)
        if (success) {
            final long finalRowId = rowId;
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    DatabaseManager.getInstance().execute(
                        "UPDATE tebex_deliveries SET delivered = 1 WHERE id = ?", finalRowId);
                } catch (SQLException e) {
                    logger.log(Level.WARNING, "[Tebex] Failed to mark delivery #" + finalRowId + " complete", e);
                }
            });
        }
    }

    // ----------------------------------------------------------------
    // Admin queries
    // ----------------------------------------------------------------

    /** Returns only undelivered (pending) rows for a player. */
    public List<String[]> getPending(UUID uuid) throws SQLException {
        return DatabaseManager.getInstance().query(
            "SELECT product_type, extra_args, tebex_transaction_id " +
            "FROM tebex_deliveries WHERE player_uuid = ? AND delivered = 0 ORDER BY id DESC",
            rs -> {
                List<String[]> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(new String[]{
                        rs.getString("product_type"),
                        rs.getString("extra_args"),
                        "PENDING",
                        "—",
                        rs.getString("tebex_transaction_id")
                    });
                }
                return list;
            },
            uuid.toString()
        );
    }

    /** Returns recent delivery rows for a player (up to 10, most recent first). */
    public List<String[]> getHistory(UUID uuid, int page) throws SQLException {
        int offset = (page - 1) * 10;
        return DatabaseManager.getInstance().query(
            "SELECT product_type, extra_args, delivered, delivered_at, tebex_transaction_id " +
            "FROM tebex_deliveries WHERE player_uuid = ? ORDER BY id DESC LIMIT 10 OFFSET ?",
            rs -> {
                List<String[]> list = new ArrayList<>();
                while (rs.next()) {
                    java.sql.Timestamp deliveredAt = rs.getTimestamp("delivered_at");
                    list.add(new String[]{
                        rs.getString("product_type"),
                        rs.getString("extra_args"),
                        rs.getInt("delivered") == 1 ? "delivered" : "PENDING",
                        deliveredAt != null ? deliveredAt.toString() : "—",
                        rs.getString("tebex_transaction_id")
                    });
                }
                return list;
            },
            uuid.toString(), offset
        );
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private void notifyPlayer(Player player, String productDesc, boolean isNew) {
        String template = isNew
            ? plugin.getConfig().getString("delivery-message",
                "<green><bold>✦ Purchase delivered!</bold></green> <white>{product}</white>")
            : plugin.getConfig().getString("pending-message",
                "<green>Pending purchase delivered: <white>{product}</white></green>");
        player.sendMessage(MM.deserialize(template.replace("{product}", productDesc)));
    }

    /** Direct DB write to crate_keys when CrateManager isn't available. */
    private void giveCrateKeysDirect(UUID uuid, String tier, int amount) {
        try {
            DatabaseManager.getInstance().execute(
                "INSERT INTO crate_keys (player_uuid, crate_tier, quantity) VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE quantity = quantity + VALUES(quantity)",
                uuid.toString(), tier, amount
            );
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "[Tebex] Failed to write crate keys directly for " + uuid, e);
        }
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static int parseIntSafe(String s, int def) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return def; }
    }
}
