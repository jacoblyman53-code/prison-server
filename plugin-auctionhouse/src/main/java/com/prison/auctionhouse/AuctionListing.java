package com.prison.auctionhouse;

import org.bukkit.inventory.ItemStack;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

public record AuctionListing(
    long id,
    UUID sellerUuid,
    String sellerName,
    ItemStack item,
    long priceIgc,
    LocalDateTime listedAt,
    LocalDateTime expiresAt,
    String status
) {
    /** Seconds remaining until expiry. Negative if expired. */
    public long secondsRemaining() {
        return Duration.between(LocalDateTime.now(), expiresAt).getSeconds();
    }

    public String formattedTimeRemaining() {
        long secs = secondsRemaining();
        if (secs <= 0) return "Expired";
        long hours = secs / 3600;
        long mins  = (secs % 3600) / 60;
        if (hours >= 24) return (hours / 24) + "d " + (hours % 24) + "h";
        if (hours > 0)   return hours + "h " + mins + "m";
        return mins + "m";
    }
}
