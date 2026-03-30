package com.prison.crates;

import com.prison.database.DatabaseManager;
import com.prison.economy.EconomyAPI;
import com.prison.economy.TransactionType;
import com.prison.permissions.PermissionEngine;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * CrateManager — owns crate configuration, block locations, key DB operations,
 * and reward delivery.
 */
public class CrateManager {

    private static CrateManager instance;

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final Logger logger;
    private final CratePlugin plugin;

    /** All configured crate tiers by ID. */
    private Map<String, CrateTier> tiers = new LinkedHashMap<>();
    /** Crate block locations → tier id. */
    private final Map<String, String> crateBlocks = new LinkedHashMap<>(); // "world:x:y:z" → tier id

    private CrateManager(CratePlugin plugin, Logger logger) {
        this.plugin = plugin;
        this.logger = logger;
        instance = this;
    }

    public static CrateManager initialize(CratePlugin plugin, Logger logger) {
        if (instance != null) throw new IllegalStateException("CrateManager already initialized");
        return new CrateManager(plugin, logger);
    }

    public static CrateManager getInstance() { return instance; }

    public static void reset() { instance = null; }

    // ----------------------------------------------------------------
    // Config loading
    // ----------------------------------------------------------------

    public void loadConfig() {
        plugin.reloadConfig();
        tiers = loadTiers();
        crateBlocks.clear();

        List<String> blockEntries = plugin.getConfig().getStringList("crate-blocks");
        for (String entry : blockEntries) {
            // format: "world:x:y:z:tier"
            String[] parts = entry.split(":");
            if (parts.length < 5) continue;
            String locKey = parts[0] + ":" + parts[1] + ":" + parts[2] + ":" + parts[3];
            String tierId = parts[4];
            crateBlocks.put(locKey, tierId);
        }

        logger.info("[Crates] Loaded " + tiers.size() + " tiers, " + crateBlocks.size() + " crate blocks.");
    }

    private Map<String, CrateTier> loadTiers() {
        Map<String, CrateTier> result = new LinkedHashMap<>();
        ConfigurationSection cratesSec = plugin.getConfig().getConfigurationSection("crates");
        if (cratesSec == null) return result;

        for (String tierId : cratesSec.getKeys(false)) {
            ConfigurationSection tierSec = cratesSec.getConfigurationSection(tierId);
            if (tierSec == null) continue;

            String displayName  = tierSec.getString("display-name", capitalize(tierId) + " Crate");
            String keyMaterialStr = tierSec.getString("key-material", "TRIPWIRE_HOOK");
            String keyColor     = tierSec.getString("key-color", "<white>");

            Material keyMaterial;
            try {
                keyMaterial = Material.valueOf(keyMaterialStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                keyMaterial = Material.TRIPWIRE_HOOK;
            }

            List<CrateReward> rewards = new ArrayList<>();
            List<?> rewardList = tierSec.getList("rewards");
            if (rewardList != null) {
                for (Object obj : rewardList) {
                    if (!(obj instanceof Map<?, ?> map)) continue;
                    CrateReward reward = parseReward(map);
                    if (reward != null) rewards.add(reward);
                }
            }

            result.put(tierId, new CrateTier(tierId, displayName, keyMaterial, keyColor, rewards));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private CrateReward parseReward(Map<?, ?> map) {
        String typeStr = getString(map, "type", "IGC").toUpperCase();
        RewardType type;
        try {
            type = RewardType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            return null;
        }

        long amount      = getLong(map, "amount", 0L);
        String keyTier   = getString(map, "tier", null);
        int keyAmount    = getInt(map, "key-amount", 1);
        int weight       = getInt(map, "weight", 1);
        boolean broadcast = getBool(map, "broadcast", false);
        String displayName = getString(map, "display-name", type.name());
        String command   = getString(map, "command", null);

        ItemStack item = null;
        if (type == RewardType.ITEM) {
            String base64 = getString(map, "item", null);
            if (base64 != null) {
                try {
                    byte[] bytes = Base64.getDecoder().decode(base64);
                    item = ItemStack.deserializeBytes(bytes);
                } catch (Exception e) {
                    logger.warning("[Crates] Failed to deserialize item for reward: " + displayName);
                }
            }
        }

        return new CrateReward(type, amount, keyTier, keyAmount, item, command, weight, broadcast, displayName);
    }

    // ----------------------------------------------------------------
    // Crate block management
    // ----------------------------------------------------------------

    public String getTierAtBlock(Location loc) {
        String key = locKey(loc);
        return crateBlocks.get(key);
    }

    public void addCrateBlock(Location loc, String tierId) {
        String key = locKey(loc);
        crateBlocks.put(key, tierId);
        saveCrateBlocks();
    }

    public void removeCrateBlock(Location loc) {
        crateBlocks.remove(locKey(loc));
        saveCrateBlocks();
    }

    private void saveCrateBlocks() {
        List<String> entries = new ArrayList<>();
        for (Map.Entry<String, String> e : crateBlocks.entrySet()) {
            entries.add(e.getKey() + ":" + e.getValue());
        }
        plugin.getConfig().set("crate-blocks", entries);
        plugin.saveConfig();
    }

    // ----------------------------------------------------------------
    // Key DB operations
    // ----------------------------------------------------------------

    /**
     * Queue keys in the DB for a player. If the player is online, also give
     * them physical key items directly.
     */
    public CompletableFuture<Void> giveKeys(UUID uuid, String tierId, int count) {
        CrateTier tier = tiers.get(tierId);
        if (tier == null) return CompletableFuture.completedFuture(null);

        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            // Deliver directly to inventory
            deliverKeyItems(online, tier, count);
            return CompletableFuture.completedFuture(null);
        }

        // Queue in DB for offline player
        return CompletableFuture.runAsync(() -> {
            try {
                DatabaseManager.getInstance().execute(
                    "INSERT INTO crate_keys (player_uuid, crate_tier, quantity) VALUES (?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE quantity = quantity + VALUES(quantity)",
                    uuid.toString(), tierId, count
                );
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "[Crates] Failed to queue keys for " + uuid, e);
            }
        });
    }

    /**
     * Called on player login — deliver any queued keys and clear the DB rows.
     */
    public void deliverPendingKeys(Player player) {
        CompletableFuture.runAsync(() -> {
            try {
                List<Object[]> rows = DatabaseManager.getInstance().query(
                    "SELECT crate_tier, quantity FROM crate_keys WHERE player_uuid = ? AND quantity > 0",
                    rs -> {
                        List<Object[]> list = new ArrayList<>();
                        while (rs.next()) {
                            list.add(new Object[]{ rs.getString("crate_tier"), rs.getInt("quantity") });
                        }
                        return list;
                    },
                    player.getUniqueId().toString()
                );

                if (rows.isEmpty()) return;

                DatabaseManager.getInstance().execute(
                    "DELETE FROM crate_keys WHERE player_uuid = ?",
                    player.getUniqueId().toString()
                );

                // Deliver on main thread
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!player.isOnline()) return;
                    for (Object[] row : rows) {
                        String tierId = (String) row[0];
                        int qty       = (int) row[1];
                        CrateTier tier = tiers.get(tierId);
                        if (tier != null) {
                            deliverKeyItems(player, tier, qty);
                            player.sendMessage(MM.deserialize(
                                "<green>You received <white>" + qty + "x " + tier.keyColor()
                                + tier.displayName() + " Key<white><green>!"));
                        }
                    }
                });

            } catch (SQLException e) {
                logger.log(Level.SEVERE, "[Crates] Failed to deliver pending keys for " + player.getName(), e);
            }
        });
    }

    private void deliverKeyItems(Player player, CrateTier tier, int count) {
        ItemStack key = CrateKey.createKey(tier);
        key.setAmount(Math.min(count, 64));
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(key);
        if (!overflow.isEmpty()) {
            // Drop at feet
            player.getWorld().dropItemNaturally(player.getLocation(), overflow.get(0));
            player.sendMessage(MM.deserialize("<yellow>Your inventory was full — key dropped at your feet."));
        }
        // If count > 64, give remaining in a second pass
        if (count > 64) {
            deliverKeyItems(player, tier, count - 64);
        }
    }

    // ----------------------------------------------------------------
    // Crate opening
    // ----------------------------------------------------------------

    /**
     * Attempt to open a crate. The player must be holding the correct key item.
     * Removes one key from their hand, starts the animation, delivers the reward.
     */
    public void tryOpenCrate(Player player, Location crateLoc) {
        String tierId = getTierAtBlock(crateLoc);
        if (tierId == null) return;

        CrateTier tier = tiers.get(tierId);
        if (tier == null) {
            player.sendMessage(MM.deserialize("<red>This crate tier is not configured."));
            return;
        }

        if (!PermissionEngine.getInstance().hasPermission(player, "prison.crate.use")) {
            player.sendMessage(MM.deserialize("<red>You don't have permission to use crates."));
            return;
        }

        if (CrateOpeningSession.isOpening(player.getUniqueId())) {
            player.sendMessage(MM.deserialize("<red>A crate is already opening!"));
            return;
        }

        if (!tier.hasRewards()) {
            player.sendMessage(MM.deserialize("<red>This crate has no rewards configured yet."));
            return;
        }

        // Find a valid key in the player's inventory
        ItemStack key = findKey(player, tierId);
        if (key == null) {
            player.sendMessage(MM.deserialize("<red>You need a " + tier.keyColor() + tier.displayName()
                + " Key</red><red> to open this crate."));
            return;
        }

        // Consume the key
        consumeKey(key);

        // Roll the reward
        CrateReward reward = tier.rollReward();

        // Start animation, then deliver on completion
        new CrateOpeningSession(player, tier, reward, won -> deliverReward(player, tier, won), plugin).start();
    }

    /**
     * Deliver a won reward to a player (called after the animation ends).
     */
    public void deliverReward(Player player, CrateTier tier, CrateReward reward) {
        EconomyAPI eco = EconomyAPI.getInstance();
        boolean broadcastWin = reward.broadcast();

        switch (reward.type()) {
            case IGC -> {
                eco.addBalance(player.getUniqueId(), reward.amount(), TransactionType.CRATE_REWARD);
                player.sendMessage(MM.deserialize(
                    "<green>You won <gold>" + String.format("%,d", reward.amount()) + " IGC</gold><green>!"));
            }
            case TOKEN -> {
                eco.addTokens(player.getUniqueId(), reward.amount(), TransactionType.CRATE_REWARD);
                player.sendMessage(MM.deserialize(
                    "<green>You won <aqua>" + reward.amount() + " Tokens</aqua><green>!"));
            }
            case CRATE_KEY -> {
                String keyTierId = reward.crateKeyTier();
                int keyCount     = Math.max(1, reward.crateKeyAmount());
                giveKeys(player.getUniqueId(), keyTierId, keyCount);
                player.sendMessage(MM.deserialize(
                    "<green>You won <white>" + keyCount + "x " + reward.displayName() + "</white><green>!"));
            }
            case ITEM -> {
                if (reward.item() != null) {
                    ItemStack drop = reward.item().clone();
                    Map<Integer, ItemStack> overflow = player.getInventory().addItem(drop);
                    if (!overflow.isEmpty()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), overflow.get(0));
                        player.sendMessage(MM.deserialize("<yellow>Inventory full — item dropped at your feet."));
                    }
                    player.sendMessage(MM.deserialize(
                        "<green>You won <white>" + reward.displayName() + "</white><green>!"));
                }
            }
            case COMMAND -> {
                if (reward.command() != null && !reward.command().isBlank()) {
                    String cmd = reward.command().replace("{player}", player.getName());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                    player.sendMessage(MM.deserialize(
                        "<green>You won <white>" + reward.displayName() + "</white><green>!"));
                }
            }
        }

        // Log to DB
        DatabaseManager.getInstance().queueWrite(
            "INSERT INTO crate_logs (player_uuid, crate_tier, reward) VALUES (?, ?, ?)",
            player.getUniqueId().toString(), tier.id(), reward.displayName()
        );

        // Broadcast rare/legendary wins
        if (broadcastWin) {
            Bukkit.broadcast(MM.deserialize(
                "<gold><bold>[Crates]</bold></gold> <aqua>" + player.getName() + "</aqua> won "
                + "<white>" + reward.displayName() + "</white> from a "
                + tier.keyColor() + tier.displayName() + "</> crate!"));
        }
    }

    // ----------------------------------------------------------------
    // Simulate (for admin testing)
    // ----------------------------------------------------------------

    /**
     * Simulate {@code count} openings of a tier and return a frequency map
     * of displayName → count. Runs synchronously — only call from async context.
     */
    public Map<String, Integer> simulate(String tierId, int count) {
        CrateTier tier = tiers.get(tierId);
        if (tier == null || !tier.hasRewards()) return Map.of();

        Map<String, Integer> results = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            CrateReward r = tier.rollReward();
            if (r != null) results.merge(r.displayName(), 1, Integer::sum);
        }
        return results;
    }

    // ----------------------------------------------------------------
    // Accessors
    // ----------------------------------------------------------------

    public Map<String, CrateTier> getTiers() { return Collections.unmodifiableMap(tiers); }

    public CrateTier getTier(String id) { return tiers.get(id); }

    public Map<String, String> getCrateBlocks() { return Collections.unmodifiableMap(crateBlocks); }

    // ----------------------------------------------------------------
    // Internal helpers
    // ----------------------------------------------------------------

    private ItemStack findKey(Player player, String tierId) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) continue;
            if (tierId.equals(CrateKey.getTierIdFromKey(item))) return item;
        }
        return null;
    }

    private void consumeKey(ItemStack key) {
        if (key.getAmount() > 1) {
            key.setAmount(key.getAmount() - 1);
        } else {
            key.setType(Material.AIR);
        }
    }

    private static String locKey(Location loc) {
        return loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }

    // ---- map helpers ----

    private static String getString(Map<?, ?> map, String key, String def) {
        Object v = map.get(key);
        return v instanceof String s ? s : def;
    }

    private static long getLong(Map<?, ?> map, String key, long def) {
        Object v = map.get(key);
        if (v instanceof Number n) return n.longValue();
        return def;
    }

    private static int getInt(Map<?, ?> map, String key, int def) {
        Object v = map.get(key);
        if (v instanceof Number n) return n.intValue();
        return def;
    }

    private static boolean getBool(Map<?, ?> map, String key, boolean def) {
        Object v = map.get(key);
        if (v instanceof Boolean b) return b;
        return def;
    }
}
