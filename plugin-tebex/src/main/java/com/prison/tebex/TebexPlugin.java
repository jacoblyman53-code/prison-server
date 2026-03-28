package com.prison.tebex;

import com.prison.permissions.PermissionEngine;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * TebexPlugin — main entry point.
 *
 * /tebexdeliver is the command Tebex fires on purchase.
 * /tebex is the admin view command.
 */
public class TebexPlugin extends JavaPlugin implements Listener {

    private static TebexPlugin instance;
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private TebexManager manager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        manager = TebexManager.initialize(this, getLogger());
        manager.runMigration();

        getServer().getPluginManager().registerEvents(this, this);

        var deliverCmd = getCommand("tebexdeliver");
        if (deliverCmd != null) deliverCmd.setExecutor(this);

        var adminCmd = getCommand("tebex");
        if (adminCmd != null) adminCmd.setExecutor(this);

        getLogger().info("TebexPlugin enabled.");
    }

    @Override
    public void onDisable() {
        TebexManager.reset();
        instance = null;
        getLogger().info("TebexPlugin disabled.");
    }

    public static TebexPlugin getInstance() { return instance; }

    // ----------------------------------------------------------------
    // Events
    // ----------------------------------------------------------------

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Deliver any pending purchases after 1 tick so other plugins have fully loaded the player
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (player.isOnline()) manager.deliverPending(player);
        }, 20L);
    }

    // ----------------------------------------------------------------
    // Commands
    // ----------------------------------------------------------------

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        switch (command.getName().toLowerCase()) {
            case "tebexdeliver" -> handleDeliver(sender, args);
            case "tebex"        -> handleAdmin(sender, args);
        }
        return true;
    }

    // ----------------------------------------------------------------
    // /tebexdeliver <username> <txn_id> <product_id> <product_type> [extra args...]
    // ----------------------------------------------------------------

    private void handleDeliver(CommandSender sender, String[] args) {
        // Only console or ops should be able to call this
        if (sender instanceof Player player && !player.isOp()) {
            player.sendMessage(MM.deserialize("<red>No permission."));
            return;
        }

        if (args.length < 4) {
            sender.sendMessage("[Tebex] Usage: /tebexdeliver <username> <txn_id> <product_id> <product_type> [args...]");
            return;
        }

        String username      = args[0];
        String transactionId = args[1];
        String productId     = args[2];
        String productTypeStr = args[3];
        String[] extraArgs   = args.length > 4 ? Arrays.copyOfRange(args, 4, args.length) : new String[0];

        DeliveryType type = DeliveryType.fromKey(productTypeStr);
        if (type == null) {
            sender.sendMessage("[Tebex] Unknown product_type: " + productTypeStr
                + ". Valid types: donor_rank, crate_key");
            return;
        }

        // Validate extra args
        String validationError = validateArgs(type, extraArgs);
        if (validationError != null) {
            sender.sendMessage("[Tebex] " + validationError);
            return;
        }

        manager.receiveDelivery(username, transactionId, productId, type, extraArgs, success -> {
            if (success) {
                getLogger().info("[Tebex] Delivery accepted: " + transactionId
                    + " → " + username + " | " + productTypeStr + " " + String.join(" ", extraArgs));
            }
            // Duplicate case already logged inside TebexManager
        });
    }

    // ----------------------------------------------------------------
    // /tebex <history|pending> <player> [page]
    // ----------------------------------------------------------------

    private void handleAdmin(CommandSender sender, String[] args) {
        if (sender instanceof Player player
            && !PermissionEngine.getInstance().hasPermission(player, "prison.staff.admin")) {
            player.sendMessage(MM.deserialize("<red>No permission."));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(MM.deserialize(
                "<gold>/tebex history <player> [page]</gold><gray> — delivery history\n"
                + "<gold>/tebex pending <player></gold><gray> — pending deliveries"));
            return;
        }

        String sub    = args[0].toLowerCase();
        String target = args[1];
        int page      = args.length >= 3 ? parsePageSafe(args[2]) : 1;

        // Resolve UUID
        Player online = Bukkit.getPlayerExact(target);
        UUID uuid;
        String displayName;
        if (online != null) {
            uuid = online.getUniqueId();
            displayName = online.getName();
        } else {
            @SuppressWarnings("deprecation")
            OfflinePlayer op = Bukkit.getOfflinePlayer(target);
            uuid = op.getUniqueId();
            displayName = op.getName() != null ? op.getName() : target;
        }

        if ("history".equals(sub)) {
            showHistory(sender, uuid, displayName, page);
        } else if ("pending".equals(sub)) {
            showHistory(sender, uuid, displayName, page); // same query, both show status
        } else {
            sender.sendMessage(MM.deserialize("<red>Unknown subcommand. Use history or pending."));
        }
    }

    private void showHistory(CommandSender sender, UUID uuid, String displayName, int page) {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                List<String[]> rows = manager.getHistory(uuid, page);
                Bukkit.getScheduler().runTask(this, () -> {
                    sender.sendMessage(MM.deserialize(
                        "<gold><bold>Tebex Deliveries — " + displayName + " (p" + page + ")</bold></gold>"));
                    if (rows.isEmpty()) {
                        sender.sendMessage(MM.deserialize("<gray>No deliveries found."));
                        return;
                    }
                    for (String[] row : rows) {
                        // row: product_type, extra_args, status, delivered_at, txn_id
                        String statusColor = "PENDING".equals(row[2]) ? "<red>" : "<green>";
                        sender.sendMessage(MM.deserialize(
                            "<yellow>" + row[0] + " <white>" + (row[1] != null ? row[1] : "")
                            + " " + statusColor + row[2] + " <dark_gray>@ " + row[3]));
                    }
                });
            } catch (SQLException e) {
                getLogger().log(java.util.logging.Level.SEVERE, "Failed to query Tebex history", e);
                Bukkit.getScheduler().runTask(this, () ->
                    sender.sendMessage(MM.deserialize("<red>Database error. Check console.")));
            }
        });
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private String validateArgs(DeliveryType type, String[] extra) {
        return switch (type) {
            case DONOR_RANK -> {
                if (extra.length < 1) yield "donor_rank requires: <rank_id>";
                List<String> valid = getConfig().getStringList("valid-donor-ranks");
                if (!valid.isEmpty() && !valid.contains(extra[0].toLowerCase())) {
                    yield "Unknown rank '" + extra[0] + "'. Valid: " + String.join(", ", valid);
                }
                yield null;
            }
            case CRATE_KEY -> {
                if (extra.length < 1) yield "crate_key requires: <tier> [amount]";
                List<String> valid = getConfig().getStringList("valid-crate-tiers");
                if (!valid.isEmpty() && !valid.contains(extra[0].toLowerCase())) {
                    yield "Unknown tier '" + extra[0] + "'. Valid: " + String.join(", ", valid);
                }
                yield null;
            }
        };
    }

    private static int parsePageSafe(String s) {
        try { return Math.max(1, Integer.parseInt(s)); } catch (NumberFormatException e) { return 1; }
    }
}
