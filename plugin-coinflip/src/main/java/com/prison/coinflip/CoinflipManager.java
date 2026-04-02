package com.prison.coinflip;

import com.prison.database.DatabaseManager;
import com.prison.economy.EconomyAPI;
import com.prison.economy.TransactionType;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * CoinflipManager — lifecycle for coinflip tickets.
 *
 * Flow:
 *   1. createTicket()    — deduct IGC from creator (hold), insert DB row, add to open map.
 *   2. acceptTicket()    — deduct IGC from acceptor, update DB state, start animation.
 *   3. resolve()         — called by animation at completion; award full pool to winner.
 *   4. cancelTicket()    — refund creator, mark CANCELLED in DB, remove from open map.
 *
 * Anti-abuse:
 *   - One open ticket per player at a time.
 *   - Self-accept is blocked.
 *   - Acceptance checks balance atomically before deducting.
 *   - DB UNIQUE on id; resolve is idempotent (state guard).
 */
public class CoinflipManager {

    private static CoinflipManager instance;
    private static final MiniMessage MM = MiniMessage.miniMessage();

    public static CoinflipManager getInstance() { return instance; }

    public static CoinflipManager initialize(JavaPlugin plugin) {
        instance = new CoinflipManager(plugin, plugin.getLogger());
        instance.ensureTables();
        return instance;
    }

    // ----------------------------------------------------------------

    private final JavaPlugin plugin;
    private final Logger logger;

    /** id → ticket for all OPEN tickets */
    private final Map<Integer, CoinflipTicket> openTickets = new ConcurrentHashMap<>();

    /** creator UUID → ticket id to enforce one-ticket-per-player */
    private final Map<UUID, Integer> creatorIndex = new ConcurrentHashMap<>();

    private CoinflipManager(JavaPlugin plugin, Logger logger) {
        this.plugin = plugin;
        this.logger = logger;
    }

    // ----------------------------------------------------------------
    // DB
    // ----------------------------------------------------------------

    private void ensureTables() {
        try {
            DatabaseManager.getInstance().execute(
                "CREATE TABLE IF NOT EXISTS coinflip_tickets (" +
                "  id                    INT UNSIGNED     NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                "  creator_uuid          VARCHAR(36)      NOT NULL," +
                "  creator_name          VARCHAR(16)      NOT NULL," +
                "  amount                BIGINT UNSIGNED  NOT NULL," +
                "  state                 VARCHAR(24)      NOT NULL DEFAULT 'OPEN'," +
                "  created_at            TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "  acceptor_uuid         VARCHAR(36)      NULL," +
                "  acceptor_name         VARCHAR(16)      NULL," +
                "  resolved_at           TIMESTAMP        NULL," +
                "  winner_uuid           VARCHAR(36)      NULL," +
                "  INDEX idx_cf_creator  (creator_uuid)," +
                "  INDEX idx_cf_state    (state)" +
                ")"
            );
        } catch (SQLException e) {
            logger.severe("[Coinflip] Failed to create tables: " + e.getMessage());
        }
    }

    // ----------------------------------------------------------------
    // Public API
    // ----------------------------------------------------------------

    /** Returns all currently OPEN tickets (unmodifiable snapshot). */
    public List<CoinflipTicket> getOpenTickets() {
        return List.copyOf(openTickets.values());
    }

    /** Returns true if this player already has an open coinflip listing. */
    public boolean hasOpenTicket(UUID uuid) {
        return creatorIndex.containsKey(uuid);
    }

    /** Returns the open ticket for a player, or null. */
    public CoinflipTicket getTicketOf(UUID uuid) {
        Integer id = creatorIndex.get(uuid);
        return id == null ? null : openTickets.get(id);
    }

    /**
     * Attempt to create a coinflip ticket for the given player.
     * Deducts the amount from their IGC balance atomically.
     *
     * @return null on success (ticket created); an error string on failure.
     */
    public String createTicket(Player player, long amount) {
        UUID uuid = player.getUniqueId();

        if (amount <= 0)
            return "Bet amount must be greater than zero.";
        if (amount > 1_000_000_000L)
            return "Maximum bet is 1,000,000,000 IGC.";
        if (hasOpenTicket(uuid))
            return "You already have an active coinflip. Cancel it first with /coinflip cancel.";

        EconomyAPI eco = EconomyAPI.getInstance();
        if (eco == null) return "Economy system not available.";

        // Deduct funds atomically — returns -1 if insufficient
        long result = eco.deductBalance(uuid, amount, TransactionType.COINFLIP_BET);
        if (result < 0)
            return "You cannot afford to bet $" + String.format("%,d", amount) + ".";

        // Persist to DB async, get generated id back on completion
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                long newId = DatabaseManager.getInstance().executeAndGetId(
                    "INSERT INTO coinflip_tickets (creator_uuid, creator_name, amount, state) VALUES (?,?,?,?)",
                    uuid.toString(), player.getName(), amount, "OPEN"
                );
                CoinflipTicket ticket = new CoinflipTicket((int) newId, uuid, player.getName(), amount);
                openTickets.put((int) newId, ticket);
                creatorIndex.put(uuid, (int) newId);


                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage(MM.deserialize(
                        "<green>✔ Coinflip created for <gold>$" + String.format("%,d", amount) +
                        "<green>. Waiting for a challenger..."));
                    // Refresh browser for all players who have it open
                    CoinflipBrowserGUI.refreshForOnlinePlayers();
                });
            } catch (SQLException e) {
                // Rollback the deduction
                eco.addBalance(uuid, amount, TransactionType.COINFLIP_REFUND);
                logger.severe("[Coinflip] DB insert failed for " + uuid + ": " + e.getMessage());
                Bukkit.getScheduler().runTask(plugin, () ->
                    player.sendMessage(MM.deserialize("<red>Failed to create coinflip. Your IGC has been refunded.")));
            }
        });
        return null; // success indicator
    }

    /**
     * Attempt to accept a coinflip ticket by id.
     * @return null on success; error string on failure.
     */
    public String acceptTicket(Player acceptor, int ticketId) {
        UUID uuid = acceptor.getUniqueId();

        CoinflipTicket ticket = openTickets.get(ticketId);
        if (ticket == null)             return "That coinflip no longer exists.";
        if (ticket.getCreatorUuid().equals(uuid)) return "You cannot accept your own coinflip.";
        if (ticket.getState() != CoinflipState.OPEN) return "That coinflip is no longer available.";

        EconomyAPI eco = EconomyAPI.getInstance();
        if (eco == null) return "Economy system not available.";

        long result = eco.deductBalance(uuid, ticket.getAmount(), TransactionType.COINFLIP_BET);
        if (result < 0)
            return "You need $" + String.format("%,d", ticket.getAmount()) + " to accept this coinflip.";

        // Mark as pending immediately to prevent double-accept
        ticket.accept(uuid, acceptor.getName());
        openTickets.remove(ticketId);
        creatorIndex.remove(ticket.getCreatorUuid());

        // Update DB async
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                DatabaseManager.getInstance().execute(
                    "UPDATE coinflip_tickets SET state='ACCEPTED_PENDING', acceptor_uuid=?, acceptor_name=? WHERE id=?",
                    uuid.toString(), acceptor.getName(), ticketId
                );
            } catch (SQLException e) {
                logger.warning("[Coinflip] Failed to update accept state for ticket " + ticketId + ": " + e.getMessage());
            }
        });

        // Start animation on main thread
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player creator = Bukkit.getPlayer(ticket.getCreatorUuid());
            CoinflipAnimationGUI.start(plugin, ticket, creator, acceptor);
        });

        return null;
    }

    /**
     * Cancel the creator's own open ticket. Refunds their IGC.
     * @return null on success; error string on failure.
     */
    public String cancelTicket(UUID creatorUuid) {
        Integer ticketId = creatorIndex.get(creatorUuid);
        if (ticketId == null) return "You don't have an active coinflip to cancel.";

        CoinflipTicket ticket = openTickets.remove(ticketId);
        creatorIndex.remove(creatorUuid);
        if (ticket == null) return "Ticket not found.";

        ticket.cancel();

        EconomyAPI eco = EconomyAPI.getInstance();
        if (eco != null) eco.addBalance(creatorUuid, ticket.getAmount(), TransactionType.COINFLIP_REFUND);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                DatabaseManager.getInstance().execute(
                    "UPDATE coinflip_tickets SET state='CANCELLED' WHERE id=?", ticket.getId());
            } catch (SQLException e) {
                logger.warning("[Coinflip] Failed to mark ticket " + ticket.getId() + " CANCELLED: " + e.getMessage());
            }
        });

        CoinflipBrowserGUI.refreshForOnlinePlayers();
        return null;
    }

    /**
     * Called by CoinflipAnimationGUI when animation completes.
     * Awards full pool to winner, broadcasts result, logs to DB.
     */
    public void resolve(CoinflipTicket ticket, UUID winnerUuid) {
        if (ticket.getState() == CoinflipState.RESOLVED) return; // idempotent guard

        ticket.resolve(winnerUuid);

        long prize = ticket.getAmount() * 2;
        EconomyAPI eco = EconomyAPI.getInstance();
        if (eco != null) eco.addBalance(winnerUuid, prize, TransactionType.COINFLIP_WIN);

        // Persist result and log completed game
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                DatabaseManager.getInstance().execute(
                    "UPDATE coinflip_tickets SET state='RESOLVED', winner_uuid=?, resolved_at=NOW() WHERE id=?",
                    winnerUuid.toString(), ticket.getId()
                );
                DatabaseManager.getInstance().execute(
                    "INSERT INTO coinflip_logs (creator_uuid, acceptor_uuid, winner_uuid, amount) VALUES (?,?,?,?)",
                    ticket.getCreatorUuid().toString(),
                    ticket.getAcceptorUuid().toString(),
                    winnerUuid.toString(),
                    ticket.getAmount()
                );
            } catch (SQLException e) {
                logger.warning("[Coinflip] Failed to persist resolve for ticket " + ticket.getId() + ": " + e.getMessage());
            }
        });

        // Broadcast
        String winnerName = winnerUuid.equals(ticket.getCreatorUuid())
            ? ticket.getCreatorName() : ticket.getAcceptorName();
        String loserName  = winnerUuid.equals(ticket.getCreatorUuid())
            ? ticket.getAcceptorName() : ticket.getCreatorName();

        String broadcast = "<gold><bold>COINFLIP</bold> <gray>| <green>" + winnerName +
            " <gray>won <gold>$" + String.format("%,d", prize) + " <gray>against <red>" + loserName + "!";
        Bukkit.broadcast(MM.deserialize(broadcast));
    }
}
