package com.prison.mines;

import com.prison.economy.SellPriceProvider;
import com.prison.permissions.PermissionEngine;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * MinesAPI — public interface for other plugins to query mine data.
 *
 * Also implements SellPriceProvider so the economy plugin can look up
 * per-mine sell prices. Registered with EconomyAPI on startup.
 *
 * Usage:
 *   MinesAPI api = MinesAPI.getInstance();
 *   MineData mine = api.getMine("A");
 *   Location spawn = api.getSpawnLocation("A");
 *   api.triggerReset("A");
 */
public class MinesAPI implements SellPriceProvider {

    private static MinesAPI instance;

    private final MineManager manager;

    MinesAPI(MineManager manager) {
        this.manager = manager;
        instance = this;
    }

    public static MinesAPI getInstance() { return instance; }

    // ----------------------------------------------------------------
    // Mine Queries
    // ----------------------------------------------------------------

    public MineData getMine(String id) {
        return manager.getMine(id);
    }

    public Map<String, MineData> getAllMines() {
        return manager.getMines();
    }

    public MineData getMineAt(Location location) {
        if (location.getWorld() == null) return null;
        return manager.getMineAt(
            location.getWorld().getName(),
            location.getBlockX(),
            location.getBlockY(),
            location.getBlockZ()
        );
    }

    /** Get the standard mine for a rank letter (e.g. "A"). Null if not configured. */
    public MineData getMineForRank(String rank) {
        return manager.getMineForRank(rank);
    }

    /** Get the spawn Location for a mine (for /mine tp and auto-teleport on rankup). */
    public Location getSpawnLocation(String mineId) {
        MineData mine = manager.getMine(mineId);
        if (mine == null) return null;
        World world = org.bukkit.Bukkit.getWorld(mine.world());
        if (world == null) return null;
        return new Location(world, mine.spawnX(), mine.spawnY(), mine.spawnZ(),
            mine.spawnYaw(), mine.spawnPitch());
    }

    // ----------------------------------------------------------------
    // Reset
    // ----------------------------------------------------------------

    public void triggerReset(String mineId) {
        manager.triggerReset(mineId);
    }

    // ----------------------------------------------------------------
    // SellPriceProvider implementation
    // ----------------------------------------------------------------

    /**
     * Returns the IGC sell price for one unit of material for this player.
     *
     * Priority:
     *   1. Per-mine sell price override (if the player is in a mine and it has one)
     *   2. Mine for the player's current rank (if they're not in a mine)
     *   3. 0 (let EconomyAPI fall back to its own config price)
     */
    @Override
    public long getSellPrice(Material material, Player player) {
        // Try the mine the player is currently standing in
        MineData mine = getMineAt(player.getLocation());

        // If not in a mine, fall back to the mine matching their rank
        if (mine == null) {
            String rank = PermissionEngine.getInstance().getMineRank(player.getUniqueId());
            mine = getMineForRank(rank);
        }

        if (mine == null) return 0L;

        // Per-mine price override
        Long price = mine.sellPrices().get(material);
        if (price != null) return price;

        // No per-mine override — return 0 so EconomyAPI uses its own config price
        return 0L;
    }
}
