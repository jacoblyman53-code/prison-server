package com.prison.pickaxe;

import com.prison.database.DatabaseManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * PickaxeManager — core logic for issuing, reading, and mutating server pickaxes.
 *
 * PDC keys used:
 *   prison:server_pickaxe  (BYTE, 1) — marks this as a server-issued pickaxe
 *   prison:pickaxe_uuid    (STRING)  — unique ID for this specific pickaxe
 *   prison:owner_uuid      (STRING)  — UUID of the owning player
 *   prison:ench_{id}       (INTEGER) — current level for each enchant (0 = not purchased)
 */
public class PickaxeManager {

    private static PickaxeManager instance;

    // PDC keys
    public static final String NS = "prison";
    private final NamespacedKey keyServerPickaxe;
    private final NamespacedKey keyPickaxeUuid;
    private final NamespacedKey keyOwnerUuid;
    private final Map<String, NamespacedKey> enchantKeys = new HashMap<>();

    private final PickaxeConfig config;
    private final Logger logger;
    private final MiniMessage mm = MiniMessage.miniMessage();

    private PickaxeManager(JavaPlugin plugin, PickaxeConfig config, Logger logger) {
        this.config = config;
        this.logger = logger;

        keyServerPickaxe = new NamespacedKey(plugin, "server_pickaxe");
        keyPickaxeUuid   = new NamespacedKey(plugin, "pickaxe_uuid");
        keyOwnerUuid     = new NamespacedKey(plugin, "owner_uuid");

        for (String id : config.allEnchantIds()) {
            enchantKeys.put(id, new NamespacedKey(plugin, "ench_" + id));
        }
    }

    public static PickaxeManager initialize(JavaPlugin plugin, PickaxeConfig config, Logger logger) {
        if (instance != null) throw new IllegalStateException("PickaxeManager already initialized");
        instance = new PickaxeManager(plugin, config, logger);
        return instance;
    }

    public static PickaxeManager getInstance() { return instance; }

    // ----------------------------------------------------------------
    // Pickaxe Identity
    // ----------------------------------------------------------------

    /** Returns true if the item is a server-issued prison pickaxe. */
    public boolean isServerPickaxe(ItemStack item) {
        if (item == null || item.getType() != Material.DIAMOND_PICKAXE) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(keyServerPickaxe, PersistentDataType.BYTE);
    }

    /** Returns the pickaxe's unique UUID string, or null if not a server pickaxe. */
    public String getPickaxeUuid(ItemStack item) {
        if (!isServerPickaxe(item)) return null;
        return item.getItemMeta().getPersistentDataContainer()
                   .get(keyPickaxeUuid, PersistentDataType.STRING);
    }

    /** Returns the owner UUID string stored in PDC, or null. */
    public String getOwnerUuid(ItemStack item) {
        if (!isServerPickaxe(item)) return null;
        return item.getItemMeta().getPersistentDataContainer()
                   .get(keyOwnerUuid, PersistentDataType.STRING);
    }

    /** Returns the current level of an enchant on this pickaxe (0 if not purchased). */
    public int getEnchantLevel(ItemStack item, String enchantId) {
        if (!isServerPickaxe(item)) return 0;
        NamespacedKey key = enchantKeys.get(enchantId);
        if (key == null) return 0;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return 0;
        Integer level = meta.getPersistentDataContainer().get(key, PersistentDataType.INTEGER);
        return level != null ? level : 0;
    }

    // ----------------------------------------------------------------
    // Issue
    // ----------------------------------------------------------------

    /**
     * Create and give a fresh server pickaxe to a player.
     * Saves the pickaxe record to the DB and stamps PDC.
     */
    public CompletableFuture<ItemStack> issuePickaxe(Player player) {
        return CompletableFuture.supplyAsync(() -> {
            String pickaxeId = UUID.randomUUID().toString();
            String ownerUuid = player.getUniqueId().toString();

            try {
                DatabaseManager.getInstance().execute(
                    "INSERT INTO pickaxes (uuid, owner_uuid) VALUES (?, ?)",
                    pickaxeId, ownerUuid
                );
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "[Pickaxe] Failed to insert pickaxe into DB", e);
            }

            return buildItem(pickaxeId, ownerUuid, new HashMap<>());
        }).thenApply(item -> {
            // Give item on main thread
            player.getInventory().addItem(item);
            return item;
        });
    }

    // ----------------------------------------------------------------
    // Enchant mutation
    // ----------------------------------------------------------------

    /**
     * Set an enchant level on the pickaxe the player is holding.
     * Updates both PDC (instantly) and DB (async).
     *
     * @return the updated ItemStack, or null on failure.
     */
    public ItemStack setEnchantLevel(Player player, ItemStack item, String enchantId, int level) {
        if (!isServerPickaxe(item)) return null;
        NamespacedKey key = enchantKeys.get(enchantId);
        if (key == null) return null;

        String pickaxeId = getPickaxeUuid(item);

        // Update PDC
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, level);

        // Collect all current levels to rebuild lore
        Map<String, Integer> levels = readAllLevels(meta.getPersistentDataContainer());
        levels.put(enchantId, level);
        applyLore(meta, levels);
        applyVanillaEnchants(meta, levels);
        item.setItemMeta(meta);

        // Update DB async
        CompletableFuture.runAsync(() -> {
            try {
                DatabaseManager.getInstance().execute(
                    "INSERT INTO pickaxe_enchants (pickaxe_uuid, enchant_id, level) VALUES (?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE level = ?",
                    pickaxeId, enchantId, level, level
                );
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "[Pickaxe] Failed to save enchant to DB", e);
            }
        });

        return item;
    }

    // ----------------------------------------------------------------
    // Sync from DB
    // ----------------------------------------------------------------

    /**
     * Rebuild a player's held pickaxe from the database.
     * Call on PlayerJoinEvent to repair any desync.
     */
    public CompletableFuture<Void> syncFromDatabase(Player player) {
        return CompletableFuture.runAsync(() -> {
            ItemStack item = player.getInventory().getItemInMainHand();
            if (!isServerPickaxe(item)) {
                // Check whole inventory
                for (ItemStack it : player.getInventory().getContents()) {
                    if (isServerPickaxe(it)) { item = it; break; }
                }
            }
            if (item == null || !isServerPickaxe(item)) return;

            String pickaxeId = getPickaxeUuid(item);
            if (pickaxeId == null) return;

            try {
                Map<String, Integer> dbLevels = DatabaseManager.getInstance().query(
                    "SELECT enchant_id, level FROM pickaxe_enchants WHERE pickaxe_uuid = ?",
                    rs -> {
                        Map<String, Integer> map = new HashMap<>();
                        while (rs.next()) {
                            map.put(rs.getString("enchant_id"), rs.getInt("level"));
                        }
                        return map;
                    },
                    pickaxeId
                );

                final ItemStack finalItem = item;
                player.getServer().getScheduler().runTask(player.getServer().getPluginManager()
                    .getPlugin("PrisonPickaxe"), () -> {
                    ItemMeta meta = finalItem.getItemMeta();
                    if (meta == null) return;
                    // Write DB levels to PDC
                    for (Map.Entry<String, Integer> e : dbLevels.entrySet()) {
                        NamespacedKey k = enchantKeys.get(e.getKey());
                        if (k != null) {
                            meta.getPersistentDataContainer().set(k, PersistentDataType.INTEGER, e.getValue());
                        }
                    }
                    applyLore(meta, dbLevels);
                    applyVanillaEnchants(meta, dbLevels);
                    finalItem.setItemMeta(meta);
                });
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "[Pickaxe] Failed to sync pickaxe from DB", e);
            }
        });
    }

    // ----------------------------------------------------------------
    // First-join check
    // ----------------------------------------------------------------

    /**
     * Returns true if the player has any pickaxe record in the database
     * (i.e., they've been issued a pickaxe before, even if they no longer have it).
     */
    public CompletableFuture<Boolean> hasPickaxeRecord(java.util.UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Integer count = DatabaseManager.getInstance().query(
                    "SELECT COUNT(*) FROM pickaxes WHERE owner_uuid = ?",
                    rs -> rs.next() ? rs.getInt(1) : 0,
                    playerUuid.toString()
                );
                return count != null && count > 0;
            } catch (SQLException e) {
                logger.warning("[Pickaxe] Failed to check pickaxe record: " + e.getMessage());
                return false;
            }
        });
    }

    // ----------------------------------------------------------------
    // Token multiplier calculation
    // ----------------------------------------------------------------

    /**
     * Returns the Tokenator multiplier for a pickaxe (1.0 if not purchased).
     */
    public double getTokenatorMultiplier(ItemStack item) {
        int level = getEnchantLevel(item, "tokenator");
        if (level == 0) return 1.0;
        Map<Integer, Double> mults = config.getTokenatorMultipliers();
        return mults.getOrDefault(level, 1.0);
    }

    // ----------------------------------------------------------------
    // Inventory sell threshold (for Sellall enchant)
    // ----------------------------------------------------------------

    /**
     * Returns the sell threshold % for the Sellall enchant (0 if not purchased).
     * e.g. level 1 → 75 means "sell when 75% full".
     */
    public int getSellallThreshold(ItemStack item) {
        int level = getEnchantLevel(item, "sellall");
        if (level == 0) return 0;
        Map<Integer, Integer> thresholds = config.getSellallThresholds();
        return thresholds.getOrDefault(level, 0);
    }

    // ----------------------------------------------------------------
    // Jackpot chance
    // ----------------------------------------------------------------

    /** Returns the jackpot trigger chance % (0 if not purchased). */
    public double getJackpotChance(ItemStack item) {
        int level = getEnchantLevel(item, "jackpot");
        if (level == 0) return 0.0;
        Map<Integer, Double> chances = config.getJackpotChances();
        return chances.getOrDefault(level, 0.0);
    }

    // ----------------------------------------------------------------
    // Internal helpers
    // ----------------------------------------------------------------

    private ItemStack buildItem(String pickaxeId, String ownerUuid, Map<String, Integer> levels) {
        ItemStack item = new ItemStack(Material.DIAMOND_PICKAXE);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(mm.deserialize("<gold><bold>Prison Pickaxe</bold></gold>"));

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(keyServerPickaxe, PersistentDataType.BYTE, (byte) 1);
        pdc.set(keyPickaxeUuid,   PersistentDataType.STRING, pickaxeId);
        pdc.set(keyOwnerUuid,     PersistentDataType.STRING, ownerUuid);

        applyLore(meta, levels);
        item.setItemMeta(meta);
        return item;
    }

    /** Write enchant levels as item lore. */
    void applyLore(ItemMeta meta, Map<String, Integer> levels) {
        List<Component> lore = new ArrayList<>();
        lore.add(mm.deserialize("<dark_gray>━━━━━━━━━━━━━━━━━━"));

        // Custom enchants first
        for (EnchantDef def : config.getCustomEnchants()) {
            int lvl = levels.getOrDefault(def.id(), 0);
            if (lvl > 0) {
                lore.add(mm.deserialize("<aqua>" + def.display() + " <white>" + toRoman(lvl)));
            }
        }

        // Vanilla enchants
        for (EnchantDef def : config.getVanillaEnchants()) {
            int lvl = levels.getOrDefault(def.id(), 0);
            if (lvl > 0) {
                lore.add(mm.deserialize("<gray>" + def.display() + " <white>" + toRoman(lvl)));
            }
        }

        lore.add(mm.deserialize("<dark_gray>━━━━━━━━━━━━━━━━━━"));
        lore.add(mm.deserialize("<yellow><i>Sneak + Right-Click to upgrade"));

        meta.lore(lore);
    }

    /** Apply vanilla enchantment objects so Minecraft shows the glow and tool tip. */
    void applyVanillaEnchants(ItemMeta meta, Map<String, Integer> levels) {
        // Remove existing to reset
        meta.removeEnchant(Enchantment.EFFICIENCY);
        meta.removeEnchant(Enchantment.FORTUNE);
        meta.removeEnchant(Enchantment.SILK_TOUCH);

        int eff = levels.getOrDefault("efficiency", 0);
        int fort = levels.getOrDefault("fortune", 0);
        int silk = levels.getOrDefault("silk_touch", 0);

        if (eff  > 0) meta.addEnchant(Enchantment.EFFICIENCY, eff,  true);
        if (fort > 0) meta.addEnchant(Enchantment.FORTUNE,    fort, true);
        if (silk > 0) meta.addEnchant(Enchantment.SILK_TOUCH, 1,    true);
    }

    /** Read all enchant levels from PDC into a map. */
    Map<String, Integer> readAllLevels(PersistentDataContainer pdc) {
        Map<String, Integer> map = new HashMap<>();
        for (Map.Entry<String, NamespacedKey> e : enchantKeys.entrySet()) {
            Integer lvl = pdc.get(e.getValue(), PersistentDataType.INTEGER);
            if (lvl != null && lvl > 0) map.put(e.getKey(), lvl);
        }
        return map;
    }

    /** Read all enchant levels from a held ItemStack. */
    public Map<String, Integer> readAllLevels(ItemStack item) {
        if (!isServerPickaxe(item)) return new HashMap<>();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return new HashMap<>();
        return readAllLevels(meta.getPersistentDataContainer());
    }

    private static final String[] ROMAN = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
    private String toRoman(int n) {
        return (n >= 0 && n < ROMAN.length) ? ROMAN[n] : String.valueOf(n);
    }

    public PickaxeConfig getConfig() { return config; }
}
