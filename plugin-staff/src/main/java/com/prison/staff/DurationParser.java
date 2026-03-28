package com.prison.staff;

import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DurationParser — parses duration strings like "30m", "2h", "7d", "1d12h30m".
 *
 * Supported units: w (weeks), d (days), h (hours), m (minutes)
 */
public final class DurationParser {

    private static final Pattern TOKEN = Pattern.compile("(\\d+)([wdhm])");

    private DurationParser() {}

    /**
     * Parse a duration string and return the resulting expiry timestamp.
     * Returns null if the string is invalid or zero.
     *
     * @throws IllegalArgumentException if the format is unrecognised
     */
    public static LocalDateTime parseExpiry(String input) {
        long totalSeconds = parseSeconds(input);
        return LocalDateTime.now().plusSeconds(totalSeconds);
    }

    /** Returns total seconds represented by the duration string. */
    public static long parseSeconds(String input) {
        Matcher m = TOKEN.matcher(input.toLowerCase().trim());
        long total = 0;
        boolean matched = false;
        while (m.find()) {
            matched = true;
            long value = Long.parseLong(m.group(1));
            total += switch (m.group(2)) {
                case "w" -> value * 7 * 24 * 3600;
                case "d" -> value * 24 * 3600;
                case "h" -> value * 3600;
                case "m" -> value * 60;
                default  -> 0;
            };
        }
        if (!matched) {
            throw new IllegalArgumentException("Unrecognised duration: " + input);
        }
        return total;
    }

    /** Human-readable format of a duration in seconds, e.g. "2d 3h 15m". */
    public static String format(long seconds) {
        if (seconds <= 0) return "0m";
        long w = seconds / (7 * 24 * 3600); seconds %= (7 * 24 * 3600);
        long d = seconds / (24 * 3600);      seconds %= (24 * 3600);
        long h = seconds / 3600;             seconds %= 3600;
        long mn = seconds / 60;

        StringBuilder sb = new StringBuilder();
        if (w  > 0) sb.append(w).append("w ");
        if (d  > 0) sb.append(d).append("d ");
        if (h  > 0) sb.append(h).append("h ");
        if (mn > 0) sb.append(mn).append("m");
        return sb.toString().trim();
    }
}
