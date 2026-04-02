package com.prison.shop;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * ShopDefaults — seeds the shop with default categories and items on first startup.
 *
 * Only runs when the shop is empty (fresh install or cleared config).
 * To re-run: clear categories in config.yml and restart, or use /shopadmin reset.
 *
 * Prices and sellability are derived from the prison_shop_economy_mines_master_spec.
 * Items marked sellable=true appear in mine compositions and can be right-click sold.
 * Items marked sellable=false are buy-only (tools, armor, food, utility, decor).
 *
 * Category overview:
 *   1. blocks      — earth, stone, and wood (mine blocks sellable, decor/wood buy-only)
 *   2. ores        — ore drops, raw resources, and processed ingots (all sellable)
 *   3. farming     — crops, seeds, and natural items (some optionally sellable)
 *   4. mob_drops   — hostile and passive mob loot (some optionally sellable)
 *   5. food        — cooked food and consumables (buy-only)
 *   6. tools       — pickaxes, swords, axes, shovels (buy-only, unenchanted)
 *   7. armor       — all tiers of armor and shields (buy-only, unenchanted)
 *   8. redstone    — redstone components and mechanisms (buy-only)
 *   9. utilities   — chests, transport, buckets, and misc functional items (buy-only)
 *  10. misc        — nether, ocean, and end blocks; prismarine mine blocks (sellable)
 */
public class ShopDefaults {

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
    // Helpers
    // ----------------------------------------------------------------

    private static void add(ShopManager mgr, String cat, Material mat, String name, long price, boolean sellable) {
        mgr.addItem(cat, new ItemStack(mat), name, price, -1, sellable);
    }

    // ----------------------------------------------------------------
    // Category 1: Blocks
    // ----------------------------------------------------------------

    private static void populateBlocks(ShopManager mgr) {
        mgr.addCategory("blocks", "<yellow><bold>Blocks", Material.OAK_LOG);

        // ── Mine blocks (sellable) ──────────────────────────────────
        add(mgr, "blocks", Material.STONE,       "Stone",       3L,  true);
        add(mgr, "blocks", Material.COBBLESTONE, "Cobblestone", 2L,  true);
        add(mgr, "blocks", Material.TUFF,        "Tuff",        8L,  true);
        add(mgr, "blocks", Material.ANDESITE,    "Andesite",    6L,  true);

        // ── Decorative / earth (buy-only) ───────────────────────────
        add(mgr, "blocks", Material.DIRT,             "Dirt",             1L,  false);
        add(mgr, "blocks", Material.COARSE_DIRT,      "Coarse Dirt",      2L,  false);
        add(mgr, "blocks", Material.MUD,              "Mud",              4L,  false);
        add(mgr, "blocks", Material.SAND,             "Sand",             4L,  false);
        add(mgr, "blocks", Material.GRAVEL,           "Gravel",           5L,  false);
        add(mgr, "blocks", Material.CLAY,             "Clay",             7L,  false);
        add(mgr, "blocks", Material.RED_SAND,         "Red Sand",         6L,  false);
        add(mgr, "blocks", Material.SMOOTH_STONE,     "Smooth Stone",     5L,  true);
        add(mgr, "blocks", Material.GRANITE,          "Granite",          6L,  false);
        add(mgr, "blocks", Material.POLISHED_GRANITE, "Polished Granite", 8L,  false);
        add(mgr, "blocks", Material.DIORITE,          "Diorite",          6L,  false);
        add(mgr, "blocks", Material.POLISHED_DIORITE, "Polished Diorite", 8L,  false);
        add(mgr, "blocks", Material.POLISHED_ANDESITE,"Polished Andesite",8L,  false);
        add(mgr, "blocks", Material.CALCITE,          "Calcite",          10L, false);
        add(mgr, "blocks", Material.DEEPSLATE,        "Deepslate",        5L,  true);
        add(mgr, "blocks", Material.COBBLED_DEEPSLATE,"Cobbled Deepslate",4L,  false);

        // ── Wood logs (buy-only) ────────────────────────────────────
        add(mgr, "blocks", Material.OAK_LOG,      "Oak Log",      8L,  false);
        add(mgr, "blocks", Material.BIRCH_LOG,    "Birch Log",    9L,  false);
        add(mgr, "blocks", Material.SPRUCE_LOG,   "Spruce Log",   10L, false);
        add(mgr, "blocks", Material.JUNGLE_LOG,   "Jungle Log",   11L, false);
        add(mgr, "blocks", Material.ACACIA_LOG,   "Acacia Log",   11L, false);
        add(mgr, "blocks", Material.DARK_OAK_LOG, "Dark Oak Log", 12L, false);
        add(mgr, "blocks", Material.MANGROVE_LOG, "Mangrove Log", 13L, false);
        add(mgr, "blocks", Material.CHERRY_LOG,   "Cherry Log",   14L, false);
        add(mgr, "blocks", Material.BAMBOO_BLOCK, "Bamboo Block", 15L, false);
        add(mgr, "blocks", Material.CRIMSON_STEM, "Crimson Stem", 16L, false);
        add(mgr, "blocks", Material.WARPED_STEM,  "Warped Stem",  16L, false);

        // ── Planks (buy-only, base log + 20%) ──────────────────────
        add(mgr, "blocks", Material.OAK_PLANKS,      "Oak Planks",      10L, false);
        add(mgr, "blocks", Material.BIRCH_PLANKS,    "Birch Planks",    11L, false);
        add(mgr, "blocks", Material.SPRUCE_PLANKS,   "Spruce Planks",   12L, false);
        add(mgr, "blocks", Material.JUNGLE_PLANKS,   "Jungle Planks",   13L, false);
        add(mgr, "blocks", Material.ACACIA_PLANKS,   "Acacia Planks",   13L, false);
        add(mgr, "blocks", Material.DARK_OAK_PLANKS, "Dark Oak Planks", 14L, false);
        add(mgr, "blocks", Material.MANGROVE_PLANKS, "Mangrove Planks", 16L, false);
        add(mgr, "blocks", Material.CHERRY_PLANKS,   "Cherry Planks",   17L, false);
        add(mgr, "blocks", Material.BAMBOO_PLANKS,   "Bamboo Planks",   18L, false);
        add(mgr, "blocks", Material.CRIMSON_PLANKS,  "Crimson Planks",  19L, false);
        add(mgr, "blocks", Material.WARPED_PLANKS,   "Warped Planks",   19L, false);
    }

    // ----------------------------------------------------------------
    // Category 2: Ores
    // ----------------------------------------------------------------

    private static void populateOres(ShopManager mgr) {
        mgr.addCategory("ores", "<aqua><bold>Ores", Material.DIAMOND);

        // ── Ore drops and raw resources (all sellable) ──────────────
        add(mgr, "ores", Material.COAL,           "Coal",           8L,    true);
        add(mgr, "ores", Material.RAW_IRON,       "Raw Iron",       15L,   true);
        add(mgr, "ores", Material.IRON_INGOT,     "Iron Ingot",     18L,   true);
        add(mgr, "ores", Material.RAW_COPPER,     "Raw Copper",     11L,   true);
        add(mgr, "ores", Material.COPPER_INGOT,   "Copper Ingot",   12L,   true);
        add(mgr, "ores", Material.LAPIS_LAZULI,   "Lapis Lazuli",   8L,    true);
        add(mgr, "ores", Material.REDSTONE,       "Redstone",       9L,    true);
        add(mgr, "ores", Material.RAW_GOLD,       "Raw Gold",       35L,   true);
        add(mgr, "ores", Material.GOLD_INGOT,     "Gold Ingot",     38L,   true);
        add(mgr, "ores", Material.QUARTZ,         "Quartz",         26L,   true);
        add(mgr, "ores", Material.DIAMOND,        "Diamond",        120L,  true);
        add(mgr, "ores", Material.EMERALD,        "Emerald",        300L,  true);
        add(mgr, "ores", Material.NETHERITE_SCRAP,"Netherite Scrap",600L,  true);
        add(mgr, "ores", Material.NETHERITE_INGOT,"Netherite Ingot",3500L, true);

        // ── Ore blocks (silk-touch sellable mine blocks) ─────────────
        add(mgr, "ores", Material.COAL_ORE,                "Coal Ore",               8L,   true);
        add(mgr, "ores", Material.DEEPSLATE_COAL_ORE,      "Deepslate Coal Ore",     9L,   true);
        add(mgr, "ores", Material.IRON_ORE,                "Iron Ore",               15L,  true);
        add(mgr, "ores", Material.DEEPSLATE_IRON_ORE,      "Deepslate Iron Ore",     16L,  true);
        add(mgr, "ores", Material.COPPER_ORE,              "Copper Ore",             11L,  true);
        add(mgr, "ores", Material.DEEPSLATE_COPPER_ORE,    "Deepslate Copper Ore",   12L,  true);
        add(mgr, "ores", Material.GOLD_ORE,                "Gold Ore",               35L,  true);
        add(mgr, "ores", Material.DEEPSLATE_GOLD_ORE,      "Deepslate Gold Ore",     38L,  true);
        add(mgr, "ores", Material.NETHER_GOLD_ORE,         "Nether Gold Ore",        24L,  true);
        add(mgr, "ores", Material.LAPIS_ORE,               "Lapis Ore",              50L,  true);
        add(mgr, "ores", Material.DEEPSLATE_LAPIS_ORE,     "Deepslate Lapis Ore",    54L,  true);
        add(mgr, "ores", Material.REDSTONE_ORE,            "Redstone Ore",           45L,  true);
        add(mgr, "ores", Material.DEEPSLATE_REDSTONE_ORE,  "Deepslate Redstone Ore", 48L,  true);
        add(mgr, "ores", Material.NETHER_QUARTZ_ORE,       "Nether Quartz Ore",      26L,  true);
        add(mgr, "ores", Material.DIAMOND_ORE,             "Diamond Ore",            120L, true);
        add(mgr, "ores", Material.DEEPSLATE_DIAMOND_ORE,   "Deepslate Diamond Ore",  132L, true);
        add(mgr, "ores", Material.EMERALD_ORE,             "Emerald Ore",            300L, true);
        add(mgr, "ores", Material.ANCIENT_DEBRIS,          "Ancient Debris",         1200L, true);
    }

    // ----------------------------------------------------------------
    // Category 3: Farming
    // ----------------------------------------------------------------

    private static void populateFarming(ShopManager mgr) {
        mgr.addCategory("farming", "<green><bold>Farming", Material.WHEAT);

        // Optional sell items (crop drops players might sell)
        add(mgr, "farming", Material.WHEAT,          "Wheat",          6L,  true);
        add(mgr, "farming", Material.CARROT,         "Carrot",         7L,  true);
        add(mgr, "farming", Material.POTATO,         "Potato",         7L,  true);
        add(mgr, "farming", Material.BEETROOT,       "Beetroot",       7L,  true);
        add(mgr, "farming", Material.SUGAR_CANE,     "Sugar Cane",     10L, true);
        add(mgr, "farming", Material.CACTUS,         "Cactus",         12L, true);

        // Buy-only
        add(mgr, "farming", Material.WHEAT_SEEDS,    "Wheat Seeds",    2L,  false);
        add(mgr, "farming", Material.BEETROOT_SEEDS, "Beetroot Seeds", 2L,  false);
        add(mgr, "farming", Material.MELON_SLICE,    "Melon Slice",    4L,  false);
        add(mgr, "farming", Material.PUMPKIN,        "Pumpkin",        10L, false);
        add(mgr, "farming", Material.BAMBOO,         "Bamboo",         8L,  false);
        add(mgr, "farming", Material.KELP,           "Kelp",           5L,  false);
        add(mgr, "farming", Material.NETHER_WART,    "Nether Wart",    14L, false);
        add(mgr, "farming", Material.COCOA_BEANS,    "Cocoa Beans",    5L,  false);
        add(mgr, "farming", Material.BONE_MEAL,      "Bone Meal",      5L,  false);
        add(mgr, "farming", Material.APPLE,          "Apple",          12L, false);
        add(mgr, "farming", Material.SWEET_BERRIES,  "Sweet Berries",  5L,  false);
    }

    // ----------------------------------------------------------------
    // Category 4: Mob Drops
    // ----------------------------------------------------------------

    private static void populateMobDrops(ShopManager mgr) {
        mgr.addCategory("mob_drops", "<red><bold>Mob Drops", Material.BONE);

        // Optional sell items
        add(mgr, "mob_drops", Material.ROTTEN_FLESH, "Rotten Flesh", 5L,  true);
        add(mgr, "mob_drops", Material.BONE,         "Bone",         6L,  true);
        add(mgr, "mob_drops", Material.STRING,       "String",       6L,  true);
        add(mgr, "mob_drops", Material.GUNPOWDER,    "Gunpowder",    12L, true);
        add(mgr, "mob_drops", Material.SLIME_BALL,   "Slimeball",    18L, true);
        add(mgr, "mob_drops", Material.ENDER_PEARL,  "Ender Pearl",  25L, true);
        add(mgr, "mob_drops", Material.BLAZE_ROD,    "Blaze Rod",    32L, true);

        // Buy-only
        add(mgr, "mob_drops", Material.SPIDER_EYE,   "Spider Eye",   7L,  false);
        add(mgr, "mob_drops", Material.MAGMA_CREAM,  "Magma Cream",  24L, false);
        add(mgr, "mob_drops", Material.GHAST_TEAR,   "Ghast Tear",   80L, false);
        add(mgr, "mob_drops", Material.PHANTOM_MEMBRANE,"Phantom Membrane",40L, false);
        add(mgr, "mob_drops", Material.PRISMARINE_SHARD,"Prismarine Shard",15L, false);
        add(mgr, "mob_drops", Material.PRISMARINE_CRYSTALS,"Prismarine Crystals",20L, false);
        add(mgr, "mob_drops", Material.SHULKER_SHELL,"Shulker Shell",200L, false);
    }

    // ----------------------------------------------------------------
    // Category 5: Food
    // ----------------------------------------------------------------

    private static void populateFood(ShopManager mgr) {
        mgr.addCategory("food", "<gold><bold>Food", Material.COOKED_BEEF);

        // All buy-only
        add(mgr, "food", Material.BREAD,              "Bread",              18L, false);
        add(mgr, "food", Material.COOKED_BEEF,        "Cooked Beef",        28L, false);
        add(mgr, "food", Material.COOKED_PORKCHOP,    "Cooked Porkchop",    26L, false);
        add(mgr, "food", Material.COOKED_CHICKEN,     "Cooked Chicken",     20L, false);
        add(mgr, "food", Material.COOKED_MUTTON,      "Cooked Mutton",      22L, false);
        add(mgr, "food", Material.COOKED_RABBIT,      "Cooked Rabbit",      24L, false);
        add(mgr, "food", Material.COOKED_SALMON,      "Cooked Salmon",      22L, false);
        add(mgr, "food", Material.COOKED_COD,         "Cooked Cod",         18L, false);
        add(mgr, "food", Material.GOLDEN_CARROT,      "Golden Carrot",      45L, false);
        add(mgr, "food", Material.GOLDEN_APPLE,       "Golden Apple",       150L, false);
        add(mgr, "food", Material.MUSHROOM_STEW,      "Mushroom Stew",      25L, false);
        add(mgr, "food", Material.RABBIT_STEW,        "Rabbit Stew",        30L, false);
    }

    // ----------------------------------------------------------------
    // Category 6: Tools
    // ----------------------------------------------------------------

    private static void populateTools(ShopManager mgr) {
        mgr.addCategory("tools", "<gray><bold>Tools", Material.IRON_PICKAXE);

        // All buy-only, unenchanted
        // ── Wooden ──────────────────────────────────────────────────
        add(mgr, "tools", Material.WOODEN_PICKAXE, "Wooden Pickaxe", 10L, false);
        add(mgr, "tools", Material.WOODEN_AXE,     "Wooden Axe",     8L,  false);
        add(mgr, "tools", Material.WOODEN_SHOVEL,  "Wooden Shovel",  6L,  false);
        add(mgr, "tools", Material.WOODEN_SWORD,   "Wooden Sword",   12L, false);
        add(mgr, "tools", Material.WOODEN_HOE,     "Wooden Hoe",     8L,  false);

        // ── Stone ───────────────────────────────────────────────────
        add(mgr, "tools", Material.STONE_PICKAXE,  "Stone Pickaxe",  20L, false);
        add(mgr, "tools", Material.STONE_AXE,      "Stone Axe",      16L, false);
        add(mgr, "tools", Material.STONE_SHOVEL,   "Stone Shovel",   12L, false);
        add(mgr, "tools", Material.STONE_SWORD,    "Stone Sword",    24L, false);
        add(mgr, "tools", Material.STONE_HOE,      "Stone Hoe",      14L, false);

        // ── Iron ────────────────────────────────────────────────────
        add(mgr, "tools", Material.IRON_PICKAXE,   "Iron Pickaxe",   120L, false);
        add(mgr, "tools", Material.IRON_AXE,       "Iron Axe",       90L,  false);
        add(mgr, "tools", Material.IRON_SHOVEL,    "Iron Shovel",    60L,  false);
        add(mgr, "tools", Material.IRON_SWORD,     "Iron Sword",     100L, false);
        add(mgr, "tools", Material.IRON_HOE,       "Iron Hoe",       50L,  false);

        // ── Diamond ─────────────────────────────────────────────────
        add(mgr, "tools", Material.DIAMOND_PICKAXE,"Diamond Pickaxe",850L,  false);
        add(mgr, "tools", Material.DIAMOND_AXE,    "Diamond Axe",    650L,  false);
        add(mgr, "tools", Material.DIAMOND_SHOVEL, "Diamond Shovel", 300L,  false);
        add(mgr, "tools", Material.DIAMOND_SWORD,  "Diamond Sword",  700L,  false);
        add(mgr, "tools", Material.DIAMOND_HOE,    "Diamond Hoe",    250L,  false);

        // ── Netherite ───────────────────────────────────────────────
        add(mgr, "tools", Material.NETHERITE_PICKAXE,"Netherite Pickaxe",2200L, false);
        add(mgr, "tools", Material.NETHERITE_AXE,    "Netherite Axe",    1800L, false);
        add(mgr, "tools", Material.NETHERITE_SHOVEL, "Netherite Shovel", 850L,  false);
        add(mgr, "tools", Material.NETHERITE_SWORD,  "Netherite Sword",  1900L, false);
        add(mgr, "tools", Material.NETHERITE_HOE,    "Netherite Hoe",    700L,  false);

        // ── Other ───────────────────────────────────────────────────
        add(mgr, "tools", Material.BOW,             "Bow",             80L,  false);
        add(mgr, "tools", Material.CROSSBOW,        "Crossbow",        120L, false);
        add(mgr, "tools", Material.TRIDENT,         "Trident",         800L, false);
        add(mgr, "tools", Material.FISHING_ROD,     "Fishing Rod",     50L,  false);
    }

    // ----------------------------------------------------------------
    // Category 7: Armor
    // ----------------------------------------------------------------

    private static void populateArmor(ShopManager mgr) {
        mgr.addCategory("armor", "<dark_gray><bold>Armor", Material.IRON_CHESTPLATE);

        // All buy-only, unenchanted
        // ── Leather ─────────────────────────────────────────────────
        add(mgr, "armor", Material.LEATHER_HELMET,     "Leather Helmet",     20L, false);
        add(mgr, "armor", Material.LEATHER_CHESTPLATE, "Leather Chestplate", 40L, false);
        add(mgr, "armor", Material.LEATHER_LEGGINGS,   "Leather Leggings",   35L, false);
        add(mgr, "armor", Material.LEATHER_BOOTS,      "Leather Boots",      25L, false);

        // ── Chainmail ───────────────────────────────────────────────
        add(mgr, "armor", Material.CHAINMAIL_HELMET,     "Chainmail Helmet",     70L,  false);
        add(mgr, "armor", Material.CHAINMAIL_CHESTPLATE, "Chainmail Chestplate", 140L, false);
        add(mgr, "armor", Material.CHAINMAIL_LEGGINGS,   "Chainmail Leggings",   120L, false);
        add(mgr, "armor", Material.CHAINMAIL_BOOTS,      "Chainmail Boots",      90L,  false);

        // ── Iron ────────────────────────────────────────────────────
        add(mgr, "armor", Material.IRON_HELMET,     "Iron Helmet",     140L, false);
        add(mgr, "armor", Material.IRON_CHESTPLATE, "Iron Chestplate", 280L, false);
        add(mgr, "armor", Material.IRON_LEGGINGS,   "Iron Leggings",   240L, false);
        add(mgr, "armor", Material.IRON_BOOTS,      "Iron Boots",      190L, false);

        // ── Diamond ─────────────────────────────────────────────────
        add(mgr, "armor", Material.DIAMOND_HELMET,     "Diamond Helmet",     800L,  false);
        add(mgr, "armor", Material.DIAMOND_CHESTPLATE, "Diamond Chestplate", 1600L, false);
        add(mgr, "armor", Material.DIAMOND_LEGGINGS,   "Diamond Leggings",   1400L, false);
        add(mgr, "armor", Material.DIAMOND_BOOTS,      "Diamond Boots",      1000L, false);

        // ── Netherite ───────────────────────────────────────────────
        add(mgr, "armor", Material.NETHERITE_HELMET,     "Netherite Helmet",     2000L, false);
        add(mgr, "armor", Material.NETHERITE_CHESTPLATE, "Netherite Chestplate", 4000L, false);
        add(mgr, "armor", Material.NETHERITE_LEGGINGS,   "Netherite Leggings",   3500L, false);
        add(mgr, "armor", Material.NETHERITE_BOOTS,      "Netherite Boots",      2500L, false);

        // ── Shield ──────────────────────────────────────────────────
        add(mgr, "armor", Material.SHIELD, "Shield", 180L, false);
    }

    // ----------------------------------------------------------------
    // Category 8: Redstone
    // ----------------------------------------------------------------

    private static void populateRedstone(ShopManager mgr) {
        mgr.addCategory("redstone", "<dark_red><bold>Redstone", Material.REDSTONE);

        // All buy-only
        add(mgr, "redstone", Material.REDSTONE,          "Redstone Dust",    10L, false);
        add(mgr, "redstone", Material.REPEATER,          "Repeater",         35L, false);
        add(mgr, "redstone", Material.COMPARATOR,        "Comparator",       45L, false);
        add(mgr, "redstone", Material.OBSERVER,          "Observer",         60L, false);
        add(mgr, "redstone", Material.PISTON,            "Piston",           40L, false);
        add(mgr, "redstone", Material.STICKY_PISTON,     "Sticky Piston",    55L, false);
        add(mgr, "redstone", Material.HOPPER,            "Hopper",           75L, false);
        add(mgr, "redstone", Material.DISPENSER,         "Dispenser",        45L, false);
        add(mgr, "redstone", Material.DROPPER,           "Dropper",          40L, false);
        add(mgr, "redstone", Material.REDSTONE_LAMP,     "Redstone Lamp",    35L, false);
        add(mgr, "redstone", Material.LEVER,             "Lever",            4L,  false);
        add(mgr, "redstone", Material.STONE_BUTTON,      "Stone Button",     5L,  false);
        add(mgr, "redstone", Material.OAK_BUTTON,        "Oak Button",       4L,  false);
        add(mgr, "redstone", Material.STONE_PRESSURE_PLATE,"Stone Pressure Plate",8L, false);
        add(mgr, "redstone", Material.OAK_PRESSURE_PLATE,"Oak Pressure Plate",6L,  false);
        add(mgr, "redstone", Material.TRIPWIRE_HOOK,     "Tripwire Hook",    10L, false);
        add(mgr, "redstone", Material.TNT,               "TNT",              80L, false);
        add(mgr, "redstone", Material.RAIL,              "Rail",             10L, false);
        add(mgr, "redstone", Material.POWERED_RAIL,      "Powered Rail",     24L, false);
        add(mgr, "redstone", Material.DETECTOR_RAIL,     "Detector Rail",    18L, false);
        add(mgr, "redstone", Material.ACTIVATOR_RAIL,    "Activator Rail",   20L, false);
        add(mgr, "redstone", Material.MINECART,          "Minecart",         60L, false);
    }

    // ----------------------------------------------------------------
    // Category 9: Utilities
    // ----------------------------------------------------------------

    private static void populateUtilities(ShopManager mgr) {
        mgr.addCategory("utilities", "<white><bold>Utilities", Material.CHEST);

        // All buy-only
        add(mgr, "utilities", Material.CHEST,            "Chest",            30L,  false);
        add(mgr, "utilities", Material.TRAPPED_CHEST,    "Trapped Chest",    35L,  false);
        add(mgr, "utilities", Material.BARREL,           "Barrel",           35L,  false);
        add(mgr, "utilities", Material.SHULKER_BOX,      "Shulker Box",      600L, false);
        add(mgr, "utilities", Material.FURNACE,          "Furnace",          25L,  false);
        add(mgr, "utilities", Material.BLAST_FURNACE,    "Blast Furnace",    50L,  false);
        add(mgr, "utilities", Material.SMOKER,           "Smoker",           40L,  false);
        add(mgr, "utilities", Material.CRAFTING_TABLE,   "Crafting Table",   15L,  false);
        add(mgr, "utilities", Material.ANVIL,            "Anvil",            300L, false);
        add(mgr, "utilities", Material.ENCHANTING_TABLE, "Enchanting Table", 500L, false);
        add(mgr, "utilities", Material.BOOKSHELF,        "Bookshelf",        40L,  false);
        add(mgr, "utilities", Material.BOOK,             "Book",             12L,  false);
        add(mgr, "utilities", Material.PAPER,            "Paper",            4L,   false);
        add(mgr, "utilities", Material.TORCH,            "Torch",            2L,   false);
        add(mgr, "utilities", Material.LANTERN,          "Lantern",          15L,  false);
        add(mgr, "utilities", Material.WATER_BUCKET,     "Water Bucket",     80L,  false);
        add(mgr, "utilities", Material.LAVA_BUCKET,      "Lava Bucket",      120L, false);
        add(mgr, "utilities", Material.MILK_BUCKET,      "Milk Bucket",      90L,  false);
        add(mgr, "utilities", Material.BUCKET,           "Bucket",           30L,  false);
        add(mgr, "utilities", Material.GLASS_BOTTLE,     "Glass Bottle",     5L,   false);
        add(mgr, "utilities", Material.SADDLE,           "Saddle",           250L, false);
        add(mgr, "utilities", Material.LEAD,             "Lead",             60L,  false);
        add(mgr, "utilities", Material.SHEARS,           "Shears",           80L,  false);
        add(mgr, "utilities", Material.FLINT_AND_STEEL,  "Flint and Steel",  65L,  false);
        add(mgr, "utilities", Material.COMPASS,          "Compass",          150L, false);
        add(mgr, "utilities", Material.CLOCK,            "Clock",            180L, false);
        add(mgr, "utilities", Material.NAME_TAG,         "Name Tag",         300L, false);
        add(mgr, "utilities", Material.ARROW,            "Arrow",            3L,   false);
        add(mgr, "utilities", Material.GLASS,            "Glass",            8L,   false);
        add(mgr, "utilities", Material.SAND,             "Sand",             4L,   false);
    }

    // ----------------------------------------------------------------
    // Category 10: Misc (Nether, Ocean, and decorative mine blocks)
    // ----------------------------------------------------------------

    private static void populateMisc(ShopManager mgr) {
        mgr.addCategory("misc", "<dark_purple><bold>Special Blocks", Material.DARK_PRISMARINE);

        // ── Nether mine blocks (sellable) ───────────────────────────
        add(mgr, "misc", Material.NETHERRACK,          "Netherrack",           15L, true);
        add(mgr, "misc", Material.BASALT,              "Basalt",               18L, true);
        add(mgr, "misc", Material.SMOOTH_BASALT,       "Smooth Basalt",        22L, true);
        add(mgr, "misc", Material.BLACKSTONE,          "Blackstone",           20L, true);
        add(mgr, "misc", Material.POLISHED_BLACKSTONE, "Polished Blackstone",  24L, true);
        add(mgr, "misc", Material.GILDED_BLACKSTONE,   "Gilded Blackstone",    42L, true);
        add(mgr, "misc", Material.RED_NETHER_BRICKS,   "Red Nether Bricks",    45L, true);
        add(mgr, "misc", Material.GOLD_BLOCK,          "Gold Block",           315L, true);
        add(mgr, "misc", Material.REDSTONE_BLOCK,      "Redstone Block",       405L, true);
        add(mgr, "misc", Material.QUARTZ_BLOCK,        "Quartz Block",         108L, true);
        add(mgr, "misc", Material.RAW_IRON_BLOCK,      "Raw Iron Block",       140L, true);

        // ── Ocean / Prismarine mine blocks (sellable) ───────────────
        add(mgr, "misc", Material.PRISMARINE,          "Prismarine",       70L,  true);
        add(mgr, "misc", Material.PRISMARINE_BRICKS,   "Prismarine Bricks",95L,  true);
        add(mgr, "misc", Material.DARK_PRISMARINE,     "Dark Prismarine",  130L, true);
        add(mgr, "misc", Material.SEA_LANTERN,         "Sea Lantern",      150L, true);
        add(mgr, "misc", Material.DIAMOND_BLOCK,       "Diamond Block",    900L, true);
        add(mgr, "misc", Material.EMERALD_BLOCK,       "Emerald Block",    2500L, true);
        add(mgr, "misc", Material.NETHERITE_BLOCK,     "Netherite Block",  6000L, true);

        // ── Decorative / buy-only ───────────────────────────────────
        add(mgr, "misc", Material.GLOWSTONE,       "Glowstone",      30L,  false);
        add(mgr, "misc", Material.MAGMA_BLOCK,     "Magma Block",    18L,  false);
        add(mgr, "misc", Material.SOUL_SAND,       "Soul Sand",      18L,  false);
        add(mgr, "misc", Material.SOUL_SOIL,       "Soul Soil",      16L,  false);
        add(mgr, "misc", Material.NETHER_BRICKS,   "Nether Bricks",  35L,  false);
        add(mgr, "misc", Material.PURPUR_BLOCK,    "Purpur Block",   90L,  false);
        add(mgr, "misc", Material.PURPUR_PILLAR,   "Purpur Pillar",  95L,  false);
        add(mgr, "misc", Material.END_STONE,       "End Stone",      70L,  false);
        add(mgr, "misc", Material.END_STONE_BRICKS,"End Stone Bricks",85L, false);
        add(mgr, "misc", Material.OBSIDIAN,        "Obsidian",       40L,  false);
        add(mgr, "misc", Material.CRYING_OBSIDIAN, "Crying Obsidian",60L,  false);
        add(mgr, "misc", Material.ICE,             "Ice",            15L,  false);
        add(mgr, "misc", Material.PACKED_ICE,      "Packed Ice",     25L,  false);
        add(mgr, "misc", Material.BLUE_ICE,        "Blue Ice",       50L,  false);
        add(mgr, "misc", Material.SPONGE,          "Sponge",         200L, false);
    }
}
