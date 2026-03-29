package com.prison.kits;

import com.prison.database.DatabaseManager;
import com.prison.permissions.PermissionEngine;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * KitsManager — core kit logic.
 *
 * Cooldown strategy:
 *   FREE/RANK kits:  claimable when (now - last_claimed) >= cooldownMs
 *                    cooldownMs = 0 means one-time only
 *   DONOR kits:      claimable when (server_start > last_claimed)
 *                    AND (now - last_claimed) >= minDonorCooldownMs
 *
 * Cooldowns are cached in memory per player (loaded on join, updated on claim, cleared on quit).
 * DB is source of truth — cache is just for fast reads.
 */
public class KitsManager {

    private static KitsManager instance;

    // kit_id → KitData
    private final Map<String, KitData> kits;

    // player UUID → (kit_id → last_claimed_ms)
    private final ConcurrentHashMap<UUID, Map<String, Long>> cooldownCache = new ConcurrentHashMap<>();

    private final Logger logger;
    private final long serverStartMs;

    private KitsManager(Map<String, KitData> kits, Logger logger, long serverStartMs) {
        this.kits          = Collections.unmodifiableMap(kits);
        this.logger        = logger;
        this.serverStartMs = serverStartMs;
    }

    public static KitsManager initialize(Map<String, KitData> kits, Logger logger, long serverStartMs) {
        if (instance != null) throw new IllegalStateException("KitsManager already initialized");
        instance = new KitsManager(kits, logger, serverStartMs);
        return instance;
    }

    public static KitsManager getInstance() { return instance; }

    // ----------------------------------------------------------------
    // Kit lookup
    // ----------------------------------------------------------------

    public KitData getKit(String id) {
        return kits.get(id.toLowerCase());
    }

    public Collection<KitData> getAllKits() {
        return kits.values();
    }

    /** Returns kits this player has access to (type + rank requirements met). */
    public List<KitData> getAccessibleKits(Player player) {
        List<KitData> accessible = new ArrayList<>();
        for (KitData kit : kits.values()) {
            if (meetsRequirements(player, kit)) accessible.add(kit);
        }
        return accessible;
    }

    // ----------------------------------------------------------------
    // Cooldown cache — load / unload
    // ----------------------------------------------------------------

    public CompletableFuture<Void> loadPlayer(UUID uuid) {
        return CompletableFuture.runAsync(() -> {
            try {
                Map<String, Long> playerCooldowns = DatabaseManager.getInstance().query(
                    "SELECT kit_id, UNIX_TIMESTAMP(last_claimed) * 1000 AS claimed_ms " +
                    "FROM kit_cooldowns WHERE player_uuid = ?",
                    rs -> {
                        Map<String, Long> map = new HashMap<>();
                        while (rs.next()) {
                            map.put(rs.getString("kit_id"), rs.getLong("claimed_ms"));
                        }
                        return map;
                    },
                    uuid.toString()
                );
                cooldownCache.put(uuid, new ConcurrentHashMap<>(playerCooldowns));
            } catch (SQLException e) {
                logger.log(Level.WARNING, "[Kits] Failed to load cooldowns for " + uuid, e);
                cooldownCache.put(uuid, new ConcurrentHashMap<>());
            }
        });
    }

    public void unloadPlayer(UUID uuid) {
        cooldownCache.remove(uuid);
    }

    // ----------------------------------------------------------------
    // Claim logic
    // ----------------------------------------------------------------

    public enum ClaimResult {
        SUCCESS,
        KIT_NOT_FOUND,
        NO_PERMISSION,       // missing donor rank or mine rank
        ON_COOLDOWN,
        ONE_TIME_CLAIMED     // cooldown == 0 and already claimed once
    }

    /**
     * Attempt to claim a kit for a player.
     * Gives items (dropping at feet if inventory full) and records the cooldown.
     */
    public ClaimResult claimKit(Player player, KitData kit) {
        if (!meetsRequirements(player, kit)) return ClaimResult.NO_PERMISSION;

        long remaining = getRemainingMs(player.getUniqueId(), kit);
        if (remaining == Long.MAX_VALUE) return ClaimResult.ONE_TIME_CLAIMED;
        if (remaining > 0)              return ClaimResult.ON_COOLDOWN;

        // Give items
        boolean hadOverflow = false;
        for (KitItem kitItem : kit.contents()) {
            ItemStack item = kitItem.toItemStack();
            Map<Integer, ItemStack> overflow = player.getInventory().addItem(item);
            if (!overflow.isEmpty()) {
                hadOverflow = true;
                for (ItemStack dropped : overflow.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), dropped);
                }
            }
        }
        if (hadOverflow) {
            player.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                .deserialize("<yellow>Your inventory was full — some kit items were dropped at your feet."));
        }

        // Record claim
        long nowMs = System.currentTimeMillis();
        Map<String, Long> playerCooldowns = cooldownCache.computeIfAbsent(
            player.getUniqueId(), k -> new ConcurrentHashMap<>());
        playerCooldowns.put(kit.id(), nowMs);

        persistCooldown(player.getUniqueId(), kit.id(), nowMs);
        logClaim(player.getUniqueId(), kit.id());

        return ClaimResult.SUCCESS;
    }

    // ----------------------------------------------------------------
    // Cooldown queries
    // ----------------------------------------------------------------

    /**
     * Returns ms remaining until the kit can be claimed again.
     * 0 = claimable now.
     * Long.MAX_VALUE = one-time kit already claimed.
     */
    public long getRemainingMs(UUID uuid, KitData kit) {
        Map<String, Long> playerCooldowns = cooldownCache.get(uuid);
        Long lastClaimed = playerCooldowns != null ? playerCooldowns.get(kit.id()) : null;

        if (lastClaimed == null) return 0; // never claimed

        long now = System.currentTimeMillis();

        if (kit.type() == KitData.KitType.DONOR) {
            // Claimable if server restarted since last claim AND min cooldown elapsed
            boolean restarted = serverStartMs > lastClaimed;
            boolean minElapsed = (now - lastClaimed) >= kit.minDonorCooldownMs();
            if (restarted && minElapsed) return 0;
            if (!minElapsed) return kit.minDonorCooldownMs() - (now - lastClaimed);
            // Restarted but min not elapsed, OR not restarted — show time until next restart opportunity
            // Since we can't know when server will restart, show min cooldown remaining (or 0 if that passed)
            return Math.max(0, kit.minDonorCooldownMs() - (now - lastClaimed));
        }

        // FREE / RANK: standard timer
        if (kit.cooldownMs() == 0) {
            return Long.MAX_VALUE; // one-time only, already claimed
        }

        long elapsed = now - lastClaimed;
        return Math.max(0, kit.cooldownMs() - elapsed);
    }

    // ----------------------------------------------------------------
    // Requirement checks
    // ----------------------------------------------------------------

    public boolean meetsRequirements(Player player, KitData kit) {
        switch (kit.type()) {
            case RANK -> {
                if (kit.requiredRank() == null) return true;
                String playerRank = PermissionEngine.getInstance().getMineRank(player.getUniqueId());
                if (playerRank == null) return false;
                // Compare rank letters: player rank >= required rank
                return playerRank.compareToIgnoreCase(kit.requiredRank()) >= 0;
            }
            case DONOR -> {
                if (kit.requiredDonorRank() == null) return true;
                try {
                    com.prison.donor.DonorAPI donorApi = com.prison.donor.DonorAPI.getInstance();
                    if (donorApi == null) return false;
                    String playerDonorRank = donorApi.getDonorRank(player.getUniqueId());
                    if (playerDonorRank == null) return false;
                    // Donor rank hierarchy: donor < donorplus < elite < eliteplus
                    return donorRankTier(playerDonorRank) >= donorRankTier(kit.requiredDonorRank());
                } catch (NoClassDefFoundError e) {
                    return false;
                }
            }
            default -> { return true; } // FREE
        }
    }

    private int donorRankTier(String rank) {
        return switch (rank.toLowerCase()) {
            case "donor"     -> 1;
            case "donorplus" -> 2;
            case "elite"     -> 3;
            case "eliteplus" -> 4;
            default          -> 0;
        };
    }

    // ----------------------------------------------------------------
    // DB persistence
    // ----------------------------------------------------------------

    private void persistCooldown(UUID uuid, String kitId, long nowMs) {
        DatabaseManager.getInstance().queueWrite(
            "INSERT INTO kit_cooldowns (player_uuid, kit_id, last_claimed) " +
            "VALUES (?, ?, FROM_UNIXTIME(?)) " +
            "ON DUPLICATE KEY UPDATE last_claimed = FROM_UNIXTIME(?)",
            uuid.toString(), kitId,
            nowMs / 1000L, nowMs / 1000L
        );
    }

    private void logClaim(UUID uuid, String kitId) {
        DatabaseManager.getInstance().queueWrite(
            "INSERT INTO kit_logs (player_uuid, kit_id) VALUES (?, ?)",
            uuid.toString(), kitId
        );
    }
}
