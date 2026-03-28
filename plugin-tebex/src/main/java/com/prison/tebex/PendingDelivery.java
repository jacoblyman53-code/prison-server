package com.prison.tebex;

/**
 * PendingDelivery — a single row from tebex_deliveries that has not yet been
 * applied to the player (delivered = 0).
 */
public record PendingDelivery(
        long id,
        String playerUuid,
        String playerUsername,
        String productId,
        String productType,
        String transactionId,
        String extraArgs          // JSON-encoded extra arguments (rank id, crate tier, etc.)
) {}
