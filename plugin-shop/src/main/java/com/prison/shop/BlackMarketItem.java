package com.prison.shop;

import org.bukkit.Material;

import java.util.List;

/**
 * Represents a single item in the Black Market item pool.
 * Stock is mutable so it can be decremented during a rotation without rebuilding the list.
 */
public class BlackMarketItem {

    private final String id;
    private final String display;        // MiniMessage string
    private final List<String> lore;     // MiniMessage strings
    private final Material material;
    private final int amount;
    private final long priceIgc;
    private final int baseStock;         // stock assigned at rotation time
    private int currentStock;            // decremented on purchase

    public BlackMarketItem(
            String id,
            String display,
            List<String> lore,
            Material material,
            int amount,
            long priceIgc,
            int stock
    ) {
        this.id = id;
        this.display = display;
        this.lore = List.copyOf(lore);
        this.material = material;
        this.amount = amount;
        this.priceIgc = priceIgc;
        this.baseStock = stock;
        this.currentStock = stock;
    }

    // ----------------------------------------------------------------
    // Accessors
    // ----------------------------------------------------------------

    public String getId() {
        return id;
    }

    public String getDisplay() {
        return display;
    }

    public List<String> getLore() {
        return lore;
    }

    public Material getMaterial() {
        return material;
    }

    public int getAmount() {
        return amount;
    }

    public long getPriceIgc() {
        return priceIgc;
    }

    public int getBaseStock() {
        return baseStock;
    }

    public int getCurrentStock() {
        return currentStock;
    }

    public boolean isInStock() {
        return currentStock > 0;
    }

    // ----------------------------------------------------------------
    // Mutators (called during purchase)
    // ----------------------------------------------------------------

    public void decrementStock() {
        if (currentStock > 0) {
            currentStock--;
        }
    }

    /** Resets stock to base value — called at rotation time. */
    public void resetStock() {
        currentStock = baseStock;
    }

    /** Overwrite current stock directly (used when loading a saved rotation). */
    public void setCurrentStock(int stock) {
        this.currentStock = stock;
    }
}
