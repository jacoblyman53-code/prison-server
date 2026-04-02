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
    private static final Component TITLE = MM.deserialize("<!italic>ACTIVE EVENTS");

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
        // Content area: slots 10-16 (row 2, columns 2-8 of a 27-slot chest)
        int[] contentSlots = {10, 11, 12, 13, 14, 15, 16};

        if (active.isEmpty()) {
            ItemStack noEvents = makeItem(
                    Material.GRAY_STAINED_GLASS_PANE,
                    "<!italic><gray>No Events Active",
                    List.of(
                        "<!italic><gray>Check back <green>later<gray>!",
                        "<!italic>",
                        "<!italic><gray>→ No events are currently running."
                    )
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
                    // Remaining slots stay empty
                    break;
                }
            }
        }

        // Close button — slot 0 (top-left)
        ItemStack close = makeItem(
                Material.BARRIER,
                "<!italic><red>✗ Close",
                List.of("<!italic><gray>Click to close this menu.")
        );
        inv.setItem(0, close);
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
        lore.add("<!italic><aqua>✦ <gray>Type: <gray>" + cfg.type().name());
        lore.add("<!italic><aqua>✦ <gray>Multiplier: <green>" + cfg.multiplier() + "x");
        lore.add("<!italic><aqua>✦ <gray>Ends in: <gray>" + mins + " minute" + (mins == 1 ? "" : "s"));
        lore.add("<!italic>");
        lore.add("<!italic><green>→ Click for event details.");

        return makeItem(mat, "<!italic><aqua>" + cfg.displayName(), lore);
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
