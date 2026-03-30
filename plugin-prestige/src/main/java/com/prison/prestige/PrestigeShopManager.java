package com.prison.prestige;

import com.prison.database.DatabaseManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * PrestigeShopManager — manages prestige points and permanent upgrade purchases.
 *
 * Upgrade tiers (stacking, not additive at the same tier):
 *   Mine Profit I/II/III  — +5%/+10%/+15% permanent sell multiplier
 *   Token Mastery I/II/III — +5%/+10%/+15% permanent token multiplier
 *
 * Points are awarded via awardPoints() when a player prestiges.
 * Both points and purchases are persisted to the database.
 */
public class PrestigeShopManager {

    // ----------------------------------------------------------------
    // Upgrade definition
    // ----------------------------------------------------------------

    public record UpgradeDef(
        String id,
        String display,
        int cost,
        String requiredUpgrade, // null if no prerequisite
        double sellBonus,       // flat sell multiplier this upgrade provides (e.g. 1.05)
        double tokenBonus       // flat token multiplier this upgrade provides
    ) {}

    // ----------------------------------------------------------------
    // Singleton
    // ----------------------------------------------------------------

    private static PrestigeShopManager instance;

    public static PrestigeShopManager getInstance() { return instance; }

    public static PrestigeShopManager initialize(JavaPlugin plugin, int pointsPerPrestige) {
        instance = new PrestigeShopManager(plugin, pointsPerPrestige);
        instance.ensureTables();
        return instance;
    }

    // ----------------------------------------------------------------
    // Fields
    // ----------------------------------------------------------------

    private final JavaPlugin plugin;
    private final Logger logger;
    private final int pointsPerPrestige;

    private final List<UpgradeDef> upgrades;

    /** prestige_points per player */
    private final ConcurrentHashMap<UUID, Integer> pointsCache = new ConcurrentHashMap<>();

    /** purchased upgrade IDs per player */
    private final ConcurrentHashMap<UUID, Set<String>> purchasedCache = new ConcurrentHashMap<>();

    // ----------------------------------------------------------------
    // Constructor
    // ----------------------------------------------------------------

    private PrestigeShopManager(JavaPlugin plugin, int pointsPerPrestige) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.pointsPerPrestige = pointsPerPrestige;

        upgrades = List.of(
            new UpgradeDef("mine_profit_1",   "Mine Profit I",    5,  null,           1.05, 1.0),
            new UpgradeDef("mine_profit_2",   "Mine Profit II",   15, "mine_profit_1",1.10, 1.0),
            new UpgradeDef("mine_profit_3",   "Mine Profit III",  30, "mine_profit_2",1.15, 1.0),
            new UpgradeDef("token_mastery_1", "Token Mastery I",  5,  null,           1.0,  1.05),
            new UpgradeDef("token_mastery_2", "Token Mastery II", 15, "token_mastery_1",1.0,1.10),
            new UpgradeDef("token_mastery_3", "Token Mastery III",30, "token_mastery_2",1.0,1.15)
        );
    }

    // ----------------------------------------------------------------
    // DB setup
    // ----------------------------------------------------------------

    private void ensureTables() {
        try {
            DatabaseManager.getInstance().execute(
                "CREATE TABLE IF NOT EXISTS prestige_shop_purchases (" +
                "  player_uuid  VARCHAR(36) NOT NULL," +
                "  upgrade_id   VARCHAR(32) NOT NULL," +
                "  purchased_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "  PRIMARY KEY (player_uuid, upgrade_id)" +
                ")"
            );
            try {
                DatabaseManager.getInstance().execute(
                    "ALTER TABLE players ADD COLUMN IF NOT EXISTS prestige_points INT DEFAULT 0"
                );
            } catch (Exception ignored) {
                // Older MySQL without IF NOT EXISTS support
                try {
                    DatabaseManager.getInstance().execute(
                        "ALTER TABLE players ADD COLUMN prestige_points INT DEFAULT 0"
                    );
                } catch (Exception ignored2) { /* column already exists */ }
            }
            logger.info("[PrestigeShop] Tables verified.");
        } catch (SQLException e) {
            logger.severe("[PrestigeShop] Failed to create tables: " + e.getMessage());
        }
    }

    // ----------------------------------------------------------------
    // Player lifecycle
    // ----------------------------------------------------------------

    /** Load a player's points and purchases from DB. Call async on join. */
    public void loadPlayer(UUID uuid) {
        try {
            int points = DatabaseManager.getInstance().query(
                "SELECT prestige_points FROM players WHERE uuid = ?",
                rs -> rs.next() ? rs.getInt("prestige_points") : 0,
                uuid.toString()
            );
            pointsCache.put(uuid, points);

            Set<String> purchased = DatabaseManager.getInstance().query(
                "SELECT upgrade_id FROM prestige_shop_purchases WHERE player_uuid = ?",
                rs -> {
                    Set<String> set = new HashSet<>();
                    while (rs.next()) set.add(rs.getString("upgrade_id"));
                    return set;
                },
                uuid.toString()
            );
            purchasedCache.put(uuid, Collections.synchronizedSet(new HashSet<>(purchased)));

        } catch (SQLException e) {
            logger.warning("[PrestigeShop] Failed to load player " + uuid + ": " + e.getMessage());
            pointsCache.put(uuid, 0);
            purchasedCache.put(uuid, Collections.synchronizedSet(new HashSet<>()));
        }
    }

    /** Remove player from cache on quit. */
    public void unloadPlayer(UUID uuid) {
        pointsCache.remove(uuid);
        purchasedCache.remove(uuid);
    }

    // ----------------------------------------------------------------
    // Points
    // ----------------------------------------------------------------

    public int getPoints(UUID uuid) {
        return pointsCache.getOrDefault(uuid, 0);
    }

    /** Award points for a new prestige. Persists async. */
    public void awardPoints(UUID uuid) {
        int current = pointsCache.getOrDefault(uuid, 0);
        int newTotal = current + pointsPerPrestige;
        pointsCache.put(uuid, newTotal);
        int finalNewTotal = newTotal;
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                DatabaseManager.getInstance().execute(
                    "UPDATE players SET prestige_points = ? WHERE uuid = ?",
                    finalNewTotal, uuid.toString()
                );
            } catch (SQLException e) {
                logger.warning("[PrestigeShop] Failed to persist prestige_points for " + uuid + ": " + e.getMessage());
            }
        });
    }

    // ----------------------------------------------------------------
    // Purchases
    // ----------------------------------------------------------------

    public enum PurchaseResult { OK, INSUFFICIENT_POINTS, ALREADY_OWNED, PREREQUISITE_NOT_MET, NOT_FOUND }

    public PurchaseResult purchase(UUID uuid, String upgradeId) {
        UpgradeDef def = getUpgrade(upgradeId);
        if (def == null) return PurchaseResult.NOT_FOUND;

        Set<String> owned = purchasedCache.computeIfAbsent(uuid, k -> Collections.synchronizedSet(new HashSet<>()));
        if (owned.contains(upgradeId)) return PurchaseResult.ALREADY_OWNED;

        if (def.requiredUpgrade() != null && !owned.contains(def.requiredUpgrade())) {
            return PurchaseResult.PREREQUISITE_NOT_MET;
        }

        int pts = pointsCache.getOrDefault(uuid, 0);
        if (pts < def.cost()) return PurchaseResult.INSUFFICIENT_POINTS;

        int newPts = pts - def.cost();
        pointsCache.put(uuid, newPts);
        owned.add(upgradeId);

        // Persist async
        int finalNewPts = newPts;
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                DatabaseManager.getInstance().execute(
                    "UPDATE players SET prestige_points = ? WHERE uuid = ?",
                    finalNewPts, uuid.toString()
                );
                DatabaseManager.getInstance().execute(
                    "INSERT IGNORE INTO prestige_shop_purchases (player_uuid, upgrade_id) VALUES (?, ?)",
                    uuid.toString(), upgradeId
                );
            } catch (SQLException e) {
                logger.warning("[PrestigeShop] Failed to persist purchase for " + uuid + ": " + e.getMessage());
            }
        });

        return PurchaseResult.OK;
    }

    public boolean hasPurchased(UUID uuid, String upgradeId) {
        Set<String> owned = purchasedCache.get(uuid);
        return owned != null && owned.contains(upgradeId);
    }

    // ----------------------------------------------------------------
    // MultiplierProvider values — used by EconomyAPI providers
    // ----------------------------------------------------------------

    /** Returns the highest-tier sell bonus the player has unlocked (multiplicative, not additive). */
    public double getSellBonus(UUID uuid) {
        Set<String> owned = purchasedCache.getOrDefault(uuid, Set.of());
        if (owned.contains("mine_profit_3")) return 1.15;
        if (owned.contains("mine_profit_2")) return 1.10;
        if (owned.contains("mine_profit_1")) return 1.05;
        return 1.0;
    }

    /** Returns the highest-tier token bonus the player has unlocked. */
    public double getTokenBonus(UUID uuid) {
        Set<String> owned = purchasedCache.getOrDefault(uuid, Set.of());
        if (owned.contains("token_mastery_3")) return 1.15;
        if (owned.contains("token_mastery_2")) return 1.10;
        if (owned.contains("token_mastery_1")) return 1.05;
        return 1.0;
    }

    // ----------------------------------------------------------------
    // Accessors
    // ----------------------------------------------------------------

    public List<UpgradeDef> getUpgrades() { return upgrades; }

    public UpgradeDef getUpgrade(String id) {
        for (UpgradeDef u : upgrades) {
            if (u.id().equals(id)) return u;
        }
        return null;
    }

    public int getPointsPerPrestige() { return pointsPerPrestige; }
}
