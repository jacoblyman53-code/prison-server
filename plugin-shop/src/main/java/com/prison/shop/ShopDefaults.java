package com.prison.shop;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

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
 *   1. Blocks      — raw and decorative building blocks
 *   2. Ores        — ores, ingots, gems, and netherite
 *   3. Farming     — crops, seeds, and natural items
 *   4. Mob Drops   — loot from hostile and passive mobs
 *   5. Food        — cooked food and consumables
 *   6. Tools       — pickaxes, swords, bows, and utility tools (unenchanted)
 *   7. Armor       — all tiers of armor and shields (unenchanted)
 *   8. Redstone    — redstone components and mechanisms
 *   9. Utilities   — chests, furnaces, transport, and misc functional items
 *  10. Misc        — wool, terracotta, nether items, ice, and other blocks
 */
public class ShopDefaults {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    public static void populate(ShopPlugin plugin) {
        ShopManager mgr = ShopManager.getInstance();

        populateBlocks(mgr);
        populateOres(mgr);
        populateFarming(mgr);
        populateMobDrops(mgr);
        populateFood(mgr);
        populateTools(mgr);
        populateArmor(mgr);
        populateRedstone(mgr);
        populateUtilities(mgr);
        populateMisc(mgr);

        plugin.getLogger().info("[Shop] Default categories populated — "
            + mgr.getCategories().size() + " categories ready.");
    }

    // ----------------------------------------------------------------
    // Category 1: Blocks
    // ----------------------------------------------------------------

    private static void populateBlocks(ShopManager mgr) {
        mgr.addCategory("blocks", "<yellow><bold>Blocks", Material.OAK_LOG);

        mgr.addItem("blocks", new ItemStack(Material.DIRT),       "Dirt",       1L,  -1);
        mgr.addItem("blocks", new ItemStack(Material.COBBLESTONE),"Cobblestone",2L,  -1);
        mgr.addItem("blocks", new ItemStack(Material.STONE),      "Stone",      3L,  -1);
        mgr.addItem("blocks", new ItemStack(Material.SAND),       "Sand",       4L,  -1);
        mgr.addItem("blocks", new ItemStack(Material.GRAVEL),     "Gravel",     5L,  -1);

        mgr.addItem("blocks", new ItemStack(Material.OAK_LOG),       "Oak Log",       8L,  -1);
        mgr.addItem("blocks", new ItemStack(Material.BIRCH_LOG),     "Birch Log",     9L,  -1);
        mgr.addItem("blocks", new ItemStack(Material.SPRUCE_LOG),    "Spruce Log",    10L, -1);
        mgr.addItem("blocks", new ItemStack(Material.JUNGLE_LOG),    "Jungle Log",    11L, -1);
        mgr.addItem("blocks", new ItemStack(Material.ACACIA_LOG),    "Acacia Log",    12L, -1);
        mgr.addItem("blocks", new ItemStack(Material.DARK_OAK_LOG),  "Dark Oak Log",  12L, -1);
        mgr.addItem("blocks", new ItemStack(Material.MANGROVE_LOG),  "Mangrove Log",  11L, -1);
        mgr.addItem("blocks", new ItemStack(Material.CHERRY_LOG),    "Cherry Log",    10L, -1);

        mgr.addItem("blocks", new ItemStack(Material.OAK_PLANKS),      "Oak Planks",      10L, -1);
        mgr.addItem("blocks", new ItemStack(Material.BIRCH_PLANKS),    "Birch Planks",    11L, -1);
        mgr.addItem("blocks", new ItemStack(Material.SPRUCE_PLANKS),   "Spruce Planks",   12L, -1);
        mgr.addItem("blocks", new ItemStack(Material.JUNGLE_PLANKS),   "Jungle Planks",   13L, -1);
        mgr.addItem("blocks", new ItemStack(Material.ACACIA_PLANKS),   "Acacia Planks",   14L, -1);
        mgr.addItem("blocks", new ItemStack(Material.DARK_OAK_PLANKS), "Dark Oak Planks", 14L, -1);
    }

    // ----------------------------------------------------------------
    // Category 2: Ores
    // ----------------------------------------------------------------

    private static void populateOres(ShopManager mgr) {
        mgr.addCategory("ores", "<aqua><bold>Ores", Material.DIAMOND);

        mgr.addItem("ores", new ItemStack(Material.COAL),           "Coal",           8L,    -1);
        mgr.addItem("ores", new ItemStack(Material.RAW_IRON),       "Raw Iron",       12L,   -1);
        mgr.addItem("ores", new ItemStack(Material.IRON_INGOT),     "Iron Ingot",     15L,   -1);
        mgr.addItem("ores", new ItemStack(Material.LAPIS_LAZULI),   "Lapis Lazuli",   10L,   -1);
        mgr.addItem("ores", new ItemStack(Material.REDSTONE),       "Redstone",       5L,    -1);
        mgr.addItem("ores", new ItemStack(Material.RAW_GOLD),       "Raw Gold",       28L,   -1);
        mgr.addItem("ores", new ItemStack(Material.GOLD_INGOT),     "Gold Ingot",     35L,   -1);
        mgr.addItem("ores", new ItemStack(Material.QUARTZ),         "Quartz",         12L,   -1);
        mgr.addItem("ores", new ItemStack(Material.DIAMOND),        "Diamond",        120L,  -1);
        mgr.addItem("ores", new ItemStack(Material.EMERALD),        "Emerald",        300L,  -1);
        mgr.addItem("ores", new ItemStack(Material.NETHERITE_SCRAP),"Netherite Scrap",600L,  -1);
        mgr.addItem("ores", new ItemStack(Material.NETHERITE_INGOT),"Netherite Ingot",2400L, -1);
    }

    // ----------------------------------------------------------------
    // Category 3: Farming
    // ----------------------------------------------------------------

    private static void populateFarming(ShopManager mgr) {
        mgr.addCategory("farming", "<green><bold>Farming", Material.WHEAT);

        mgr.addItem("farming", new ItemStack(Material.WHEAT_SEEDS),   "Wheat Seeds",  1L,  -1);
        mgr.addItem("farming", new ItemStack(Material.WHEAT),         "Wheat",        3L,  -1);
        mgr.addItem("farming", new ItemStack(Material.CARROT),        "Carrot",       4L,  -1);
        mgr.addItem("farming", new ItemStack(Material.POTATO),        "Potato",       4L,  -1);
        mgr.addItem("farming", new ItemStack(Material.BEETROOT_SEEDS),"Beetroot Seeds",1L, -1);
        mgr.addItem("farming", new ItemStack(Material.BEETROOT),      "Beetroot",     3L,  -1);
        mgr.addItem("farming", new ItemStack(Material.MELON_SLICE),   "Melon Slice",  4L,  -1);
        mgr.addItem("farming", new ItemStack(Material.PUMPKIN),       "Pumpkin",      8L,  -1);
        mgr.addItem("farming", new ItemStack(Material.SUGAR_CANE),    "Sugar Cane",   3L,  -1);
        mgr.addItem("farming", new ItemStack(Material.CACTUS),        "Cactus",       4L,  -1);
        mgr.addItem("farming", new ItemStack(Material.COCOA_BEANS),   "Cocoa Beans",  5L,  -1);
        mgr.addItem("farming", new ItemStack(Material.NETHER_WART),   "Nether Wart",  8L,  -1);
        mgr.addItem("farming", new ItemStack(Material.BONE_MEAL),     "Bone Meal",    5L,  -1);
        mgr.addItem("farming", new ItemStack(Material.APPLE),         "Apple",        8L,  -1);
        mgr.addItem("farming", new ItemStack(Material.SWEET_BERRIES), "Sweet Berries",5L,  -1);
        mgr.addItem("farming", new ItemStack(Material.KELP),          "Kelp",         2L,  -1);
    }

    // ----------------------------------------------------------------
    // Category 4: Mob Drops
    // ----------------------------------------------------------------

    private static void populateMobDrops(ShopManager mgr) {
        mgr.addCategory("mob_drops", "<red><bold>Mob Drops", Material.BONE);

        mgr.addItem("mob_drops", new ItemStack(Material.ROTTEN_FLESH),       "Rotten Flesh",       2L,   -1);
        mgr.addItem("mob_drops", new ItemStack(Material.BONE),               "Bone",               5L,   -1);
        mgr.addItem("mob_drops", new ItemStack(Material.STRING),             "String",             6L,   -1);
        mgr.addItem("mob_drops", new ItemStack(Material.SPIDER_EYE),         "Spider Eye",         8L,   -1);
        mgr.addItem("mob_drops", new ItemStack(Material.GUNPOWDER),          "Gunpowder",          12L,  -1);
        mgr.addItem("mob_drops", new ItemStack(Material.SLIME_BALL),          "Slimeball",          15L,  -1);
        mgr.addItem("mob_drops", new ItemStack(Material.LEATHER),            "Leather",            8L,   -1);
        mgr.addItem("mob_drops", new ItemStack(Material.FEATHER),            "Feather",            5L,   -1);
        mgr.addItem("mob_drops", new ItemStack(Material.INK_SAC),            "Ink Sac",            6L,   -1);
        mgr.addItem("mob_drops", new ItemStack(Material.BLAZE_ROD),          "Blaze Rod",          25L,  -1);
        mgr.addItem("mob_drops", new ItemStack(Material.BLAZE_POWDER),       "Blaze Powder",       15L,  -1);
        mgr.addItem("mob_drops", new ItemStack(Material.GHAST_TEAR),         "Ghast Tear",         50L,  -1);
        mgr.addItem("mob_drops", new ItemStack(Material.MAGMA_CREAM),        "Magma Cream",        20L,  -1);
        mgr.addItem("mob_drops", new ItemStack(Material.ENDER_PEARL),        "Ender Pearl",        30L,  -1);
        mgr.addItem("mob_drops", new ItemStack(Material.PHANTOM_MEMBRANE),   "Phantom Membrane",   40L,  -1);
        mgr.addItem("mob_drops", new ItemStack(Material.PRISMARINE_SHARD),   "Prismarine Shard",   12L,  -1);
        mgr.addItem("mob_drops", new ItemStack(Material.PRISMARINE_CRYSTALS),"Prismarine Crystals",18L,  -1);
        mgr.addItem("mob_drops", new ItemStack(Material.SHULKER_SHELL),      "Shulker Shell",      200L, -1);
    }

    // ----------------------------------------------------------------
    // Category 5: Food
    // ----------------------------------------------------------------

    private static void populateFood(ShopManager mgr) {
        mgr.addCategory("food", "<gold><bold>Food", Material.COOKED_BEEF);

        mgr.addItem("food", new ItemStack(Material.BREAD),          "Bread",          5L,   -1);
        mgr.addItem("food", new ItemStack(Material.COOKED_BEEF),    "Cooked Beef",    8L,   -1);
        mgr.addItem("food", new ItemStack(Material.COOKED_CHICKEN), "Cooked Chicken", 6L,   -1);

        ItemStack porkchop = new ItemStack(Material.COOKED_PORKCHOP);
        setMeta(porkchop, "Cooked Pork Chop");
        mgr.addItem("food", porkchop, "Cooked Pork Chop", 7L, -1);

        mgr.addItem("food", new ItemStack(Material.COOKED_MUTTON),  "Cooked Mutton",  6L,   -1);
        mgr.addItem("food", new ItemStack(Material.COOKED_SALMON),  "Cooked Salmon",  7L,   -1);
        mgr.addItem("food", new ItemStack(Material.COOKED_COD),     "Cooked Cod",     5L,   -1);
        mgr.addItem("food", new ItemStack(Material.BAKED_POTATO),   "Baked Potato",   6L,   -1);
        mgr.addItem("food", new ItemStack(Material.PUMPKIN_PIE),    "Pumpkin Pie",    10L,  -1);
        mgr.addItem("food", new ItemStack(Material.GOLDEN_APPLE),   "Golden Apple",   150L, -1);
        mgr.addItem("food", new ItemStack(Material.COOKIE),         "Cookie",         3L,   -1);
        mgr.addItem("food", new ItemStack(Material.MUSHROOM_STEW),  "Mushroom Stew",  12L,  -1);
    }

    // ----------------------------------------------------------------
    // Category 6: Tools
    // ----------------------------------------------------------------

    private static void populateTools(ShopManager mgr) {
        mgr.addCategory("tools", "<white><bold>Tools", Material.IRON_PICKAXE);

        // Pickaxes
        mgr.addItem("tools", new ItemStack(Material.WOODEN_PICKAXE), "Wooden Pickaxe", 5L,   -1);
        mgr.addItem("tools", new ItemStack(Material.STONE_PICKAXE),  "Stone Pickaxe",  15L,  -1);
        mgr.addItem("tools", new ItemStack(Material.IRON_PICKAXE),   "Iron Pickaxe",   50L,  -1);
        mgr.addItem("tools", new ItemStack(Material.GOLDEN_PICKAXE), "Golden Pickaxe", 40L,  -1);
        mgr.addItem("tools", new ItemStack(Material.DIAMOND_PICKAXE),"Diamond Pickaxe",300L, -1);

        // Swords
        mgr.addItem("tools", new ItemStack(Material.WOODEN_SWORD),  "Wooden Sword",  4L,   -1);
        mgr.addItem("tools", new ItemStack(Material.STONE_SWORD),   "Stone Sword",   12L,  -1);
        mgr.addItem("tools", new ItemStack(Material.IRON_SWORD),    "Iron Sword",    40L,  -1);
        mgr.addItem("tools", new ItemStack(Material.GOLDEN_SWORD),  "Golden Sword",  30L,  -1);
        mgr.addItem("tools", new ItemStack(Material.DIAMOND_SWORD), "Diamond Sword", 250L, -1);

        // Ranged & utility
        mgr.addItem("tools", new ItemStack(Material.BOW),            "Bow",            35L,  -1);
        mgr.addItem("tools", new ItemStack(Material.ARROW),          "Arrow",          1L,   -1);
        mgr.addItem("tools", new ItemStack(Material.CROSSBOW),       "Crossbow",       60L,  -1);
        mgr.addItem("tools", new ItemStack(Material.FISHING_ROD),    "Fishing Rod",    20L,  -1);
        mgr.addItem("tools", new ItemStack(Material.SHEARS),         "Shears",         25L,  -1);
        mgr.addItem("tools", new ItemStack(Material.FLINT_AND_STEEL),"Flint and Steel",15L,  -1);

        // Axes & shovels
        mgr.addItem("tools", new ItemStack(Material.IRON_AXE),      "Iron Axe",      40L,  -1);
        mgr.addItem("tools", new ItemStack(Material.DIAMOND_AXE),   "Diamond Axe",   200L, -1);
        mgr.addItem("tools", new ItemStack(Material.IRON_SHOVEL),   "Iron Shovel",   30L,  -1);
        mgr.addItem("tools", new ItemStack(Material.DIAMOND_SHOVEL),"Diamond Shovel",150L, -1);

        // Hoes
        mgr.addItem("tools", new ItemStack(Material.IRON_HOE),    "Iron Hoe",    25L,  -1);
        mgr.addItem("tools", new ItemStack(Material.DIAMOND_HOE), "Diamond Hoe", 100L, -1);
    }

    // ----------------------------------------------------------------
    // Category 7: Armor
    // ----------------------------------------------------------------

    private static void populateArmor(ShopManager mgr) {
        mgr.addCategory("armor", "<gray><bold>Armor", Material.IRON_CHESTPLATE);

        // Leather
        mgr.addItem("armor", new ItemStack(Material.LEATHER_HELMET),     "Leather Helmet",     15L,  -1);
        mgr.addItem("armor", new ItemStack(Material.LEATHER_CHESTPLATE), "Leather Chestplate", 25L,  -1);
        mgr.addItem("armor", new ItemStack(Material.LEATHER_LEGGINGS),   "Leather Leggings",   20L,  -1);
        mgr.addItem("armor", new ItemStack(Material.LEATHER_BOOTS),      "Leather Boots",      12L,  -1);

        // Iron
        mgr.addItem("armor", new ItemStack(Material.IRON_HELMET),     "Iron Helmet",     80L,  -1);
        mgr.addItem("armor", new ItemStack(Material.IRON_CHESTPLATE), "Iron Chestplate", 140L, -1);
        mgr.addItem("armor", new ItemStack(Material.IRON_LEGGINGS),   "Iron Leggings",   120L, -1);
        mgr.addItem("armor", new ItemStack(Material.IRON_BOOTS),      "Iron Boots",      75L,  -1);

        // Golden
        mgr.addItem("armor", new ItemStack(Material.GOLDEN_HELMET),     "Golden Helmet",     55L,  -1);
        mgr.addItem("armor", new ItemStack(Material.GOLDEN_CHESTPLATE), "Golden Chestplate", 90L,  -1);
        mgr.addItem("armor", new ItemStack(Material.GOLDEN_LEGGINGS),   "Golden Leggings",   80L,  -1);
        mgr.addItem("armor", new ItemStack(Material.GOLDEN_BOOTS),      "Golden Boots",      50L,  -1);

        // Diamond
        mgr.addItem("armor", new ItemStack(Material.DIAMOND_HELMET),     "Diamond Helmet",     350L, -1);
        mgr.addItem("armor", new ItemStack(Material.DIAMOND_CHESTPLATE), "Diamond Chestplate", 600L, -1);
        mgr.addItem("armor", new ItemStack(Material.DIAMOND_LEGGINGS),   "Diamond Leggings",   500L, -1);
        mgr.addItem("armor", new ItemStack(Material.DIAMOND_BOOTS),      "Diamond Boots",      320L, -1);

        // Misc
        mgr.addItem("armor", new ItemStack(Material.SHIELD),        "Shield",        40L,  -1);
        mgr.addItem("armor", new ItemStack(Material.TURTLE_HELMET), "Turtle Helmet", 500L, -1);
    }

    // ----------------------------------------------------------------
    // Category 8: Redstone
    // ----------------------------------------------------------------

    private static void populateRedstone(ShopManager mgr) {
        mgr.addCategory("redstone", "<red><bold>Redstone", Material.REDSTONE);

        mgr.addItem("redstone", new ItemStack(Material.REDSTONE),              "Redstone",              5L,  -1);
        mgr.addItem("redstone", new ItemStack(Material.TORCH),                 "Torch",                 2L,  -1);
        mgr.addItem("redstone", new ItemStack(Material.REDSTONE_TORCH),        "Redstone Torch",        4L,  -1);
        mgr.addItem("redstone", new ItemStack(Material.LEVER),                 "Lever",                 3L,  -1);
        mgr.addItem("redstone", new ItemStack(Material.STONE_BUTTON),          "Stone Button",          3L,  -1);
        mgr.addItem("redstone", new ItemStack(Material.STONE_PRESSURE_PLATE),  "Stone Pressure Plate",  5L,  -1);
        mgr.addItem("redstone", new ItemStack(Material.OAK_PRESSURE_PLATE),    "Oak Pressure Plate",    4L,  -1);
        mgr.addItem("redstone", new ItemStack(Material.REPEATER),              "Repeater",              8L,  -1);
        mgr.addItem("redstone", new ItemStack(Material.COMPARATOR),            "Comparator",            12L, -1);
        mgr.addItem("redstone", new ItemStack(Material.PISTON),                "Piston",                15L, -1);
        mgr.addItem("redstone", new ItemStack(Material.STICKY_PISTON),         "Sticky Piston",         20L, -1);
        mgr.addItem("redstone", new ItemStack(Material.OBSERVER),              "Observer",              20L, -1);
        mgr.addItem("redstone", new ItemStack(Material.HOPPER),                "Hopper",                35L, -1);
        mgr.addItem("redstone", new ItemStack(Material.DROPPER),               "Dropper",               12L, -1);
        mgr.addItem("redstone", new ItemStack(Material.DISPENSER),             "Dispenser",             15L, -1);
        mgr.addItem("redstone", new ItemStack(Material.DAYLIGHT_DETECTOR),     "Daylight Detector",     18L, -1);
        mgr.addItem("redstone", new ItemStack(Material.TNT),                   "TNT",                   25L, -1);
        mgr.addItem("redstone", new ItemStack(Material.NOTE_BLOCK),            "Note Block",            12L, -1);
        mgr.addItem("redstone", new ItemStack(Material.TRIPWIRE_HOOK),         "Tripwire Hook",         6L,  -1);
        mgr.addItem("redstone", new ItemStack(Material.REDSTONE_LAMP),         "Redstone Lamp",         15L, -1);
        mgr.addItem("redstone", new ItemStack(Material.TARGET),                "Target",                10L, -1);
    }

    // ----------------------------------------------------------------
    // Category 9: Utilities
    // ----------------------------------------------------------------

    private static void populateUtilities(ShopManager mgr) {
        mgr.addCategory("utilities", "<light_purple><bold>Utilities", Material.CHEST);

        // Crafting & smelting
        mgr.addItem("utilities", new ItemStack(Material.CRAFTING_TABLE), "Crafting Table", 10L,  -1);
        mgr.addItem("utilities", new ItemStack(Material.FURNACE),        "Furnace",        8L,   -1);
        mgr.addItem("utilities", new ItemStack(Material.BLAST_FURNACE),  "Blast Furnace",  20L,  -1);
        mgr.addItem("utilities", new ItemStack(Material.SMOKER),         "Smoker",         15L,  -1);

        // Storage
        mgr.addItem("utilities", new ItemStack(Material.CHEST),    "Chest",    12L, -1);
        mgr.addItem("utilities", new ItemStack(Material.BARREL),   "Barrel",   15L, -1);
        mgr.addItem("utilities", new ItemStack(Material.BOOKSHELF),"Bookshelf",20L, -1);
        mgr.addItem("utilities", new ItemStack(Material.LADDER),   "Ladder",   3L,  -1);

        // Glass & buckets
        mgr.addItem("utilities", new ItemStack(Material.GLASS),        "Glass",        5L,  -1);
        mgr.addItem("utilities", new ItemStack(Material.GLASS_PANE),   "Glass Pane",   3L,  -1);
        mgr.addItem("utilities", new ItemStack(Material.BUCKET),       "Bucket",       12L, -1);
        mgr.addItem("utilities", new ItemStack(Material.WATER_BUCKET), "Water Bucket", 15L, -1);
        mgr.addItem("utilities", new ItemStack(Material.LAVA_BUCKET),  "Lava Bucket",  20L, -1);

        // Special items
        mgr.addItem("utilities", new ItemStack(Material.NAME_TAG), "Name Tag", 50L, -1);
        mgr.addItem("utilities", new ItemStack(Material.SADDLE),   "Saddle",   80L, -1);
        mgr.addItem("utilities", new ItemStack(Material.LEAD),     "Lead",     25L, -1);
        mgr.addItem("utilities", new ItemStack(Material.COMPASS),  "Compass",  15L, -1);
        mgr.addItem("utilities", new ItemStack(Material.CLOCK),    "Clock",    20L, -1);
        mgr.addItem("utilities", new ItemStack(Material.MAP),      "Map",      10L, -1);

        // Navigation & lighting
        mgr.addItem("utilities", new ItemStack(Material.SPYGLASS), "Spyglass", 75L, -1);
        mgr.addItem("utilities", new ItemStack(Material.LANTERN),  "Lantern",  8L,  -1);

        // Transport
        mgr.addItem("utilities", new ItemStack(Material.MINECART),    "Minecart",    25L, -1);
        mgr.addItem("utilities", new ItemStack(Material.RAIL),        "Rail",        8L,  -1);
        mgr.addItem("utilities", new ItemStack(Material.POWERED_RAIL),"Powered Rail",30L, -1);
        mgr.addItem("utilities", new ItemStack(Material.ANVIL),       "Anvil",       40L, -1);

        // Endgame
        mgr.addItem("utilities", new ItemStack(Material.ENCHANTING_TABLE),"Enchanting Table",200L, -1);
        mgr.addItem("utilities", new ItemStack(Material.ENDER_CHEST),     "Ender Chest",     300L, -1);
    }

    // ----------------------------------------------------------------
    // Category 10: Misc
    // ----------------------------------------------------------------

    private static void populateMisc(ShopManager mgr) {
        mgr.addCategory("misc", "<dark_gray><bold>Misc", Material.PAPER);

        // Books & basic
        mgr.addItem("misc", new ItemStack(Material.PAPER),    "Paper",    2L, -1);
        mgr.addItem("misc", new ItemStack(Material.BOOK),     "Book",     5L, -1);

        // Stone variants
        mgr.addItem("misc", new ItemStack(Material.NETHER_BRICK), "Nether Brick", 8L, -1);
        mgr.addItem("misc", new ItemStack(Material.DIORITE),      "Diorite",      3L, -1);
        mgr.addItem("misc", new ItemStack(Material.ANDESITE),     "Andesite",     3L, -1);
        mgr.addItem("misc", new ItemStack(Material.GRANITE),      "Granite",      3L, -1);

        // Nether blocks
        mgr.addItem("misc", new ItemStack(Material.BLACKSTONE),    "Blackstone",    4L,  -1);
        mgr.addItem("misc", new ItemStack(Material.NETHERRACK),    "Netherrack",    2L,  -1);
        mgr.addItem("misc", new ItemStack(Material.SOUL_SAND),     "Soul Sand",     6L,  -1);
        mgr.addItem("misc", new ItemStack(Material.GLOWSTONE_DUST),"Glowstone Dust",8L,  -1);
        mgr.addItem("misc", new ItemStack(Material.QUARTZ_BLOCK),  "Quartz Block",  15L, -1);
        mgr.addItem("misc", new ItemStack(Material.MAGMA_BLOCK),   "Magma Block",   15L, -1);
        mgr.addItem("misc", new ItemStack(Material.BONE_BLOCK),    "Bone Block",    12L, -1);

        // Wool
        mgr.addItem("misc", new ItemStack(Material.WHITE_WOOL), "White Wool", 5L, -1);
        mgr.addItem("misc", new ItemStack(Material.BLACK_WOOL), "Black Wool", 8L, -1);

        // Decorative & special blocks
        mgr.addItem("misc", new ItemStack(Material.TERRACOTTA), "Terracotta", 4L,  -1);
        mgr.addItem("misc", new ItemStack(Material.ICE),        "Ice",        8L,  -1);
        mgr.addItem("misc", new ItemStack(Material.PACKED_ICE), "Packed Ice", 20L, -1);
        mgr.addItem("misc", new ItemStack(Material.BLUE_ICE),   "Blue Ice",   50L, -1);
        mgr.addItem("misc", new ItemStack(Material.SPONGE),     "Sponge",     50L, -1);
        mgr.addItem("misc", new ItemStack(Material.SEA_LANTERN),"Sea Lantern",25L, -1);
        mgr.addItem("misc", new ItemStack(Material.SNOW_BLOCK), "Snow Block", 5L,  -1);
        mgr.addItem("misc", new ItemStack(Material.CLAY_BALL),  "Clay Ball",  5L,  -1);
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
