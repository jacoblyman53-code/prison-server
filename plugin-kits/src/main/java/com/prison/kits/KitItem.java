package com.prison.kits;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * KitItem — one item entry in a kit's contents list.
 * Supports material, amount, optional enchants, and optional custom display name.
 */
public record KitItem(
    Material material,
    int amount,
    Map<Enchantment, Integer> enchants,
    String displayName   // null = use default item name
) {

    /** Build from a YAML section entry. Returns null and logs a warning on bad material. */
    public static KitItem fromConfig(ConfigurationSection section, Logger logger) {
        String matName = section.getString("material", "AIR").toUpperCase();
        Material material;
        try {
            material = Material.valueOf(matName);
        } catch (IllegalArgumentException e) {
            logger.warning("[Kits] Unknown material '" + matName + "' — skipping kit item.");
            return null;
        }

        int amount = Math.max(1, section.getInt("amount", 1));
        String displayName = section.getString("display", null);

        Map<Enchantment, Integer> enchants = new LinkedHashMap<>();
        ConfigurationSection enchSection = section.getConfigurationSection("enchants");
        if (enchSection != null) {
            for (String enchName : enchSection.getKeys(false)) {
                Enchantment ench = Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft(enchName.toLowerCase()));
                if (ench == null) {
                    logger.warning("[Kits] Unknown enchantment '" + enchName + "' — skipping.");
                    continue;
                }
                enchants.put(ench, enchSection.getInt(enchName, 1));
            }
        }

        return new KitItem(material, amount, enchants, displayName);
    }

    /** Build the Bukkit ItemStack for this kit item. */
    public ItemStack toItemStack() {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (displayName != null && !displayName.isBlank()) {
                meta.displayName(MiniMessage.miniMessage().deserialize(displayName));
            }
            for (Map.Entry<Enchantment, Integer> e : enchants.entrySet()) {
                meta.addEnchant(e.getKey(), e.getValue(), true);
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
