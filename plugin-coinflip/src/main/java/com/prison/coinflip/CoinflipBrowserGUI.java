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

    public static final Component TITLE = MiniMessage.miniMessage().deserialize("<!italic><gold>Coinflip");
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
        Gui.fillAll(inv);

        CoinflipManager mgr = CoinflipManager.getInstance();
        List<CoinflipTicket> tickets = mgr.getOpenTickets();
        int perPage    = LISTING_SLOTS.length;
        int totalPages = Math.max(1, (int) Math.ceil((double) tickets.size() / perPage));
        if (page >= totalPages) page = totalPages - 1;
        if (page < 0) page = 0;

        // Balance card
        long bal = 0;
        try {
            com.prison.economy.EconomyAPI api = com.prison.economy.EconomyAPI.getInstance();
            if (api != null) bal = api.getBalance(player.getUniqueId());
        } catch (Exception ignored) {}
        inv.setItem(SLOT_BALANCE, Gui.make(Material.SUNFLOWER, "<yellow>Your Balance",
            "<gray>IGC: <gold>" + Fmt.number(bal)));

        // Info card
        inv.setItem(SLOT_INFO, Gui.make(Material.BOOK, "<yellow>How Coinflip Works",
            "<gray>Bet IGC against another player.",
            "<gray>The winner takes the full pool.",
            "",
            "<yellow>Left-click a ticket to accept.",
            "<green>Click Create to start your own."));

        // Create button
        boolean hasOpen = mgr.hasOpenTicket(player.getUniqueId());
        if (hasOpen) {
            inv.setItem(SLOT_CREATE, Gui.make(Material.GOLD_INGOT, "<yellow>Your Active Flip",
                "<gray>You have an open coinflip.",
                "<red>Cancel it with /coinflip cancel"));
        } else {
            inv.setItem(SLOT_CREATE, Gui.make(Material.GOLD_INGOT, "<green>Create Flip",
                "<gray>Start a new coinflip challenge.",
                "",
                "<green>Click to set your bet amount."));
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
        String nameColor = isOwn ? "<yellow>" : "<white>";
        long pot = ticket.getAmount() * 2;

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(MM.deserialize("<!italic><gray>Creator: " + nameColor + ticket.getCreatorName()));
        lore.add(MM.deserialize("<!italic><gray>Bet:     <gold>" + Fmt.number(ticket.getAmount()) + " IGC"));
        lore.add(MM.deserialize("<!italic><gray>Prize:   <green>" + Fmt.number(pot) + " IGC"));
        lore.add(Component.empty());
        if (isOwn) {
            lore.add(MM.deserialize("<!italic><yellow>This is your flip."));
            lore.add(MM.deserialize("<!italic><dark_gray>/coinflip cancel to remove it."));
        } else {
            lore.add(MM.deserialize("<!italic><green>Click to accept this bet!"));
        }

        Material mat = isOwn ? Material.GOLD_INGOT : Material.PAPER;
        return Gui.make(mat, nameColor + ticket.getCreatorName() + "'s Flip", lore);
    }
}
