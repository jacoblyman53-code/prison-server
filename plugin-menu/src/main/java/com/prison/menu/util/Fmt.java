package com.prison.menu.util;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Fmt — number and text formatting utilities shared across all menus.
 */
public final class Fmt {

    private static final NumberFormat NF = NumberFormat.getInstance(Locale.US);

    private Fmt() {}

    /** Format a long as "1,234,567". */
    public static String number(long n) { return NF.format(n); }

    /** Format a double multiplier as "1.25x". */
    public static String multiplier(double d) {
        return String.format("%.2fx", d);
    }

    /** Format milliseconds as "1h 23m", "45m 12s", or "30s". */
    public static String duration(long ms) {
        if (ms <= 0) return "0s";
        long hours   = TimeUnit.MILLISECONDS.toHours(ms);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(ms) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60;
        if (hours > 0)   return hours + "h " + minutes + "m";
        if (minutes > 0) return minutes + "m " + seconds + "s";
        return seconds + "s";
    }

    /** Shorten large numbers: 1_200_000 → "1.2M", 5000 → "5K", else normal. */
    public static String compact(long n) {
        if (n >= 1_000_000_000L) return String.format("%.1fB", n / 1_000_000_000.0);
        if (n >= 1_000_000L)    return String.format("%.1fM", n / 1_000_000.0);
        if (n >= 1_000L)        return String.format("%.1fK", n / 1_000.0);
        return String.valueOf(n);
    }

    /** "DIAMOND_PICKAXE" → "Diamond Pickaxe". */
    public static String mat(String name) {
        return Gui.formatMat(name);
    }
}
