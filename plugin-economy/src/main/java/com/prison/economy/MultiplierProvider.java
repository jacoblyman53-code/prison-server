package com.prison.economy;

import java.util.UUID;

/**
 * MultiplierProvider — pluggable multiplier source for sell or token bonuses.
 *
 * Other plugins (Gangs, Events, etc.) register implementations via
 * EconomyAPI.setGangSellBonusProvider() / setEventSellBonusProvider() etc.
 * The economy system multiplies all active sources together.
 */
@FunctionalInterface
public interface MultiplierProvider {

    /**
     * Return the bonus multiplier for this player.
     * Must return 1.0 (not 0.0) when no bonus is active.
     */
    double getMultiplier(UUID playerUuid);
}
