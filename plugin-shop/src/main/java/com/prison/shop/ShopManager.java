package com.prison.shop;

import com.prison.database.DatabaseManager;
import com.prison.economy.EconomyAPI;
import com.prison.economy.TransactionType;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.logging.Logger;

public class ShopManager {

    public enum PurchaseResult {
        OK, ITEM_NOT_FOUND, OUT_OF_STOCK, INSUFFICIENT_FUNDS, INVENTORY_ERROR
    }

    private static ShopManager instance;

    private final JavaPlugin plugin;
    private final Logger logger;
    private List<ShopCategory> categories = new ArrayList<>();

    private ShopManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public static void initialize(JavaPlugin plugin) {
        instance = new ShopManager(plugin);
    }

    public static ShopManager getInstance() {
        return instance;
    }

    // ----------------------------------------------------------------
    // Load / Save
    // ----------------------------------------------------------------

    public void loadCategories() {
        List<ShopCategory> loaded = new ArrayList<>();
        ConfigurationSection cats = plugin.getConfig().getConfigurationSection("categories");
        if (cats == null) {
            this.categories = loaded;
            return;
        }

        for (String catKey : cats.getKeys(false)) {
            try {
                ConfigurationSection catSec = cats.getConfigurationSection(catKey);
                if (catSec == null) continue;

                String display = catSec.getString("display", "<white>" + catKey);
                String iconStr = catSec.getString("icon", "CHEST");

                Material icon;
                try {
                    icon = Material.valueOf(iconStr);
                } catch (IllegalArgumentException e) {
                    logger.warning("[Shop] Unknown material '" + iconStr + "' for category '" + catKey + "', defaulting to CHEST");
                    icon = Material.CHEST;
                }

                List<ShopItem> items = new ArrayList<>();
                ConfigurationSection itemsSec = catSec.getConfigurationSection("items");
                if (itemsSec != null) {
                    for (String itemKey : itemsSec.getKeys(false)) {
                        try {
                            ConfigurationSection itemSec = itemsSec.getConfigurationSection(itemKey);
                            if (itemSec == null) continue;

                            String itemDisplay = itemSec.getString("display"); // nullable
                            long price = itemSec.getLong("price", 0L);
                            int stock = itemSec.getInt("stock", -1);
                            String b64 = itemSec.getString("item-data");

                            if (b64 == null || b64.isEmpty()) {
                                logger.warning("[Shop] Item '" + itemKey + "' in category '" + catKey + "' has no item-data, skipping.");
                                continue;
                            }

                            byte[] bytes = Base64.getDecoder().decode(b64);
                            ItemStack stack = ItemStack.deserializeBytes(bytes);

                            items.add(new ShopItem(itemKey, itemDisplay, price, stock, stack));
                        } catch (Exception e) {
                            logger.warning("[Shop] Failed to load item '" + itemKey + "' in category '" + catKey + "': " + e.getMessage());
                        }
                    }
                }

                loaded.add(new ShopCategory(catKey, display, icon, items));
            } catch (Exception e) {
                logger.warning("[Shop] Failed to load category '" + catKey + "': " + e.getMessage());
            }
        }

        this.categories = loaded;
    }

    public void saveCategories() {
        plugin.getConfig().set("categories", null);

        for (ShopCategory cat : categories) {
            String catPath = "categories." + cat.id();
            plugin.getConfig().set(catPath + ".display", cat.displayName());
            plugin.getConfig().set(catPath + ".icon", cat.icon().name());

            for (ShopItem item : cat.items()) {
                String itemPath = catPath + ".items." + item.id();
                plugin.getConfig().set(itemPath + ".display", item.displayName());
                plugin.getConfig().set(itemPath + ".price", item.priceIgc());
                plugin.getConfig().set(itemPath + ".stock", item.stock());
                plugin.getConfig().set(itemPath + ".item-data",
                    Base64.getEncoder().encodeToString(item.item().serializeAsBytes()));
            }
        }

        plugin.saveConfig();
    }

    // ----------------------------------------------------------------
    // Queries
    // ----------------------------------------------------------------

    public List<ShopCategory> getCategories() {
        return List.copyOf(categories);
    }

    public Optional<ShopCategory> getCategory(String id) {
        return categories.stream().filter(c -> c.id().equals(id)).findFirst();
    }

    public Optional<ShopItem> getItem(String catId, String itemId) {
        return getCategory(catId)
            .flatMap(cat -> cat.items().stream().filter(i -> i.id().equals(itemId)).findFirst());
    }

    // ----------------------------------------------------------------
    // Purchase
    // ----------------------------------------------------------------

    public PurchaseResult purchase(Player player, String catId, String itemId) {
        UUID uuid = player.getUniqueId();

        Optional<ShopCategory> catOpt = getCategory(catId);
        if (catOpt.isEmpty()) return PurchaseResult.ITEM_NOT_FOUND;
        ShopCategory cat = catOpt.get();

        int itemIdx = -1;
        ShopItem item = null;
        List<ShopItem> items = cat.items();
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).id().equals(itemId)) {
                itemIdx = i;
                item = items.get(i);
                break;
            }
        }
        if (item == null) return PurchaseResult.ITEM_NOT_FOUND;

        if (!item.isInStock()) return PurchaseResult.OUT_OF_STOCK;

        if (EconomyAPI.getInstance().getBalance(uuid) < item.priceIgc()) {
            return PurchaseResult.INSUFFICIENT_FUNDS;
        }

        // Refuse purchase if inventory is full — don't charge money for items that would drop
        int freeSlots = 0;
        for (ItemStack slot : player.getInventory().getStorageContents()) {
            if (slot == null) freeSlots++;
        }
        if (freeSlots == 0) return PurchaseResult.INVENTORY_ERROR;

        long result = EconomyAPI.getInstance().deductBalance(uuid, item.priceIgc(), TransactionType.IGC_SHOP_PURCHASE);
        if (result < 0) return PurchaseResult.INSUFFICIENT_FUNDS;

        // Give item
        player.getInventory().addItem(item.item().clone());

        // Log purchase
        final String logItemId = catId + ":" + itemId;
        final long logPrice = item.priceIgc();
        DatabaseManager.getInstance().queueWrite(
            "INSERT INTO igc_shop_purchases (player_uuid, item_id, price_igc) VALUES (?, ?, ?)",
            uuid.toString(), logItemId, logPrice
        );

        // Decrement limited stock
        if (item.stock() != -1) {
            List<ShopItem> newItems = new ArrayList<>(cat.items());
            ShopItem updated = new ShopItem(item.id(), item.displayName(), item.priceIgc(), item.stock() - 1, item.item());
            newItems.set(itemIdx, updated);

            int catIdx = -1;
            for (int i = 0; i < categories.size(); i++) {
                if (categories.get(i).id().equals(catId)) {
                    catIdx = i;
                    break;
                }
            }
            ShopCategory updatedCat = new ShopCategory(cat.id(), cat.displayName(), cat.icon(), newItems);
            List<ShopCategory> newCats = new ArrayList<>(categories);
            newCats.set(catIdx, updatedCat);
            categories = newCats;
            saveCategories();
        }

        return PurchaseResult.OK;
    }

    // ----------------------------------------------------------------
    // Admin: Categories
    // ----------------------------------------------------------------

    public boolean addCategory(String id, String displayName, Material icon) {
        if (getCategory(id).isPresent()) return false;
        if (!id.matches("[a-z0-9_]+")) return false;

        List<ShopCategory> newCats = new ArrayList<>(categories);
        newCats.add(new ShopCategory(id, displayName, icon, new ArrayList<>()));
        categories = newCats;
        saveCategories();
        return true;
    }

    public void removeCategory(String id) {
        List<ShopCategory> newCats = new ArrayList<>(categories);
        newCats.removeIf(c -> c.id().equals(id));
        categories = newCats;
        saveCategories();
    }

    // ----------------------------------------------------------------
    // Admin: Items
    // ----------------------------------------------------------------

    public boolean addItem(String catId, ItemStack stack, String displayName, long price, int stock) {
        Optional<ShopCategory> catOpt = getCategory(catId);
        if (catOpt.isEmpty()) return false;
        ShopCategory cat = catOpt.get();

        // Generate ID from display name or material
        String base;
        if (displayName != null && !displayName.isBlank()) {
            // Strip MiniMessage tags for the slug
            base = displayName.replaceAll("<[^>]+>", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        } else {
            base = stack.getType().name().toLowerCase().replace('_', '_');
        }
        if (base.isBlank()) base = "item";

        String id = base;
        int suffix = 2;
        Set<String> existingIds = new HashSet<>();
        for (ShopItem existing : cat.items()) existingIds.add(existing.id());
        while (existingIds.contains(id)) {
            id = base + "_" + suffix++;
        }

        ShopItem newItem = new ShopItem(id, displayName, price, stock, stack.clone());

        List<ShopItem> newItems = new ArrayList<>(cat.items());
        newItems.add(newItem);

        replaceCategory(new ShopCategory(cat.id(), cat.displayName(), cat.icon(), newItems));
        saveCategories();
        return true;
    }

    public void removeItem(String catId, String itemId) {
        getCategory(catId).ifPresent(cat -> {
            List<ShopItem> newItems = new ArrayList<>(cat.items());
            newItems.removeIf(i -> i.id().equals(itemId));
            replaceCategory(new ShopCategory(cat.id(), cat.displayName(), cat.icon(), newItems));
            saveCategories();
        });
    }

    public void updateItemPrice(String catId, String itemId, long newPrice) {
        getCategory(catId).ifPresent(cat -> {
            List<ShopItem> newItems = new ArrayList<>(cat.items());
            for (int i = 0; i < newItems.size(); i++) {
                if (newItems.get(i).id().equals(itemId)) {
                    ShopItem old = newItems.get(i);
                    newItems.set(i, new ShopItem(old.id(), old.displayName(), newPrice, old.stock(), old.item()));
                    break;
                }
            }
            replaceCategory(new ShopCategory(cat.id(), cat.displayName(), cat.icon(), newItems));
            saveCategories();
        });
    }

    public void updateItemStock(String catId, String itemId, int newStock) {
        getCategory(catId).ifPresent(cat -> {
            List<ShopItem> newItems = new ArrayList<>(cat.items());
            for (int i = 0; i < newItems.size(); i++) {
                if (newItems.get(i).id().equals(itemId)) {
                    ShopItem old = newItems.get(i);
                    newItems.set(i, new ShopItem(old.id(), old.displayName(), old.priceIgc(), newStock, old.item()));
                    break;
                }
            }
            replaceCategory(new ShopCategory(cat.id(), cat.displayName(), cat.icon(), newItems));
            saveCategories();
        });
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private void replaceCategory(ShopCategory updated) {
        List<ShopCategory> newCats = new ArrayList<>(categories);
        for (int i = 0; i < newCats.size(); i++) {
            if (newCats.get(i).id().equals(updated.id())) {
                newCats.set(i, updated);
                break;
            }
        }
        categories = newCats;
    }
}
