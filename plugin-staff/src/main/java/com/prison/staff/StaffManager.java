package com.prison.staff;

import com.prison.database.DatabaseManager;
import com.prison.permissions.PermissionEngine;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * StaffManager — core punishment and report logic.
 *
 * Active bans and mutes are kept in memory for fast login/chat checks.
 * The cache is loaded on startup from the database.
 *
 * IP bans are stored as a set of IP address strings for O(1) lookup.
 */
public class StaffManager {

    private static StaffManager instance;

    // Active punishment caches — UUID string key
    private final ConcurrentHashMap<String, PunishmentData> activeBans  = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, PunishmentData> activeMutes = new ConcurrentHashMap<>();
    private final Set<String> activeBannedIps = ConcurrentHashMap.newKeySet();

    // Frozen player UUIDs
    private final Set<UUID> frozenPlayers = ConcurrentHashMap.newKeySet();

    // Report duplicate cooldown: reporterUUID -> Map<reportedUUID -> lastReportMs>
    private final ConcurrentHashMap<UUID, Map<UUID, Long>> reportCooldowns = new ConcurrentHashMap<>();

    private final Logger logger;

    private StaffManager(Logger logger) {
        this.logger = logger;
    }

    public static StaffManager initialize(Logger logger) {
        if (instance != null) throw new IllegalStateException("StaffManager already initialized");
        instance = new StaffManager(logger);
        return instance;
    }

    public static StaffManager getInstance() { return instance; }

    // ----------------------------------------------------------------
    // Cache loading
    // ----------------------------------------------------------------

    public CompletableFuture<Void> loadActivePunishments() {
        return CompletableFuture.runAsync(() -> {
            try {
                List<PunishmentData> punishments = DatabaseManager.getInstance().query(
                    "SELECT id, player_uuid, ip_address, type, reason, issued_by_uuid, " +
                    "issued_at, expires_at, active FROM punishments WHERE active = 1",
                    rs -> {
                        List<PunishmentData> list = new ArrayList<>();
                        while (rs.next()) {
                            Timestamp expiresTs = rs.getTimestamp("expires_at");
                            list.add(new PunishmentData(
                                rs.getLong("id"),
                                rs.getString("player_uuid"),
                                rs.getString("ip_address"),
                                PunishmentType.valueOf(rs.getString("type").toUpperCase()),
                                rs.getString("reason"),
                                rs.getString("issued_by_uuid"),
                                rs.getTimestamp("issued_at").toLocalDateTime(),
                                expiresTs != null ? expiresTs.toLocalDateTime() : null,
                                true
                            ));
                        }
                        return list;
                    }
                );

                activeBans.clear();
                activeMutes.clear();
                activeBannedIps.clear();

                for (PunishmentData p : punishments) {
                    if (!p.isCurrentlyActive()) {
                        // Expired — deactivate in DB (fire and forget)
                        deactivatePunishment(p.id());
                        continue;
                    }
                    if (p.type().isBan()) {
                        if (p.type() == PunishmentType.IPBAN && p.ipAddress() != null) {
                            activeBannedIps.add(p.ipAddress());
                        } else {
                            activeBans.put(p.playerUuid(), p);
                        }
                    } else if (p.type().isMute()) {
                        activeMutes.put(p.playerUuid(), p);
                    }
                }
                logger.info("[Staff] Loaded " + activeBans.size() + " active bans, "
                    + activeMutes.size() + " active mutes, "
                    + activeBannedIps.size() + " IP bans.");
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "[Staff] Failed to load active punishments", e);
            }
        });
    }

    // ----------------------------------------------------------------
    // Punishment checks
    // ----------------------------------------------------------------

    /** Returns the active ban for this UUID, or null if not banned. Handles expiry. */
    public PunishmentData getActiveBan(String uuid) {
        PunishmentData ban = activeBans.get(uuid);
        if (ban == null) return null;
        if (!ban.isCurrentlyActive()) {
            activeBans.remove(uuid);
            deactivatePunishment(ban.id());
            return null;
        }
        return ban;
    }

    /** Returns true if this IP address is banned. */
    public boolean isIpBanned(String ip) {
        return activeBannedIps.contains(ip);
    }

    /** Returns the active mute for this UUID, or null if not muted. Handles expiry. */
    public PunishmentData getActiveMute(String uuid) {
        PunishmentData mute = activeMutes.get(uuid);
        if (mute == null) return null;
        if (!mute.isCurrentlyActive()) {
            activeMutes.remove(uuid);
            deactivatePunishment(mute.id());
            return null;
        }
        return mute;
    }

    // ----------------------------------------------------------------
    // Issue punishments
    // ----------------------------------------------------------------

    public CompletableFuture<PunishmentData> issuePunishment(
        String targetUuid, String targetName, String ipAddress,
        PunishmentType type, String reason, String issuedByUuid, LocalDateTime expiresAt) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                Timestamp expTs = expiresAt != null ? Timestamp.valueOf(expiresAt) : null;
                long id = DatabaseManager.getInstance().executeAndGetId(
                    "INSERT INTO punishments (player_uuid, ip_address, type, reason, issued_by_uuid, expires_at, active) " +
                    "VALUES (?, ?, ?, ?, ?, ?, 1)",
                    targetUuid, ipAddress,
                    type.name().toLowerCase(), reason,
                    issuedByUuid, expTs
                );

                LocalDateTime now = LocalDateTime.now();
                PunishmentData p = new PunishmentData(
                    id, targetUuid, ipAddress, type, reason, issuedByUuid, now, expiresAt, true);

                // Update cache
                if (type == PunishmentType.IPBAN && ipAddress != null) {
                    activeBannedIps.add(ipAddress);
                } else if (type.isBan()) {
                    activeBans.put(targetUuid, p);
                } else if (type.isMute()) {
                    activeMutes.put(targetUuid, p);
                }

                // Log staff action
                logStaffAction(issuedByUuid, targetUuid, type.name().toLowerCase(),
                    "{\"reason\":\"" + reason + "\",\"expires\":\"" + (expiresAt != null ? expiresAt : "permanent") + "\"}");

                return p;
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "[Staff] Failed to issue punishment for " + targetName, e);
                return null;
            }
        });
    }

    /** Deactivate all active bans/mutes for a player (for /unban, /unmute). */
    public CompletableFuture<Boolean> removePunishment(String targetUuid, boolean isBan, String removedByUuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String typeFilter = isBan ? "type IN ('ban','tempban')" : "type IN ('mute','tempmute')";
                DatabaseManager.getInstance().execute(
                    "UPDATE punishments SET active = 0 WHERE player_uuid = ? AND active = 1 AND " + typeFilter,
                    targetUuid
                );
                if (isBan) {
                    activeBans.remove(targetUuid);
                } else {
                    activeMutes.remove(targetUuid);
                }
                logStaffAction(removedByUuid, targetUuid, isBan ? "unban" : "unmute", "{}");
                return true;
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "[Staff] Failed to remove punishment for " + targetUuid, e);
                return false;
            }
        });
    }

    private void deactivatePunishment(long id) {
        try {
            DatabaseManager.getInstance().execute(
                "UPDATE punishments SET active = 0 WHERE id = ?", id);
        } catch (SQLException e) {
            logger.log(Level.WARNING, "[Staff] Failed to deactivate expired punishment #" + id, e);
        }
    }

    // ----------------------------------------------------------------
    // Freeze
    // ----------------------------------------------------------------

    public boolean toggleFreeze(UUID uuid) {
        if (frozenPlayers.contains(uuid)) {
            frozenPlayers.remove(uuid);
            return false; // unfrozen
        } else {
            frozenPlayers.add(uuid);
            return true;  // frozen
        }
    }

    public boolean isFrozen(UUID uuid) {
        return frozenPlayers.contains(uuid);
    }

    public void unfreezePlayer(UUID uuid) {
        frozenPlayers.remove(uuid);
    }

    public void unfreezeAll() {
        frozenPlayers.clear();
    }

    // ----------------------------------------------------------------
    // Reports
    // ----------------------------------------------------------------

    /**
     * Returns true if the reporter is allowed to report the target right now.
     * Enforces the per-pair cooldown.
     */
    public boolean canReport(UUID reporterUuid, UUID reportedUuid, long cooldownMs) {
        Map<UUID, Long> map = reportCooldowns.get(reporterUuid);
        if (map == null) return true;
        Long last = map.get(reportedUuid);
        if (last == null) return true;
        return System.currentTimeMillis() - last >= cooldownMs;
    }

    public void recordReportCooldown(UUID reporterUuid, UUID reportedUuid) {
        reportCooldowns
            .computeIfAbsent(reporterUuid, k -> new ConcurrentHashMap<>())
            .put(reportedUuid, System.currentTimeMillis());
    }

    public CompletableFuture<Long> submitReport(String reporterUuid, String reportedUuid,
                                                 String reporterName, String reportedName,
                                                 String reason) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return DatabaseManager.getInstance().executeAndGetId(
                    "INSERT INTO reports (reporter_uuid, reported_uuid, reporter_name, reported_name, reason, status) " +
                    "VALUES (?, ?, ?, ?, ?, 'pending')",
                    reporterUuid, reportedUuid, reporterName, reportedName, reason
                );
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "[Staff] Failed to submit report", e);
                return -1L;
            }
        });
    }

    public CompletableFuture<List<ReportData>> getPendingReports() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return DatabaseManager.getInstance().query(
                    "SELECT id, reporter_uuid, reported_uuid, reporter_name, reported_name, " +
                    "reason, status, resolution_note, assigned_to_uuid, created_at " +
                    "FROM reports WHERE status = 'pending' ORDER BY created_at ASC",
                    rs -> {
                        List<ReportData> list = new ArrayList<>();
                        while (rs.next()) {
                            list.add(new ReportData(
                                rs.getLong("id"),
                                rs.getString("reporter_uuid"),
                                rs.getString("reported_uuid"),
                                rs.getString("reporter_name"),
                                rs.getString("reported_name"),
                                rs.getString("reason"),
                                ReportData.ReportStatus.valueOf(rs.getString("status").toUpperCase()),
                                rs.getString("resolution_note"),
                                rs.getString("assigned_to_uuid"),
                                rs.getTimestamp("created_at").toLocalDateTime()
                            ));
                        }
                        return list;
                    }
                );
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "[Staff] Failed to load pending reports", e);
                return List.of();
            }
        });
    }

    public CompletableFuture<Boolean> closeReport(long reportId, String resolvedByUuid, String resolutionNote) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                DatabaseManager.getInstance().execute(
                    "UPDATE reports SET status = 'closed', resolution_note = ?, assigned_to_uuid = ? WHERE id = ?",
                    resolutionNote, resolvedByUuid, reportId
                );
                logStaffAction(resolvedByUuid, null, "report_close",
                    "{\"report_id\":" + reportId + ",\"note\":\"" + resolutionNote + "\"}");
                return true;
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "[Staff] Failed to close report #" + reportId, e);
                return false;
            }
        });
    }

    // ----------------------------------------------------------------
    // Staff action log
    // ----------------------------------------------------------------

    public void logStaffAction(String actorUuid, String targetUuid, String actionType, String detailsJson) {
        DatabaseManager.getInstance().queueWrite(
            "INSERT INTO staff_actions (actor_uuid, target_uuid, action_type, details) VALUES (?, ?, ?, ?)",
            actorUuid, targetUuid, actionType, detailsJson
        );
    }

    // ----------------------------------------------------------------
    // Staff rank checks (delegates to PermissionEngine)
    // ----------------------------------------------------------------

    public boolean isStaff(UUID uuid) {
        return hasStaffPerm(uuid, "helper");
    }

    public boolean isHelper(UUID uuid) {
        return hasStaffPerm(uuid, "helper");
    }

    public boolean isModerator(UUID uuid) {
        return hasStaffPerm(uuid, "moderator");
    }

    public boolean isSeniorMod(UUID uuid) {
        return hasStaffPerm(uuid, "seniormod");
    }

    public boolean isAdmin(UUID uuid) {
        return hasStaffPerm(uuid, "admin");
    }

    private boolean hasStaffPerm(UUID uuid, String tier) {
        String node = "prison.staff." + tier;
        return PermissionEngine.getInstance().hasPermission(uuid, node)
            || PermissionEngine.getInstance().hasPermission(uuid, "prison.admin.*");
    }

    // Helper to check permission for online player
    public boolean hasStaffPermPlayer(Player player, String tier) {
        return PermissionEngine.getInstance().hasPermission(player, "prison.staff." + tier)
            || PermissionEngine.getInstance().hasPermission(player, "prison.admin.*");
    }
}
