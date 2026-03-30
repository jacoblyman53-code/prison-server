package com.prison.prestige;

import com.prison.database.DatabaseManager;
import com.prison.permissions.PermissionEngine;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * PrestigeManager — handles prestige logic and exposes the public API.
 *
 * On prestige:
 *   1. Mine rank → A, IGC balance → 0  (single atomic DB update)
 *   2. Prestige level incremented in DB
 *   3. Transaction logged
 *   4. Permission cache rebuilt (via PermissionEngine)
 *   5. Reward commands dispatched (console)
 *   6. Server-wide broadcast sent
 *   7. Auto-teleport to mine A (stubbed — warp system not yet built)
 *
 * Token balance, donor rank, and pickaxe enchants are NOT touched.
 */
public class PrestigeManager {

    private static PrestigeManager instance;

    private final PrestigeConfig config;
    private final Logger logger;

    private PrestigeManager(PrestigeConfig config, Logger logger) {
        this.config = config;
        this.logger = logger;
    }

    public static PrestigeManager getInstance() { return instance; }

    public static PrestigeManager initialize(PrestigeConfig config, Logger logger) {
        if (instance != null) throw new IllegalStateException("PrestigeManager already initialized");
        instance = new PrestigeManager(config, logger);
        return instance;
    }

    // ----------------------------------------------------------------
    // Public API
    // ----------------------------------------------------------------

    /**
     * Returns the current prestige level for a player (0 = not prestiged).
     */
    public int getPrestigeLevel(UUID uuid) {
        return PermissionEngine.getInstance().getPrestige(uuid);
    }

    /**
     * Returns true if the player is eligible to prestige (must be rank Z).
     */
    public boolean canPrestige(UUID uuid) {
        String rank = PermissionEngine.getInstance().getMineRank(uuid);
        return "Z".equalsIgnoreCase(rank);
    }

    /**
     * Execute a prestige for the given player.
     * Caller is responsible for checking canPrestige() first.
     *
     * @return the new prestige level, or -1 on error.
     */
    public int executePrestige(Player player) {
        UUID uuid       = player.getUniqueId();
        int  currentP   = getPrestigeLevel(uuid);
        int  newPrestige = currentP + 1;

        try {
            // Atomic DB update: reset rank + IGC, increment prestige
            DatabaseManager.getInstance().execute(
                "UPDATE players SET mine_rank = 'A', igc_balance = 0, prestige = ? WHERE uuid = ?",
                newPrestige, uuid.toString()
            );

            // Log transaction
            DatabaseManager.getInstance().execute(
                "INSERT INTO transactions (player_uuid, currency_type, type, amount, balance_after) VALUES (?, 'IGC', 'prestige', 0, 0)",
                uuid.toString()
            );

            // Rebuild permission cache (updates mine_rank + prestige node)
            PermissionEngine.getInstance().setPrestige(uuid, newPrestige).join();
            PermissionEngine.getInstance().setMineRank(uuid, "A").join();

            // Dispatch reward commands (run on main thread via Bukkit)
            List<String> rewards = config.getRewardCommands(newPrestige);
            for (String cmd : rewards) {
                String resolved = cmd.replace("{player}", player.getName())
                                     .replace("{prestige}", String.valueOf(newPrestige));
                boolean cmdOk = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), resolved);
                if (!cmdOk) {
                    logger.warning("[Prestige] Reward command returned false — check config: " + resolved);
                }
            }

            // Broadcast
            String broadcast = config.getBroadcastMessage()
                .replace("{player}", player.getName())
                .replace("{prestige}", String.valueOf(newPrestige));
            Bukkit.broadcast(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                .deserialize(broadcast));

            // Auto-teleport to mine A
            com.prison.mines.MinesAPI minesApi = com.prison.mines.MinesAPI.getInstance();
            if (minesApi != null) {
                org.bukkit.Location spawnLoc = minesApi.getSpawnLocation("A");
                if (spawnLoc != null) {
                    player.teleport(spawnLoc);
                } else {
                    logger.warning("[Prestige] No spawn location for mine A — player not teleported.");
                }
            } else {
                logger.warning("[Prestige] MinesAPI unavailable — player not teleported to mine A.");
            }

            // Award prestige shop points
            PrestigeShopManager shopMgr = PrestigeShopManager.getInstance();
            if (shopMgr != null) {
                shopMgr.awardPoints(uuid);
            }

            logger.info("[Prestige] " + player.getName() + " prestiged to P" + newPrestige);
            return newPrestige;

        } catch (SQLException e) {
            logger.severe("[Prestige] Failed to execute prestige for " + player.getName() + ": " + e.getMessage());
            return -1;
        }
    }

    public PrestigeConfig getConfig() { return config; }
}
