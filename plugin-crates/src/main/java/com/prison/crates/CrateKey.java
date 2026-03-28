package com.prison.crates;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/**
 * CrateKey — utility class for creating and reading PDC-tagged crate key items.
 *
 * The tag {@code prison:crate_key_tier} holds the tier ID string.
 * Any item without this exact PDC tag is rejected as a counterfeit.
 */
public class CrateKey {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    /** PDC key that stores the crate tier ID on a key item. */
    static NamespacedKey KEY_TIER_KEY;

    /** Called once from CratePlugin.onEnable() to register the NamespacedKey. */
    static void init(CratePlugin plugin) {
        KEY_TIER_KEY = new NamespacedKey(plugin, "crate_key_tier");
    }

    /**
     * Create a physical key item for the given tier.
     * The item has the PDC tag set — cannot be crafted or faked.
     */
    public static ItemStack createKey(CrateTier tier) {
        ItemStack key = new ItemStack(tier.keyMaterial());
        ItemMeta meta = key.getItemMeta();
        meta.displayName(MM.deserialize(tier.keyColor() + "<bold>" + tier.displayName() + " Key"));
        meta.lore(List.of(
            MM.deserialize("<gray>Right-click a <white>" + tier.displayName() + "</white><gray> to open it."),
            MM.deserialize("<dark_gray>Tier: " + tier.id())
        ));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        meta.getPersistentDataContainer().set(KEY_TIER_KEY, PersistentDataType.STRING, tier.id());
        key.setItemMeta(meta);
        return key;
    }

    /**
     * Returns the crate tier ID stored in this item's PDC, or {@code null} if
     * the item is not a valid crate key.
     */
    public static String getTierIdFromKey(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (KEY_TIER_KEY == null) return null;
        return meta.getPersistentDataContainer().get(KEY_TIER_KEY, PersistentDataType.STRING);
    }
}
