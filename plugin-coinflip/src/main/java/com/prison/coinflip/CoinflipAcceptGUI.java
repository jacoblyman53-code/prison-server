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
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CoinflipAcceptGUI — 27-slot confirm dialog before accepting a flip.
 *
 * Slot 11 = creator head
 * Slot 13 = bet amount
 * Slot 15 = acceptor head (viewer)
 * Slot 22 = accept (GREEN_CONCRETE)
 * Slot 24 = decline (RED_CONCRETE)
 */
public class CoinflipAcceptGUI {

    public static final Component TITLE = MiniMessage.miniMessage().deserialize("<!italic>ACCEPT COINFLIP");
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private static final Map<UUID, Integer> pendingTicket = new ConcurrentHashMap<>();

    private static final int SLOT_CREATOR  = 11;
    private static final int SLOT_BET      = 13;
    private static final int SLOT_ACCEPTOR = 15;
    private static final int SLOT_CONFIRM  = 22;
    private static final int SLOT_CANCEL   = 24;

    public static void open(Player player, CoinflipTicket ticket) {
        pendingTicket.put(player.getUniqueId(), ticket.getId());
        player.openInventory(build(player, ticket));
    }

    public static void handleClick(Player player, int slot, CoinflipPlugin plugin) {
        UUID uuid = player.getUniqueId();

        if (slot == SLOT_CANCEL) {
            Sounds.nav(player);
            CoinflipBrowserGUI.open(player);
            pendingTicket.remove(uuid);
            return;
        }

        if (slot == SLOT_CONFIRM) {
            Integer ticketId = pendingTicket.get(uuid);
            if (ticketId == null) {
                Sounds.deny(player);
                CoinflipBrowserGUI.open(player);
                return;
            }
            String error = CoinflipManager.getInstance().acceptTicket(player, ticketId);
            if (error != null) {
                Sounds.deny(player);
                player.sendMessage(MM.deserialize("<red>" + error));
                CoinflipBrowserGUI.open(player);
            } else {
                Sounds.buy(player);
                // Animation will open itself
            }
            pendingTicket.remove(uuid);
        }
    }

    // ----------------------------------------------------------------

    private static Inventory build(Player player, CoinflipTicket ticket) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);

        long bal = 0;
        try {
            com.prison.economy.EconomyAPI api = com.prison.economy.EconomyAPI.getInstance();
            if (api != null) bal = api.getBalance(player.getUniqueId());
        } catch (Exception ignored) {}

        boolean canAfford = bal >= ticket.getAmount();

        // Close / Back button (slot 0)
        inv.setItem(0, Gui.make(Material.BARRIER,
            "<!italic><red>← Back to Coinflip",
            "<!italic><gray>Return to coinflip browser."));

        // Creator head (slot 11)
        inv.setItem(SLOT_CREATOR, makeHead(ticket.getCreatorName(),
            "<!italic><aqua>" + ticket.getCreatorName(),
            "<!italic><aqua>✦ <gray>Player: <yellow>" + ticket.getCreatorName(),
            "<!italic><aqua>✦ <gray>Bet: <gold>$" + Fmt.number(ticket.getAmount()),
            "<!italic>",
            "<!italic><green>→ Click to accept this coinflip!"));

        // Bet info (slot 13)
        inv.setItem(SLOT_BET, Gui.make(Material.GOLD_INGOT,
            "<!italic><aqua>✦ Bet Details",
            "<!italic><aqua>✦ <gray>Bet Amount: <gold>$" + Fmt.number(ticket.getAmount()),
            "<!italic><aqua>✦ <gray>Potential Win: <green>$" + Fmt.number(ticket.getAmount() * 2),
            "<!italic><aqua>✦ <gray>Affordability: " + (canAfford
                ? "<!italic><green>✓ Can afford"
                : "<!italic><red>✗ Cannot afford"),
            "<!italic>",
            "<!italic><green>→ Click to place this bet!"));

        // Acceptor head (slot 15)
        inv.setItem(SLOT_ACCEPTOR, makeHead(player.getName(),
            "<!italic><aqua>" + player.getName(),
            "<!italic><aqua>✦ <gray>Player: <yellow>" + player.getName(),
            canAfford ? "<!italic><green>✓ Ready to accept" : "<!italic><red>✗ Cannot afford"));

        // Accept / decline
        if (canAfford) {
            inv.setItem(SLOT_CONFIRM, Gui.make(Material.GREEN_CONCRETE,
                "<!italic><green>✓ Confirm",
                "<!italic><gray>Click to confirm <green>accepting<gray> this coinflip.",
                "<!italic><aqua>✦ <gray>Locks <gold>$" + Fmt.number(ticket.getAmount()) + "<gray> of your funds.",
                "<!italic><aqua>✦ <gray>Winner takes <green>$" + Fmt.number(ticket.getAmount() * 2) + "<gray>.",
                "<!italic>",
                "<!italic><green>→ Click to confirm."));
        } else {
            inv.setItem(SLOT_CONFIRM, Gui.make(Material.RED_CONCRETE,
                "<!italic><red>✗ Cannot Afford",
                "<!italic><aqua>✦ <gray>Required: <gold>$" + Fmt.number(ticket.getAmount()),
                "<!italic><aqua>✦ <gray>Your balance: <gold>$" + Fmt.number(bal)));
        }

        inv.setItem(SLOT_CANCEL, Gui.make(Material.RED_CONCRETE,
            "<!italic><red>✗ Decline",
            "<!italic><gray>Return to coinflip browser."));

        return inv;
    }

    private static ItemStack makeHead(String playerName, String... lore) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(playerName));
        meta.displayName(MiniMessage.miniMessage().deserialize(lore[0]));
        if (lore.length > 1) {
            java.util.List<Component> loreList = new java.util.ArrayList<>();
            for (int i = 1; i < lore.length; i++) {
                loreList.add(MiniMessage.miniMessage().deserialize(lore[i]));
            }
            meta.lore(loreList);
        }
        skull.setItemMeta(meta);
        return skull;
    }
}
