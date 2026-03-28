package com.prison.anticheat;

/**
 * FlagType — the categories of exploit the anti-cheat monitors.
 * The name() value is stored directly in the anticheat_flags.flag_type column.
 */
public enum FlagType {
    SELL_RATE("Sell Rate Spam"),
    BLOCK_BREAK_RATE("Block Break Rate"),
    TOKEN_EARN_RATE("Token Earn Rate");

    /** Human-readable label shown in staff alerts and the /acflags GUI. */
    public final String display;

    FlagType(String display) {
        this.display = display;
    }
}
