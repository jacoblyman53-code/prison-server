package com.prison.menu;

import com.prison.cosmetics.ChatTag;
import com.prison.cosmetics.CosmeticsAPI;
import com.prison.menu.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CosmeticsMenuGUI {

    public static final Component TITLE = MiniMessage.miniMessage().deserialize("<!italic>Your Tags");
    private static final MiniMessage MM = MiniMessage.miniMessage();

    // Content slots — four rows of 7
    private static final int[] CONTENT_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43
    };

    private static final int SLOTS_PER_PAGE = CONTENT_SLOTS.length; // 28

    private static final int SLOT_BACK     = 45;
    private static final int SLOT_PREV     = 46;
    private static final int SLOT_INFO     = 49;
    private static final int SLOT_NEXT     = 52;

    // Per-player page state
    private static final ConcurrentHashMap<UUID, Integer> pageMap = new ConcurrentHashMap<>();

    // ----------------------------------------------------------------

    public static void open(Player player) {
        pageMap.putIfAbsent(player.getUniqueId(), 0);
        player.openInventory(build(player));
    }

    public static void handleClick(Player player, int slot, MenuPlugin plugin) {
        UUID uuid = player.getUniqueId();

        if (slot == 8 || slot == SLOT_BACK) {
            Sounds.nav(player);
            pageMap.remove(uuid);
            MainMenuGUI.open(player);
            return;
        }

        CosmeticsAPI api = CosmeticsAPI.getInstance();
        if (api == null) return;

        Collection<ChatTag> allTags = safeGetTags(api);
        List<ChatTag> tagList = new ArrayList<>(allTags);

        int page = pageMap.getOrDefault(uuid, 0);
        int totalPages = Math.max(1, (int) Math.ceil((double) tagList.size() / SLOTS_PER_PAGE));

        if (slot == SLOT_PREV) {
            if (page > 0) {
                pageMap.put(uuid, page - 1);
                Sounds.nav(player);
                player.openInventory(build(player));
            } else {
                Sounds.deny(player);
            }
            return;
        }

        if (slot == SLOT_NEXT) {
            if (page < totalPages - 1) {
                pageMap.put(uuid, page + 1);
                Sounds.nav(player);
                player.openInventory(build(player));
            } else {
                Sounds.deny(player);
            }
            return;
        }

        // Content slot click?
        int contentIndex = slotToIndex(slot);
        if (contentIndex < 0) return;

        int tagIndex = page * SLOTS_PER_PAGE + contentIndex;
        if (tagIndex >= tagList.size()) return;

        ChatTag tag = tagList.get(tagIndex);

        Set<String> ownedTags = safeGetOwned(api, uuid);
        String equipped = safeGetEquipped(api, uuid);

        boolean owns     = ownedTags.contains(tag.id());
        boolean isEquipped = tag.id().equals(equipped);

        if (isEquipped) {
            // Unequip
            try { api.unequipTag(player); } catch (Exception ignored) {}
            Sounds.nav(player);
            player.openInventory(build(player));
        } else if (owns) {
            // Equip
            try { api.equipTag(player, tag.id()); } catch (Exception ignored) {}
            Sounds.buy(player);
            player.sendMessage(MM.deserialize("<green>Tag equipped!"));
            player.openInventory(build(player));
        } else {
            // Not owned
            Sounds.deny(player);
            player.sendMessage(MM.deserialize("<red>You don't own this tag."));
        }
    }

    // ----------------------------------------------------------------

    private static Inventory build(Player player) {
        UUID uuid = player.getUniqueId();
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        TopBand.apply(inv, player);
        inv.setItem(8, Gui.back());

        // Slot 0: back
        inv.setItem(0, Gui.back());

        CosmeticsAPI api = CosmeticsAPI.getInstance();

        if (api == null) {
            inv.setItem(22, Gui.make(Material.BARRIER, "<red>✗ Cosmetics Unavailable",
                "<gray>✦ The Cosmetics plugin is not loaded."));
            inv.setItem(SLOT_BACK, Gui.back());
            return inv;
        }

        Collection<ChatTag> allTags = safeGetTags(api);
        if (allTags.isEmpty()) {
            inv.setItem(22, Gui.make(Material.PAPER, "<gray>No Tags Available",
                "<gray>✦ There are no chat tags to display."));
            inv.setItem(SLOT_BACK, Gui.back());
            return inv;
        }

        List<ChatTag> tagList = new ArrayList<>(allTags);
        int page = pageMap.getOrDefault(uuid, 0);
        int totalPages = Math.max(1, (int) Math.ceil((double) tagList.size() / SLOTS_PER_PAGE));

        // Clamp page
        if (page >= totalPages) {
            page = totalPages - 1;
            pageMap.put(uuid, page);
        }

        Set<String> ownedTags = safeGetOwned(api, uuid);
        String equipped = safeGetEquipped(api, uuid);

        int startIndex = page * SLOTS_PER_PAGE;
        for (int i = 0; i < CONTENT_SLOTS.length; i++) {
            int tagIndex = startIndex + i;
            if (tagIndex >= tagList.size()) break;
            ChatTag tag = tagList.get(tagIndex);
            inv.setItem(CONTENT_SLOTS[i], buildTagItem(tag, ownedTags, equipped));
        }

        // Pagination controls
        inv.setItem(SLOT_BACK, Gui.back());

        if (page > 0) {
            inv.setItem(SLOT_PREV, Gui.prevPage(page + 1, totalPages));
        }

        inv.setItem(SLOT_INFO, Gui.make(Material.PAPER, "<aqua>✦ Your Tags (" + tagList.size() + ")",
            "<gray>✦ Page: <white>" + (page + 1) + "<gray> / <white>" + totalPages,
            "<gray>✦ Total tags: <white>" + tagList.size()));

        if (page < totalPages - 1) {
            inv.setItem(SLOT_NEXT, Gui.nextPage(page + 1, totalPages));
        }

        return inv;
    }

    private static ItemStack buildTagItem(ChatTag tag, Set<String> ownedTags, String equipped) {
        boolean owns     = ownedTags.contains(tag.id());
        boolean isEquipped = tag.id().equals(equipped);

        String tagDisplay = tag.display();

        List<Component> lore = new ArrayList<>();

        // Try to show description
        try {
            String desc = tag.description();
            if (desc != null && !desc.isBlank()) {
                lore.add(MM.deserialize("<!italic><gray>✦ " + desc));
                lore.add(Component.empty());
            }
        } catch (Exception ignored) {}

        // Show rarity if not owned
        if (!owns) {
            lore.add(MM.deserialize("<!italic><gray>✦ Rarity: <yellow>" + tag.rarity().name()));
            lore.add(Component.empty());
        }

        // Chat preview section
        lore.add(MM.deserialize("<!italic><aqua>✦ Chat Preview:"));
        lore.add(MM.deserialize("<!italic><gray>" + tagDisplay + " <white>Username<gray>: Hey!"));
        lore.add(Component.empty());

        if (isEquipped) {
            lore.add(MM.deserialize("<!italic><green>✓ Currently equipped."));
            lore.add(Component.empty());
            lore.add(MM.deserialize("<!italic><green>→ Click to unequip this tag!"));
            return Gui.make(Material.LIME_CONCRETE,
                "<green>\u2714 " + tagDisplay + " <green>[EQUIPPED]", lore);
        } else if (owns) {
            lore.add(MM.deserialize("<!italic><green>✓ Owned."));
            lore.add(Component.empty());
            lore.add(MM.deserialize("<!italic><green>→ Click to equip this tag!"));
            return Gui.make(Material.NAME_TAG,
                "<aqua>" + tagDisplay, lore);
        } else {
            lore.add(MM.deserialize("<!italic><red>✗ Not Owned."));
            lore.add(Component.empty());
            lore.add(MM.deserialize("<!italic><gray>→ Obtain this tag to equip it."));
            return Gui.make(Material.PAPER,
                "<gray>" + tagDisplay, lore);
        }
    }

    // ----------------------------------------------------------------
    // Safe API helpers
    // ----------------------------------------------------------------

    private static Collection<ChatTag> safeGetTags(CosmeticsAPI api) {
        try {
            Collection<ChatTag> tags = api.getAllTagDefinitions();
            return tags != null ? tags : Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private static Set<String> safeGetOwned(CosmeticsAPI api, UUID uuid) {
        try {
            Set<String> owned = api.getOwnedTags(uuid);
            return owned != null ? owned : Collections.emptySet();
        } catch (Exception e) {
            return Collections.emptySet();
        }
    }

    private static String safeGetEquipped(CosmeticsAPI api, UUID uuid) {
        try {
            return api.getEquippedTag(uuid);
        } catch (Exception e) {
            return null;
        }
    }

    private static int slotToIndex(int slot) {
        for (int i = 0; i < CONTENT_SLOTS.length; i++) {
            if (CONTENT_SLOTS[i] == slot) return i;
        }
        return -1;
    }
}
