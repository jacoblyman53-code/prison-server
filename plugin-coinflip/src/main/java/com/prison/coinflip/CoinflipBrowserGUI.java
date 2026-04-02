package com.prison.coinflip;

import com.prison.menu.util.Gui;
import com.prison.menu.util.Fmt;
import com.prison.menu.util.Sounds;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * CoinflipBrowserGUI — 54-slot browser listing all open coinflip tickets.
 *
 * Slots 10-25, 28-34 = ticket listings (up to 20 per page).
 * Slot 0  = your balance (read-only)
 * Slot 4  = how it works info
 * Slot 31 = Create Flip button
 * Slot 45 = prev page
 * Slot 49 = back to main menu
 * Slot 53 = next page
 */
public class CoinflipBrowserGUI {

    public static final Component TITLE = MiniMessage.miniMessage().deserialize("<!italic>Coinflip");
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private static final int[] LISTING_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30,       32, 33, 34
    };
    private static final int SLOT_BALANCE    = 0;
    private static final int SLOT_INFO       = 4;
    private static final int SLOT_CREATE     = 31;
    private static final int SLOT_PREV       = 45;
    private static final int SLOT_BACK       = 49;
    private static final int SLOT_NEXT       = 53;

    private static final java.util.Map<UUID, Integer> pages = new java.util.concurrent.ConcurrentHashMap<>();

    public static void open(Player player) {
        open(player, pages.getOrDefault(player.getUniqueId(), 0));
    }

    public static void open(Player player, int page) {
        pages.put(player.getUniqueId(), page);
        player.openInventory(build(player, page));
    }

    public static void refreshForOnlinePlayers() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (TITLE.equals(p.getOpenInventory().title())) {
                int page = pages.getOrDefault(p.getUniqueId(), 0);
                Bukkit.getScheduler().runTask(CoinflipPlugin.getInstance(), () -> open(p, page));
            }
        }
    }

    public static void handleClick(Player player, int slot, CoinflipPlugin plugin) {
        CoinflipManager mgr = CoinflipManager.getInstance();
        List<CoinflipTicket> tickets = mgr.getOpenTickets();
        int page = pages.getOrDefault(player.getUniqueId(), 0);
        int perPage = LISTING_SLOTS.length;
        int totalPages = Math.max(1, (int) Math.ceil((double) tickets.size() / perPage));

        if (slot == SLOT_BACK) {
            // Return to main menu if plugin-menu is present, else close
            try {
                com.prison.menu.MainMenuGUI.open(player);
            } catch (NoClassDefFoundError | Exception ignored) {
                player.closeInventory();
            }
            Sounds.nav(player);
            return;
        }

        if (slot == SLOT_PREV && page > 0) {
            Sounds.nav(player);
            open(player, page - 1);
            return;
        }

        if (slot == SLOT_NEXT && page < totalPages - 1) {
            Sounds.nav(player);
            open(player, page + 1);
            return;
        }

        if (slot == SLOT_CREATE) {
            Sounds.nav(player);
            CoinflipCreateGUI.open(player);
            return;
        }

        // Listing slot clicked
        for (int i = 0; i < LISTING_SLOTS.length; i++) {
            if (LISTING_SLOTS[i] == slot) {
                int idx = page * perPage + i;
                if (idx < tickets.size()) {
                    CoinflipTicket ticket = tickets.get(idx);
                    Sounds.nav(player);
                    CoinflipAcceptGUI.open(player, ticket);
                }
                return;
            }
        }
    }

    // ----------------------------------------------------------------

    private static Inventory build(Player player, int page) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);

        CoinflipManager mgr = CoinflipManager.getInstance();
        List<CoinflipTicket> tickets = mgr.getOpenTickets();
        int perPage    = LISTING_SLOTS.length;
        int totalPages = Math.max(1, (int) Math.ceil((double) tickets.size() / perPage));
        if (page >= totalPages) page = totalPages - 1;
        if (page < 0) page = 0;

        // Balance card (slot 0 — also serves as the close/back anchor per spec)
        long bal = 0;
        try {
            com.prison.economy.EconomyAPI api = com.prison.economy.EconomyAPI.getInstance();
            if (api != null) bal = api.getBalance(player.getUniqueId());
        } catch (Exception ignored) {}
        inv.setItem(SLOT_BALANCE, Gui.make(Material.SUNFLOWER,
            "<!italic><aqua>✦ Your Balance",
            "<!italic><aqua>✦ <gray>Balance: <gold>$" + Fmt.number(bal)));

        // Info card (slot 4)
        inv.setItem(SLOT_INFO, Gui.make(Material.BOOK,
            "<!italic><aqua>How Coinflip Works",
            "<!italic><gray>Bet <green>IGC<gray> against another player.",
            "<!italic><gray>The <green>winner<gray> takes the full pool.",
            "<!italic>",
            "<!italic><green>→ Left-click a ticket to accept.",
            "<!italic><green>→ Click Create to start your own."));

        // Create button (slot 31)
        boolean hasOpen = mgr.hasOpenTicket(player.getUniqueId());
        if (hasOpen) {
            inv.setItem(SLOT_CREATE, Gui.make(Material.GOLD_INGOT,
                "<!italic><yellow>Your Active Flip",
                "<!italic><aqua>✦ <gray>You have an <green>open<gray> coinflip.",
                "<!italic><red>✗ Cancel it with /coinflip cancel"));
        } else {
            inv.setItem(SLOT_CREATE, Gui.make(Material.GOLD_INGOT,
                "<!italic><green>→ Create Flip",
                "<!italic><gray>Start a new coinflip <green>challenge<gray>.",
                "<!italic>",
                "<!italic><green>→ Click to set your bet amount."));
        }

        // Listings
        int start = page * perPage;
        for (int i = 0; i < perPage; i++) {
            int idx = start + i;
            if (idx < tickets.size()) {
                inv.setItem(LISTING_SLOTS[i], buildTicketItem(tickets.get(idx), player.getUniqueId()));
            }
        }

        // Pagination
        if (page > 0)
            inv.setItem(SLOT_PREV, Gui.prevPage(page, totalPages));
        if (page < totalPages - 1)
            inv.setItem(SLOT_NEXT, Gui.nextPage(page + 2, totalPages));

        inv.setItem(SLOT_BACK, Gui.back());
        return inv;
    }

    private static ItemStack buildTicketItem(CoinflipTicket ticket, UUID viewerUuid) {
        boolean isOwn = ticket.getCreatorUuid().equals(viewerUuid);
        long amount = ticket.getAmount();
        long pot = amount * 2;

        // Tier-based colour + material scaling with bet size
        // < 50k    → gray   PAPER
        // 50k–250k → white  GOLD_NUGGET
        // 250k–1M  → gold   GOLD_INGOT
        // 1M–5M    → orange GOLD_BLOCK  + glow
        // > 5M     → red    NETHER_STAR + glow (whale bet)
        String tierColor;
        Material mat;
        boolean glow;
        String tierLabel;

        if (amount >= 5_000_000L) {
            tierColor = "<red>";
            mat = Material.NETHER_STAR;
            glow = true;
            tierLabel = "<red>🔥 WHALE BET";
        } else if (amount >= 1_000_000L) {
            tierColor = "<gold>";
            mat = Material.GOLD_BLOCK;
            glow = true;
            tierLabel = "<gold>⭐ High Roller";
        } else if (amount >= 250_000L) {
            tierColor = "<yellow>";
            mat = Material.GOLD_INGOT;
            glow = false;
            tierLabel = "<yellow>Large Bet";
        } else if (amount >= 50_000L) {
            tierColor = "<white>";
            mat = Material.GOLD_NUGGET;
            glow = false;
            tierLabel = "<gray>Medium Bet";
        } else {
            tierColor = "<gray>";
            mat = Material.PAPER;
            glow = false;
            tierLabel = "<dark_gray>Small Bet";
        }

        if (isOwn) {
            tierColor = "<yellow>";
            mat = Material.GOLD_INGOT;
        }

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(MiniMessage.miniMessage().deserialize("<!italic>" + tierLabel));
        lore.add(Component.empty());
        lore.add(MiniMessage.miniMessage().deserialize("<!italic><aqua>✦ <gray>Player: " + tierColor + ticket.getCreatorName()));
        lore.add(MiniMessage.miniMessage().deserialize("<!italic><aqua>✦ <gray>Bet: <gold>$" + Fmt.number(amount)));
        lore.add(Component.empty());
        if (isOwn) {
            lore.add(MiniMessage.miniMessage().deserialize("<!italic><yellow>This is your flip."));
            lore.add(MiniMessage.miniMessage().deserialize("<!italic><dark_gray>/coinflip cancel to remove it."));
        } else {
            lore.add(MiniMessage.miniMessage().deserialize("<!italic><green>→ Click to accept this coinflip!"));
        }

        ItemStack item = Gui.make(mat, "<!italic>" + tierColor + ticket.getCreatorName() + "'s Flip", lore);

        // Add enchant glow for high-value bets
        if (glow) {
            item.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.LUCK_OF_THE_SEA, 1);
            org.bukkit.inventory.meta.ItemMeta glowMeta = item.getItemMeta();
            glowMeta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(glowMeta);
        }

        return item;
    }
}
