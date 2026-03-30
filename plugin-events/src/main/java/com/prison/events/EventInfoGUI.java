package com.prison.events;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 27-slot read-only GUI showing all currently active events.
 *
 * <p>Each active event occupies one slot as a GREEN_STAINED_GLASS_PANE with
 * the event's display name and remaining-time lore. If no events are active,
 * all content slots show a GRAY_STAINED_GLASS_PANE with "No events active".
 * Slot 26 (bottom-right) is always a red "Close" button.
 */
public final class EventInfoGUI {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    /** Title component used to recognise this GUI in click events. */
    private static final Component TITLE = MM.deserialize("<!italic><gold><bold>Active Events");

    private EventInfoGUI() {}

    // -------------------------------------------------------------------------
    // Open
    // -------------------------------------------------------------------------

    public static void open(Player player, EventManager manager) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);
        populate(inv, manager.getActiveEvents());
        player.openInventory(inv);
    }

    private static void populate(Inventory inv, Collection<ActiveEvent> active) {
        // Border: fill all slots with black glass
        ItemStack border = makeItem(Material.BLACK_STAINED_GLASS_PANE,
                "<!italic><dark_gray> ", List.of());
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, border);
        }

        // Content area: slots 10-16 (row 2, columns 2-8 of a 27-slot chest)
        int[] contentSlots = {10, 11, 12, 13, 14, 15, 16};

        if (active.isEmpty()) {
            ItemStack noEvents = makeItem(
                    Material.GRAY_STAINED_GLASS_PANE,
                    "<!italic><gray>No events active",
                    List.of("<!italic><dark_gray>Check back later!")
            );
            for (int slot : contentSlots) {
                inv.setItem(slot, noEvents);
            }
        } else {
            List<ActiveEvent> list = new ArrayList<>(active);
            for (int i = 0; i < contentSlots.length; i++) {
                if (i < list.size()) {
                    inv.setItem(contentSlots[i], buildEventItem(list.get(i)));
                } else {
                    // Remaining slots stay as border
                    break;
                }
            }
        }

        // Close button — slot 26 (bottom-right)
        ItemStack close = makeItem(
                Material.RED_STAINED_GLASS_PANE,
                "<!italic><red><bold>Close",
                List.of("<!italic><gray>Click to close this menu.")
        );
        inv.setItem(26, close);
    }

    // -------------------------------------------------------------------------
    // Click handling
    // -------------------------------------------------------------------------

    /**
     * Handles a click inside this GUI. Only the close button at slot 26 does
     * anything — everything else is cancelled to prevent item theft.
     */
    public static void handleClick(Player player, InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getRawSlot() == 26) {
            player.closeInventory();
        }
    }

    /**
     * Returns {@code true} if the given inventory title belongs to this GUI.
     */
    public static boolean isTitle(Component title) {
        return TITLE.equals(title);
    }

    // -------------------------------------------------------------------------
    // Item builders
    // -------------------------------------------------------------------------

    private static ItemStack buildEventItem(ActiveEvent active) {
        EventConfig cfg = active.config();
        int mins = active.remainingMinutes();

        // Choose glass colour by event type
        Material mat = switch (cfg.type()) {
            case SELL_BOOST   -> Material.LIME_STAINED_GLASS_PANE;
            case TOKEN_STORM  -> Material.CYAN_STAINED_GLASS_PANE;
            case JACKPOT_HOUR -> Material.PURPLE_STAINED_GLASS_PANE;
        };

        // Build lore
        List<String> lore = new ArrayList<>();
        lore.add("<!italic><gray>Type: <white>" + cfg.type().name());
        lore.add("<!italic><gray>Multiplier: <white>x" + cfg.multiplier());
        lore.add("<!italic><gray>Time remaining: <yellow>" + mins + " minute" + (mins == 1 ? "" : "s"));

        return makeItem(mat, "<!italic>" + cfg.displayName(), lore);
    }

    /**
     * Creates a named, lored {@link ItemStack} with all item flags hidden and
     * unbreakable so it presents cleanly in a GUI.
     */
    private static ItemStack makeItem(Material material, String displayMiniMsg, List<String> loreMiniMsg) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(MM.deserialize(displayMiniMsg));

        if (!loreMiniMsg.isEmpty()) {
            List<Component> loreComponents = new ArrayList<>();
            for (String line : loreMiniMsg) {
                loreComponents.add(MM.deserialize(line));
            }
            meta.lore(loreComponents);
        }

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);

        item.setItemMeta(meta);
        return item;
    }
}
