package com.prison.cosmetics;

/**
 * Rarity tiers for cosmetic items.
 * Each tier carries a MiniMessage color string used when rendering rarity labels
 * and a corresponding GUI material for the tag icon.
 */
public enum Rarity {

    COMMON    ("<white>",        "Common"),
    UNCOMMON  ("<green>",        "Uncommon"),
    RARE      ("<aqua>",         "Rare"),
    LEGENDARY ("<light_purple>", "Legendary"),
    PRESTIGE  ("<gradient:#FFD700:#FF4500>", "Prestige");

    /** MiniMessage opening color tag — does NOT include the closing tag. */
    private final String colorTag;

    /** Human-readable display name for lore lines. */
    private final String displayName;

    Rarity(String colorTag, String displayName) {
        this.colorTag    = colorTag;
        this.displayName = displayName;
    }

    /** e.g. {@code "<aqua>"} */
    public String colorTag() {
        return colorTag;
    }

    /** e.g. {@code "Rare"} */
    public String displayName() {
        return displayName;
    }

    /**
     * Returns a fully formatted MiniMessage rarity label suitable for lore lines,
     * e.g. {@code "<aqua>◆ Rare"}.
     */
    public String loreLabel() {
        return colorTag + "◆ " + displayName;
    }

    /**
     * Parses a string (case-insensitive) to a Rarity, defaulting to {@link #COMMON}
     * if the string is null or unrecognised.
     */
    public static Rarity fromString(String value) {
        if (value == null) return COMMON;
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return COMMON;
        }
    }
}
