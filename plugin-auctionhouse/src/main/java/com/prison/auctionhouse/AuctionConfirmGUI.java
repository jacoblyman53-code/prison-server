package com.prison.auctionhouse;

import com.prison.economy.EconomyAPI;
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
 * AuctionConfirmGUI — 27-slot purchase confirmation dialog.
 *
 * Slot 11 = Confirm (green pane, or gray if can't afford)
 * Slot 13 = Item preview with price info
 * Slot 15 = Cancel (red pane)
 */
public class AuctionConfirmGUI {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    static final Component TITLE = MM.deserialize("<green><bold>Confirm Purchase");

    // Pending purchases per player UUID
    private static final Map<UUID, AuctionListing> pendingPurchase = new ConcurrentHashMap<>();

    // Filler
    private static ItemStack makeFiller(Material material) {
        ItemStack pane = new ItemStack(material);
        ItemMeta meta  = pane.getItemMeta();
        meta.displayName(Component.empty());
        pane.setItemMeta(meta);
        return pane;
    }

    // ----------------------------------------------------------------
    // Open
    // ----------------------------------------------------------------

    public static void open(Player player, AuctionListing listing) {
        pendingPurchase.put(player.getUniqueId(), listing);

        Inventory inv = Bukkit.createInventory(null, 27, TITLE);

        // Fill all slots with cyan glass panes
        ItemStack cyan = makeFiller(Material.CYAN_STAINED_GLASS_PANE);
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, cyan.clone());
        }

        long playerBalance = EconomyAPI.getInstance().getBalance(player.getUniqueId());
        long price         = listing.priceIgc();
        boolean canAfford  = playerBalance >= price;

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
        previewLore.add(MM.deserialize("<yellow>Price: <gold>" + String.format("%,d", price) + " IGC"));
        previewLore.add(MM.deserialize("<gray>Seller: <white>" + listing.sellerName()));
        previewLore.add(MM.deserialize("<gray>Your Balance: <white>" +
            String.format("%,d", playerBalance) + " IGC"));
        previewLore.add(Component.empty());
        previewLore.add(MM.deserialize("<green>Confirm: slot 11 <gray>| <red>Cancel: slot 15"));
        pm.lore(previewLore);
        preview.setItemMeta(pm);
        inv.setItem(13, preview);

        // Slot 11: Confirm or Cannot Afford
        if (canAfford) {
            ItemStack confirm = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
            ItemMeta cm = confirm.getItemMeta();
            cm.displayName(MM.deserialize("<green>✓ Confirm Purchase"));
            List<Component> confirmLore = new ArrayList<>();
            confirmLore.add(MM.deserialize("<gold>" + String.format("%,d", price) +
                " IGC<gray> will be deducted."));
            confirmLore.add(MM.deserialize("<gray>Balance after: <white>" +
                String.format("%,d", playerBalance - price) + " IGC"));
            cm.lore(confirmLore);
            confirm.setItemMeta(cm);
            inv.setItem(11, confirm);
        } else {
            ItemStack cantAfford = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta cam = cantAfford.getItemMeta();
            cam.displayName(MM.deserialize("<red>Cannot Afford"));
            List<Component> caLore = new ArrayList<>();
            caLore.add(MM.deserialize("<red>You need <gold>" + String.format("%,d", price) +
                " IGC<red>."));
            caLore.add(MM.deserialize("<gray>You have: <white>" +
                String.format("%,d", playerBalance) + " IGC"));
            cam.lore(caLore);
            cantAfford.setItemMeta(cam);
            inv.setItem(11, cantAfford);
        }

        // Slot 15: Cancel
        ItemStack cancel = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta cancM = cancel.getItemMeta();
        cancM.displayName(MM.deserialize("<red>✗ Cancel"));
        List<Component> cancelLore = new ArrayList<>();
        cancelLore.add(MM.deserialize("<gray>Go back without purchasing."));
        cancM.lore(cancelLore);
        cancel.setItemMeta(cancM);
        inv.setItem(15, cancel);

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
        UUID uuid    = player.getUniqueId();
        AuctionListing listing = pendingPurchase.get(uuid);

        if (listing == null) {
            player.closeInventory();
            return;
        }

        if (slot == 11) {
            // Check that slot 11 is the green confirm pane (not gray "cannot afford")
            ItemStack slotItem = player.getOpenInventory().getTopInventory().getItem(11);
            if (slotItem == null || slotItem.getType() != Material.LIME_STAINED_GLASS_PANE) {
                // Can't afford — inform and close
                player.sendMessage(MM.deserialize("<red>You cannot afford this listing."));
                pendingPurchase.remove(uuid);
                player.closeInventory();
                return;
            }

            // Attempt purchase
            AuctionManager.PurchaseResult result =
                AuctionManager.getInstance().purchaseListing(player, listing.id());

            pendingPurchase.remove(uuid);

            switch (result) {
                case PURCHASE_OK -> {
                    // Success message sent by purchaseListing async callback
                    player.closeInventory();
                }
                case LISTING_NOT_FOUND, LISTING_GONE -> {
                    player.sendMessage(MM.deserialize(
                        "<red>This listing is no longer available."));
                    player.closeInventory();
                    // Refresh and re-open AH on next tick
                    Bukkit.getScheduler().runTask(AuctionHousePlugin.getInstance(), (Runnable) () ->
                        AuctionGUI.open(player, 0, null)
                    );
                }
                case CANNOT_BUY_OWN ->
                    player.sendMessage(MM.deserialize(
                        "<red>You cannot buy your own listing."));
                case INSUFFICIENT_FUNDS ->
                    player.sendMessage(MM.deserialize("<red>Insufficient funds."));
                case INVENTORY_FULL ->
                    player.sendMessage(MM.deserialize(
                        "<red>Your inventory is full. Free up space and try again."));
            }

            if (result != AuctionManager.PurchaseResult.PURCHASE_OK &&
                result != AuctionManager.PurchaseResult.LISTING_NOT_FOUND &&
                result != AuctionManager.PurchaseResult.LISTING_GONE) {
                player.closeInventory();
            }

        } else if (slot == 15) {
            // Cancel — go back to AH
            pendingPurchase.remove(uuid);
            player.closeInventory();
            Bukkit.getScheduler().runTask(AuctionHousePlugin.getInstance(), (Runnable) () ->
                AuctionGUI.open(player, 0, null)
            );
        }
    }

    // ----------------------------------------------------------------
    // Cleanup
    // ----------------------------------------------------------------

    public static void cleanup(UUID uuid) {
        pendingPurchase.remove(uuid);
    }
}
