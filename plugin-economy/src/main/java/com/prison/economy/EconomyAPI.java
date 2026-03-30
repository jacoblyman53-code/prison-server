package com.prison.economy;

import com.prison.database.DatabaseManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * EconomyAPI — the public interface for all balance and token operations.
 *
 * Other plugins call this instead of touching the database directly.
 * All balance operations are atomic (AtomicLong CAS loops) and save async.
 *
 * Usage:
 *   EconomyAPI api = EconomyAPI.getInstance();
 *   long bal  = api.getBalance(uuid);
 *   long toks = api.getTokens(uuid);
 *   boolean ok = api.deductBalance(uuid, cost, TransactionType.RANKUP) >= 0;
 */
public class EconomyAPI {

    private static EconomyAPI instance;

    private final Logger logger;
    private final ConcurrentHashMap<UUID, PlayerWallet> wallets = new ConcurrentHashMap<>();

    // Sell price provider — replaced by mines plugin at startup for mine-tier pricing
    private volatile SellPriceProvider sellPriceProvider;

    // External sell/token multiplier providers — set by Gangs, Events, Prestige, etc.
    // Each returns 1.0 when inactive. Products are stacked together.
    private volatile MultiplierProvider gangSellBonusProvider     = uuid -> 1.0;
    private volatile MultiplierProvider eventSellBonusProvider    = uuid -> 1.0;
    private volatile MultiplierProvider prestigeSellBonusProvider = uuid -> 1.0;
    private volatile MultiplierProvider boostSellProvider         = uuid -> 1.0;
    private volatile MultiplierProvider gangTokenBonusProvider     = uuid -> 1.0;
    private volatile MultiplierProvider eventTokenBonusProvider    = uuid -> 1.0;
    private volatile MultiplierProvider prestigeTokenBonusProvider = uuid -> 1.0;
    private volatile MultiplierProvider boostTokenProvider         = uuid -> 1.0;

    // Cached baltop list — refreshed on a schedule
    private volatile List<BaltopEntry> baltopCache = List.of();

    EconomyAPI(Logger logger, SellPriceProvider defaultProvider) {
        this.logger = logger;
        this.sellPriceProvider = defaultProvider;
        instance = this;
    }

    public static EconomyAPI getInstance() { return instance; }

    // ----------------------------------------------------------------
    // Sell Price Provider
    // ----------------------------------------------------------------

    /**
     * Replace the sell price provider. Called by the mines plugin on startup
     * to provide mine-tier-aware prices. Safe to call at any time.
     */
    public void setSellPriceProvider(SellPriceProvider provider) {
        this.sellPriceProvider = provider;
    }

    /** Returns the IGC sell price for one unit of a material for this player. */
    public long getSellPrice(Material material, Player player) {
        return sellPriceProvider.getSellPrice(material, player);
    }

    // ----------------------------------------------------------------
    // External Multiplier Providers (set by Gangs, Events plugins)
    // ----------------------------------------------------------------

    public void setGangSellBonusProvider(MultiplierProvider p)     { this.gangSellBonusProvider     = p; }
    public void setEventSellBonusProvider(MultiplierProvider p)    { this.eventSellBonusProvider    = p; }
    public void setPrestigeSellBonusProvider(MultiplierProvider p) { this.prestigeSellBonusProvider = p; }
    public void setBoostSellProvider(MultiplierProvider p)         { this.boostSellProvider         = p; }
    public void setGangTokenBonusProvider(MultiplierProvider p)     { this.gangTokenBonusProvider     = p; }
    public void setEventTokenBonusProvider(MultiplierProvider p)    { this.eventTokenBonusProvider    = p; }
    public void setPrestigeTokenBonusProvider(MultiplierProvider p) { this.prestigeTokenBonusProvider = p; }
    public void setBoostTokenProvider(MultiplierProvider p)         { this.boostTokenProvider         = p; }

    // Individual getters used by EconomyPlugin for the action bar breakdown display
    public double getGangSellBonus(UUID uuid)     { return gangSellBonusProvider.getMultiplier(uuid); }
    public double getEventSellBonus(UUID uuid)    { return eventSellBonusProvider.getMultiplier(uuid); }
    public double getPrestigeSellBonus(UUID uuid) { return prestigeSellBonusProvider.getMultiplier(uuid); }
    public double getBoostSellBonus(UUID uuid)    { return boostSellProvider.getMultiplier(uuid); }

    /**
     * Returns the combined external sell multiplier for a player
     * (gang × event × prestige × boost). Returns 1.0 when no bonuses are active.
     */
    public double getExternalSellMultiplier(UUID uuid) {
        return gangSellBonusProvider.getMultiplier(uuid)
             * eventSellBonusProvider.getMultiplier(uuid)
             * prestigeSellBonusProvider.getMultiplier(uuid)
             * boostSellProvider.getMultiplier(uuid);
    }

    /**
     * Returns the combined external token earn multiplier for a player
     * (gang × event × prestige × boost). Returns 1.0 when no bonuses are active.
     */
    public double getExternalTokenMultiplier(UUID uuid) {
        return gangTokenBonusProvider.getMultiplier(uuid)
             * eventTokenBonusProvider.getMultiplier(uuid)
             * prestigeTokenBonusProvider.getMultiplier(uuid)
             * boostTokenProvider.getMultiplier(uuid);
    }

    // ----------------------------------------------------------------
    // Wallet Loading / Saving
    // ----------------------------------------------------------------

    /**
     * Load a player's wallet from the database into the cache.
     * Called async on PlayerJoinEvent after the permission cache loads.
     */
    public CompletableFuture<Void> loadPlayer(UUID uuid) {
        return CompletableFuture.runAsync(() -> {
            try {
                PlayerWallet wallet = DatabaseManager.getInstance().query(
                    "SELECT igc_balance, token_balance FROM players WHERE uuid = ?",
                    rs -> rs.next()
                        ? new PlayerWallet(uuid, rs.getLong("igc_balance"), rs.getLong("token_balance"))
                        : new PlayerWallet(uuid, 0L, 0L),
                    uuid.toString()
                );
                wallets.put(uuid, wallet);
                logger.fine("[Economy] Loaded wallet for " + uuid);
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "[Economy] Failed to load wallet for " + uuid, e);
                wallets.put(uuid, new PlayerWallet(uuid, 0L, 0L));
            }
        });
    }

    /**
     * Save and remove a player's wallet from the cache.
     * Called on PlayerQuitEvent.
     */
    public CompletableFuture<Void> saveAndUnload(UUID uuid) {
        PlayerWallet wallet = wallets.remove(uuid);
        if (wallet == null) return CompletableFuture.completedFuture(null);
        return persist(wallet);
    }

    /**
     * Persist a wallet to the database without removing it from the cache.
     * Called on every balance change and on the auto-save tick.
     */
    public CompletableFuture<Void> saveWallet(UUID uuid) {
        PlayerWallet wallet = wallets.get(uuid);
        if (wallet == null) return CompletableFuture.completedFuture(null);
        return persist(wallet);
    }

    /** Save every loaded wallet — called by the 5-minute auto-save task. */
    public void saveAllWallets() {
        for (PlayerWallet wallet : wallets.values()) {
            persist(wallet);
        }
    }

    private CompletableFuture<Void> persist(PlayerWallet wallet) {
        return CompletableFuture.runAsync(() -> {
            try {
                DatabaseManager.getInstance().execute(
                    "UPDATE players SET igc_balance = ?, token_balance = ? WHERE uuid = ?",
                    wallet.getIgc(),
                    wallet.getTokens(),
                    wallet.getUuid().toString()
                );
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "[Economy] Failed to save wallet for " + wallet.getUuid(), e);
            }
        });
    }

    // ----------------------------------------------------------------
    // Balance Queries
    // ----------------------------------------------------------------

    /** Returns the player's current IGC balance, or 0 if not loaded. */
    public long getBalance(UUID uuid) {
        PlayerWallet w = wallets.get(uuid);
        return w != null ? w.getIgc() : 0L;
    }

    /** Returns the player's current token balance, or 0 if not loaded. */
    public long getTokens(UUID uuid) {
        PlayerWallet w = wallets.get(uuid);
        return w != null ? w.getTokens() : 0L;
    }

    // ----------------------------------------------------------------
    // IGC Operations
    // ----------------------------------------------------------------

    /**
     * Add IGC to a player's balance.
     * Saves async and logs the transaction. Returns the new balance.
     */
    public long addBalance(UUID uuid, long amount, TransactionType type) {
        PlayerWallet w = wallets.get(uuid);
        if (w == null) return 0L;
        long newBal = w.addIgc(amount);
        persist(w);
        logTransaction(uuid, "IGC", type, amount, newBal);
        return newBal;
    }

    /** Convenience — uses MINE_SELL transaction type. */
    public long addBalance(UUID uuid, long amount) {
        return addBalance(uuid, amount, TransactionType.MINE_SELL);
    }

    /**
     * Deduct IGC from a player's balance if they have enough.
     * Returns the new balance, or -1 if the player cannot afford it.
     * Saves async and logs on success.
     */
    public long deductBalance(UUID uuid, long amount, TransactionType type) {
        PlayerWallet w = wallets.get(uuid);
        if (w == null) return -1L;
        long newBal = w.deductIgc(amount);
        if (newBal < 0) return -1L;
        persist(w);
        logTransaction(uuid, "IGC", type, -amount, newBal);
        return newBal;
    }

    /** Convenience — uses RANKUP transaction type. */
    public long deductBalance(UUID uuid, long amount) {
        return deductBalance(uuid, amount, TransactionType.RANKUP);
    }

    /**
     * Force-set a player's IGC balance (admin use).
     * Does NOT log as a normal transaction — caller should log separately.
     */
    public void setBalance(UUID uuid, long amount, TransactionType type) {
        PlayerWallet w = wallets.get(uuid);
        if (w == null) return;
        w.setIgc(amount);
        persist(w);
        logTransaction(uuid, "IGC", type, amount, w.getIgc());
    }

    // ----------------------------------------------------------------
    // Token Operations
    // ----------------------------------------------------------------

    /**
     * Add tokens to a player's balance.
     * For TOKEN_EARN transactions, the gang + event token multipliers are applied automatically.
     * Returns the new token balance.
     */
    public long addTokens(UUID uuid, long amount, TransactionType type) {
        PlayerWallet w = wallets.get(uuid);
        if (w == null) return 0L;
        long finalAmount = (type == TransactionType.TOKEN_EARN)
            ? (long)(amount * getExternalTokenMultiplier(uuid))
            : amount;
        long newBal = w.addTokens(finalAmount);
        persist(w);
        logTransaction(uuid, "TOKEN", type, finalAmount, newBal);
        return newBal;
    }

    /** Convenience — uses TOKEN_EARN type. */
    public long addTokens(UUID uuid, long amount) {
        return addTokens(uuid, amount, TransactionType.TOKEN_EARN);
    }

    /**
     * Deduct tokens if the player has enough.
     * Returns new balance, or -1 if insufficient.
     */
    public long deductTokens(UUID uuid, long amount, TransactionType type) {
        PlayerWallet w = wallets.get(uuid);
        if (w == null) return -1L;
        long newBal = w.deductTokens(amount);
        if (newBal < 0) return -1L;
        persist(w);
        logTransaction(uuid, "TOKEN", type, -amount, newBal);
        return newBal;
    }

    /** Convenience — uses ENCHANT_PURCHASE type. */
    public long deductTokens(UUID uuid, long amount) {
        return deductTokens(uuid, amount, TransactionType.ENCHANT_PURCHASE);
    }

    /**
     * Force-set token balance (admin use).
     */
    public void setTokens(UUID uuid, long amount, TransactionType type) {
        PlayerWallet w = wallets.get(uuid);
        if (w == null) return;
        w.setTokens(amount);
        persist(w);
        logTransaction(uuid, "TOKEN", type, amount, w.getTokens());
    }

    // ----------------------------------------------------------------
    // Auto-Sell
    // ----------------------------------------------------------------

    /** Returns true if the player has auto-sell enabled. */
    public boolean hasAutoSell(UUID uuid) {
        PlayerWallet w = wallets.get(uuid);
        return w != null && w.isAutoSell();
    }

    /** Toggle or set auto-sell for a player. */
    public void setAutoSell(UUID uuid, boolean enabled) {
        PlayerWallet w = wallets.get(uuid);
        if (w != null) w.setAutoSell(enabled);
    }

    // ----------------------------------------------------------------
    // Sell Rate Limiting (manual /sell and /sellall only)
    // ----------------------------------------------------------------

    /** Returns true if enough time has passed since the player's last manual sell. */
    public boolean canSell(UUID uuid, long minIntervalMs) {
        PlayerWallet w = wallets.get(uuid);
        if (w == null) return false;
        return System.currentTimeMillis() - w.getLastSellTime() >= minIntervalMs;
    }

    /** Record a manual sell, updating streak and last-sell timestamp. Returns new streak count. */
    public int recordSell(UUID uuid, long streakTimeoutMs) {
        PlayerWallet w = wallets.get(uuid);
        if (w == null) return 1;
        long now  = System.currentTimeMillis();
        long last = w.getLastSellTime();
        int streak;
        if (last > 0 && (now - last) <= streakTimeoutMs) {
            streak = w.getSellStreak() + 1;
        } else {
            streak = 1;
        }
        w.setSellStreak(streak);
        w.setLastSellTime(now);
        return streak;
    }

    /** Legacy no-arg overload — records sell with no streak tracking. */
    public void recordSell(UUID uuid) {
        PlayerWallet w = wallets.get(uuid);
        if (w != null) w.setLastSellTime(System.currentTimeMillis());
    }

    /** Returns the sell multiplier for a given streak count. Thresholds: 5=1.05x 10=1.10x 25=1.20x 50=1.35x 100=1.50x */
    public double getStreakMultiplier(int streak) {
        if (streak >= 100) return 1.50;
        if (streak >= 50)  return 1.35;
        if (streak >= 25)  return 1.20;
        if (streak >= 10)  return 1.10;
        if (streak >= 5)   return 1.05;
        return 1.0;
    }

    /** Returns the current sell streak for a player (0 if not loaded). */
    public int getSellStreak(UUID uuid) {
        PlayerWallet w = wallets.get(uuid);
        return w != null ? w.getSellStreak() : 0;
    }

    // ----------------------------------------------------------------
    // Baltop
    // ----------------------------------------------------------------

    public record BaltopEntry(String name, long balance) {}

    /** Returns the last cached baltop list (may be up to 60s stale). */
    public List<BaltopEntry> getBaltop() { return baltopCache; }

    /** Refresh the baltop cache from the database. Called on a schedule. */
    public CompletableFuture<Void> refreshBaltop() {
        return CompletableFuture.runAsync(() -> {
            try {
                List<BaltopEntry> top = DatabaseManager.getInstance().query(
                    "SELECT username, igc_balance FROM players ORDER BY igc_balance DESC LIMIT 10",
                    rs -> {
                        List<BaltopEntry> list = new ArrayList<>();
                        while (rs.next()) {
                            list.add(new BaltopEntry(
                                rs.getString("username"),
                                rs.getLong("igc_balance")
                            ));
                        }
                        return list;
                    }
                );
                baltopCache = Collections.unmodifiableList(top);
            } catch (SQLException e) {
                logger.log(Level.WARNING, "[Economy] Failed to refresh baltop", e);
            }
        });
    }

    // ----------------------------------------------------------------
    // Offline Balance Operations
    // ----------------------------------------------------------------
    // These methods work whether or not the player is currently online.
    // If online, they use the in-memory wallet (instant). If offline,
    // they perform a direct DB operation async and return a future.
    // Used by admin tools so staff can adjust balances for offline players.

    /** Add IGC for a player regardless of online status. Returns new balance, or -1 on error. */
    public CompletableFuture<Long> addBalanceAsync(UUID uuid, long amount, TransactionType type) {
        PlayerWallet w = wallets.get(uuid);
        if (w != null) {
            long newBal = addBalance(uuid, amount, type);
            return CompletableFuture.completedFuture(newBal);
        }
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = DatabaseManager.getInstance().getConnection()) {
                try (PreparedStatement upd = conn.prepareStatement(
                        "UPDATE players SET igc_balance = igc_balance + ? WHERE uuid = ?")) {
                    upd.setLong(1, amount);
                    upd.setString(2, uuid.toString());
                    upd.executeUpdate();
                }
                try (PreparedStatement sel = conn.prepareStatement(
                        "SELECT igc_balance FROM players WHERE uuid = ?")) {
                    sel.setString(1, uuid.toString());
                    try (ResultSet rs = sel.executeQuery()) {
                        long newBal = rs.next() ? rs.getLong(1) : 0L;
                        logTransaction(uuid, "IGC", type, amount, newBal);
                        return newBal;
                    }
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "[Economy] addBalanceAsync failed for " + uuid, e);
                return -1L;
            }
        });
    }

    /**
     * Deduct IGC for a player regardless of online status.
     * Returns new balance, or -1 if insufficient funds or error.
     */
    public CompletableFuture<Long> deductBalanceAsync(UUID uuid, long amount, TransactionType type) {
        PlayerWallet w = wallets.get(uuid);
        if (w != null) {
            long newBal = deductBalance(uuid, amount, type);
            return CompletableFuture.completedFuture(newBal);
        }
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = DatabaseManager.getInstance().getConnection()) {
                // Atomic conditional deduct — only succeeds if balance is sufficient
                int rows;
                try (PreparedStatement upd = conn.prepareStatement(
                        "UPDATE players SET igc_balance = igc_balance - ? WHERE uuid = ? AND igc_balance >= ?")) {
                    upd.setLong(1, amount);
                    upd.setString(2, uuid.toString());
                    upd.setLong(3, amount);
                    rows = upd.executeUpdate();
                }
                if (rows == 0) return -1L; // insufficient funds
                try (PreparedStatement sel = conn.prepareStatement(
                        "SELECT igc_balance FROM players WHERE uuid = ?")) {
                    sel.setString(1, uuid.toString());
                    try (ResultSet rs = sel.executeQuery()) {
                        long newBal = rs.next() ? rs.getLong(1) : 0L;
                        logTransaction(uuid, "IGC", type, -amount, newBal);
                        return newBal;
                    }
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "[Economy] deductBalanceAsync failed for " + uuid, e);
                return -1L;
            }
        });
    }

    /** Force-set IGC for a player regardless of online status. */
    public CompletableFuture<Void> setBalanceAsync(UUID uuid, long amount, TransactionType type) {
        PlayerWallet w = wallets.get(uuid);
        if (w != null) {
            setBalance(uuid, amount, type);
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.runAsync(() -> {
            try {
                DatabaseManager.getInstance().execute(
                    "UPDATE players SET igc_balance = ? WHERE uuid = ?",
                    amount, uuid.toString());
                logTransaction(uuid, "IGC", type, amount, amount);
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "[Economy] setBalanceAsync failed for " + uuid, e);
            }
        });
    }

    /** Add tokens for a player regardless of online status. Returns new balance, or -1 on error. */
    public CompletableFuture<Long> addTokensAsync(UUID uuid, long amount, TransactionType type) {
        PlayerWallet w = wallets.get(uuid);
        if (w != null) {
            long newBal = addTokens(uuid, amount, type);
            return CompletableFuture.completedFuture(newBal);
        }
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = DatabaseManager.getInstance().getConnection()) {
                try (PreparedStatement upd = conn.prepareStatement(
                        "UPDATE players SET token_balance = token_balance + ? WHERE uuid = ?")) {
                    upd.setLong(1, amount);
                    upd.setString(2, uuid.toString());
                    upd.executeUpdate();
                }
                try (PreparedStatement sel = conn.prepareStatement(
                        "SELECT token_balance FROM players WHERE uuid = ?")) {
                    sel.setString(1, uuid.toString());
                    try (ResultSet rs = sel.executeQuery()) {
                        long newBal = rs.next() ? rs.getLong(1) : 0L;
                        logTransaction(uuid, "TOKEN", type, amount, newBal);
                        return newBal;
                    }
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "[Economy] addTokensAsync failed for " + uuid, e);
                return -1L;
            }
        });
    }

    /** Deduct tokens regardless of online status. Returns new balance, or -1 if insufficient. */
    public CompletableFuture<Long> deductTokensAsync(UUID uuid, long amount, TransactionType type) {
        PlayerWallet w = wallets.get(uuid);
        if (w != null) {
            long newBal = deductTokens(uuid, amount, type);
            return CompletableFuture.completedFuture(newBal);
        }
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = DatabaseManager.getInstance().getConnection()) {
                int rows;
                try (PreparedStatement upd = conn.prepareStatement(
                        "UPDATE players SET token_balance = token_balance - ? WHERE uuid = ? AND token_balance >= ?")) {
                    upd.setLong(1, amount);
                    upd.setString(2, uuid.toString());
                    upd.setLong(3, amount);
                    rows = upd.executeUpdate();
                }
                if (rows == 0) return -1L;
                try (PreparedStatement sel = conn.prepareStatement(
                        "SELECT token_balance FROM players WHERE uuid = ?")) {
                    sel.setString(1, uuid.toString());
                    try (ResultSet rs = sel.executeQuery()) {
                        long newBal = rs.next() ? rs.getLong(1) : 0L;
                        logTransaction(uuid, "TOKEN", type, -amount, newBal);
                        return newBal;
                    }
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "[Economy] deductTokensAsync failed for " + uuid, e);
                return -1L;
            }
        });
    }

    /** Force-set tokens for a player regardless of online status. */
    public CompletableFuture<Void> setTokensAsync(UUID uuid, long amount, TransactionType type) {
        PlayerWallet w = wallets.get(uuid);
        if (w != null) {
            setTokens(uuid, amount, type);
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.runAsync(() -> {
            try {
                DatabaseManager.getInstance().execute(
                    "UPDATE players SET token_balance = ? WHERE uuid = ?",
                    amount, uuid.toString());
                logTransaction(uuid, "TOKEN", type, amount, amount);
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "[Economy] setTokensAsync failed for " + uuid, e);
            }
        });
    }

    // ----------------------------------------------------------------
    // Transaction Logging
    // ----------------------------------------------------------------

    private void logTransaction(UUID uuid, String currencyType, TransactionType type,
                                long amount, long balanceAfter) {
        // Use queueWrite for logs — slight delay is fine, avoids DB pressure
        DatabaseManager.getInstance().queueWrite(
            "INSERT INTO transactions (player_uuid, currency_type, type, amount, balance_after) VALUES (?, ?, ?, ?, ?)",
            uuid.toString(),
            currencyType,
            type.name().toLowerCase(),
            amount,
            balanceAfter
        );
    }

    /**
     * Fetch recent transactions for a player (used by /tokenlog).
     * Async — call from a CompletableFuture or async thread.
     */
    public List<TransactionRecord> getRecentTransactions(UUID uuid, String currencyType, int limit)
            throws SQLException {
        return DatabaseManager.getInstance().query(
            "SELECT type, amount, balance_after, timestamp FROM transactions " +
            "WHERE player_uuid = ? AND currency_type = ? ORDER BY timestamp DESC LIMIT ?",
            rs -> {
                List<TransactionRecord> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(new TransactionRecord(
                        rs.getString("type"),
                        rs.getLong("amount"),
                        rs.getLong("balance_after"),
                        rs.getTimestamp("timestamp").toLocalDateTime()
                    ));
                }
                return list;
            },
            uuid.toString(), currencyType, limit
        );
    }

    public record TransactionRecord(String type, long amount, long balanceAfter,
                                    java.time.LocalDateTime timestamp) {}
}
