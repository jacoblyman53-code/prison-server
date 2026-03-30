package com.prison.permissions;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * ItemOriginTag — stamps items with their economy origin.
 *
 * <ul>
 *   <li>{@link Origin#IN_GAME} — purchased from the IGC shop; earned in-game.</li>
 *   <li>{@link Origin#PREMIUM} — obtained via crates, boosters, or Tebex delivery.
 *       Tradable in the auction house but NOT purchasable in the IGC shop.</li>
 *   <li>{@link Origin#ADMIN} — spawned by an admin; flagged for auditing.</li>
 * </ul>
 *
 * The key uses the literal namespace {@code "prisoncore"} so it is consistent
 * across all plugins without requiring a plugin-instance reference.
 */
public final class ItemOriginTag {

    public enum Origin { IN_GAME, PREMIUM, ADMIN }

    /** Shared PDC key — namespace "prisoncore", key "item_origin". */
    @SuppressWarnings("deprecation")
    private static final NamespacedKey KEY = new NamespacedKey("prisoncore", "item_origin");

    private ItemOriginTag() {}

    /**
     * Stamp {@code item} with the given origin. Mutates the item's meta in-place.
     * No-op if the item is null or has no meta.
     */
    public static void set(ItemStack item, Origin origin) {
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(KEY, PersistentDataType.STRING, origin.name());
        item.setItemMeta(meta);
    }

    /**
     * Returns the origin stored on {@code item}, or {@code null} if untagged.
     */
    public static Origin get(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String val = item.getItemMeta().getPersistentDataContainer()
                         .get(KEY, PersistentDataType.STRING);
        if (val == null) return null;
        try {
            return Origin.valueOf(val);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** {@code true} if the item is tagged {@link Origin#PREMIUM}. */
    public static boolean isPremium(ItemStack item) {
        return Origin.PREMIUM == get(item);
    }

    /** {@code true} if the item is tagged {@link Origin#IN_GAME}. */
    public static boolean isInGame(ItemStack item) {
        return Origin.IN_GAME == get(item);
    }

    /** {@code true} if the item is tagged {@link Origin#ADMIN}. */
    public static boolean isAdmin(ItemStack item) {
        return Origin.ADMIN == get(item);
    }
}
