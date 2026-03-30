package com.prison.economy;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * PlayerWallet — in-memory balance holder for one player.
 *
 * IGC and token balances are AtomicLongs so concurrent transactions
 * (e.g. auto-sell firing during a rankup check) never corrupt values.
 * All operations use compare-and-set loops so deductions are always atomic.
 */
public class PlayerWallet {

    private final UUID uuid;
    private final AtomicLong igcBalance;
    private final AtomicLong tokenBalance;

    // Not persisted to DB — resets on each login. Fine for a toggle.
    private volatile boolean autoSell = false;

    // Tracks last manual sell time for rate limiting AND streak window
    private volatile long lastSellTime = 0;

    // Sell streak — consecutive manual sells within the streak timeout window
    private volatile int sellStreak = 0;

    public PlayerWallet(UUID uuid, long igc, long tokens) {
        this.uuid = uuid;
        this.igcBalance   = new AtomicLong(Math.max(0, igc));
        this.tokenBalance = new AtomicLong(Math.max(0, tokens));
    }

    public UUID  getUuid()        { return uuid; }
    public long  getIgc()         { return igcBalance.get(); }
    public long  getTokens()      { return tokenBalance.get(); }
    public boolean isAutoSell()   { return autoSell; }
    public long  getLastSellTime(){ return lastSellTime; }

    public int  getSellStreak()           { return sellStreak; }
    public void setSellStreak(int s)      { this.sellStreak = s; }
    public void setAutoSell(boolean v)    { this.autoSell = v; }
    public void setLastSellTime(long t)   { this.lastSellTime = t; }

    /** Add IGC. Always succeeds. Returns new balance. */
    public long addIgc(long amount) {
        return igcBalance.addAndGet(amount);
    }

    /**
     * Deduct IGC if funds are sufficient.
     * Returns the new balance, or -1 if the player cannot afford it.
     * Uses a CAS loop — safe to call from multiple threads simultaneously.
     */
    public long deductIgc(long amount) {
        while (true) {
            long current = igcBalance.get();
            if (current < amount) return -1L;
            long next = current - amount;
            if (igcBalance.compareAndSet(current, next)) return next;
        }
    }

    /** Force-set IGC (admin use only). */
    public void setIgc(long amount) {
        igcBalance.set(Math.max(0, amount));
    }

    /** Add tokens. Always succeeds. Returns new balance. */
    public long addTokens(long amount) {
        return tokenBalance.addAndGet(amount);
    }

    /**
     * Deduct tokens if sufficient.
     * Returns new balance, or -1 if insufficient.
     */
    public long deductTokens(long amount) {
        while (true) {
            long current = tokenBalance.get();
            if (current < amount) return -1L;
            long next = current - amount;
            if (tokenBalance.compareAndSet(current, next)) return next;
        }
    }

    /** Force-set tokens (admin use only). */
    public void setTokens(long amount) {
        tokenBalance.set(Math.max(0, amount));
    }
}
