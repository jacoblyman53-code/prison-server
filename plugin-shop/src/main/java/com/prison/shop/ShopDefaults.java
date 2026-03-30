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
 *   6. Ores            — ores, ingots, gems
 *   7. Farming         — crops, seeds, natural items
 *   8. Combat          — armor, bows, shields
 *   9. Redstone        — redstone components and mechanisms
 *  10. Misc            — wool, string, mob drops, nether items
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
        populateOres(mgr);
        populateFarming(mgr);
        populateCombat(mgr);
        populateRedstone(mgr);
        populateMisc(mgr);

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
    // Category 6: Ores
    // ----------------------------------------------------------------

    private static void populateOres(ShopManager mgr) {
        mgr.addCategory("ores", "<yellow><bold>Ores & Materials", Material.DIAMOND_ORE);

        // --- Coal ---
        ItemStack coal = new ItemStack(Material.COAL, 64);
        setMeta(coal, "<gray>Coal <dark_gray>×64",
            "<gray>Fuel for furnaces and torches.",
            "<dark_gray>Basic early-game resource.");
        mgr.addItem("ores", coal, "<gray>Coal <dark_gray>×64", 2_000L, -1);

        // --- Iron ---
        ItemStack rawIron = new ItemStack(Material.RAW_IRON, 16);
        setMeta(rawIron, "<white>Raw Iron <dark_gray>×16",
            "<gray>Smelt into iron ingots.",
            "<dark_gray>Mid-game crafting staple.");
        mgr.addItem("ores", rawIron, "<white>Raw Iron <dark_gray>×16", 3_000L, -1);

        ItemStack ironIngot = new ItemStack(Material.IRON_INGOT, 16);
        setMeta(ironIngot, "<white>Iron Ingot <dark_gray>×16",
            "<gray>Versatile crafting material.",
            "<dark_gray>Used for tools, armor, and mechanisms.");
        mgr.addItem("ores", ironIngot, "<white>Iron Ingot <dark_gray>×16", 5_000L, -1);

        // --- Gold ---
        ItemStack rawGold = new ItemStack(Material.RAW_GOLD, 8);
        setMeta(rawGold, "<gold>Raw Gold <dark_gray>×8",
            "<gray>Smelt into gold ingots.",
            "<dark_gray>Mid-game crafting resource.");
        mgr.addItem("ores", rawGold, "<gold>Raw Gold <dark_gray>×8", 4_000L, -1);

        ItemStack goldIngot = new ItemStack(Material.GOLD_INGOT, 8);
        setMeta(goldIngot, "<gold>Gold Ingot <dark_gray>×8",
            "<gray>Used for golden tools and decorative blocks.",
            "<dark_gray>Also trades with piglins.");
        mgr.addItem("ores", goldIngot, "<gold>Gold Ingot <dark_gray>×8", 6_000L, -1);

        // --- Lapis ---
        ItemStack lapis = new ItemStack(Material.LAPIS_LAZULI, 16);
        setMeta(lapis, "<blue>Lapis Lazuli <dark_gray>×16",
            "<gray>Required for enchanting.",
            "<dark_gray>Also a blue dye.");
        mgr.addItem("ores", lapis, "<blue>Lapis Lazuli <dark_gray>×16", 3_500L, -1);

        // --- Redstone ---
        ItemStack redstone = new ItemStack(Material.REDSTONE, 64);
        setMeta(redstone, "<red>Redstone Dust <dark_gray>×64",
            "<gray>Power source for mechanisms.",
            "<dark_gray>Core component for all redstone builds.");
        mgr.addItem("ores", redstone, "<red>Redstone Dust <dark_gray>×64", 4_000L, -1);

        // --- Diamond ---
        ItemStack diamond = new ItemStack(Material.DIAMOND, 1);
        setMeta(diamond, "<aqua>Diamond",
            "<gray>Rare and valuable.",
            "<dark_gray>Craft top-tier tools and armor.");
        mgr.addItem("ores", diamond, "<aqua>Diamond", 8_000L, -1);

        // --- Emerald ---
        ItemStack emerald = new ItemStack(Material.EMERALD, 1);
        setMeta(emerald, "<green>Emerald",
            "<gray>Villager trading currency.",
            "<dark_gray>Rare late-game gem.");
        mgr.addItem("ores", emerald, "<green>Emerald", 12_000L, -1);

        // --- Netherite ---
        ItemStack netheriteScrap = new ItemStack(Material.NETHERITE_SCRAP, 1);
        setMeta(netheriteScrap, "<dark_gray>Netherite Scrap",
            "<gray>Combine 4 scraps + 4 gold ingots = 1 netherite ingot.",
            "<dark_gray>Top-tier upgrade material.");
        mgr.addItem("ores", netheriteScrap, "<dark_gray>Netherite Scrap", 40_000L, -1);

        ItemStack netheriteIngot = new ItemStack(Material.NETHERITE_INGOT, 1);
        setMeta(netheriteIngot, "<dark_gray><bold>Netherite Ingot",
            "<gray>Upgrade diamond tools/armor to netherite.",
            "<dark_gray>Best tier in the game.");
        mgr.addItem("ores", netheriteIngot, "<dark_gray>Netherite Ingot", 150_000L, -1);
    }

    // ----------------------------------------------------------------
    // Category 7: Farming
    // ----------------------------------------------------------------

    private static void populateFarming(ShopManager mgr) {
        mgr.addCategory("farming", "<green><bold>Farming", Material.WHEAT);

        // --- Seeds & crops ---
        ItemStack seeds = new ItemStack(Material.WHEAT_SEEDS, 64);
        setMeta(seeds, "<green>Wheat Seeds <dark_gray>×64",
            "<gray>Plant on farmland to grow wheat.",
            "<dark_gray>Basic farming starter.");
        mgr.addItem("farming", seeds, "<green>Wheat Seeds <dark_gray>×64", 500L, -1);

        ItemStack wheat = new ItemStack(Material.WHEAT, 64);
        setMeta(wheat, "<yellow>Wheat <dark_gray>×64",
            "<gray>Breed animals or craft bread.",
            "<dark_gray>Essential farming crop.");
        mgr.addItem("farming", wheat, "<yellow>Wheat <dark_gray>×64", 1_000L, -1);

        ItemStack carrotItem = new ItemStack(Material.CARROT, 64);
        setMeta(carrotItem, "<orange>Carrot <dark_gray>×64",
            "<gray>Food and animal bait.",
            "<dark_gray>Also crafts golden carrots.");
        mgr.addItem("farming", carrotItem, "<orange>Carrot <dark_gray>×64", 1_200L, -1);

        ItemStack potato = new ItemStack(Material.POTATO, 64);
        setMeta(potato, "<yellow>Potato <dark_gray>×64",
            "<gray>Cook for baked potatoes.",
            "<dark_gray>Good food source.");
        mgr.addItem("farming", potato, "<yellow>Potato <dark_gray>×64", 1_000L, -1);

        ItemStack beetroot = new ItemStack(Material.BEETROOT, 64);
        setMeta(beetroot, "<dark_red>Beetroot <dark_gray>×64",
            "<gray>Food and red dye source.",
            "<dark_gray>Uncommon early crop.");
        mgr.addItem("farming", beetroot, "<dark_red>Beetroot <dark_gray>×64", 800L, -1);

        ItemStack melon = new ItemStack(Material.MELON_SLICE, 64);
        setMeta(melon, "<green>Melon Slice <dark_gray>×64",
            "<gray>Quick food. Low hunger but fast.",
            "<dark_gray>Great for budget snacking.");
        mgr.addItem("farming", melon, "<green>Melon Slice <dark_gray>×64", 1_500L, -1);

        ItemStack pumpkin = new ItemStack(Material.PUMPKIN, 16);
        setMeta(pumpkin, "<gold>Pumpkin <dark_gray>×16",
            "<gray>Decoration and crafting (jack-o-lantern).",
            "<dark_gray>Seasonal building block.");
        mgr.addItem("farming", pumpkin, "<gold>Pumpkin <dark_gray>×16", 2_000L, -1);

        ItemStack sugarcane = new ItemStack(Material.SUGAR_CANE, 64);
        setMeta(sugarcane, "<green>Sugar Cane <dark_gray>×64",
            "<gray>Craft sugar and paper.",
            "<dark_gray>Also used for sugar cane farms.");
        mgr.addItem("farming", sugarcane, "<green>Sugar Cane <dark_gray>×64", 1_500L, -1);

        ItemStack cactus = new ItemStack(Material.CACTUS, 64);
        setMeta(cactus, "<dark_green>Cactus <dark_gray>×64",
            "<gray>Decoration and trap building.",
            "<dark_gray>Desert defensive item.");
        mgr.addItem("farming", cactus, "<dark_green>Cactus <dark_gray>×64", 800L, -1);

        ItemStack boneMeal = new ItemStack(Material.BONE_MEAL, 64);
        setMeta(boneMeal, "<white>Bone Meal <dark_gray>×64",
            "<gray>Fertilizer — instantly grows plants.",
            "<dark_gray>Essential for fast farming.");
        mgr.addItem("farming", boneMeal, "<white>Bone Meal <dark_gray>×64", 2_500L, -1);

        ItemStack apple = new ItemStack(Material.APPLE, 16);
        setMeta(apple, "<red>Apple <dark_gray>×16",
            "<gray>Simple food. Craft golden apples.",
            "<dark_gray>Useful in early game.");
        mgr.addItem("farming", apple, "<red>Apple <dark_gray>×16", 1_500L, -1);

        ItemStack cocoa = new ItemStack(Material.COCOA_BEANS, 16);
        setMeta(cocoa, "<gold>Cocoa Beans <dark_gray>×16",
            "<gray>Craft cookies. Brown dye source.",
            "<dark_gray>Grow on jungle logs.");
        mgr.addItem("farming", cocoa, "<gold>Cocoa Beans <dark_gray>×16", 1_200L, -1);
    }

    // ----------------------------------------------------------------
    // Category 8: Combat
    // ----------------------------------------------------------------

    private static void populateCombat(ShopManager mgr) {
        mgr.addCategory("combat", "<red><bold>Combat", Material.DIAMOND_CHESTPLATE);

        // --- Iron armor ---
        ItemStack iHelm = new ItemStack(Material.IRON_HELMET);
        enchant(iHelm, Enchantment.PROTECTION, 2);
        setMeta(iHelm, "<white>Iron Helmet",
            "<gray>Protection II",
            "<dark_gray>Solid early-game head protection.");
        mgr.addItem("combat", iHelm, "<white>Iron Helmet", 6_000L, -1);

        ItemStack iChest = new ItemStack(Material.IRON_CHESTPLATE);
        enchant(iChest, Enchantment.PROTECTION, 2);
        setMeta(iChest, "<white>Iron Chestplate",
            "<gray>Protection II",
            "<dark_gray>Mid-tier chest armor.");
        mgr.addItem("combat", iChest, "<white>Iron Chestplate", 10_000L, -1);

        ItemStack iLegs = new ItemStack(Material.IRON_LEGGINGS);
        enchant(iLegs, Enchantment.PROTECTION, 2);
        setMeta(iLegs, "<white>Iron Leggings",
            "<gray>Protection II",
            "<dark_gray>Solid mid-game leg armor.");
        mgr.addItem("combat", iLegs, "<white>Iron Leggings", 8_000L, -1);

        ItemStack iBoots = new ItemStack(Material.IRON_BOOTS);
        enchant(iBoots, Enchantment.PROTECTION, 2);
        enchant(iBoots, Enchantment.FEATHER_FALLING, 2);
        setMeta(iBoots, "<white>Iron Boots",
            "<gray>Protection II · Feather Falling II",
            "<dark_gray>Good early-game boots.");
        mgr.addItem("combat", iBoots, "<white>Iron Boots", 6_000L, -1);

        // --- Diamond armor ---
        ItemStack dHelm = new ItemStack(Material.DIAMOND_HELMET);
        enchant(dHelm, Enchantment.PROTECTION, 3);
        enchant(dHelm, Enchantment.UNBREAKING, 3);
        setMeta(dHelm, "<aqua>Diamond Helmet",
            "<gray>Protection III · Unbreaking III",
            "<dark_gray>Strong late-game head armor.");
        mgr.addItem("combat", dHelm, "<aqua>Diamond Helmet", 80_000L, -1);

        ItemStack dChest = new ItemStack(Material.DIAMOND_CHESTPLATE);
        enchant(dChest, Enchantment.PROTECTION, 3);
        enchant(dChest, Enchantment.UNBREAKING, 3);
        setMeta(dChest, "<aqua>Diamond Chestplate",
            "<gray>Protection III · Unbreaking III",
            "<dark_gray>Top-tier chest armor.");
        mgr.addItem("combat", dChest, "<aqua>Diamond Chestplate", 150_000L, -1);

        ItemStack dLegs = new ItemStack(Material.DIAMOND_LEGGINGS);
        enchant(dLegs, Enchantment.PROTECTION, 3);
        enchant(dLegs, Enchantment.UNBREAKING, 3);
        setMeta(dLegs, "<aqua>Diamond Leggings",
            "<gray>Protection III · Unbreaking III",
            "<dark_gray>Top-tier leg armor.");
        mgr.addItem("combat", dLegs, "<aqua>Diamond Leggings", 120_000L, -1);

        ItemStack dBoots = new ItemStack(Material.DIAMOND_BOOTS);
        enchant(dBoots, Enchantment.PROTECTION, 3);
        enchant(dBoots, Enchantment.UNBREAKING, 3);
        enchant(dBoots, Enchantment.FEATHER_FALLING, 4);
        setMeta(dBoots, "<aqua>Diamond Boots",
            "<gray>Protection III · Unbreaking III · Feather Falling IV",
            "<dark_gray>Top-tier boots.");
        mgr.addItem("combat", dBoots, "<aqua>Diamond Boots", 80_000L, -1);

        // --- Ranged ---
        ItemStack bow = new ItemStack(Material.BOW);
        enchant(bow, Enchantment.POWER, 3);
        enchant(bow, Enchantment.PUNCH, 1);
        enchant(bow, Enchantment.UNBREAKING, 2);
        setMeta(bow, "<yellow>Bow",
            "<gray>Power III · Punch I · Unbreaking II",
            "<dark_gray>Solid ranged combat weapon.");
        mgr.addItem("combat", bow, "<yellow>Bow", 18_000L, -1);

        ItemStack arrows = new ItemStack(Material.ARROW, 64);
        setMeta(arrows, "<white>Arrow <dark_gray>×64",
            "<gray>Ammunition for bows.",
            "<dark_gray>Always keep a supply.");
        mgr.addItem("combat", arrows, "<white>Arrow <dark_gray>×64", 3_000L, -1);

        ItemStack crossbow = new ItemStack(Material.CROSSBOW);
        enchant(crossbow, Enchantment.QUICK_CHARGE, 3);
        enchant(crossbow, Enchantment.PIERCING, 2);
        setMeta(crossbow, "<white>Crossbow",
            "<gray>Quick Charge III · Piercing II",
            "<dark_gray>High burst ranged damage.");
        mgr.addItem("combat", crossbow, "<white>Crossbow", 30_000L, -1);

        // --- Shield ---
        ItemStack shield = new ItemStack(Material.SHIELD);
        enchant(shield, Enchantment.UNBREAKING, 3);
        setMeta(shield, "<white>Shield",
            "<gray>Unbreaking III",
            "<dark_gray>Block incoming attacks. Durable.");
        mgr.addItem("combat", shield, "<white>Shield", 12_000L, -1);
    }

    // ----------------------------------------------------------------
    // Category 9: Redstone
    // ----------------------------------------------------------------

    private static void populateRedstone(ShopManager mgr) {
        mgr.addCategory("redstone", "<red><bold>Redstone", Material.REDSTONE);

        // --- Dust & torches ---
        ItemStack redDust = new ItemStack(Material.REDSTONE, 64);
        setMeta(redDust, "<red>Redstone Dust <dark_gray>×64",
            "<gray>Wire power between components.",
            "<dark_gray>Backbone of all redstone circuits.");
        mgr.addItem("redstone", redDust, "<red>Redstone Dust <dark_gray>×64", 3_500L, -1);

        ItemStack redTorch = new ItemStack(Material.REDSTONE_TORCH, 16);
        setMeta(redTorch, "<red>Redstone Torch <dark_gray>×16",
            "<gray>Always-on power source. Also an inverter.",
            "<dark_gray>Essential for logic gates.");
        mgr.addItem("redstone", redTorch, "<red>Redstone Torch <dark_gray>×16", 2_000L, -1);

        // --- Logic ---
        ItemStack repeater = new ItemStack(Material.REPEATER, 8);
        setMeta(repeater, "<white>Repeater <dark_gray>×8",
            "<gray>Delay or extend redstone signals.",
            "<dark_gray>One-directional signal booster.");
        mgr.addItem("redstone", repeater, "<white>Repeater <dark_gray>×8", 3_000L, -1);

        ItemStack comparator = new ItemStack(Material.COMPARATOR, 4);
        setMeta(comparator, "<white>Comparator <dark_gray>×4",
            "<gray>Compare or subtract signal strength.",
            "<dark_gray>Complex logic and item detection.");
        mgr.addItem("redstone", comparator, "<white>Comparator <dark_gray>×4", 4_000L, -1);

        // --- Activation ---
        ItemStack lever = new ItemStack(Material.LEVER, 16);
        setMeta(lever, "<gray>Lever <dark_gray>×16",
            "<gray>Toggle switch for redstone circuits.",
            "<dark_gray>Most common manual input.");
        mgr.addItem("redstone", lever, "<gray>Lever <dark_gray>×16", 1_000L, -1);

        ItemStack button = new ItemStack(Material.STONE_BUTTON, 16);
        setMeta(button, "<gray>Stone Button <dark_gray>×16",
            "<gray>Momentary redstone pulse.",
            "<dark_gray>Ideal for doors and traps.");
        mgr.addItem("redstone", button, "<gray>Stone Button <dark_gray>×16", 800L, -1);

        ItemStack pressurePlate = new ItemStack(Material.STONE_PRESSURE_PLATE, 8);
        setMeta(pressurePlate, "<gray>Pressure Plate <dark_gray>×8",
            "<gray>Activates when walked on.",
            "<dark_gray>Great for automatic door systems.");
        mgr.addItem("redstone", pressurePlate, "<gray>Pressure Plate <dark_gray>×8", 1_500L, -1);

        // --- Mechanisms ---
        ItemStack piston = new ItemStack(Material.PISTON, 4);
        setMeta(piston, "<white>Piston <dark_gray>×4",
            "<gray>Push blocks when powered.",
            "<dark_gray>Core mechanism for movable structures.");
        mgr.addItem("redstone", piston, "<white>Piston <dark_gray>×4", 5_000L, -1);

        ItemStack stickyPiston = new ItemStack(Material.STICKY_PISTON, 4);
        setMeta(stickyPiston, "<green>Sticky Piston <dark_gray>×4",
            "<gray>Push and pull blocks.",
            "<dark_gray>Required for retractable doors and bridges.");
        mgr.addItem("redstone", stickyPiston, "<green>Sticky Piston <dark_gray>×4", 8_000L, -1);

        ItemStack observer = new ItemStack(Material.OBSERVER, 4);
        setMeta(observer, "<dark_gray>Observer <dark_gray>×4",
            "<gray>Detects block state changes.",
            "<dark_gray>Essential for automatic farms.");
        mgr.addItem("redstone", observer, "<dark_gray>Observer <dark_gray>×4", 6_000L, -1);

        ItemStack hopper = new ItemStack(Material.HOPPER, 2);
        setMeta(hopper, "<gray>Hopper <dark_gray>×2",
            "<gray>Transfer items between containers.",
            "<dark_gray>Core of automated sorting systems.");
        mgr.addItem("redstone", hopper, "<gray>Hopper <dark_gray>×2", 8_000L, -1);

        ItemStack dropper = new ItemStack(Material.DROPPER, 4);
        setMeta(dropper, "<gray>Dropper <dark_gray>×4",
            "<gray>Eject items when powered.",
            "<dark_gray>Useful for dispensing systems.");
        mgr.addItem("redstone", dropper, "<gray>Dropper <dark_gray>×4", 3_500L, -1);

        ItemStack dispenser = new ItemStack(Material.DISPENSER, 4);
        setMeta(dispenser, "<gray>Dispenser <dark_gray>×4",
            "<gray>Launch projectiles or use items when powered.",
            "<dark_gray>Shoot arrows, fire potions, or fill buckets.");
        mgr.addItem("redstone", dispenser, "<gray>Dispenser <dark_gray>×4", 5_500L, -1);

        ItemStack tnt = new ItemStack(Material.TNT, 4);
        setMeta(tnt, "<red>TNT <dark_gray>×4",
            "<gray>Explodes when powered or lit.",
            "<dark_gray>High-blast demolition material.");
        mgr.addItem("redstone", tnt, "<red>TNT <dark_gray>×4", 20_000L, -1);

        ItemStack daylightSensor = new ItemStack(Material.DAYLIGHT_DETECTOR, 4);
        setMeta(daylightSensor, "<yellow>Daylight Detector <dark_gray>×4",
            "<gray>Outputs signal based on sunlight.",
            "<dark_gray>Time-based automation.");
        mgr.addItem("redstone", daylightSensor, "<yellow>Daylight Detector <dark_gray>×4", 4_000L, -1);
    }

    // ----------------------------------------------------------------
    // Category 10: Misc
    // ----------------------------------------------------------------

    private static void populateMisc(ShopManager mgr) {
        mgr.addCategory("misc", "<white><bold>Misc", Material.CHEST);

        // --- Fabric & string ---
        ItemStack string = new ItemStack(Material.STRING, 64);
        setMeta(string, "<white>String <dark_gray>×64",
            "<gray>Craft bows, leads, and wool.",
            "<dark_gray>Spider drop — hard to farm in prison.");
        mgr.addItem("misc", string, "<white>String <dark_gray>×64", 2_500L, -1);

        ItemStack wool = new ItemStack(Material.WHITE_WOOL, 64);
        setMeta(wool, "<white>White Wool <dark_gray>×64",
            "<gray>Soft building block. Dye to any color.",
            "<dark_gray>Versatile decorative material.");
        mgr.addItem("misc", wool, "<white>White Wool <dark_gray>×64", 3_000L, -1);

        ItemStack leather = new ItemStack(Material.LEATHER, 16);
        setMeta(leather, "<gold>Leather <dark_gray>×16",
            "<gray>Craft leather armor and item frames.",
            "<dark_gray>Cow drop — limited in prison.");
        mgr.addItem("misc", leather, "<gold>Leather <dark_gray>×16", 4_000L, -1);

        // --- Mob drops ---
        ItemStack bone = new ItemStack(Material.BONE, 16);
        setMeta(bone, "<white>Bone <dark_gray>×16",
            "<gray>Craft bone meal (9 bones = 9 meal).",
            "<dark_gray>Skeleton drop.");
        mgr.addItem("misc", bone, "<white>Bone <dark_gray>×16", 2_000L, -1);

        ItemStack feather = new ItemStack(Material.FEATHER, 64);
        setMeta(feather, "<white>Feather <dark_gray>×64",
            "<gray>Craft arrows (1 feather per arrow).",
            "<dark_gray>Chicken drop.");
        mgr.addItem("misc", feather, "<white>Feather <dark_gray>×64", 2_000L, -1);

        ItemStack slimeball = new ItemStack(Material.SLIME_BALL, 8);
        setMeta(slimeball, "<green>Slime Ball <dark_gray>×8",
            "<gray>Craft sticky pistons and leads.",
            "<dark_gray>Rare slime drop.");
        mgr.addItem("misc", slimeball, "<green>Slime Ball <dark_gray>×8", 5_000L, -1);

        ItemStack ink = new ItemStack(Material.INK_SAC, 16);
        setMeta(ink, "<dark_gray>Ink Sac <dark_gray>×16",
            "<gray>Black dye. Also used for book writing.",
            "<dark_gray>Squid drop.");
        mgr.addItem("misc", ink, "<dark_gray>Ink Sac <dark_gray>×16", 2_000L, -1);

        // --- Paper/books ---
        ItemStack paper = new ItemStack(Material.PAPER, 64);
        setMeta(paper, "<white>Paper <dark_gray>×64",
            "<gray>Craft books, maps, and banners.",
            "<dark_gray>Made from sugar cane.");
        mgr.addItem("misc", paper, "<white>Paper <dark_gray>×64", 1_500L, -1);

        ItemStack book = new ItemStack(Material.BOOK, 16);
        setMeta(book, "<white>Book <dark_gray>×16",
            "<gray>Craft bookshelves and writable books.",
            "<dark_gray>3 paper + 1 leather = 1 book.");
        mgr.addItem("misc", book, "<white>Book <dark_gray>×16", 5_000L, -1);

        // --- Nether items ---
        ItemStack blazeRod = new ItemStack(Material.BLAZE_ROD, 8);
        setMeta(blazeRod, "<gold>Blaze Rod <dark_gray>×8",
            "<gray>Craft brewing stands and blaze powder.",
            "<dark_gray>Blaze drop from the Nether.");
        mgr.addItem("misc", blazeRod, "<gold>Blaze Rod <dark_gray>×8", 15_000L, -1);

        ItemStack enderPearl = new ItemStack(Material.ENDER_PEARL, 8);
        setMeta(enderPearl, "<dark_purple>Ender Pearl <dark_gray>×8",
            "<gray>Throw to teleport. Craft Eyes of Ender.",
            "<dark_gray>Enderman drop.");
        mgr.addItem("misc", enderPearl, "<dark_purple>Ender Pearl <dark_gray>×8", 20_000L, -1);

        ItemStack netherWart = new ItemStack(Material.NETHER_WART, 16);
        setMeta(netherWart, "<dark_red>Nether Wart <dark_gray>×16",
            "<gray>Base ingredient for most potions.",
            "<dark_gray>Grows on soul sand in the Nether.");
        mgr.addItem("misc", netherWart, "<dark_red>Nether Wart <dark_gray>×16", 5_000L, -1);

        ItemStack glowstone = new ItemStack(Material.GLOWSTONE_DUST, 16);
        setMeta(glowstone, "<yellow>Glowstone Dust <dark_gray>×16",
            "<gray>Craft glowstone blocks or upgrad potions.",
            "<dark_gray>Bright Nether light source material.");
        mgr.addItem("misc", glowstone, "<yellow>Glowstone Dust <dark_gray>×16", 4_000L, -1);

        // --- Wool colors ---
        ItemStack redWool = new ItemStack(Material.RED_WOOL, 64);
        setMeta(redWool, "<red>Red Wool <dark_gray>×64",
            "<gray>Dyed building block.",
            "<dark_gray>Popular for decorative builds.");
        mgr.addItem("misc", redWool, "<red>Red Wool <dark_gray>×64", 3_500L, -1);

        ItemStack blackWool = new ItemStack(Material.BLACK_WOOL, 64);
        setMeta(blackWool, "<dark_gray>Black Wool <dark_gray>×64",
            "<gray>Dark dyed building block.",
            "<dark_gray>Great for modern interiors.");
        mgr.addItem("misc", blackWool, "<dark_gray>Black Wool <dark_gray>×64", 3_500L, -1);
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
