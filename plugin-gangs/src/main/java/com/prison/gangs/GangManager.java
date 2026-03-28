package com.prison.gangs;

import com.prison.database.DatabaseManager;
import com.prison.economy.EconomyAPI;
import com.prison.economy.TransactionType;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * GangManager — all gang business logic.
 *
 * Caches all gang data in memory for fast lookups. All mutations go to the
 * database first, then update the in-memory caches on success.
 */
public class GangManager {

    private static GangManager instance;

    // ----------------------------------------------------------------
    // Caches
    // ----------------------------------------------------------------

    /** All gangs by their DB id. */
    private final ConcurrentHashMap<Long, GangData> gangsById = new ConcurrentHashMap<>();
    /** Lowercase name → gang id. */
    private final ConcurrentHashMap<String, Long> nameIndex = new ConcurrentHashMap<>();
    /** Lowercase tag → gang id. */
    private final ConcurrentHashMap<String, Long> tagIndex = new ConcurrentHashMap<>();
    /** Player UUID → gang id they belong to. */
    private final ConcurrentHashMap<UUID, Long> playerGangId = new ConcurrentHashMap<>();
    /** Player UUID → their gang role. */
    private final ConcurrentHashMap<UUID, GangRole> playerRole = new ConcurrentHashMap<>();
    /** Gang id → set of member UUIDs. */
    private final ConcurrentHashMap<Long, Set<UUID>> gangMembers = new ConcurrentHashMap<>();
    /** Player UUID → gang id of the pending invite. */
    private final ConcurrentHashMap<UUID, Long> pendingInvites = new ConcurrentHashMap<>();
    /** Leaderboard — sorted by level desc, bank_balance desc. Refreshed every 60s. */
    private volatile List<GangData> leaderboardCache = List.of();

    // ----------------------------------------------------------------
    // Config
    // ----------------------------------------------------------------

    private final int defaultMaxMembers;
    private final Map<String, Integer> donorMaxMembers; // donor rank name (lowercase) → max
    private final long[] levelThresholds;
    private final Logger logger;

    private GangManager(int defaultMaxMembers, Map<String, Integer> donorMaxMembers,
                        long[] levelThresholds, Logger logger) {
        this.defaultMaxMembers = defaultMaxMembers;
        this.donorMaxMembers   = donorMaxMembers;
        this.levelThresholds   = levelThresholds;
        this.logger            = logger;
        instance = this;
    }

    public static GangManager initialize(int defaultMaxMembers, Map<String, Integer> donorMaxMembers,
                                         long[] levelThresholds, Logger logger) {
        if (instance != null) throw new IllegalStateException("GangManager already initialized");
        return new GangManager(defaultMaxMembers, donorMaxMembers, levelThresholds, logger);
    }

    public static GangManager getInstance() { return instance; }

    // ----------------------------------------------------------------
    // Startup load
    // ----------------------------------------------------------------

    public CompletableFuture<Void> loadAll() {
        return CompletableFuture.runAsync(() -> {
            try {
                // Load gangs
                List<GangData> gangs = DatabaseManager.getInstance().query(
                    "SELECT id, name, tag, level, bank_balance, created_at FROM gangs",
                    rs -> {
                        List<GangData> list = new ArrayList<>();
                        while (rs.next()) {
                            Timestamp ts = rs.getTimestamp("created_at");
                            list.add(new GangData(
                                rs.getLong("id"),
                                rs.getString("name"),
                                rs.getString("tag"),
                                rs.getInt("level"),
                                rs.getLong("bank_balance"),
                                ts != null ? ts.toLocalDateTime() : java.time.LocalDateTime.now()
                            ));
                        }
                        return list;
                    }
                );

                for (GangData g : gangs) {
                    gangsById.put(g.id(), g);
                    nameIndex.put(g.name().toLowerCase(), g.id());
                    tagIndex.put(g.tag().toLowerCase(), g.id());
                    gangMembers.put(g.id(), ConcurrentHashMap.newKeySet());
                }

                // Load members
                List<GangMember> members = DatabaseManager.getInstance().query(
                    "SELECT gang_id, player_uuid, role, joined_at FROM gang_members",
                    rs -> {
                        List<GangMember> list = new ArrayList<>();
                        while (rs.next()) {
                            Timestamp ts = rs.getTimestamp("joined_at");
                            String roleStr = rs.getString("role");
                            GangRole role;
                            try {
                                role = GangRole.valueOf(roleStr.toUpperCase());
                            } catch (IllegalArgumentException e) {
                                role = GangRole.MEMBER;
                            }
                            list.add(new GangMember(
                                rs.getLong("gang_id"),
                                rs.getString("player_uuid"),
                                role,
                                ts != null ? ts.toLocalDateTime() : java.time.LocalDateTime.now()
                            ));
                        }
                        return list;
                    }
                );

                for (GangMember m : members) {
                    UUID uuid = UUID.fromString(m.playerUuid());
                    playerGangId.put(uuid, m.gangId());
                    playerRole.put(uuid, m.role());
                    Set<UUID> set = gangMembers.get(m.gangId());
                    if (set != null) set.add(uuid);
                }

                rebuildLeaderboard();
                logger.info("[Gangs] Loaded " + gangs.size() + " gangs, " + members.size() + " members.");
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "[Gangs] Failed to load gang data", e);
            }
        });
    }

    // ----------------------------------------------------------------
    // Gang queries
    // ----------------------------------------------------------------

    /** Returns the gang a player belongs to, or null. */
    public GangData getGangOf(UUID uuid) {
        Long id = playerGangId.get(uuid);
        return id == null ? null : gangsById.get(id);
    }

    /** Returns the player's gang role, or null if not in a gang. */
    public GangRole getMemberRole(UUID uuid) {
        return playerRole.get(uuid);
    }

    /** Returns whether the name is already taken (case-insensitive). */
    public boolean isNameTaken(String name) {
        return nameIndex.containsKey(name.toLowerCase());
    }

    /** Returns whether the tag is already taken (case-insensitive). */
    public boolean isTagTaken(String tag) {
        return tagIndex.containsKey(tag.toLowerCase());
    }

    /** Returns the cached leaderboard (top gangs, sorted by level desc → bank desc). */
    public List<GangData> getLeaderboard() {
        return leaderboardCache;
    }

    /** Returns a GangData by name (case-insensitive), or null. */
    public GangData getGangByName(String name) {
        Long id = nameIndex.get(name.toLowerCase());
        return id == null ? null : gangsById.get(id);
    }

    /** Returns the current member count of a gang. */
    public int getMemberCount(long gangId) {
        Set<UUID> set = gangMembers.get(gangId);
        return set == null ? 0 : set.size();
    }

    /** Returns the UUIDs of all members of a gang. */
    public Set<UUID> getMembers(long gangId) {
        Set<UUID> set = gangMembers.get(gangId);
        return set == null ? Set.of() : Collections.unmodifiableSet(set);
    }

    /** Returns the pending invite gang for a player, or null. */
    public GangData getPendingInvite(UUID uuid) {
        Long gangId = pendingInvites.get(uuid);
        return gangId == null ? null : gangsById.get(gangId);
    }

    /** Returns max members allowed for a given donor rank (null = no donor rank). */
    public int getMaxMembers(String donorRank) {
        if (donorRank == null) return defaultMaxMembers;
        return donorMaxMembers.getOrDefault(donorRank.toLowerCase(), defaultMaxMembers);
    }

    // ----------------------------------------------------------------
    // Gang creation / disbanding
    // ----------------------------------------------------------------

    public enum CreateResult { SUCCESS, NAME_TAKEN, TAG_TAKEN, ALREADY_IN_GANG, INVALID_NAME, INVALID_TAG, DB_ERROR }

    public CompletableFuture<CreateResult> createGang(UUID leaderUuid, String name, String tag) {
        // In-memory validation first
        if (playerGangId.containsKey(leaderUuid)) return done(CreateResult.ALREADY_IN_GANG);
        if (!isValidName(name)) return done(CreateResult.INVALID_NAME);
        if (!isValidTag(tag))   return done(CreateResult.INVALID_TAG);
        if (isNameTaken(name))  return done(CreateResult.NAME_TAKEN);
        if (isTagTaken(tag))    return done(CreateResult.TAG_TAKEN);

        return CompletableFuture.supplyAsync(() -> {
            try {
                long gangId = DatabaseManager.getInstance().executeAndGetId(
                    "INSERT INTO gangs (name, tag, level, bank_balance) VALUES (?, ?, 1, 0)",
                    name, tag
                );
                DatabaseManager.getInstance().execute(
                    "INSERT INTO gang_members (gang_id, player_uuid, role) VALUES (?, ?, 'LEADER')",
                    gangId, leaderUuid.toString()
                );

                GangData gang = new GangData(gangId, name, tag, 1, 0, java.time.LocalDateTime.now());
                gangsById.put(gangId, gang);
                nameIndex.put(name.toLowerCase(), gangId);
                tagIndex.put(tag.toLowerCase(), gangId);

                Set<UUID> members = ConcurrentHashMap.newKeySet();
                members.add(leaderUuid);
                gangMembers.put(gangId, members);
                playerGangId.put(leaderUuid, gangId);
                playerRole.put(leaderUuid, GangRole.LEADER);

                rebuildLeaderboard();
                return CreateResult.SUCCESS;
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "[Gangs] Failed to create gang", e);
                return CreateResult.DB_ERROR;
            }
        });
    }

    public enum DisbandResult { SUCCESS, NOT_IN_GANG, NOT_LEADER, DB_ERROR }

    public CompletableFuture<DisbandResult> disbandGang(UUID leaderUuid) {
        Long gangId = playerGangId.get(leaderUuid);
        if (gangId == null) return done(DisbandResult.NOT_IN_GANG);
        GangRole role = playerRole.get(leaderUuid);
        if (role != GangRole.LEADER) return done(DisbandResult.NOT_LEADER);

        return CompletableFuture.supplyAsync(() -> {
            try {
                DatabaseManager.getInstance().execute("DELETE FROM gang_members WHERE gang_id = ?", gangId);
                DatabaseManager.getInstance().execute("DELETE FROM gangs WHERE id = ?", gangId);
                removeCachedGang(gangId);
                rebuildLeaderboard();
                return DisbandResult.SUCCESS;
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "[Gangs] Failed to disband gang " + gangId, e);
                return DisbandResult.DB_ERROR;
            }
        });
    }

    // ----------------------------------------------------------------
    // Invitations
    // ----------------------------------------------------------------

    public enum InviteResult { SUCCESS, NOT_IN_GANG, NO_PERMISSION, TARGET_IN_GANG, ALREADY_INVITED, GANG_FULL }

    public InviteResult invitePlayer(UUID inviterUuid, UUID targetUuid, String targetDonorRank) {
        Long gangId = playerGangId.get(inviterUuid);
        if (gangId == null) return InviteResult.NOT_IN_GANG;
        GangRole role = playerRole.get(inviterUuid);
        if (role == null || !role.canInvite()) return InviteResult.NO_PERMISSION;
        if (playerGangId.containsKey(targetUuid)) return InviteResult.TARGET_IN_GANG;
        if (pendingInvites.containsKey(targetUuid)) return InviteResult.ALREADY_INVITED;

        // Size check — use gang leader's donor rank for the cap
        GangData gang = gangsById.get(gangId);
        Set<UUID> members = gangMembers.get(gangId);
        if (gang != null && members != null) {
            String leaderDonorRank = getLeaderDonorRank(gangId);
            if (members.size() >= getMaxMembers(leaderDonorRank)) return InviteResult.GANG_FULL;
        }

        pendingInvites.put(targetUuid, gangId);
        return InviteResult.SUCCESS;
    }

    public enum JoinResult { SUCCESS, NO_INVITE, GANG_FULL, DB_ERROR }

    public CompletableFuture<JoinResult> acceptInvite(UUID playerUuid) {
        Long gangId = pendingInvites.remove(playerUuid);
        if (gangId == null) return done(JoinResult.NO_INVITE);

        GangData gang = gangsById.get(gangId);
        if (gang == null) return done(JoinResult.NO_INVITE); // gang was disbanded

        // Re-check size
        Set<UUID> members = gangMembers.get(gangId);
        String leaderDonorRank = getLeaderDonorRank(gangId);
        if (members != null && members.size() >= getMaxMembers(leaderDonorRank)) {
            return done(JoinResult.GANG_FULL);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                DatabaseManager.getInstance().execute(
                    "INSERT INTO gang_members (gang_id, player_uuid, role) VALUES (?, ?, 'MEMBER')",
                    gangId, playerUuid.toString()
                );
                playerGangId.put(playerUuid, gangId);
                playerRole.put(playerUuid, GangRole.MEMBER);
                if (members != null) members.add(playerUuid);
                return JoinResult.SUCCESS;
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "[Gangs] Failed to add member to gang " + gangId, e);
                return JoinResult.DB_ERROR;
            }
        });
    }

    /** Deny (or cancel) a pending invite. Returns true if there was an invite to deny. */
    public boolean denyInvite(UUID playerUuid) {
        return pendingInvites.remove(playerUuid) != null;
    }

    // ----------------------------------------------------------------
    // Leaving / kicking
    // ----------------------------------------------------------------

    public enum LeaveResult {
        SUCCESS, NOT_IN_GANG, LEADER_MUST_TRANSFER, DB_ERROR
    }

    public CompletableFuture<LeaveResult> leaveGang(UUID playerUuid) {
        Long gangId = playerGangId.get(playerUuid);
        if (gangId == null) return done(LeaveResult.NOT_IN_GANG);

        GangRole role = playerRole.get(playerUuid);
        Set<UUID> members = gangMembers.get(gangId);
        int size = members == null ? 0 : members.size();

        if (role == GangRole.LEADER && size > 1) {
            return done(LeaveResult.LEADER_MUST_TRANSFER);
        }

        // If leader and alone, disband
        if (role == GangRole.LEADER && size <= 1) {
            return disbandGang(playerUuid).thenApply(r ->
                r == DisbandResult.SUCCESS ? LeaveResult.SUCCESS : LeaveResult.DB_ERROR);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                DatabaseManager.getInstance().execute(
                    "DELETE FROM gang_members WHERE gang_id = ? AND player_uuid = ?",
                    gangId, playerUuid.toString()
                );
                removeMemberFromCache(playerUuid, gangId);
                return LeaveResult.SUCCESS;
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "[Gangs] Failed to remove member from gang", e);
                return LeaveResult.DB_ERROR;
            }
        });
    }

    public enum KickResult { SUCCESS, NOT_IN_GANG, NO_PERMISSION, TARGET_NOT_IN_GANG, CANNOT_KICK_HIGHER, DB_ERROR }

    public CompletableFuture<KickResult> kickMember(UUID actorUuid, UUID targetUuid) {
        Long gangId = playerGangId.get(actorUuid);
        if (gangId == null) return done(KickResult.NOT_IN_GANG);
        if (!gangId.equals(playerGangId.get(targetUuid))) return done(KickResult.TARGET_NOT_IN_GANG);

        GangRole actorRole  = playerRole.get(actorUuid);
        GangRole targetRole = playerRole.get(targetUuid);
        if (actorRole == null || !actorRole.canKick()) return done(KickResult.NO_PERMISSION);
        if (actorRole == GangRole.OFFICER && targetRole != GangRole.MEMBER) return done(KickResult.CANNOT_KICK_HIGHER);

        return CompletableFuture.supplyAsync(() -> {
            try {
                DatabaseManager.getInstance().execute(
                    "DELETE FROM gang_members WHERE gang_id = ? AND player_uuid = ?",
                    gangId, targetUuid.toString()
                );
                removeMemberFromCache(targetUuid, gangId);
                return KickResult.SUCCESS;
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "[Gangs] Failed to kick member", e);
                return KickResult.DB_ERROR;
            }
        });
    }

    // ----------------------------------------------------------------
    // Role changes
    // ----------------------------------------------------------------

    public enum RoleChangeResult { SUCCESS, NOT_IN_GANG, NO_PERMISSION, TARGET_NOT_IN_GANG, ALREADY_THAT_ROLE, DB_ERROR }

    public CompletableFuture<RoleChangeResult> promoteToOfficer(UUID actorUuid, UUID targetUuid) {
        return changeRole(actorUuid, targetUuid, GangRole.OFFICER, GangRole.MEMBER);
    }

    public CompletableFuture<RoleChangeResult> demoteToMember(UUID actorUuid, UUID targetUuid) {
        return changeRole(actorUuid, targetUuid, GangRole.MEMBER, GangRole.OFFICER);
    }

    private CompletableFuture<RoleChangeResult> changeRole(UUID actorUuid, UUID targetUuid,
                                                            GangRole newRole, GangRole requiredCurrentRole) {
        Long gangId = playerGangId.get(actorUuid);
        if (gangId == null) return done(RoleChangeResult.NOT_IN_GANG);
        if (!gangId.equals(playerGangId.get(targetUuid))) return done(RoleChangeResult.TARGET_NOT_IN_GANG);

        GangRole actorRole  = playerRole.get(actorUuid);
        GangRole targetRole = playerRole.get(targetUuid);
        if (actorRole == null || !actorRole.canPromote()) return done(RoleChangeResult.NO_PERMISSION);
        if (targetRole != requiredCurrentRole) return done(RoleChangeResult.ALREADY_THAT_ROLE);

        return CompletableFuture.supplyAsync(() -> {
            try {
                DatabaseManager.getInstance().execute(
                    "UPDATE gang_members SET role = ? WHERE gang_id = ? AND player_uuid = ?",
                    newRole.name(), gangId, targetUuid.toString()
                );
                playerRole.put(targetUuid, newRole);
                return RoleChangeResult.SUCCESS;
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "[Gangs] Failed to change role", e);
                return RoleChangeResult.DB_ERROR;
            }
        });
    }

    public enum TransferResult { SUCCESS, NOT_IN_GANG, NOT_LEADER, TARGET_NOT_IN_GANG, DB_ERROR }

    public CompletableFuture<TransferResult> transferLeadership(UUID actorUuid, UUID targetUuid) {
        Long gangId = playerGangId.get(actorUuid);
        if (gangId == null) return done(TransferResult.NOT_IN_GANG);
        if (playerRole.get(actorUuid) != GangRole.LEADER) return done(TransferResult.NOT_LEADER);
        if (!gangId.equals(playerGangId.get(targetUuid))) return done(TransferResult.TARGET_NOT_IN_GANG);

        return CompletableFuture.supplyAsync(() -> {
            try {
                DatabaseManager.getInstance().execute(
                    "UPDATE gang_members SET role = 'OFFICER' WHERE gang_id = ? AND player_uuid = ?",
                    gangId, actorUuid.toString()
                );
                DatabaseManager.getInstance().execute(
                    "UPDATE gang_members SET role = 'LEADER' WHERE gang_id = ? AND player_uuid = ?",
                    gangId, targetUuid.toString()
                );
                playerRole.put(actorUuid, GangRole.OFFICER);
                playerRole.put(targetUuid, GangRole.LEADER);
                return TransferResult.SUCCESS;
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "[Gangs] Failed to transfer leadership", e);
                return TransferResult.DB_ERROR;
            }
        });
    }

    // ----------------------------------------------------------------
    // Gang bank
    // ----------------------------------------------------------------

    public enum BankResult { SUCCESS, NOT_IN_GANG, INSUFFICIENT_FUNDS, NO_PERMISSION, DB_ERROR }

    public CompletableFuture<BankResult> deposit(UUID playerUuid, long amount) {
        Long gangId = playerGangId.get(playerUuid);
        if (gangId == null) return done(BankResult.NOT_IN_GANG);

        EconomyAPI eco = EconomyAPI.getInstance();
        long remaining = eco.deductBalance(playerUuid, amount, TransactionType.GANG_DEPOSIT);
        if (remaining < 0) return done(BankResult.INSUFFICIENT_FUNDS);

        return CompletableFuture.supplyAsync(() -> {
            try {
                DatabaseManager.getInstance().execute(
                    "UPDATE gangs SET bank_balance = bank_balance + ? WHERE id = ?",
                    amount, gangId
                );
                GangData current = gangsById.get(gangId);
                if (current != null) {
                    GangData updated = current.withBank(current.bankBalance() + amount, levelThresholds);
                    gangsById.put(gangId, updated);
                }
                return BankResult.SUCCESS;
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "[Gangs] Failed to deposit to gang bank", e);
                // Refund on failure
                eco.addBalance(playerUuid, amount, TransactionType.GANG_DEPOSIT);
                return BankResult.DB_ERROR;
            }
        });
    }

    public CompletableFuture<BankResult> withdraw(UUID playerUuid, long amount) {
        Long gangId = playerGangId.get(playerUuid);
        if (gangId == null) return done(BankResult.NOT_IN_GANG);

        GangRole role = playerRole.get(playerUuid);
        if (role == null || !role.canWithdraw()) return done(BankResult.NO_PERMISSION);

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Atomic withdraw — only succeeds if balance is sufficient
                int rows = DatabaseManager.getInstance().execute(
                    "UPDATE gangs SET bank_balance = bank_balance - ? WHERE id = ? AND bank_balance >= ?",
                    amount, gangId, amount
                );
                if (rows == 0) return BankResult.INSUFFICIENT_FUNDS;

                EconomyAPI.getInstance().addBalance(playerUuid, amount, TransactionType.GANG_WITHDRAW);

                GangData current = gangsById.get(gangId);
                if (current != null) {
                    GangData updated = current.withBank(current.bankBalance() - amount, levelThresholds);
                    gangsById.put(gangId, updated);
                }
                return BankResult.SUCCESS;
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "[Gangs] Failed to withdraw from gang bank", e);
                return BankResult.DB_ERROR;
            }
        });
    }

    // ----------------------------------------------------------------
    // Scheduled tasks
    // ----------------------------------------------------------------

    /** Recalculate levels for all gangs. Called every 5 minutes async. */
    public void recalculateLevels() {
        for (Map.Entry<Long, GangData> entry : gangsById.entrySet()) {
            GangData gang = entry.getValue();
            int expected = GangData.computeLevel(gang.bankBalance(), levelThresholds);
            if (expected != gang.level()) {
                GangData updated = gang.withLevel(expected);
                gangsById.put(gang.id(), updated);
                DatabaseManager.getInstance().queueWrite(
                    "UPDATE gangs SET level = ? WHERE id = ?",
                    expected, gang.id()
                );
            }
        }
        rebuildLeaderboard();
    }

    /** Rebuild the cached leaderboard. */
    public void rebuildLeaderboard() {
        List<GangData> sorted = new ArrayList<>(gangsById.values());
        sorted.sort(Comparator
            .comparingInt(GangData::level).reversed()
            .thenComparingLong(GangData::bankBalance).reversed());
        leaderboardCache = List.copyOf(sorted.subList(0, Math.min(10, sorted.size())));
    }

    // ----------------------------------------------------------------
    // Cleanup on disable
    // ----------------------------------------------------------------

    public static void reset() { instance = null; }

    // ----------------------------------------------------------------
    // Internal helpers
    // ----------------------------------------------------------------

    private void removeMemberFromCache(UUID uuid, long gangId) {
        playerGangId.remove(uuid);
        playerRole.remove(uuid);
        Set<UUID> members = gangMembers.get(gangId);
        if (members != null) members.remove(uuid);
    }

    private void removeCachedGang(long gangId) {
        GangData gang = gangsById.remove(gangId);
        if (gang != null) {
            nameIndex.remove(gang.name().toLowerCase());
            tagIndex.remove(gang.tag().toLowerCase());
        }
        Set<UUID> members = gangMembers.remove(gangId);
        if (members != null) {
            for (UUID uuid : members) {
                playerGangId.remove(uuid);
                playerRole.remove(uuid);
            }
        }
    }

    /** Returns the leader's donor rank for a gang's member limit calculation, or null. */
    private String getLeaderDonorRank(long gangId) {
        Set<UUID> members = gangMembers.get(gangId);
        if (members == null) return null;
        for (UUID uuid : members) {
            if (playerRole.get(uuid) == GangRole.LEADER) {
                com.prison.donor.DonorAPI donorApi = com.prison.donor.DonorAPI.getInstance();
                if (donorApi != null) return donorApi.getDonorRank(uuid);
                return null;
            }
        }
        return null;
    }

    private static boolean isValidName(String name) {
        if (name == null) return false;
        int len = name.length();
        if (len < 3 || len > 20) return false;
        return name.matches("[A-Za-z0-9_]+");
    }

    private static boolean isValidTag(String tag) {
        if (tag == null) return false;
        int len = tag.length();
        if (len < 2 || len > 6) return false;
        return tag.matches("[A-Za-z0-9]+");
    }

    private static <T> CompletableFuture<T> done(T value) {
        return CompletableFuture.completedFuture(value);
    }
}
