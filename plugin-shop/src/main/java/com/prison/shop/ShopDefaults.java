package com.prison.shop;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.List;

/**
 * ShopDefaults — seeds the shop with default categories and items on first startup.
 *
 * Only runs when the shop is empty (fresh install or cleared config).
 * To re-run: clear categories in config.yml and restart, or use /shopadmin.
 *
 * Economy separation: crate keys and boosters are PREMIUM items — they are NOT
 * sold in the IGC shop (players obtain them from crates/Tebex only).
 * All items here are tagged IN_GAME on purchase by ShopManager.
 *
 * Category overview:
 *   1. Tools           — pickaxes & swords (early → late)
 *   2. Consumables     — food & potions
 *   3. Building Blocks — raw and decorative blocks (early → late)
 *   4. Enchanted Books — upgrade books for gear
 *   5. Utilities       — misc non-craftable items
 */
public class ShopDefaults {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    public static void populate(ShopPlugin plugin) {
        ShopManager mgr = ShopManager.getInstance();

        populateTools(mgr);
        populateConsumables(mgr);
        populateBuildingBlocks(mgr);
        populateEnchantedBooks(mgr);
        populateUtilities(mgr);

        plugin.getLogger().info("[Shop] Default categories populated — "
            + mgr.getCategories().size() + " categories ready.");
    }

    // ----------------------------------------------------------------
    // Category 1: Tools
    // ----------------------------------------------------------------

    private static void populateTools(ShopManager mgr) {
        mgr.addCategory("tools", "<aqua><bold>Tools", Material.IRON_PICKAXE);

        // --- Early game ---
        ItemStack woodPick = new ItemStack(Material.WOODEN_PICKAXE);
        setMeta(woodPick, "<gray>Wooden Pickaxe",
            "<dark_gray>Early-game. Gets the job done.",
            "<dark_gray>Upgrade ASAP.");
        mgr.addItem("tools", woodPick, "<gray>Wooden Pickaxe", 200L, -1);

        ItemStack stonePick = new ItemStack(Material.STONE_PICKAXE);
        setMeta(stonePick, "<gray>Stone Pickaxe",
            "<dark_gray>Better than wood. Decent for new players.");
        mgr.addItem("tools", stonePick, "<gray>Stone Pickaxe", 600L, -1);

        // --- Mid game ---
        ItemStack ironPick = new ItemStack(Material.IRON_PICKAXE);
        enchant(ironPick, Enchantment.EFFICIENCY, 2);
        enchant(ironPick, Enchantment.UNBREAKING, 1);
        setMeta(ironPick, "<white>Iron Pickaxe",
            "<gray>Efficiency II · Unbreaking I",
            "<dark_gray>Solid mid-game mining pick.");
        mgr.addItem("tools", ironPick, "<white>Iron Pickaxe", 4_000L, -1);

        ItemStack goldPick = new ItemStack(Material.GOLDEN_PICKAXE);
        enchant(goldPick, Enchantment.EFFICIENCY, 3);
        enchant(goldPick, Enchantment.UNBREAKING, 2);
        setMeta(goldPick, "<gold>Golden Pickaxe",
            "<gray>Efficiency III · Unbreaking II",
            "<dark_gray>Fastest base speed — great for prison mines.");
        mgr.addItem("tools", goldPick, "<gold>Golden Pickaxe", 12_000L, -1);

        // --- Late game ---
        ItemStack diamondPick = new ItemStack(Material.DIAMOND_PICKAXE);
        enchant(diamondPick, Enchantment.EFFICIENCY, 4);
        enchant(diamondPick, Enchantment.UNBREAKING, 3);
        enchant(diamondPick, Enchantment.FORTUNE, 2);
        setMeta(diamondPick, "<aqua>Diamond Pickaxe",
            "<gray>Efficiency IV · Unbreaking III · Fortune II",
            "<dark_gray>Strong late-game option before custom enchants.");
        mgr.addItem("tools", diamondPick, "<aqua>Diamond Pickaxe", 60_000L, -1);

        // --- Swords (mid + late) ---
        ItemStack ironSword = new ItemStack(Material.IRON_SWORD);
        enchant(ironSword, Enchantment.SHARPNESS, 2);
        setMeta(ironSword, "<white>Iron Sword",
            "<gray>Sharpness II",
            "<dark_gray>Mid-game PvP or mob combat.");
        mgr.addItem("tools", ironSword, "<white>Iron Sword", 3_500L, -1);

        ItemStack diamondSword = new ItemStack(Material.DIAMOND_SWORD);
        enchant(diamondSword, Enchantment.SHARPNESS, 4);
        enchant(diamondSword, Enchantment.UNBREAKING, 3);
        setMeta(diamondSword, "<aqua>Diamond Sword",
            "<gray>Sharpness IV · Unbreaking III",
            "<dark_gray>Late-game combat weapon.");
        mgr.addItem("tools", diamondSword, "<aqua>Diamond Sword", 45_000L, -1);
    }

    // ----------------------------------------------------------------
    // Category 2: Consumables
    // ----------------------------------------------------------------

    private static void populateConsumables(ShopManager mgr) {
        mgr.addCategory("consumables", "<yellow><bold>Consumables", Material.COOKED_BEEF);

        // --- Food (early → late) ---
        ItemStack bread = new ItemStack(Material.BREAD, 16);
        setMeta(bread, "<yellow>Bread <dark_gray>×16",
            "<gray>Basic food. Decent saturation.",
            "<dark_gray>Cheap early-game option.");
        mgr.addItem("consumables", bread, "<yellow>Bread <dark_gray>×16", 800L, -1);

        ItemStack beef = new ItemStack(Material.COOKED_BEEF, 16);
        setMeta(beef, "<white>Cooked Beef <dark_gray>×16",
            "<gray>High saturation. Best vanilla food.",
            "<dark_gray>Keep your hunger bar full.");
        mgr.addItem("consumables", beef, "<white>Cooked Beef <dark_gray>×16", 2_500L, -1);

        ItemStack carrot = new ItemStack(Material.GOLDEN_CARROT, 8);
        setMeta(carrot, "<gold>Golden Carrot <dark_gray>×8",
            "<gray>Highest saturation in the game.",
            "<dark_gray>Best value per slot for long sessions.");
        mgr.addItem("consumables", carrot, "<gold>Golden Carrot <dark_gray>×8", 3_200L, -1);

        ItemStack notch = new ItemStack(Material.ENCHANTED_GOLDEN_APPLE);
        setMeta(notch, "<gold><bold>Enchanted Golden Apple",
            "<gray>Absorption IV, Regeneration II,",
            "<gray>Resistance and Fire Resistance for 30s.",
            "<dark_gray>Ideal for dangerous PvP situations.");
        mgr.addItem("consumables", notch, "<gold><bold>Enchanted Golden Apple", 30_000L, -1);

        ItemStack xp = new ItemStack(Material.EXPERIENCE_BOTTLE, 16);
        setMeta(xp, "<green>Experience Bottle <dark_gray>×16",
            "<gray>Throw to gain experience.",
            "<dark_gray>Useful for anvil repairs.");
        mgr.addItem("consumables", xp, "<green>Experience Bottle <dark_gray>×16", 5_000L, -1);

        // --- Potions ---
        ItemStack healSplash = makeSplashPotion(PotionType.HEALING,
            "<red>Healing Potion",
            "<gray>Splash for Instant Health II.",
            "<dark_gray>Quick burst heal in combat.");
        mgr.addItem("consumables", healSplash, "<red>Healing Potion", 2_000L, -1);

        ItemStack firePot = makeSplashPotion(PotionType.FIRE_RESISTANCE,
            "<red>Fire Resistance Potion",
            "<gray>Splash for Fire Resistance.",
            "<dark_gray>Lasts 3 minutes.");
        mgr.addItem("consumables", firePot, "<red>Fire Resistance Potion", 3_500L, -1);

        ItemStack speedPot = makeSplashPotion(PotionType.SWIFTNESS,
            "<white>Speed Potion",
            "<gray>Splash for Speed II.",
            "<dark_gray>Lasts 1 min 30 s. Useful for mine running.");
        mgr.addItem("consumables", speedPot, "<white>Speed Potion", 2_500L, -1);

        ItemStack strPot = makeSplashPotion(PotionType.STRENGTH,
            "<dark_red>Strength Potion",
            "<gray>Splash for Strength II.",
            "<dark_gray>Lasts 1 min 30 s. Significant PvP boost.");
        mgr.addItem("consumables", strPot, "<dark_red>Strength Potion", 5_000L, -1);

        ItemStack regenPot = makeSplashPotion(PotionType.REGENERATION,
            "<light_purple>Regeneration Potion",
            "<gray>Splash for Regeneration II.",
            "<dark_gray>Lasts 22 s. Strong sustain in prolonged fights.");
        mgr.addItem("consumables", regenPot, "<light_purple>Regeneration Potion", 6_000L, -1);
    }

    // ----------------------------------------------------------------
    // Category 3: Building Blocks
    // ----------------------------------------------------------------

    private static void populateBuildingBlocks(ShopManager mgr) {
        mgr.addCategory("building", "<green><bold>Building Blocks", Material.BRICKS);

        // --- Early game (cheap, common) ---
        ItemStack cobble = new ItemStack(Material.COBBLESTONE, 64);
        setMeta(cobble, "<gray>Cobblestone <dark_gray>×64",
            "<gray>Basic building block.",
            "<dark_gray>Cheap filler and wall material.");
        mgr.addItem("building", cobble, "<gray>Cobblestone <dark_gray>×64", 300L, -1);

        ItemStack stone = new ItemStack(Material.STONE, 64);
        setMeta(stone, "<gray>Stone <dark_gray>×64",
            "<gray>Smooth look for builds.",
            "<dark_gray>Great base material.");
        mgr.addItem("building", stone, "<gray>Stone <dark_gray>×64", 500L, -1);

        ItemStack sand = new ItemStack(Material.SAND, 64);
        setMeta(sand, "<yellow>Sand <dark_gray>×64",
            "<gray>Crafting and decoration.",
            "<dark_gray>Used for glass and sandstone.");
        mgr.addItem("building", sand, "<yellow>Sand <dark_gray>×64", 400L, -1);

        ItemStack gravel = new ItemStack(Material.GRAVEL, 64);
        setMeta(gravel, "<gray>Gravel <dark_gray>×64",
            "<gray>Path material and flint source.",
            "<dark_gray>Useful for decorative paths.");
        mgr.addItem("building", gravel, "<gray>Gravel <dark_gray>×64", 350L, -1);

        // --- Mid game ---
        ItemStack oak = new ItemStack(Material.OAK_PLANKS, 64);
        setMeta(oak, "<gold>Oak Planks <dark_gray>×64",
            "<gray>Versatile wood for builds.",
            "<dark_gray>Warm and classic prison aesthetic.");
        mgr.addItem("building", oak, "<gold>Oak Planks <dark_gray>×64", 800L, -1);

        ItemStack spruce = new ItemStack(Material.SPRUCE_PLANKS, 64);
        setMeta(spruce, "<dark_gray>Spruce Planks <dark_gray>×64",
            "<gray>Dark wood — modern build look.",
            "<dark_gray>Popular for floor and wall accents.");
        mgr.addItem("building", spruce, "<dark_gray>Spruce Planks <dark_gray>×64", 800L, -1);

        ItemStack bricks = new ItemStack(Material.BRICKS, 64);
        setMeta(bricks, "<red>Bricks <dark_gray>×64",
            "<gray>Classic prison aesthetic.",
            "<dark_gray>Industrial and durable look.");
        mgr.addItem("building", bricks, "<red>Bricks <dark_gray>×64", 2_000L, -1);

        ItemStack smoothStone = new ItemStack(Material.SMOOTH_STONE, 64);
        setMeta(smoothStone, "<gray>Smooth Stone <dark_gray>×64",
            "<gray>Clean flat surface.",
            "<dark_gray>Great for modern industrial builds.");
        mgr.addItem("building", smoothStone, "<gray>Smooth Stone <dark_gray>×64", 1_500L, -1);

        // --- Late game (premium building materials) ---
        ItemStack glass = new ItemStack(Material.GLASS, 64);
        setMeta(glass, "<aqua>Glass <dark_gray>×64",
            "<gray>Transparent window block.",
            "<dark_gray>Great for stylish cell windows.");
        mgr.addItem("building", glass, "<aqua>Glass <dark_gray>×64", 3_000L, -1);

        ItemStack quartz = new ItemStack(Material.QUARTZ_BLOCK, 64);
        setMeta(quartz, "<white>Quartz Block <dark_gray>×64",
            "<gray>Clean premium block.",
            "<dark_gray>High-end build material.");
        mgr.addItem("building", quartz, "<white>Quartz Block <dark_gray>×64", 8_000L, -1);

        ItemStack blackstone = new ItemStack(Material.POLISHED_BLACKSTONE, 64);
        setMeta(blackstone, "<dark_gray>Polished Blackstone <dark_gray>×64",
            "<gray>Dark premium block.",
            "<dark_gray>Ideal for high-end dark builds.");
        mgr.addItem("building", blackstone, "<dark_gray>Polished Blackstone <dark_gray>×64", 6_000L, -1);

        ItemStack obsidian = new ItemStack(Material.OBSIDIAN, 8);
        setMeta(obsidian, "<dark_purple>Obsidian <dark_gray>×8",
            "<gray>Blast-resistant luxury block.",
            "<dark_gray>Required for portals and high-security builds.");
        mgr.addItem("building", obsidian, "<dark_purple>Obsidian <dark_gray>×8", 15_000L, -1);
    }

    // ----------------------------------------------------------------
    // Category 4: Enchanted Books
    // ----------------------------------------------------------------

    private static void populateEnchantedBooks(ShopManager mgr) {
        mgr.addCategory("books", "<light_purple><bold>Enchanted Books", Material.ENCHANTED_BOOK);

        // --- Efficiency ---
        mgr.addItem("books",
            makeBook(Enchantment.EFFICIENCY, 3),
            "<aqua>Efficiency III Book", 10_000L, -1);

        mgr.addItem("books",
            makeBook(Enchantment.EFFICIENCY, 4),
            "<aqua>Efficiency IV Book", 30_000L, -1);

        mgr.addItem("books",
            makeBook(Enchantment.EFFICIENCY, 5),
            "<aqua>Efficiency V Book", 80_000L, -1);

        // --- Fortune ---
        mgr.addItem("books",
            makeBook(Enchantment.FORTUNE, 2),
            "<gold>Fortune II Book", 20_000L, -1);

        mgr.addItem("books",
            makeBook(Enchantment.FORTUNE, 3),
            "<gold>Fortune III Book", 60_000L, -1);

        // --- Protection ---
        mgr.addItem("books",
            makeBook(Enchantment.PROTECTION, 3),
            "<blue>Protection III Book", 15_000L, -1);

        mgr.addItem("books",
            makeBook(Enchantment.PROTECTION, 4),
            "<blue>Protection IV Book", 40_000L, -1);

        // --- Unbreaking ---
        mgr.addItem("books",
            makeBook(Enchantment.UNBREAKING, 3),
            "<gray>Unbreaking III Book", 12_000L, -1);

        // --- Sharpness ---
        mgr.addItem("books",
            makeBook(Enchantment.SHARPNESS, 4),
            "<red>Sharpness IV Book", 25_000L, -1);

        mgr.addItem("books",
            makeBook(Enchantment.SHARPNESS, 5),
            "<red>Sharpness V Book", 70_000L, -1);

        // --- High value ---
        mgr.addItem("books",
            makeBook(Enchantment.LOOTING, 3),
            "<yellow>Looting III Book", 35_000L, -1);

        mgr.addItem("books",
            makeBook(Enchantment.FEATHER_FALLING, 4),
            "<green>Feather Falling IV Book", 8_000L, -1);

        mgr.addItem("books",
            makeBook(Enchantment.MENDING, 1),
            "<green><bold>Mending Book", 150_000L, -1);
    }

    // ----------------------------------------------------------------
    // Category 5: Utilities
    // ----------------------------------------------------------------

    private static void populateUtilities(ShopManager mgr) {
        mgr.addCategory("utilities", "<aqua><bold>Utilities", Material.CHEST);

        // --- Storage ---
        ItemStack chest = new ItemStack(Material.CHEST, 4);
        setMeta(chest, "<yellow>Chest <dark_gray>×4",
            "<gray>27-slot storage container.",
            "<dark_gray>Basic storage for your cell.");
        mgr.addItem("utilities", chest, "<yellow>Chest <dark_gray>×4", 1_000L, -1);

        ItemStack ec = new ItemStack(Material.ENDER_CHEST);
        setMeta(ec, "<light_purple>Ender Chest",
            "<gray>Personal 27-slot ender storage.",
            "<dark_gray>Survives death — accessible from anywhere.");
        mgr.addItem("utilities", ec, "<light_purple>Ender Chest", 15_000L, -1);

        ItemStack barrel = new ItemStack(Material.BARREL);
        setMeta(barrel, "<gold>Barrel",
            "<gray>27 slots like a chest, but opens from all sides.",
            "<dark_gray>Space-efficient storage option.");
        mgr.addItem("utilities", barrel, "<gold>Barrel", 2_500L, -1);

        // --- Lighting / misc ---
        ItemStack torches = new ItemStack(Material.TORCH, 64);
        setMeta(torches, "<yellow>Torch <dark_gray>×64",
            "<gray>A full stack of torches.",
            "<dark_gray>Light up your mine or cell.");
        mgr.addItem("utilities", torches, "<yellow>Torch <dark_gray>×64", 1_500L, -1);

        ItemStack lantern = new ItemStack(Material.LANTERN, 16);
        setMeta(lantern, "<yellow>Lantern <dark_gray>×16",
            "<gray>Brighter decorative light source.",
            "<dark_gray>Great for cell interiors.");
        mgr.addItem("utilities", lantern, "<yellow>Lantern <dark_gray>×16", 3_000L, -1);

        // --- Crafting / utilities ---
        ItemStack anvil = new ItemStack(Material.ANVIL);
        setMeta(anvil, "<gray>Anvil",
            "<gray>Repair and combine enchanted gear.",
            "<dark_gray>Essential for maintaining your kit.");
        mgr.addItem("utilities", anvil, "<gray>Anvil", 8_000L, -1);

        ItemStack craftTable = new ItemStack(Material.CRAFTING_TABLE);
        setMeta(craftTable, "<white>Crafting Table",
            "<gray>Craft items using a 3×3 grid.",
            "<dark_gray>Useful if your cell lacks one.");
        mgr.addItem("utilities", craftTable, "<white>Crafting Table", 500L, -1);

        ItemStack nameTag = new ItemStack(Material.NAME_TAG);
        setMeta(nameTag, "<white>Name Tag",
            "<gray>Rename a mob or item using an anvil.",
            "<dark_gray>Not craftable in vanilla survival.");
        mgr.addItem("utilities", nameTag, "<white>Name Tag", 25_000L, -1);

        ItemStack saddle = new ItemStack(Material.SADDLE);
        setMeta(saddle, "<white>Saddle",
            "<gray>Equip on a horse to ride it.",
            "<dark_gray>Not craftable in vanilla survival.");
        mgr.addItem("utilities", saddle, "<white>Saddle", 8_000L, -1);

        // --- Fishing / misc ---
        ItemStack fishRod = new ItemStack(Material.FISHING_ROD);
        enchant(fishRod, Enchantment.LUCK_OF_THE_SEA, 3);
        enchant(fishRod, Enchantment.LURE, 3);
        setMeta(fishRod, "<aqua>Fishing Rod",
            "<gray>Luck of the Sea III · Lure III",
            "<dark_gray>Fish quickly for XP and rare drops.");
        mgr.addItem("utilities", fishRod, "<aqua>Fishing Rod", 18_000L, -1);
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private static void setMeta(ItemStack item, String displayName, String... loreLines) {
        ItemMeta meta = item.getItemMeta();
        meta.displayName(MM.deserialize("<!italic>" + displayName));
        List<Component> lore = new ArrayList<>();
        for (String line : loreLines) {
            lore.add(MM.deserialize("<!italic>" + line));
        }
        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS,
            ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        item.setItemMeta(meta);
    }

    /** Apply an enchantment at the given level (unsafe — allows above-vanilla). */
    private static void enchant(ItemStack item, Enchantment ench, int level) {
        item.addUnsafeEnchantment(ench, level);
    }

    /** Create an enchanted book storing the given enchantment. */
    private static ItemStack makeBook(Enchantment ench, int level) {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();

        // Display name: format "minecraft:efficiency_4" → "Efficiency IV"
        String enchName = capitalize(ench.getKey().getKey().replace("_", " "));
        String roman = toRoman(level);
        meta.displayName(MM.deserialize("<!italic><light_purple>" + enchName + " " + roman));
        meta.lore(List.of(
            MM.deserialize("<!italic><gray>Apply to compatible gear via anvil."),
            MM.deserialize("<!italic><dark_gray>Tier: " + roman)
        ));
        meta.addStoredEnchant(ench, level, true);
        meta.addItemFlags(ItemFlag.HIDE_STORED_ENCHANTS);
        book.setItemMeta(meta);
        return book;
    }

    private static ItemStack makeSplashPotion(PotionType type, String name, String... lore) {
        ItemStack pot = new ItemStack(Material.SPLASH_POTION);
        PotionMeta meta = (PotionMeta) pot.getItemMeta();
        meta.setBasePotionType(type);
        meta.displayName(MM.deserialize("<!italic>" + name));
        List<Component> loreList = new ArrayList<>();
        for (String line : lore) {
            loreList.add(MM.deserialize("<!italic>" + line));
        }
        meta.lore(loreList);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS,
            ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        pot.setItemMeta(meta);
        return pot;
    }

    private static String capitalize(String s) {
        String[] words = s.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1).toLowerCase());
        }
        return sb.toString();
    }

    private static String toRoman(int n) {
        return switch (n) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(n);
        };
    }
}
