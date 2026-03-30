package com.prison.shop;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.List;

/**
 * ShopDefaults — seeds the shop with default categories and items on first startup.
 *
 * Called by ShopPlugin.onEnable() only when the shop is empty (fresh install or
 * cleared config). Does nothing if categories already exist.
 *
 * To re-run: clear categories in config.yml and restart the server, or use
 * /shopadmin to add/remove items manually at any time.
 */
public class ShopDefaults {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    /**
     * Populate all default categories. Each category + item is saved to config
     * as it's added (ShopManager.addItem calls saveCategories internally).
     */
    public static void populate(ShopPlugin plugin) {
        ShopManager mgr = ShopManager.getInstance();

        populateCrateKeys(plugin, mgr);
        populateConsumables(mgr);
        populateUtilities(mgr);

        plugin.getLogger().info("[Shop] Default categories populated — "
            + mgr.getCategories().size() + " categories ready.");
    }

    // ----------------------------------------------------------------
    // Category 1: Crate Keys
    // ----------------------------------------------------------------

    private static void populateCrateKeys(ShopPlugin plugin, ShopManager mgr) {
        mgr.addCategory("crate_keys", "<gold><bold>Crate Keys", Material.TRIPWIRE_HOOK);

        // Keys use a PDC tag set by the crates plugin to prevent counterfeits.
        // We look up PrisonCrates at runtime to get the correct NamespacedKey.
        Plugin cratesPlugin = Bukkit.getPluginManager().getPlugin("PrisonCrates");
        if (cratesPlugin == null) {
            plugin.getLogger().warning("[Shop] PrisonCrates not found — crate key PDC tags not set. Keys will not open crates.");
        }

        mgr.addItem("crate_keys",
            makeKey(Material.TRIPWIRE_HOOK, "Common Crate", "<white>", "common", cratesPlugin),
            "<white><bold>Common Crate Key", 10_000L, -1);

        mgr.addItem("crate_keys",
            makeKey(Material.ENDER_EYE, "Rare Crate", "<light_purple>", "rare", cratesPlugin),
            "<light_purple><bold>Rare Crate Key", 50_000L, -1);

        mgr.addItem("crate_keys",
            makeKey(Material.NETHER_STAR, "Legendary Crate", "<gold>", "legendary", cratesPlugin),
            "<gold><bold>Legendary Crate Key", 200_000L, -1);
    }

    private static ItemStack makeKey(Material mat, String displayName, String color,
                                      String tierId, Plugin cratesPlugin) {
        ItemStack key = new ItemStack(mat);
        ItemMeta meta = key.getItemMeta();
        meta.displayName(MM.deserialize(color + "<bold>" + displayName + " Key"));
        meta.lore(List.of(
            MM.deserialize("<gray>Right-click a <white>" + displayName + "</white><gray> crate to open it."),
            MM.deserialize("<dark_gray>Tier: " + tierId)
        ));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);

        if (cratesPlugin != null) {
            NamespacedKey pdcKey = new NamespacedKey(cratesPlugin, "crate_key_tier");
            meta.getPersistentDataContainer().set(pdcKey, PersistentDataType.STRING, tierId);
        }

        key.setItemMeta(meta);
        return key;
    }

    // ----------------------------------------------------------------
    // Category 2: Consumables
    // ----------------------------------------------------------------

    private static void populateConsumables(ShopManager mgr) {
        mgr.addCategory("consumables", "<yellow><bold>Consumables", Material.HONEY_BOTTLE);

        // Enchanted Golden Apple
        ItemStack notch = new ItemStack(Material.ENCHANTED_GOLDEN_APPLE);
        setMeta(notch, "<gold><bold>Enchanted Golden Apple",
            "<gray>Grants Absorption IV, Regeneration II,",
            "<gray>Resistance and Fire Resistance for 30s.",
            "<dark_gray>Ideal for dangerous PvP situations.");
        mgr.addItem("consumables", notch, "<gold><bold>Enchanted Golden Apple", 30_000L, -1);

        // XP Bottles x16
        ItemStack xp = new ItemStack(Material.EXPERIENCE_BOTTLE, 16);
        setMeta(xp, "<green>Experience Bottle <dark_gray>×16",
            "<gray>Throw to gain experience.",
            "<dark_gray>Useful for anvil repairs.");
        mgr.addItem("consumables", xp, "<green>Experience Bottle <dark_gray>×16", 5_000L, -1);

        // Fire Resistance Splash Potion
        ItemStack firePot = new ItemStack(Material.SPLASH_POTION);
        PotionMeta potMeta = (PotionMeta) firePot.getItemMeta();
        potMeta.setBasePotionType(PotionType.FIRE_RESISTANCE);
        potMeta.displayName(MM.deserialize("<red>Fire Resistance Potion"));
        potMeta.lore(List.of(
            MM.deserialize("<gray>Splash for Fire Resistance."),
            MM.deserialize("<dark_gray>Lasts 3 minutes.")
        ));
        potMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS,
            ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        firePot.setItemMeta(potMeta);
        mgr.addItem("consumables", firePot, "<red>Fire Resistance Potion", 3_500L, -1);

        // Golden Carrot x16
        ItemStack carrot = new ItemStack(Material.GOLDEN_CARROT, 16);
        setMeta(carrot, "<gold>Golden Carrot <dark_gray>×16",
            "<gray>High-saturation food.",
            "<dark_gray>Best food-to-hunger ratio in the game.");
        mgr.addItem("consumables", carrot, "<gold>Golden Carrot <dark_gray>×16", 4_000L, -1);
    }

    // ----------------------------------------------------------------
    // Category 3: Utilities
    // ----------------------------------------------------------------

    private static void populateUtilities(ShopManager mgr) {
        mgr.addCategory("utilities", "<aqua><bold>Utilities", Material.CHEST);

        // Ender Chest
        ItemStack ec = new ItemStack(Material.ENDER_CHEST);
        setMeta(ec, "<light_purple>Ender Chest",
            "<gray>Place to access your personal ender storage.",
            "<dark_gray>27 slots, unique per player — survives death.");
        mgr.addItem("utilities", ec, "<light_purple>Ender Chest", 15_000L, -1);

        // Torches x64
        ItemStack torches = new ItemStack(Material.TORCH, 64);
        setMeta(torches, "<yellow>Torch <dark_gray>×64",
            "<gray>A full stack of torches.",
            "<dark_gray>Light up your mine.");
        mgr.addItem("utilities", torches, "<yellow>Torch <dark_gray>×64", 1_500L, -1);

        // Name Tag
        ItemStack nameTag = new ItemStack(Material.NAME_TAG);
        setMeta(nameTag, "<white>Name Tag",
            "<gray>Rename a mob or item using an anvil.",
            "<dark_gray>Not craftable in vanilla survival.");
        mgr.addItem("utilities", nameTag, "<white>Name Tag", 25_000L, -1);

        // Saddle
        ItemStack saddle = new ItemStack(Material.SADDLE);
        setMeta(saddle, "<white>Saddle",
            "<gray>Equip on a horse to ride it.",
            "<dark_gray>Not craftable in vanilla survival.");
        mgr.addItem("utilities", saddle, "<white>Saddle", 8_000L, -1);
    }

    // ----------------------------------------------------------------
    // Helper
    // ----------------------------------------------------------------

    private static void setMeta(ItemStack item, String displayName, String... loreLines) {
        ItemMeta meta = item.getItemMeta();
        meta.displayName(MM.deserialize(displayName));
        List<Component> lore = new ArrayList<>();
        for (String line : loreLines) {
            lore.add(MM.deserialize(line));
        }
        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
    }
}
