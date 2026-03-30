package com.prison.economy;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BoostManager — in-memory time-limited boost system.
 *
 * Boosts are per-player, per-type (SELL or TOKEN), and expire automatically.
 * No DB persistence — boosts are intentionally ephemeral.
 *
 * External plugins (e.g. plugin-shop token shop) call grantBoost() to apply a boost.
 * EconomyAPI uses this via registered MultiplierProviders.
 */
public class BoostManager {

    public enum BoostType { SELL, TOKEN }

    record ActiveBoost(double multiplier, long expiresAt) {
        boolean isActive() { return System.currentTimeMillis() < expiresAt; }
        long remainingMs() { return Math.max(0L, expiresAt - System.currentTimeMillis()); }
    }

    // ----------------------------------------------------------------
    // Singleton
    // ----------------------------------------------------------------

    private static BoostManager instance;

    public static BoostManager getInstance() { return instance; }

    public static BoostManager initialize() {
        instance = new BoostManager();
        return instance;
    }

    // ----------------------------------------------------------------
    // State
    // ----------------------------------------------------------------

    private final ConcurrentHashMap<UUID, ConcurrentHashMap<BoostType, ActiveBoost>> boosts
            = new ConcurrentHashMap<>();

    private BoostManager() {}

    // ----------------------------------------------------------------
    // Grant / Query
    // ----------------------------------------------------------------

    /**
     * Grant a boost to a player. If they already have an active boost of this type,
     * the new grant replaces it (no stacking).
     */
    public void grantBoost(UUID uuid, BoostType type, double multiplier, long durationMs) {
        long expiry = System.currentTimeMillis() + durationMs;
        boosts.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>())
              .put(type, new ActiveBoost(multiplier, expiry));
    }

    /** Returns the active sell multiplier for a player (1.0 if none). */
    public double getSellMultiplier(UUID uuid) {
        return getMultiplier(uuid, BoostType.SELL);
    }

    /** Returns the active token multiplier for a player (1.0 if none). */
    public double getTokenMultiplier(UUID uuid) {
        return getMultiplier(uuid, BoostType.TOKEN);
    }

    private double getMultiplier(UUID uuid, BoostType type) {
        ConcurrentHashMap<BoostType, ActiveBoost> map = boosts.get(uuid);
        if (map == null) return 1.0;
        ActiveBoost boost = map.get(type);
        return (boost != null && boost.isActive()) ? boost.multiplier() : 1.0;
    }

    public boolean hasBoost(UUID uuid, BoostType type) {
        return getMultiplier(uuid, type) > 1.0;
    }

    /** Remaining milliseconds on an active boost, or 0 if expired/absent. */
    public long getRemainingMs(UUID uuid, BoostType type) {
        ConcurrentHashMap<BoostType, ActiveBoost> map = boosts.get(uuid);
        if (map == null) return 0L;
        ActiveBoost boost = map.get(type);
        return (boost != null && boost.isActive()) ? boost.remainingMs() : 0L;
    }

    /** Remove all boosts for a player (call on quit to free memory). */
    public void cleanup(UUID uuid) {
        boosts.remove(uuid);
    }

    // ----------------------------------------------------------------
    // Formatting helper
    // ----------------------------------------------------------------

    public String formatRemaining(UUID uuid, BoostType type) {
        long ms = getRemainingMs(uuid, type);
        if (ms <= 0) return "";
        long secs = ms / 1000;
        long mins = secs / 60;
        long remainSecs = secs % 60;
        if (mins > 0) return mins + "m " + remainSecs + "s";
        return remainSecs + "s";
    }
}
