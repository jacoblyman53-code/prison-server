package com.prison.crates;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * CrateReward — one entry in a crate's reward pool.
 *
 * @param type        What kind of reward this is.
 * @param amount      IGC or token amount (used for IGC / TOKEN types).
 * @param crateKeyTier  The crate tier id for CRATE_KEY rewards (null otherwise).
 * @param crateKeyAmount  Number of keys for CRATE_KEY rewards.
 * @param item        The serialized ItemStack for ITEM rewards (null otherwise).
 * @param weight      Relative drop weight — higher means more common.
 * @param broadcast   Whether winning this reward broadcasts to the whole server.
 * @param displayName MiniMessage display name shown in the GUI.
 */
public record CrateReward(
        RewardType type,
        long amount,
        String crateKeyTier,
        int crateKeyAmount,
        ItemStack item,
        int weight,
        boolean broadcast,
        String displayName
) {
    private static final MiniMessage MM = MiniMessage.miniMessage();

    /**
     * Build a display ItemStack for use in the spinning GUI.
     * The item returned is a clone — safe to modify.
     */
    public ItemStack buildDisplayItem() {
        ItemStack display;

        if (type == RewardType.ITEM && item != null) {
            display = item.clone();
        } else {
            Material mat = switch (type) {
                case IGC       -> Material.GOLD_INGOT;
                case TOKEN     -> Material.EMERALD;
                case CRATE_KEY -> Material.TRIPWIRE_HOOK;
                case ITEM      -> Material.PAPER;
            };
            display = new ItemStack(mat);
        }

        ItemMeta meta = display.getItemMeta();
        if (meta == null) return display;

        meta.displayName(MM.deserialize(displayName));
        meta.lore(List.of(
            MM.deserialize("<dark_gray>Weight: " + weight),
            broadcast ? MM.deserialize("<gold>✦ Broadcast on win!") : MM.deserialize("<gray>No broadcast")
        ));
        display.setItemMeta(meta);
        return display;
    }
}
