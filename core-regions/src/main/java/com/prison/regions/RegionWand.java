package com.prison.regions;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * RegionWand — tracks per-player corner selections for region creation.
 *
 * The wand is a golden axe with a PDC tag marking it as the region tool.
 * Left-click sets corner 1, right-click sets corner 2.
 * When both corners are set, the GUI opens automatically.
 *
 * Particle effects show corner markers so admins can see what they selected.
 */
public class RegionWand {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final org.bukkit.NamespacedKey wandKey;

    // Per-player selections: corner1 and corner2
    private final Map<UUID, Location> corner1 = new HashMap<>();
    private final Map<UUID, Location> corner2 = new HashMap<>();

    public RegionWand(Plugin plugin) {
        this.wandKey = new org.bukkit.NamespacedKey(plugin, "region_wand");
    }

    /**
     * Create and return the wand item.
     */
    public ItemStack createWand() {
        ItemStack item = new ItemStack(Material.GOLDEN_AXE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(MM.deserialize("<gold><bold>Region Wand"));
        meta.lore(List.of(
            MM.deserialize("<gray>Left-click: set corner 1"),
            MM.deserialize("<gray>Right-click: set corner 2"),
            MM.deserialize("<yellow>Both corners set: GUI opens")
        ));
        // PDC tag so we can identify this item in the event listener
        meta.getPersistentDataContainer().set(wandKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Returns true if the held item is the region wand.
     */
    public boolean isWand(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer()
                   .has(wandKey, PersistentDataType.BYTE);
    }

    /**
     * Set corner 1 for a player and show particle feedback.
     */
    public void setCorner1(Player player, Location loc) {
        corner1.put(player.getUniqueId(), loc.toBlockLocation());
        player.sendMessage(MM.deserialize("<green>Corner 1 set at <white>"
            + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ()));
        spawnCornerParticle(player, loc, Particle.HAPPY_VILLAGER);
        notifyIfBothSet(player);
    }

    /**
     * Set corner 2 for a player and show particle feedback.
     */
    public void setCorner2(Player player, Location loc) {
        corner2.put(player.getUniqueId(), loc.toBlockLocation());
        player.sendMessage(MM.deserialize("<green>Corner 2 set at <white>"
            + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ()));
        spawnCornerParticle(player, loc, Particle.HAPPY_VILLAGER);
        notifyIfBothSet(player);
    }

    private void notifyIfBothSet(Player player) {
        if (hasBothCorners(player)) {
            player.sendMessage(MM.deserialize("<yellow>Both corners set. Use <white>/rg create <name></white> to create the region."));
        }
    }

    public boolean hasBothCorners(Player player) {
        return corner1.containsKey(player.getUniqueId())
            && corner2.containsKey(player.getUniqueId());
    }

    public Location getCorner1(Player player) { return corner1.get(player.getUniqueId()); }
    public Location getCorner2(Player player) { return corner2.get(player.getUniqueId()); }

    public void clearSelection(Player player) {
        corner1.remove(player.getUniqueId());
        corner2.remove(player.getUniqueId());
    }

    private void spawnCornerParticle(Player player, Location loc, Particle particle) {
        loc.getWorld().spawnParticle(particle,
            loc.getBlockX() + 0.5, loc.getBlockY() + 1.0, loc.getBlockZ() + 0.5,
            20, 0.3, 0.3, 0.3, 0);
    }
}
