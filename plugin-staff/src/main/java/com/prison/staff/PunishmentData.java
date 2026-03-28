package com.prison.staff;

import java.time.LocalDateTime;

/**
 * PunishmentData — one row from the punishments table.
 *
 * expiresAt is null for permanent punishments.
 * ipAddress is only populated for IPBAN records.
 */
public record PunishmentData(
    long id,
    String playerUuid,
    String ipAddress,        // null unless IPBAN
    PunishmentType type,
    String reason,
    String issuedByUuid,
    LocalDateTime issuedAt,
    LocalDateTime expiresAt, // null = permanent
    boolean active
) {
    /** True if this punishment is still in effect right now. */
    public boolean isCurrentlyActive() {
        if (!active) return false;
        if (expiresAt == null) return true;
        return expiresAt.isAfter(LocalDateTime.now());
    }

    /** Human-readable expiry string for ban screens / messages. */
    public String expiryString() {
        if (expiresAt == null) return "Never (permanent)";
        return expiresAt.toString().replace("T", " ").substring(0, 16);
    }
}
