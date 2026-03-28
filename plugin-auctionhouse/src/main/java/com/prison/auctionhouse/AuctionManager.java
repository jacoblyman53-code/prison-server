package com.prison.auctionhouse;

import com.prison.database.DatabaseManager;
import com.prison.economy.EconomyAPI;
import com.prison.economy.TransactionType;
import com.prison.permissions.PermissionEngine;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * AuctionManager — all business logic for the auction house.
 *
 * Called from the main thread for balance/inventory operations.
 * DB writes are dispatched async. Cache is refreshed async.
 */
public class AuctionManager {

    public enum CreateResult {
        CREATE_OK, NO_PERMISSION, INVALID_ITEM, PRICE_TOO_LOW,
        LISTING_LIMIT_REACHED, INSUFFICIENT_FUNDS
    }

    public enum PurchaseResult {
        PURCHASE_OK, LISTING_NOT_FOUND, CANNOT_BUY_OWN,
        INSUFFICIENT_FUNDS, LISTING_GONE
    }

    public enum CancelResult {
        CANCEL_OK, LISTING_NOT_FOUND, NOT_OWNER
    }

    // ----------------------------------------------------------------
    // Singleton
    // ----------------------------------------------------------------

    private static AuctionManager instance;

    public static AuctionManager getInstance() { return instance; }

    public static void initialize(JavaPlugin plugin) {
        instance = new AuctionManager(plugin);
    }

    // ----------------------------------------------------------------
    // Fields
    // ----------------------------------------------------------------

    private final JavaPlugin plugin;
    private volatile List<AuctionListing> listingCache = Collections.emptyList();

    // Config values — loaded by AuctionHousePlugin from config.yml
    double listingFeePercent      = 5.0;
    int    listingDurationHours   = 48;
    int    maxListingsDefault     = 3;
    int    maxListingsDonor       = 5;
    int    maxListingsDonorPlus   = 8;
    int    maxListingsElite       = 12;
    int    maxListingsElitePlus   = 16;

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private AuctionManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // ----------------------------------------------------------------
    // Config helpers
    // ----------------------------------------------------------------

    public int getMaxListingsForPlayer(UUID uuid) {
        String rank = PermissionEngine.getInstance().getDonorRank(uuid);
        if (rank == null) return maxListingsDefault;
        return switch (rank.toLowerCase()) {
            case "donorplus"  -> maxListingsDonorPlus;
            case "donor"      -> maxListingsDonor;
            case "elite"      -> maxListingsElite;
            case "eliteplus"  -> maxListingsElitePlus;
            default           -> maxListingsDefault;
        };
    }

    // ----------------------------------------------------------------
    // Cache accessors
    // ----------------------------------------------------------------

    public List<AuctionListing> getActiveListings() {
        return listingCache;
    }

    public List<AuctionListing> getActiveListings(Material filter) {
        if (filter == null) return listingCache;
        List<AuctionListing> result = new ArrayList<>();
        for (AuctionListing l : listingCache) {
            if (l.item().getType() == filter) result.add(l);
        }
        return Collections.unmodifiableList(result);
    }

    public AuctionListing getActiveListing(long id) {
        for (AuctionListing l : listingCache) {
            if (l.id() == id) return l;
        }
        return null;
    }

    public long getPlayerActiveListingCount(UUID uuid) {
        return listingCache.stream()
            .filter(l -> l.sellerUuid().equals(uuid) && "ACTIVE".equals(l.status()))
            .count();
    }

    // ----------------------------------------------------------------
    // Cache refresh (async-safe)
    // ----------------------------------------------------------------

    public void refreshCache() {
        try {
            List<AuctionListing> listings = DatabaseManager.getInstance().query(
                "SELECT id, seller_uuid, seller_name, item_data, price_igc, listed_at, expires_at, status " +
                "FROM auction_listings WHERE status = 'ACTIVE' ORDER BY listed_at DESC",
                rs -> {
                    List<AuctionListing> list = new ArrayList<>();
                    while (rs.next()) {
                        byte[] itemBytes = rs.getBytes("item_data");
                        ItemStack item;
                        try {
                            item = ItemStack.deserializeBytes(itemBytes);
                        } catch (Exception e) {
                            plugin.getLogger().log(Level.WARNING,
                                "[AuctionHouse] Skipping corrupted listing id=" + rs.getLong("id"), e);
                            continue;
                        }
                        list.add(new AuctionListing(
                            rs.getLong("id"),
                            UUID.fromString(rs.getString("seller_uuid")),
                            rs.getString("seller_name"),
                            item,
                            rs.getLong("price_igc"),
                            rs.getTimestamp("listed_at").toLocalDateTime(),
                            rs.getTimestamp("expires_at").toLocalDateTime(),
                            rs.getString("status")
                        ));
                    }
                    return list;
                }
            );
            this.listingCache = Collections.unmodifiableList(listings);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[AuctionHouse] Failed to refresh listing cache", e);
        }
    }

    // ----------------------------------------------------------------
    // Create listing (called from main thread)
    // ----------------------------------------------------------------

    public CreateResult createListing(Player player, ItemStack item, long price) {
        UUID uuid = player.getUniqueId();

        // Validate item
        if (item == null || item.getType().isAir()) {
            player.sendMessage(MM.deserialize("<red>You must be holding a valid item to list."));
            return CreateResult.INVALID_ITEM;
        }

        // Validate price
        if (price < 1) {
            player.sendMessage(MM.deserialize("<red>Price must be at least 1 IGC."));
            return CreateResult.PRICE_TOO_LOW;
        }

        // Permission check
        if (!PermissionEngine.getInstance().hasPermission(uuid, "prison.sell")) {
            player.sendMessage(MM.deserialize("<red>You do not have permission to list items."));
            return CreateResult.NO_PERMISSION;
        }

        // Listing limit check
        int maxListings = getMaxListingsForPlayer(uuid);
        if (getPlayerActiveListingCount(uuid) >= maxListings) {
            player.sendMessage(MM.deserialize(
                "<red>You have reached your listing limit (<gold>" + maxListings + "</gold>). " +
                "Cancel a listing or wait for one to expire."));
            return CreateResult.LISTING_LIMIT_REACHED;
        }

        // Fee calculation
        long fee = Math.max(1L, Math.round(price * listingFeePercent / 100.0));

        // Balance check
        if (EconomyAPI.getInstance().getBalance(uuid) < fee) {
            player.sendMessage(MM.deserialize(
                "<red>You need <gold>" + String.format("%,d", fee) +
                " IGC</gold> as a listing fee but cannot afford it."));
            return CreateResult.INSUFFICIENT_FUNDS;
        }

        // Deduct fee on main thread before async DB work
        long result = EconomyAPI.getInstance().deductBalance(uuid, fee, TransactionType.AUCTION_PURCHASE);
        if (result < 0) {
            player.sendMessage(MM.deserialize("<red>Insufficient funds for listing fee."));
            return CreateResult.INSUFFICIENT_FUNDS;
        }

        // Serialize item
        byte[] itemBytes;
        try {
            itemBytes = item.serializeAsBytes();
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[AuctionHouse] Failed to serialize item for " + player.getName(), e);
            // Refund fee since we couldn't list
            EconomyAPI.getInstance().addBalance(uuid, fee, TransactionType.AUCTION_SALE);
            player.sendMessage(MM.deserialize("<red>Failed to serialize item. Please try again."));
            return CreateResult.INVALID_ITEM;
        }

        // Remove item from inventory on main thread
        String itemDisplayName = getItemDisplayName(item);
        ItemStack toInsert = item.clone();
        player.getInventory().setItemInMainHand(null);

        // Async: INSERT into DB then refresh cache
        String sellerName = player.getName();
        int durationHours = this.listingDurationHours;

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                DatabaseManager.getInstance().executeAndGetId(
                    "INSERT INTO auction_listings (seller_uuid, seller_name, item_data, price_igc, expires_at) " +
                    "VALUES (?, ?, ?, ?, DATE_ADD(NOW(), INTERVAL ? HOUR))",
                    uuid.toString(), sellerName, itemBytes, price, durationHours
                );
                refreshCache();
                plugin.getLogger().fine("[AuctionHouse] " + sellerName + " listed " + itemDisplayName + " for " + price + " IGC");
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "[AuctionHouse] Failed to insert listing for " + sellerName, e);
                // Try to refund the fee and return the item — schedule on main thread
                Bukkit.getScheduler().runTask(plugin, (Runnable) () -> {
                    EconomyAPI.getInstance().addBalance(uuid, fee, TransactionType.AUCTION_SALE);
                    Map<Integer, ItemStack> overflow = player.getInventory().addItem(toInsert);
                    if (!overflow.isEmpty()) {
                        for (ItemStack drop : overflow.values()) {
                            player.getWorld().dropItemNaturally(player.getLocation(), drop);
                        }
                    }
                    player.sendMessage(MM.deserialize("<red>Failed to create listing. Item and fee have been returned."));
                });
            }
        });

        player.sendMessage(MM.deserialize(
            "<green>Listed <white>" + itemDisplayName + "</white> for <gold>" +
            String.format("%,d", price) + " IGC</gold><green>. Listing fee: <gold>" +
            String.format("%,d", fee) + " IGC</gold><green>."));
        return CreateResult.CREATE_OK;
    }

    // ----------------------------------------------------------------
    // Purchase listing (called from main thread)
    // ----------------------------------------------------------------

    public PurchaseResult purchaseListing(Player buyer, long listingId) {
        UUID buyerUuid = buyer.getUniqueId();

        // Find in cache
        AuctionListing listing = getActiveListing(listingId);
        if (listing == null) return PurchaseResult.LISTING_NOT_FOUND;

        // Cannot buy own listing
        if (listing.sellerUuid().equals(buyerUuid)) return PurchaseResult.CANNOT_BUY_OWN;

        // Balance check
        if (EconomyAPI.getInstance().getBalance(buyerUuid) < listing.priceIgc()) {
            return PurchaseResult.INSUFFICIENT_FUNDS;
        }

        // Deduct from buyer on main thread
        long deductResult = EconomyAPI.getInstance().deductBalance(
            buyerUuid, listing.priceIgc(), TransactionType.AUCTION_PURCHASE);
        if (deductResult < 0) return PurchaseResult.INSUFFICIENT_FUNDS;

        // Snapshot values for async use
        long price          = listing.priceIgc();
        UUID sellerUuid     = listing.sellerUuid();
        ItemStack itemCopy  = listing.item().clone();
        String itemName     = getItemDisplayName(listing.item());

        // Async DB work
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Try to atomically claim the listing
                int rowsAffected = DatabaseManager.getInstance().execute(
                    "UPDATE auction_listings SET status='SOLD', seller_notified=0 " +
                    "WHERE id=? AND status='ACTIVE'",
                    listingId
                );

                if (rowsAffected == 0) {
                    // Another buyer got it first — refund
                    Bukkit.getScheduler().runTask(plugin, (Runnable) () -> {
                        EconomyAPI.getInstance().addBalance(buyerUuid, price, TransactionType.AUCTION_SALE);
                        buyer.sendMessage(MM.deserialize(
                            "<red>That listing is no longer available. Your funds have been refunded."));
                    });
                    refreshCache();
                    return;
                }

                // Record transaction
                DatabaseManager.getInstance().execute(
                    "INSERT INTO auction_transactions (listing_id, seller_uuid, buyer_uuid, price_igc) " +
                    "VALUES (?, ?, ?, ?)",
                    listingId, sellerUuid.toString(), buyerUuid.toString(), price
                );

                // Pay the seller
                Player sellerOnline = Bukkit.getPlayer(sellerUuid);
                if (sellerOnline != null) {
                    Bukkit.getScheduler().runTask(plugin, (Runnable) () ->
                        EconomyAPI.getInstance().addBalance(sellerUuid, price, TransactionType.AUCTION_SALE)
                    );
                } else {
                    // Offline seller: direct DB update
                    DatabaseManager.getInstance().execute(
                        "UPDATE players SET igc_balance = igc_balance + ? WHERE uuid = ?",
                        price, sellerUuid.toString()
                    );
                    // seller_notified stays 0 so they get notified on next join
                }

                // Give item to buyer on main thread
                Bukkit.getScheduler().runTask(plugin, (Runnable) () -> {
                    Map<Integer, ItemStack> overflow = buyer.getInventory().addItem(itemCopy);
                    if (!overflow.isEmpty()) {
                        for (ItemStack drop : overflow.values()) {
                            buyer.getWorld().dropItemNaturally(buyer.getLocation(), drop);
                        }
                        buyer.sendMessage(MM.deserialize(
                            "<yellow>Inventory full — item dropped at your feet!"));
                    }
                    buyer.sendMessage(MM.deserialize(
                        "<green>Purchased <white>" + itemName + "</white> for <gold>" +
                        String.format("%,d", price) + " IGC</gold><green>!"));
                });

                refreshCache();

            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE,
                    "[AuctionHouse] Failed to process purchase of listing " + listingId, e);
                // Refund buyer
                Bukkit.getScheduler().runTask(plugin, (Runnable) () -> {
                    EconomyAPI.getInstance().addBalance(buyerUuid, price, TransactionType.AUCTION_SALE);
                    buyer.sendMessage(MM.deserialize(
                        "<red>Purchase failed due to a server error. Funds refunded."));
                });
            }
        });

        return PurchaseResult.PURCHASE_OK;
    }

    // ----------------------------------------------------------------
    // Cancel listing (called from main thread)
    // ----------------------------------------------------------------

    public CancelResult cancelListing(Player owner, long listingId) {
        UUID ownerUuid = owner.getUniqueId();

        AuctionListing listing = getActiveListing(listingId);
        if (listing == null) return CancelResult.LISTING_NOT_FOUND;
        if (!listing.sellerUuid().equals(ownerUuid)) return CancelResult.NOT_OWNER;

        ItemStack itemCopy = listing.item().clone();

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                DatabaseManager.getInstance().execute(
                    "UPDATE auction_listings SET status='REMOVED' " +
                    "WHERE id=? AND seller_uuid=? AND status='ACTIVE'",
                    listingId, ownerUuid.toString()
                );
                refreshCache();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE,
                    "[AuctionHouse] Failed to cancel listing " + listingId, e);
            }
        });

        // Return item on main thread (we're already on main thread here)
        Map<Integer, ItemStack> overflow = owner.getInventory().addItem(itemCopy);
        if (!overflow.isEmpty()) {
            for (ItemStack drop : overflow.values()) {
                owner.getWorld().dropItemNaturally(owner.getLocation(), drop);
            }
            owner.sendMessage(MM.deserialize("<yellow>Inventory full — item dropped at your feet!"));
        }
        owner.sendMessage(MM.deserialize("<green>Listing cancelled. Item returned to your inventory."));
        return CancelResult.CANCEL_OK;
    }

    // ----------------------------------------------------------------
    // Admin remove (marks REMOVED, returns item if seller online)
    // ----------------------------------------------------------------

    public void adminRemoveListing(long listingId, String removedByName) {
        AuctionListing listing = getActiveListing(listingId);
        if (listing == null) return;

        ItemStack itemCopy  = listing.item().clone();
        UUID sellerUuid     = listing.sellerUuid();

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                DatabaseManager.getInstance().execute(
                    "UPDATE auction_listings SET status='REMOVED', seller_notified=0 WHERE id=? AND status='ACTIVE'",
                    listingId
                );
                refreshCache();

                // Return item if seller is online, otherwise let login delivery handle it
                Player sellerOnline = Bukkit.getPlayer(sellerUuid);
                if (sellerOnline != null) {
                    Bukkit.getScheduler().runTask(plugin, (Runnable) () -> {
                        Map<Integer, ItemStack> overflow = sellerOnline.getInventory().addItem(itemCopy);
                        if (!overflow.isEmpty()) {
                            for (ItemStack drop : overflow.values()) {
                                sellerOnline.getWorld().dropItemNaturally(sellerOnline.getLocation(), drop);
                            }
                            sellerOnline.sendMessage(MM.deserialize(
                                "<yellow>Inventory full — item dropped at your feet!"));
                        }
                        sellerOnline.sendMessage(MM.deserialize(
                            "<red>Your listing was removed by an admin (<white>" +
                            removedByName + "</white>). Item returned."));
                        // Mark as notified
                        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                            try {
                                DatabaseManager.getInstance().execute(
                                    "UPDATE auction_listings SET seller_notified=1 WHERE id=?", listingId);
                            } catch (SQLException ex) {
                                plugin.getLogger().log(Level.WARNING,
                                    "[AuctionHouse] Failed to mark admin-removed listing notified", ex);
                            }
                        });
                    });
                }
                // If offline, seller_notified=0 means they'll be notified on next login
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE,
                    "[AuctionHouse] Failed to admin-remove listing " + listingId, e);
            }
        });
    }

    // ----------------------------------------------------------------
    // Expire listings (runs async every 60s)
    // ----------------------------------------------------------------

    public void processExpiredListings() {
        try {
            // Mark expired listings
            int count = DatabaseManager.getInstance().execute(
                "UPDATE auction_listings SET status='EXPIRED', seller_notified=0 " +
                "WHERE status='ACTIVE' AND expires_at < NOW()"
            );
            if (count > 0) {
                plugin.getLogger().info("[AuctionHouse] Expired " + count + " listing(s).");
                refreshCache();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[AuctionHouse] Failed to process expired listings", e);
        }
    }

    // ----------------------------------------------------------------
    // Deliver expired/sold items on player join (async → main thread)
    // ----------------------------------------------------------------

    public void deliverExpiredItemsAndNotify(Player player) {
        UUID uuid = player.getUniqueId();

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Fetch all unnotified, non-active listings for this seller
                record DeliveryEntry(long id, ItemStack item, long price, String status) {}
                List<DeliveryEntry> entries = DatabaseManager.getInstance().query(
                    "SELECT id, item_data, price_igc, status FROM auction_listings " +
                    "WHERE seller_uuid = ? AND seller_notified = 0 AND status IN ('EXPIRED', 'REMOVED', 'SOLD')",
                    rs -> {
                        List<DeliveryEntry> list = new ArrayList<>();
                        while (rs.next()) {
                            long   id     = rs.getLong("id");
                            long   price  = rs.getLong("price_igc");
                            String status = rs.getString("status");

                            byte[] itemBytes = rs.getBytes("item_data");
                            ItemStack item = null;
                            if (!"SOLD".equals(status)) {
                                try {
                                    item = ItemStack.deserializeBytes(itemBytes);
                                } catch (Exception e) {
                                    plugin.getLogger().log(Level.WARNING,
                                        "[AuctionHouse] Corrupted item in listing id=" + id + ", skipping", e);
                                }
                            }
                            list.add(new DeliveryEntry(id, item, price, status));
                        }
                        return list;
                    },
                    uuid.toString()
                );

                if (entries.isEmpty()) return;

                // Separate items to return vs sold notifications
                List<DeliveryEntry> toReturn  = new ArrayList<>();
                List<DeliveryEntry> soldNotify = new ArrayList<>();
                List<Long> processedIds       = new ArrayList<>();

                for (DeliveryEntry entry : entries) {
                    if ("SOLD".equals(entry.status())) {
                        soldNotify.add(entry);
                        processedIds.add(entry.id());
                    } else if (entry.item() != null) {
                        toReturn.add(entry);
                        processedIds.add(entry.id());
                    }
                }

                // Deliver on main thread
                Bukkit.getScheduler().runTask(plugin, (Runnable) () -> {
                    if (!player.isOnline()) return;

                    for (DeliveryEntry entry : toReturn) {
                        Map<Integer, ItemStack> overflow =
                            player.getInventory().addItem(entry.item());
                        if (!overflow.isEmpty()) {
                            for (ItemStack drop : overflow.values()) {
                                player.getWorld().dropItemNaturally(player.getLocation(), drop);
                            }
                            player.sendMessage(MM.deserialize(
                                "<yellow>Inventory full — expired auction item dropped at your feet!"));
                        } else {
                            player.sendMessage(MM.deserialize(
                                "<yellow>Your expired auction item has been returned to your inventory."));
                        }
                    }

                    for (DeliveryEntry entry : soldNotify) {
                        // We don't have item name easily here — just use generic message
                        player.sendMessage(MM.deserialize(
                            "<green>Your auction sold for <gold>" +
                            String.format("%,d", entry.price()) + " IGC</gold><green>!"));
                    }

                    // Mark all processed as notified async
                    if (!processedIds.isEmpty()) {
                        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                            for (long id : processedIds) {
                                try {
                                    DatabaseManager.getInstance().execute(
                                        "UPDATE auction_listings SET seller_notified=1 " +
                                        "WHERE id=? AND seller_uuid=?",
                                        id, uuid.toString()
                                    );
                                } catch (SQLException ex) {
                                    plugin.getLogger().log(Level.WARNING,
                                        "[AuctionHouse] Failed to mark listing " + id + " as notified", ex);
                                }
                            }
                        });
                    }
                });

            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE,
                    "[AuctionHouse] Failed to deliver items to " + player.getName(), e);
            }
        });
    }

    // ----------------------------------------------------------------
    // Utility
    // ----------------------------------------------------------------

    /**
     * Returns the display name of an item, falling back to a formatted
     * version of the material name if no custom name is set.
     */
    public static String getItemDisplayName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer plain =
                net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText();
            net.kyori.adventure.text.Component displayName = item.getItemMeta().displayName();
            if (displayName != null) return plain.serialize(displayName);
        }
        return formatMaterialName(item.getType().name());
    }

    /**
     * Converts MATERIAL_NAME → "Material Name".
     */
    public static String formatMaterialName(String name) {
        String[] words = name.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!sb.isEmpty()) sb.append(' ');
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)));
                sb.append(word.substring(1));
            }
        }
        return sb.toString();
    }
}
