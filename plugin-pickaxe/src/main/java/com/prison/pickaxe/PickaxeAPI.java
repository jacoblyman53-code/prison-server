package com.prison.pickaxe;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * PickaxeAPI — public interface for other plugins to query pickaxe state.
 *
 * Usage:
 *   PickaxeAPI api = PickaxeAPI.getInstance();
 *   boolean isServer = api.isServerPickaxe(item);
 *   int level = api.getEnchantLevel(item, "explosive");
 */
public class PickaxeAPI {

    private static PickaxeAPI instance;

    PickaxeAPI() {
        instance = this;
    }

    public static PickaxeAPI getInstance() { return instance; }

    /** Returns true if this is a server-issued prison pickaxe. */
    public boolean isServerPickaxe(ItemStack item) {
        return PickaxeManager.getInstance().isServerPickaxe(item);
    }

    /** Returns the current level of an enchant on this pickaxe (0 = not purchased). */
    public int getEnchantLevel(ItemStack item, String enchantId) {
        return PickaxeManager.getInstance().getEnchantLevel(item, enchantId);
    }

    /** Returns all enchant levels as a map (only non-zero levels included). */
    public Map<String, Integer> getAllEnchantLevels(ItemStack item) {
        return PickaxeManager.getInstance().readAllLevels(item);
    }

    /** Issue a new server pickaxe to a player (async DB insert, then give on main thread). */
    public CompletableFuture<ItemStack> issuePickaxe(Player player) {
        return PickaxeManager.getInstance().issuePickaxe(player);
    }

    /** Get the Tokenator multiplier for a pickaxe. Returns 1.0 if not purchased. */
    public double getTokenatorMultiplier(ItemStack item) {
        return PickaxeManager.getInstance().getTokenatorMultiplier(item);
    }

    /** Get the Sellall inventory threshold % for this pickaxe. 0 = not purchased. */
    public int getSellallThreshold(ItemStack item) {
        return PickaxeManager.getInstance().getSellallThreshold(item);
    }

    /** Get the Jackpot chance % for this pickaxe. 0.0 = not purchased. */
    public double getJackpotChance(ItemStack item) {
        return PickaxeManager.getInstance().getJackpotChance(item);
    }
}
