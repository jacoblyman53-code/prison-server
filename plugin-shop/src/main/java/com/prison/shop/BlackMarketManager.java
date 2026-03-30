package com.prison.shop;

import com.prison.economy.EconomyAPI;
import com.prison.economy.TransactionType;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Manages the Black Market rotation: loading the item pool from blackmarket.yml,
 * selecting 6 random items every N hours, tracking per-item stock, and handling
 * purchases.
 */
public class BlackMarketManager {

    // ----------------------------------------------------------------
    // Purchase result enum
    // ----------------------------------------------------------------

    public enum PurchaseResult {
        OK,
        ITEM_NOT_FOUND,
        OUT_OF_STOCK,
        INSUFFICIENT_FUNDS
    }

    // ----------------------------------------------------------------
    // Singleton
    // ----------------------------------------------------------------

    private static BlackMarketManager instance;

    public static BlackMarketManager getInstance() {
        return instance;
    }

    public static void initialize(ShopPlugin plugin) {
        instance = new BlackMarketManager(plugin);
        instance.load();
    }

    // ----------------------------------------------------------------
    // Fields
    // ----------------------------------------------------------------

    private final ShopPlugin plugin;
    private final Logger logger;

    private File dataFile;
    private YamlConfiguration dataConfig;

    /** Full pool of items defined in the config. */
    private final List<BlackMarketItem> itemPool = new ArrayList<>();

    /** Currently active rotation (up to items-per-rotation items). */
    private final List<BlackMarketItem> currentItems = new ArrayList<>();

    private long lastRotationTime = 0L;
    private long intervalMs = 6L * 60 * 60 * 1000; // 6 hours default
    private int itemsPerRotation = 6;

    private BukkitTask rotationTask;

    // ----------------------------------------------------------------
    // Constructor
    // ----------------------------------------------------------------

    private BlackMarketManager(ShopPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    // ----------------------------------------------------------------
    // Init / Load
    // ----------------------------------------------------------------

    private void load() {
        dataFile = new File(plugin.getDataFolder(), "blackmarket.yml");

        // Write the default config the first time
        if (!dataFile.exists()) {
            plugin.saveResource("blackmarket.yml", false);
        }

        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        // Read rotation settings
        intervalMs = (long) dataConfig.getInt("rotation.interval-hours", 6) * 60L * 60 * 1000;
        itemsPerRotation = dataConfig.getInt("rotation.items-per-rotation", 6);

        // Load item pool
        loadItemPool();

        // Load or create rotation
        lastRotationTime = dataConfig.getLong("state.last-rotation", 0L);
        if (needsRotation()) {
            rotate();
        } else {
            loadSavedRotation();
        }

        scheduleRotationTask();
    }

    private void loadItemPool() {
        itemPool.clear();

        List<?> poolList = dataConfig.getList("item-pool");
        if (poolList == null || poolList.isEmpty()) {
            logger.warning("[BlackMarket] item-pool is empty in blackmarket.yml");
            return;
        }

        for (Object raw : poolList) {
            if (!(raw instanceof Map<?, ?> map)) continue;
            try {
                String id = getString(map, "id", null);
                if (id == null) continue;

                String display = getString(map, "display", "<!italic><white>" + id);
                String itemStr = getString(map, "item", "STONE");
                int amount = getInt(map, "amount", 1);
                long price = getLong(map, "price", 1000L);
                int stock = getInt(map, "stock", 1);

                Object rawLore = map.get("lore");
                @SuppressWarnings("unchecked")
                List<String> lore = (rawLore instanceof List<?> l)
                    ? (List<String>)(List<?>)l : List.of();

                Material mat;
                try {
                    mat = Material.valueOf(itemStr.toUpperCase());
                } catch (IllegalArgumentException e) {
                    logger.warning("[BlackMarket] Unknown material '" + itemStr + "' for item '" + id + "', skipping.");
                    continue;
                }

                itemPool.add(new BlackMarketItem(id, display, lore, mat, amount, price, stock));
            } catch (Exception e) {
                logger.warning("[BlackMarket] Failed to load pool entry: " + e.getMessage());
            }
        }

        logger.info("[BlackMarket] Loaded " + itemPool.size() + " items in pool.");
    }

    /**
     * Loads the saved rotation (item IDs + current stocks) from the state section.
     * Falls back to a fresh rotate() if any item is missing from the pool.
     */
    private void loadSavedRotation() {
        currentItems.clear();

        ConfigurationSection state = dataConfig.getConfigurationSection("state.current-rotation");
        if (state == null) {
            rotate();
            return;
        }

        Map<String, BlackMarketItem> poolById = new HashMap<>();
        for (BlackMarketItem bmi : itemPool) {
            poolById.put(bmi.getId(), bmi);
        }

        List<BlackMarketItem> loaded = new ArrayList<>();
        for (String key : state.getKeys(false)) {
            String itemId = state.getString(key + ".id");
            int savedStock = state.getInt(key + ".stock", 0);

            BlackMarketItem template = poolById.get(itemId);
            if (template == null) {
                logger.warning("[BlackMarket] Saved rotation references unknown item '" + itemId + "' — forcing re-rotation.");
                rotate();
                return;
            }

            // Clone by constructing a fresh instance with the saved stock
            BlackMarketItem live = new BlackMarketItem(
                    template.getId(),
                    template.getDisplay(),
                    template.getLore(),
                    template.getMaterial(),
                    template.getAmount(),
                    template.getPriceIgc(),
                    template.getBaseStock()
            );
            live.setCurrentStock(savedStock);
            loaded.add(live);
        }

        currentItems.addAll(loaded);
        logger.info("[BlackMarket] Loaded saved rotation with " + currentItems.size() + " items.");
    }

    // ----------------------------------------------------------------
    // Rotation
    // ----------------------------------------------------------------

    /**
     * Selects a fresh set of random items from the pool, resets their stock,
     * saves the timestamp and rotation to blackmarket.yml.
     */
    public void rotate() {
        if (itemPool.isEmpty()) {
            logger.warning("[BlackMarket] Cannot rotate — item pool is empty.");
            return;
        }

        currentItems.clear();

        List<BlackMarketItem> shuffled = new ArrayList<>(itemPool);
        Collections.shuffle(shuffled);

        int count = Math.min(itemsPerRotation, shuffled.size());
        for (int i = 0; i < count; i++) {
            BlackMarketItem template = shuffled.get(i);
            // Create a fresh live instance so the pool is never mutated
            BlackMarketItem live = new BlackMarketItem(
                    template.getId(),
                    template.getDisplay(),
                    template.getLore(),
                    template.getMaterial(),
                    template.getAmount(),
                    template.getPriceIgc(),
                    template.getBaseStock()
            );
            live.resetStock();
            currentItems.add(live);
        }

        lastRotationTime = System.currentTimeMillis();
        saveState();

        logger.info("[BlackMarket] Rotated — " + currentItems.size() + " items now active.");

        // Notify all online players
        String msg = "<dark_red>☠ <white>The <dark_red>Black Market<white> has refreshed! Use <gold>/bm<white> to check the new stock.";
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            p.sendMessage(MiniMessage.miniMessage().deserialize(msg));
        }
    }

    public boolean needsRotation() {
        return System.currentTimeMillis() > lastRotationTime + intervalMs;
    }

    private void scheduleRotationTask() {
        if (rotationTask != null && !rotationTask.isCancelled()) {
            rotationTask.cancel();
        }

        // Check every 30 seconds whether a rotation is due
        long checkPeriodTicks = 30L * 20L;
        rotationTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (needsRotation()) {
                rotate();
            }
        }, checkPeriodTicks, checkPeriodTicks);
    }

    // ----------------------------------------------------------------
    // Persistence
    // ----------------------------------------------------------------

    private void saveState() {
        dataConfig.set("state.last-rotation", lastRotationTime);
        dataConfig.set("state.current-rotation", null); // wipe old

        for (int i = 0; i < currentItems.size(); i++) {
            BlackMarketItem bmi = currentItems.get(i);
            String path = "state.current-rotation." + i;
            dataConfig.set(path + ".id", bmi.getId());
            dataConfig.set(path + ".stock", bmi.getCurrentStock());
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            logger.severe("[BlackMarket] Failed to save state: " + e.getMessage());
        }
    }

    /** Persists just the stock values for the current rotation (lightweight). */
    private void saveStockOnly() {
        for (int i = 0; i < currentItems.size(); i++) {
            BlackMarketItem bmi = currentItems.get(i);
            String path = "state.current-rotation." + i + ".stock";
            dataConfig.set(path, bmi.getCurrentStock());
        }
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            logger.severe("[BlackMarket] Failed to save stock: " + e.getMessage());
        }
    }

    // ----------------------------------------------------------------
    // Public API
    // ----------------------------------------------------------------

    /** Returns an unmodifiable view of the current rotation list. */
    public List<BlackMarketItem> getCurrentItems() {
        return Collections.unmodifiableList(currentItems);
    }

    /** Milliseconds until the next rotation. Never negative. */
    public long getTimeUntilRefresh() {
        long remaining = (lastRotationTime + intervalMs) - System.currentTimeMillis();
        return Math.max(0L, remaining);
    }

    /**
     * Formats a millisecond duration as "Xh Ym" when >= 1 minute, or "Xm Ys" otherwise.
     * Returns "now" if ms is 0.
     */
    public String formatCountdown(long ms) {
        if (ms <= 0) return "now";

        long totalSecs = ms / 1000;
        long hours = totalSecs / 3600;
        long mins  = (totalSecs % 3600) / 60;
        long secs  = totalSecs % 60;

        if (hours > 0) {
            return hours + "h " + mins + "m";
        } else if (mins > 0) {
            return mins + "m " + secs + "s";
        } else {
            return secs + "s";
        }
    }

    /**
     * Attempts to purchase a Black Market item on behalf of the player.
     *
     * @return PurchaseResult indicating the outcome
     */
    public PurchaseResult purchaseItem(Player player, String itemId) {
        UUID uuid = player.getUniqueId();

        // Find the item in the current rotation
        BlackMarketItem item = null;
        for (BlackMarketItem bmi : currentItems) {
            if (bmi.getId().equals(itemId)) {
                item = bmi;
                break;
            }
        }
        if (item == null) return PurchaseResult.ITEM_NOT_FOUND;

        if (!item.isInStock()) return PurchaseResult.OUT_OF_STOCK;

        long balance = EconomyAPI.getInstance().getBalance(uuid);
        if (balance < item.getPriceIgc()) return PurchaseResult.INSUFFICIENT_FUNDS;

        long result = EconomyAPI.getInstance().deductBalance(uuid, item.getPriceIgc(), TransactionType.IGC_SHOP_PURCHASE);
        if (result < 0) return PurchaseResult.INSUFFICIENT_FUNDS;

        // Give item
        var stack = new org.bukkit.inventory.ItemStack(item.getMaterial(), item.getAmount());
        var overflow = player.getInventory().addItem(stack);
        if (!overflow.isEmpty()) {
            for (var leftover : overflow.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<yellow>Your inventory was full — some items were dropped at your feet."));
        }

        item.decrementStock();
        saveStockOnly();

        return PurchaseResult.OK;
    }

    // ----------------------------------------------------------------
    // YAML map helpers (maps deserialized from List<?> may have Object keys)
    // ----------------------------------------------------------------

    private static String getString(Map<?, ?> map, String key, String def) {
        Object v = map.get(key);
        return v instanceof String s ? s : def;
    }

    private static int getInt(Map<?, ?> map, String key, int def) {
        Object v = map.get(key);
        if (v instanceof Number n) return n.intValue();
        return def;
    }

    private static long getLong(Map<?, ?> map, String key, long def) {
        Object v = map.get(key);
        if (v instanceof Number n) return n.longValue();
        return def;
    }
}
