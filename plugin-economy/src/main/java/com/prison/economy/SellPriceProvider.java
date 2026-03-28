package com.prison.economy;

import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * SellPriceProvider — determines how much IGC one unit of a material sells for.
 *
 * The default implementation reads from economy config.yml.
 * The mines plugin replaces this at startup to provide mine-tier-aware pricing
 * (better blocks in higher mines sell for more).
 *
 * Usage:
 *   EconomyAPI.getInstance().setSellPriceProvider(myProvider);
 */
@FunctionalInterface
public interface SellPriceProvider {

    /**
     * Returns the IGC sell price for one unit of the given material,
     * or 0 if the material is not sellable.
     *
     * @param material the block/item material
     * @param player   the selling player — used by mine-aware providers to look up
     *                 which mine tier the player is currently in
     */
    long getSellPrice(Material material, Player player);
}
