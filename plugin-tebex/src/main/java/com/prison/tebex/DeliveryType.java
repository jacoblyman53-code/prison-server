package com.prison.tebex;

/**
 * DeliveryType — what kind of product a Tebex purchase delivers.
 *
 * The {@link #key} value is passed as the {@code product_type} argument in the
 * /tebexdeliver command and stored in tebex_deliveries.product_type.
 */
public enum DeliveryType {

    /**
     * Grant the player a donor rank.
     * Args: [0] = rank ID (e.g. "elite")
     */
    DONOR_RANK("donor_rank"),

    /**
     * Give the player one or more crate keys.
     * Args: [0] = tier ID (e.g. "legendary"), [1] = amount (e.g. "3")
     */
    CRATE_KEY("crate_key");

    /** The string token used in the /tebexdeliver command. */
    public final String key;

    DeliveryType(String key) { this.key = key; }

    /** Parse from the command argument, or null if unrecognised. */
    public static DeliveryType fromKey(String s) {
        if (s == null) return null;
        for (DeliveryType t : values()) {
            if (t.key.equalsIgnoreCase(s)) return t;
        }
        return null;
    }
}
