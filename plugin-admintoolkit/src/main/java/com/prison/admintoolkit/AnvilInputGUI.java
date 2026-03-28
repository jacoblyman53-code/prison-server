package com.prison.admintoolkit;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * AnvilInputGUI — collects text input from players using an anvil GUI.
 *
 * Open the GUI with open(player, placeholder, callback). The callback
 * receives the text entered (or the placeholder if nothing was changed).
 * Clicking the result slot (slot 2) confirms and fires the callback.
 */
public class AnvilInputGUI {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private record AnvilSession(Inventory inventory, Consumer<String> callback) {}

    private static final Map<UUID, AnvilSession> sessions = new ConcurrentHashMap<>();

    // ----------------------------------------------------------------
    // Open
    // ----------------------------------------------------------------

    public static void open(Player player, String placeholder, Consumer<String> callback) {
        Inventory inv = Bukkit.createInventory(null, InventoryType.ANVIL, MM.deserialize("<gray>Enter value"));

        ItemStack paper = new ItemStack(org.bukkit.Material.PAPER);
        ItemMeta meta = paper.getItemMeta();
        // Set display name to the placeholder text — player renames it to enter their value
        meta.displayName(Component.text(placeholder, NamedTextColor.WHITE));
        paper.setItemMeta(meta);

        inv.setItem(0, paper);

        sessions.put(player.getUniqueId(), new AnvilSession(inv, callback));
        player.openInventory(inv);
    }

    // ----------------------------------------------------------------
    // Query
    // ----------------------------------------------------------------

    public static boolean isAnvilSession(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    // ----------------------------------------------------------------
    // Click handler — called from AdminToolkitPlugin
    // ----------------------------------------------------------------

    public static void handleClick(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();

        // Cancel clicks in input slots (0 and 1) to prevent item removal
        if (slot != 2) {
            if (slot >= 0 && slot < 3) {
                event.setCancelled(true);
            }
            return;
        }

        // Slot 2 — result slot clicked
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

        // Close the inventory 1 tick later to avoid inventory glitch
        Bukkit.getScheduler().runTask(AdminToolkitPlugin.getInstance(), (Runnable) player::closeInventory);

        // Fire callback — may open another GUI on the next tick
        callback.accept(text);
    }

    // ----------------------------------------------------------------
    // Prepare handler — keeps result slot populated while typing
    // ----------------------------------------------------------------

    public static void handlePrepare(Player player, PrepareAnvilEvent event) {
        AnvilSession session = sessions.get(player.getUniqueId());
        if (session == null || event.getInventory() != session.inventory()) return;

        ItemStack left = event.getInventory().getItem(0);
        if (left == null) return;

        // Mirror left slot into result slot with zero repair cost
        ItemStack resultItem = left.clone();
        event.setResult(resultItem);
        ((org.bukkit.inventory.AnvilInventory) event.getInventory()).setRepairCost(0);
        event.getInventory().setItem(2, resultItem);
    }

    // ----------------------------------------------------------------
    // Close handler — called when inventory is closed without confirming
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
