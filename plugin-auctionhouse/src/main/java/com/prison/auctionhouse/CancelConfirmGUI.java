package com.prison.auctionhouse;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CancelConfirmGUI — 27-slot confirmation before cancelling a listing.
 *
 * Slot 11 = Yes, Cancel (green pane)
 * Slot 13 = Item preview
 * Slot 15 = Keep Listing (gray pane)
 *
 * Borders are filled with red/orange glass panes to make the danger clear.
 */
public class CancelConfirmGUI {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    static final Component TITLE = MM.deserialize("<red><bold>Cancel Listing?");

    private static final Map<UUID, AuctionListing> pendingCancel = new ConcurrentHashMap<>();

    // ----------------------------------------------------------------
    // Open
    // ----------------------------------------------------------------

    public static void open(Player player, AuctionListing listing) {
        pendingCancel.put(player.getUniqueId(), listing);

        Inventory inv = Bukkit.createInventory(null, 27, TITLE);

        // Fill borders with red glass panes
        ItemStack redPane = makeFiller(Material.RED_STAINED_GLASS_PANE);
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, redPane.clone());
        }

        // Override centre row slots with orange for contrast
        ItemStack orangePane = makeFiller(Material.ORANGE_STAINED_GLASS_PANE);
        for (int i = 9; i < 18; i++) {
            inv.setItem(i, orangePane.clone());
        }

        // Slot 13: Item preview
        ItemStack preview = listing.item().clone();
        ItemMeta pm = preview.getItemMeta();
        if (pm == null) pm = Bukkit.getItemFactory().getItemMeta(preview.getType());

        if (!pm.hasDisplayName()) {
            pm.displayName(MM.deserialize("<white>" +
                AuctionManager.formatMaterialName(listing.item().getType().name())));
        }
        List<Component> previewLore = pm.lore() != null ? new ArrayList<>(pm.lore()) : new ArrayList<>();
        previewLore.add(Component.empty());
        previewLore.add(MM.deserialize("<yellow>Price: <gold>$" +
            String.format("%,d", listing.priceIgc())));
        previewLore.add(MM.deserialize("<gray>This will be cancelled."));
        previewLore.add(MM.deserialize("<red>Listing fee is non-refundable."));
        previewLore.add(MM.deserialize("<green>Item will be returned."));
        pm.lore(previewLore);
        preview.setItemMeta(pm);
        inv.setItem(13, preview);

        // Slot 11: Confirm cancel
        ItemStack confirm = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta cm = confirm.getItemMeta();
        cm.displayName(MM.deserialize("<green>Yes, Cancel Listing"));
        List<Component> confirmLore = new ArrayList<>();
        confirmLore.add(MM.deserialize("<gray>Remove this listing and get item back."));
        cm.lore(confirmLore);
        confirm.setItemMeta(cm);
        inv.setItem(11, confirm);

        // Slot 15: Keep listing
        ItemStack keep = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta km = keep.getItemMeta();
        km.displayName(MM.deserialize("<gray>Keep Listing"));
        List<Component> keepLore = new ArrayList<>();
        keepLore.add(MM.deserialize("<gray>Go back without cancelling."));
        km.lore(keepLore);
        keep.setItemMeta(km);
        inv.setItem(15, keep);

        player.openInventory(inv);
    }

    // ----------------------------------------------------------------
    // isTitle
    // ----------------------------------------------------------------

    public static boolean isTitle(Component title) {
        return TITLE.equals(title);
    }

    // ----------------------------------------------------------------
    // handleClick
    // ----------------------------------------------------------------

    public static void handleClick(Player player, int slot) {
        UUID uuid = player.getUniqueId();
        AuctionListing listing = pendingCancel.remove(uuid);

        if (listing == null) {
            player.closeInventory();
            return;
        }

        if (slot == 11) {
            // Confirm cancel
            AuctionManager.CancelResult result =
                AuctionManager.getInstance().cancelListing(player, listing.id());

            player.closeInventory();

            switch (result) {
                case CANCEL_OK -> {
                    // Message already sent by cancelListing
                }
                case LISTING_NOT_FOUND ->
                    player.sendMessage(MM.deserialize("<red>That listing could not be found."));
                case NOT_OWNER ->
                    player.sendMessage(MM.deserialize("<red>You do not own that listing."));
            }
        } else if (slot == 15) {
            // Keep listing — go back to My Listings
            player.closeInventory();
            Bukkit.getScheduler().runTask(AuctionHousePlugin.getInstance(), (Runnable) () ->
                MyListingsGUI.open(player)
            );
        }
        // Any other slot click just does nothing (borders are purely decorative)
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private static ItemStack makeFiller(Material material) {
        ItemStack pane = new ItemStack(material);
        ItemMeta meta  = pane.getItemMeta();
        meta.displayName(Component.empty());
        pane.setItemMeta(meta);
        return pane;
    }

    // ----------------------------------------------------------------
    // Cleanup
    // ----------------------------------------------------------------

    public static void cleanup(UUID uuid) {
        pendingCancel.remove(uuid);
    }
}
