package com.prison.cosmetics;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CosmeticsGUI — handles building and responding to the 54-slot cosmetics wardrobe.
 *
 * <pre>
 * Slot layout (54 slots):
 *  0  1  2  3  [4]  5  6  7  8    ← header row — slot 4 = Chat Tags tab; rest = glass
 *  9 10 11 12  13 14 15 16 17    ─┐
 * 18 19 20 21  22 23 24 25 26     │  content area (slots 9-44): up to 36 tag icons
 * 27 28 29 30  31 32 33 34 35     │
 * 36 37 38 39  40 41 42 43 44    ─┘
 * 45 46 47 48 [49] 50 51 52 53    ← footer row — slot 49 = Close button; rest = glass
 * </pre>
 *
 * Tags are laid out left-to-right, top-to-bottom starting at slot 9.
 * The first 36 tags defined in config are shown; additional tags are silently ignored
 * (pagination can be added later).
 */
public class CosmeticsGUI implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    /** Title rendered at the top of every cosmetics inventory. */
    private static final String TITLE = "<light_purple><bold>✦ Cosmetics";

    /** Tracks which players have our GUI open so we can intercept clicks precisely. */
    private final ConcurrentHashMap<UUID, Inventory> openInventories = new ConcurrentHashMap<>();

    /**
     * Maps GUI content slots (9-44) → tag ID for the currently-open inventory
     * of each player.  Rebuilt every time the GUI is opened.
     */
    private final ConcurrentHashMap<UUID, Map<Integer, String>> slotTagMap = new ConcurrentHashMap<>();

    private final CosmeticsManager manager;
    private final JavaPlugin plugin;

    CosmeticsGUI(CosmeticsManager manager, JavaPlugin plugin) {
        this.manager = manager;
        this.plugin  = plugin;
    }

    // ----------------------------------------------------------------
    // Open GUI
    // ----------------------------------------------------------------

    /**
     * Builds and opens the cosmetics GUI for the given player.
     * Must be called on the main thread.
     */
    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, MM.deserialize(TITLE));

        // --- Header row (slots 0–8) ---
        ItemStack glassHeader = grayGlass();
        for (int i = 0; i <= 8; i++) {
            inv.setItem(i, glassHeader);
        }
        // Slot 4: Chat Tags tab indicator
        inv.setItem(4, buildTabIcon());

        // --- Footer row (slots 45–53) ---
        ItemStack glassFooter = grayGlass();
        for (int i = 45; i <= 53; i++) {
            inv.setItem(i, glassFooter);
        }
        // Slot 49: Close button
        inv.setItem(49, buildCloseButton());

        // --- Content area (slots 9–44) ---
        Map<Integer, String> map = populateContent(inv, player);

        // Register tracking state BEFORE opening — InventoryClickEvent fires immediately on some builds
        openInventories.put(player.getUniqueId(), inv);
        slotTagMap.put(player.getUniqueId(), map);

        player.openInventory(inv);
    }

    /**
     * Fills the content area (slots 9–44) with tag icons.
     * Returns the slot→tagId map for click handling.
     */
    private Map<Integer, String> populateContent(Inventory inv, Player player) {
        UUID uuid = player.getUniqueId();
        Set<String> owned    = manager.getOwnedTags(uuid);
        String      equipped = manager.getEquippedTag(uuid);

        // Ensure we have a non-null set to work with
        if (owned == null) owned = Set.of();

        Collection<ChatTag> allTags = manager.getAllTagDefinitions();
        Map<Integer, String> map = new LinkedHashMap<>();

        int slot = 9;   // content starts at slot 9
        for (ChatTag tag : allTags) {
            if (slot > 44) break;  // content area full

            ItemStack icon;
            if (owned.contains(tag.id())) {
                boolean isEquipped = tag.id().equals(equipped);
                icon = buildOwnedTagIcon(tag, isEquipped);
            } else {
                icon = buildLockedTagIcon(tag);
            }

            inv.setItem(slot, icon);
            map.put(slot, tag.id());
            slot++;
        }

        // Fill remaining empty content slots with black glass to tidy up the grid
        ItemStack empty = buildItem(Material.BLACK_STAINED_GLASS_PANE, "<dark_gray> ", List.of());
        for (int s = slot; s <= 44; s++) {
            inv.setItem(s, empty);
        }

        return map;
    }

    // ----------------------------------------------------------------
    // Icon builders
    // ----------------------------------------------------------------

    /** Tab icon for the "Chat Tags" section in the header. */
    private ItemStack buildTabIcon() {
        return buildItem(
            Material.NAME_TAG,
            "<!italic><white><bold>Chat Tags",
            List.of(
                "<!italic><gray>Browse and equip your",
                "<!italic><gray>collected chat tags."
            )
        );
    }

    /** Close button in the footer. */
    private ItemStack buildCloseButton() {
        return buildItem(
            Material.BARRIER,
            "<!italic><red>Close",
            List.of("<!italic><gray>Close this menu.")
        );
    }

    /** Gray glass pane filler — no display name, no lore. */
    private ItemStack grayGlass() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta  = item.getItemMeta();
        meta.displayName(Component.empty());
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ITEM_SPECIFICS);
        item.setItemMeta(meta);
        return item;
    }

    /** Icon for a tag the player owns. */
    private ItemStack buildOwnedTagIcon(ChatTag tag, boolean equipped) {
        List<String> lore = new ArrayList<>();
        lore.add("<!italic>" + tag.rarity().loreLabel());
        lore.add("<!italic><dark_gray>" + tag.description());
        lore.add("<!italic><dark_gray> ");
        lore.add("<!italic><white>Preview: " + tag.display() + " <white>YourName");
        lore.add("<!italic><dark_gray> ");
        if (equipped) {
            lore.add("<!italic><green>✔ Equipped");
            lore.add("<!italic><gray>Click to <red>unequip</red>.");
        } else {
            lore.add("<!italic><gray>Click to <green>equip</green>.");
        }

        ItemStack icon = buildItem(tag.iconMaterial(), "<!italic>" + tag.display(), lore);

        // Add a green glowing enchant effect for equipped tags (visual cue)
        if (equipped) {
            icon.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.UNBREAKING, 1);
            ItemMeta meta = icon.getItemMeta();
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            icon.setItemMeta(meta);
        }

        return icon;
    }

    /** Icon for a tag the player does NOT own — shows as locked glass pane. */
    private ItemStack buildLockedTagIcon(ChatTag tag) {
        List<String> lore = new ArrayList<>();
        lore.add("<!italic>" + tag.rarity().loreLabel());
        lore.add("<!italic><dark_gray>" + tag.description());
        lore.add("<!italic><dark_gray> ");
        lore.add("<!italic><white>Preview: " + tag.display() + " <white>YourName");
        lore.add("<!italic><dark_gray> ");
        lore.add("<!italic><red>✘ Not owned");

        return buildItem(Material.GRAY_STAINED_GLASS_PANE, "<!italic><dark_gray>??? Locked", lore);
    }

    // ----------------------------------------------------------------
    // Click handling
    // ----------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();

        Inventory tracked = openInventories.get(uuid);
        if (tracked == null) return;

        // Make sure the click is in OUR inventory (top), not their personal inventory
        if (!event.getInventory().equals(tracked)) return;
        if (!event.getClickedInventory().equals(tracked)) {
            // They clicked their own bottom inventory — cancel to prevent item movement
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);  // never let items move

        int slot = event.getSlot();

        // Close button
        if (slot == 49) {
            player.closeInventory();
            return;
        }

        // Content area click
        Map<Integer, String> map = slotTagMap.get(uuid);
        if (map == null) return;

        String tagId = map.get(slot);
        if (tagId == null) return;  // filler slot or header/footer

        // Only act if the player owns this tag
        if (!manager.hasTag(uuid, tagId)) {
            player.sendMessage(MM.deserialize("<red>You don't own the <white>" + tagId + " <red>tag!"));
            return;
        }

        String currentlyEquipped = manager.getEquippedTag(uuid);
        if (tagId.equals(currentlyEquipped)) {
            // Already equipped — unequip
            manager.unequipTag(player);
            player.sendMessage(MM.deserialize("<gray>Unequipped tag."));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 0.8f);
        } else {
            // Equip
            manager.equipTag(player, tagId);
            ChatTag tag = manager.getTagDefinition(tagId);
            String display = tag != null ? tag.display() : tagId;
            player.sendMessage(MM.deserialize("<gray>Equipped tag: " + display));
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.2f);
        }

        // Refresh the GUI so the equipped/unequipped state is reflected immediately
        refreshGUI(player);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();
        openInventories.remove(uuid);
        slotTagMap.remove(uuid);
    }

    // ----------------------------------------------------------------
    // Refresh helper
    // ----------------------------------------------------------------

    /**
     * Rebuilds the content area of an already-open GUI in place, so the player
     * does not see the inventory flicker closed and reopened.
     */
    private void refreshGUI(Player player) {
        UUID uuid = player.getUniqueId();
        Inventory inv = openInventories.get(uuid);
        if (inv == null) return;

        Map<Integer, String> map = populateContent(inv, player);
        slotTagMap.put(uuid, map);
        player.updateInventory();
    }

    // ----------------------------------------------------------------
    // Generic ItemStack builder
    // ----------------------------------------------------------------

    /**
     * Builds an ItemStack with a MiniMessage display name, lore, and all
     * standard flags applied (HIDE_ATTRIBUTES, HIDE_ITEM_SPECIFICS).
     *
     * @param material    Item material.
     * @param nameTag     MiniMessage string for the display name.
     * @param loreTags    List of MiniMessage strings for each lore line.
     */
    private ItemStack buildItem(Material material, String nameTag, List<String> loreTags) {
        ItemStack item = new ItemStack(material);
        ItemMeta  meta = item.getItemMeta();

        meta.displayName(MM.deserialize(nameTag));

        if (!loreTags.isEmpty()) {
            List<Component> loreComponents = new ArrayList<>(loreTags.size());
            for (String line : loreTags) {
                loreComponents.add(MM.deserialize(line));
            }
            meta.lore(loreComponents);
        }

        meta.addItemFlags(
            ItemFlag.HIDE_ATTRIBUTES,
            ItemFlag.HIDE_ITEM_SPECIFICS,
            ItemFlag.HIDE_ENCHANTS,
            ItemFlag.HIDE_UNBREAKABLE
        );

        item.setItemMeta(meta);
        return item;
    }
}
