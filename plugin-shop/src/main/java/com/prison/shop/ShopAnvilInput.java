package com.prison.shop;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * ShopAnvilInput — collects text input from players using an anvil GUI.
 * Self-contained copy of the admin toolkit pattern, isolated to plugin-shop.
 */
public class ShopAnvilInput {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private record AnvilSession(Inventory inventory, Consumer<String> callback) {}

    private static final Map<UUID, AnvilSession> sessions = new ConcurrentHashMap<>();

    // ----------------------------------------------------------------
    // Open
    // ----------------------------------------------------------------

    public static void open(Player player, String placeholder, Consumer<String> callback) {
        Inventory inv = Bukkit.createInventory(null, InventoryType.ANVIL, MM.deserialize("<gray>Enter value"));

        ItemStack paper = new ItemStack(Material.PAPER);
        ItemMeta meta = paper.getItemMeta();
        meta.displayName(Component.text(placeholder, NamedTextColor.WHITE));
        paper.setItemMeta(meta);

        inv.setItem(0, paper);

        sessions.put(player.getUniqueId(), new AnvilSession(inv, callback));
        player.openInventory(inv);
    }

    // ----------------------------------------------------------------
    // Query
    // ----------------------------------------------------------------

    public static boolean isSession(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    // ----------------------------------------------------------------
    // Click handler
    // ----------------------------------------------------------------

    public static void handleClick(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();

        if (slot != 2) {
            if (slot >= 0 && slot < 3) {
                event.setCancelled(true);
            }
            return;
        }

        event.setCancelled(true);

        ItemStack result = event.getCurrentItem();
        if (result == null || !result.hasItemMeta()) return;

        ItemMeta meta = result.getItemMeta();
        if (meta.displayName() == null) return;

        String text = PlainTextComponentSerializer.plainText().serialize(meta.displayName());

        UUID uuid = player.getUniqueId();
        AnvilSession session = sessions.remove(uuid);
        if (session == null) return;

        Consumer<String> callback = session.callback();

        // Close 1 tick later to avoid inventory glitch
        Bukkit.getScheduler().runTask(ShopPlugin.getInstance(), (Runnable) player::closeInventory);

        callback.accept(text);
    }

    // ----------------------------------------------------------------
    // Prepare handler — keeps result slot populated while typing
    // ----------------------------------------------------------------

    @SuppressWarnings("removal")
    public static void handlePrepare(Player player, PrepareAnvilEvent event) {
        AnvilSession session = sessions.get(player.getUniqueId());
        if (session == null || event.getInventory() != session.inventory()) return;

        ItemStack left = event.getInventory().getItem(0);
        if (left == null) return;

        ItemStack resultItem = left.clone();
        event.setResult(resultItem);
        ((org.bukkit.inventory.AnvilInventory) event.getInventory()).setRepairCost(0);
        event.getInventory().setItem(2, resultItem);
    }

    // ----------------------------------------------------------------
    // Close handler
    // ----------------------------------------------------------------

    public static void handleClose(Player player) {
        sessions.remove(player.getUniqueId());
    }

    // ----------------------------------------------------------------
    // Cleanup on quit
    // ----------------------------------------------------------------

    public static void cleanup(UUID uuid) {
        sessions.remove(uuid);
    }
}
