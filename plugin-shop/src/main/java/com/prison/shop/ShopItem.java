package com.prison.shop;

import org.bukkit.inventory.ItemStack;

public record ShopItem(
    String id,
    String displayName,   // MiniMessage string, or null to use item's default name
    long priceIgc,
    int stock,            // -1 = unlimited, 0 = sold out, positive = remaining
    ItemStack item,       // the actual item to give on purchase
    boolean sellable      // whether players can right-click sell this item
) {
    public boolean isInStock() {
        return stock == -1 || stock > 0;
    }
}
