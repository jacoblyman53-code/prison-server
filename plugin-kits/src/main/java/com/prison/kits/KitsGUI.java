package com.prison.kits;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * KitsGUI — 54-slot GUI showing all kits with their status.
 *
 * Layout:
 *   Row 0 (0-8):   empty
 *   Rows 1-3 (9-35): kit items in columns 1-7; columns 0 and 8 are empty
 *   Row 4 (36-44): empty
 *   Row 5 (45-53): empty + close at slot 49
 *
 * Available kit slots (up to 21): 10-16, 19-25, 28-34
 *
 * Status colours:
 *   READY        → green concrete / first item's material
 *   ON_COOLDOWN  → yellow stained glass pane
 *   ONE_TIME_DONE → red stained glass pane
 *   LOCKED       → gray stained glass pane
 */
public class KitsGUI {

    public static final String TITLE_STRING = "KITS";

    private static final Material READY_MAT     = Material.LIME_CONCRETE;
    private static final Material COOLDOWN_MAT  = Material.YELLOW_STAINED_GLASS_PANE;
    private static final Material DONE_MAT      = Material.RED_STAINED_GLASS_PANE;
    private static final Material LOCKED_MAT    = Material.GRAY_STAINED_GLASS_PANE;

    private static final MiniMessage MM = MiniMessage.miniMessage();

    // Kit item slots: left-to-right, rows 1-3, skipping column 0 and 8
    private static final int[] KIT_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34
    };

    private static final int SLOT_CLOSE = 49;

    // ----------------------------------------------------------------
    // Open
    // ----------------------------------------------------------------

    public static void open(Player player) {
        player.openInventory(build(player));
    }

    public static boolean isTitle(Component title) {
        return title.equals(MM.deserialize(TITLE_STRING));
    }

    // ----------------------------------------------------------------
    // Click handler
    // ----------------------------------------------------------------

    public static void handleClick(Player player, int rawSlot, KitsPlugin plugin) {
        if (rawSlot == SLOT_CLOSE) {
            player.closeInventory();
            return;
        }

        // Find which kit slot was clicked
        int kitIndex = -1;
        for (int i = 0; i < KIT_SLOTS.length; i++) {
            if (KIT_SLOTS[i] == rawSlot) { kitIndex = i; break; }
        }
        if (kitIndex < 0) return;

        KitsManager manager = KitsManager.getInstance();
        if (manager == null) return;

        // Get ordered kit list (same order as GUI)
        List<KitData> allKits = new ArrayList<>(manager.getAllKits());
        if (kitIndex >= allKits.size()) return;

        KitData kit = allKits.get(kitIndex);

        // Check access
        if (!manager.meetsRequirements(player, kit)) {
            player.sendMessage(MM.deserialize("<red>You don't have access to this kit."));
            return;
        }

        // Check cooldown
        UUID uuid = player.getUniqueId();
        long remaining = manager.getRemainingMs(uuid, kit);
        if (remaining == Long.MAX_VALUE) {
            player.sendMessage(MM.deserialize(
                "<red>You've already claimed <white>" + stripTags(kit.display()) + " <red>(one-time only)."));
            return;
        }
        if (remaining > 0) {
            player.sendMessage(MM.deserialize(
                "<red>Kit on cooldown. <gray>Available in: <white>" + KitsPlugin.formatDuration(remaining)));
            return;
        }

        // Claim
        KitsManager.ClaimResult result = manager.claimKit(player, kit);
        if (result == KitsManager.ClaimResult.SUCCESS) {
            player.sendMessage(MM.deserialize("<green>You claimed the " + kit.display() + " <green>kit!"));
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.2f);
            // Refresh GUI on next tick so item state updates
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline() && isTitle(player.getOpenInventory().title())) {
                    player.openInventory(build(player));
                }
            }, 1L);
        }
    }

    // ----------------------------------------------------------------
    // Builder
    // ----------------------------------------------------------------

    private static Inventory build(Player player) {
        UUID uuid = player.getUniqueId();
        KitsManager manager = KitsManager.getInstance();

        Inventory inv = Bukkit.createInventory(null, 54, MM.deserialize(TITLE_STRING));

        // Close button
        inv.setItem(SLOT_CLOSE, makeCloseItem());

        if (manager == null) return inv;

        List<KitData> allKits = new ArrayList<>(manager.getAllKits());
        for (int i = 0; i < allKits.size() && i < KIT_SLOTS.length; i++) {
            inv.setItem(KIT_SLOTS[i], makeKitItem(player, uuid, allKits.get(i), manager));
        }

        return inv;
    }

    // ----------------------------------------------------------------
    // Kit item builder
    // ----------------------------------------------------------------

    private static ItemStack makeKitItem(Player player, UUID uuid, KitData kit, KitsManager manager) {
        boolean hasAccess = manager.meetsRequirements(player, kit);
        long remaining    = hasAccess ? manager.getRemainingMs(uuid, kit) : -1L;

        boolean ready       = hasAccess && remaining == 0;
        boolean onCooldown  = hasAccess && remaining > 0 && remaining != Long.MAX_VALUE;
        boolean doneClaimed = hasAccess && remaining == Long.MAX_VALUE;
        boolean locked      = !hasAccess;

        // Pick material
        Material mat;
        if (ready) {
            // Use the first item in the kit's contents for a preview feel
            mat = kit.contents().isEmpty() ? READY_MAT : kit.contents().get(0).material();
            if (mat == null || mat == Material.AIR) mat = READY_MAT;
        } else if (onCooldown) {
            mat = COOLDOWN_MAT;
        } else if (doneClaimed) {
            mat = DONE_MAT;
        } else {
            mat = LOCKED_MAT;
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();

        // Display name
        String namePrefix = ready ? "<green>✓ " : (onCooldown ? "<yellow>⏳ " : (doneClaimed ? "<red>✗ " : "<dark_gray>✗ "));
        meta.displayName(MM.deserialize("<!italic>" + namePrefix + "<bold>" + stripTags(kit.display())));

        // Lore
        List<Component> lore = new ArrayList<>();

        // Requirement line
        lore.add(MM.deserialize("<!italic><aqua>✦ <gray>Access:"));
        switch (kit.type()) {
            case RANK  -> lore.add(MM.deserialize("<!italic><dark_aqua>  ◆ <gray>Requires rank: <yellow>" + kit.requiredRank()));
            case DONOR -> lore.add(MM.deserialize("<!italic><dark_aqua>  ◆ <gray>Requires: <yellow>" + kit.requiredDonorRank()));
            default    -> lore.add(MM.deserialize("<!italic><dark_aqua>  ◆ <green>Available to all players"));
        }

        lore.add(Component.empty());

        // Contents preview (up to 4 items)
        lore.add(MM.deserialize("<!italic><aqua>✦ <gray>Contents:"));
        int shown = 0;
        for (KitItem ki : kit.contents()) {
            if (shown++ >= 4) { lore.add(MM.deserialize("<!italic><dark_aqua>  ◆ <gray>...")); break; }
            lore.add(MM.deserialize("<!italic><dark_aqua>  ◆ <gray>" + ki.amount() + "x <white>" + formatMat(ki.material().name())));
        }

        lore.add(Component.empty());

        // Cooldown / status
        if (ready) {
            lore.add(MM.deserialize("<!italic><aqua>✦ <green>✓ Ready to claim!"));
        } else if (onCooldown) {
            lore.add(MM.deserialize("<!italic><aqua>✦ <gray>Cooldown: <white>" + KitsPlugin.formatDuration(remaining)));
        } else if (doneClaimed) {
            lore.add(MM.deserialize("<!italic><aqua>✦ <gray>Cooldown: <red>✗ Already claimed <dark_gray>(one-time)"));
        } else {
            String req = kit.type() == KitData.KitType.RANK
                ? "Requires mine rank <yellow>" + kit.requiredRank()
                : kit.type() == KitData.KitType.DONOR
                    ? "Requires <yellow>" + kit.requiredDonorRank()
                    : "No access";
            lore.add(MM.deserialize("<!italic><aqua>✦ <red>✗ <gray>" + req));
        }

        lore.add(Component.empty());

        // CTA
        if (ready) {
            lore.add(MM.deserialize("<!italic><green>→ <green>Click to <green>claim</green> this kit!"));
        } else if (onCooldown) {
            lore.add(MM.deserialize("<!italic><red>✗ On cooldown — ready in <white>" + KitsPlugin.formatDuration(remaining)));
        } else if (doneClaimed) {
            lore.add(MM.deserialize("<!italic><red>✗ One-time kit already claimed."));
        } else {
            lore.add(MM.deserialize("<!italic><red>✗ You do not meet the requirements."));
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    // ----------------------------------------------------------------
    // Utility items
    // ----------------------------------------------------------------

    private static ItemStack makeCloseItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(MM.deserialize("<!italic><red>✗ Close"));
        meta.lore(List.of(MM.deserialize("<!italic><gray>Click to close this menu.")));
        item.setItemMeta(meta);
        return item;
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    /** "DIAMOND_PICKAXE" → "Diamond Pickaxe" */
    private static String formatMat(String name) {
        String[] parts = name.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return sb.toString();
    }

    /** Strip MiniMessage tags for use in plain comparisons. */
    private static String stripTags(String s) {
        return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
            .plainText().serialize(MM.deserialize(s));
    }
}
