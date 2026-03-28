package com.prison.crates;

/** The possible types of reward a crate can contain. */
public enum RewardType {
    /** Award a flat amount of IGC directly to the player's wallet. */
    IGC,
    /** Award a flat amount of Tokens directly to the player's wallet. */
    TOKEN,
    /** Give the player one or more crate keys of a specified tier. */
    CRATE_KEY,
    /** Give the player a custom serialized ItemStack. */
    ITEM
}
