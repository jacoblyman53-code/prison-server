package com.prison.events;

/**
 * The category of boost that a server event applies.
 */
public enum EventType {

    /** Multiplies IGC earned from selling items. */
    SELL_BOOST,

    /** Multiplies tokens earned from mining. */
    TOKEN_STORM,

    /**
     * Special sell multiplier event — uses the sell multiplier pipeline
     * and additionally fires confetti particles for sellers.
     */
    JACKPOT_HOUR
}
