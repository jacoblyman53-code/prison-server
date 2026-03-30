package com.prison.menu;

import com.prison.kits.KitData;
import com.prison.kits.KitItem;
import com.prison.kits.KitsAPI;
import com.prison.menu.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class KitsMenuGUI {

    public static final Component TITLE = MiniMessage.miniMessage().deserialize("<!italic><dark_gray>[ <green>Kits <dark_gray>]");
    private static final MiniMessage MM = MiniMessage.miniMessage();

    // Content slots — three rows of 7 (mirrors plugin-kits KitsGUI layout)
    private static final int[] CONTENT_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34
    };

    private static final int SLOT_BACK = 45;

    // ----------------------------------------------------------------

    public static void open(Player player) {
        player.openInventory(build(player));
    }

    public static void handleClick(Player player, int slot, MenuPlugin plugin) {
        if (slot == 8 || slot == SLOT_BACK) {
            Sounds.nav(player);
            MainMenuGUI.open(player);
            return;
        }

        // Map slot back to kit index
        int kitIndex = slotToIndex(slot);
        if (kitIndex < 0) return;

        KitsAPI api = KitsAPI.getInstance();
        if (api == null) return;

        Collection<KitData> allKits = api.getAllKits();
        if (kitIndex >= allKits.size()) return;

        KitData kit = new ArrayList<>(allKits).get(kitIndex);
        UUID uuid = player.getUniqueId();

        Set<String> accessible = new HashSet<>();
        try {
            for (KitData k : api.getAccessibleKits(player)) accessible.add(k.id());
        } catch (Exception ignored) {}

        boolean hasAccess = accessible.contains(kit.id());
        long remaining = Long.MAX_VALUE;
        try {
            remaining = api.getRemainingCooldownMs(uuid, kit.id());
        } catch (Exception ignored) {}

        if (hasAccess && remaining == 0) {
            // READY — claim it
            Sounds.buy(player);
            player.closeInventory();
            Bukkit.dispatchCommand(player, "kit " + kit.id());
        } else if (hasAccess && remaining != Long.MAX_VALUE && remaining > 0) {
            // ON COOLDOWN
            Sounds.deny(player);
            player.sendMessage(MM.deserialize("<red>That kit is on cooldown for <yellow>"
                + Fmt.duration(remaining) + "<red>."));
        } else if (hasAccess) {
            // CLAIMED (one-time)
            Sounds.deny(player);
            player.sendMessage(MM.deserialize("<red>You have already claimed that kit."));
        } else {
            // LOCKED
            Sounds.deny(player);
            player.sendMessage(MM.deserialize("<red>You do not meet the requirements for that kit."));
        }
    }

    // ----------------------------------------------------------------

    private static Inventory build(Player player) {
        UUID uuid = player.getUniqueId();
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        Gui.fillAll(inv);
        TopBand.apply(inv, player);
        inv.setItem(8, Gui.back());

        KitsAPI api = KitsAPI.getInstance();

        if (api == null) {
            inv.setItem(22, Gui.make(Material.BARRIER, "<red>Kits Unavailable",
                "<gray>The Kits plugin is not loaded."));
            inv.setItem(SLOT_BACK, Gui.back());
            return inv;
        }

        Set<String> accessible = new HashSet<>();
        try {
            for (KitData k : api.getAccessibleKits(player)) accessible.add(k.id());
        } catch (Exception ignored) {}

        Collection<KitData> allKits;
        try {
            allKits = api.getAllKits();
        } catch (Exception e) {
            allKits = List.of();
        }

        List<KitData> kitList = new ArrayList<>(allKits);

        for (int i = 0; i < kitList.size() && i < CONTENT_SLOTS.length; i++) {
            KitData kit = kitList.get(i);
            int targetSlot = CONTENT_SLOTS[i];
            inv.setItem(targetSlot, buildKitItem(kit, uuid, accessible, api));
        }

        inv.setItem(SLOT_BACK, Gui.back());
        return inv;
    }

    private static ItemStack buildKitItem(KitData kit, UUID uuid, Set<String> accessible, KitsAPI api) {
        boolean hasAccess = accessible.contains(kit.id());
        long remaining = Long.MAX_VALUE;
        try {
            remaining = api.getRemainingCooldownMs(uuid, kit.id());
        } catch (Exception ignored) {}

        String plain = stripTags(kit.display());

        // Requirement line
        String reqLine;
        try {
            KitData.KitType type = kit.type();
            if (type == KitData.KitType.RANK) {
                reqLine = "<gray>Requires rank: <white>" + kit.requiredRank();
            } else if (type == KitData.KitType.DONOR) {
                reqLine = "<gray>Requires donor rank: <white>" + kit.requiredDonorRank();
            } else {
                reqLine = "<gray>Available to all players";
            }
        } catch (Exception e) {
            reqLine = "<gray>Requirement unknown";
        }

        // Contents preview (up to 4 items)
        List<String> contentLines = new ArrayList<>();
        try {
            List<KitItem> contents = kit.contents();
            int shown = 0;
            for (KitItem ki : contents) {
                if (shown >= 4) {
                    contentLines.add("<dark_gray>  ...and more");
                    break;
                }
                contentLines.add("<gray>  " + ki.amount() + "x <white>" + Fmt.mat(ki.material().name()));
                shown++;
            }
        } catch (Exception ignored) {}
        if (contentLines.isEmpty()) {
            contentLines.add("<gray>  (no preview available)");
        }

        // Determine status, material, and name
        Material mat;
        String displayName;
        String statusLine;

        if (hasAccess && remaining == 0) {
            // READY
            mat = Material.LIME_CONCRETE;
            try {
                List<KitItem> contents = kit.contents();
                if (!contents.isEmpty()) mat = contents.get(0).material();
            } catch (Exception ignored) {}
            displayName = "<green>\u2714 " + plain;
            statusLine = "<green>\u25cf Ready to claim!";
        } else if (hasAccess && remaining != Long.MAX_VALUE && remaining > 0) {
            // COOLDOWN
            mat = Material.YELLOW_STAINED_GLASS_PANE;
            displayName = "<yellow>\u23f3 " + plain;
            statusLine = "<yellow>On cooldown: <white>" + Fmt.duration(remaining);
        } else if (hasAccess) {
            // CLAIMED (Long.MAX_VALUE)
            mat = Material.RED_STAINED_GLASS_PANE;
            displayName = "<red>\u2716 " + plain;
            statusLine = "<red>Already claimed (one-time kit)";
        } else {
            // LOCKED
            mat = Material.GRAY_STAINED_GLASS_PANE;
            displayName = "<dark_gray>\uD83D\uDD12 " + plain;
            statusLine = "<dark_gray>Locked — requirements not met";
        }

        // Build lore
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(MM.deserialize("<!italic>" + reqLine));
        lore.add(Component.empty());
        lore.add(MM.deserialize("<!italic><gray>Contents:"));
        for (String cl : contentLines) {
            lore.add(MM.deserialize("<!italic>" + cl));
        }
        lore.add(Component.empty());
        lore.add(MM.deserialize("<!italic>" + statusLine));

        return Gui.make(mat, displayName, lore);
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private static String stripTags(String mmString) {
        try {
            return PlainTextComponentSerializer.plainText().serialize(MM.deserialize(mmString));
        } catch (Exception e) {
            return mmString;
        }
    }

    private static int slotToIndex(int slot) {
        for (int i = 0; i < CONTENT_SLOTS.length; i++) {
            if (CONTENT_SLOTS[i] == slot) return i;
        }
        return -1;
    }
}
