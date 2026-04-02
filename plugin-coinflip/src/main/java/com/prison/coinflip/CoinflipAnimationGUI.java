package com.prison.coinflip;

import com.prison.menu.util.Gui;
import com.prison.menu.util.Sounds;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * CoinflipAnimationGUI — 27-slot spinning animation.
 *
 * Shows 2 player heads rapidly alternating in the center slot,
 * slowing to a stop over ~3 seconds, then reveals the winner.
 */
public class CoinflipAnimationGUI {

    public static final Component TITLE = MiniMessage.miniMessage().deserialize("<!italic>⚡ Coinflip ⚡");
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private static final int SLOT_CENTER = 13;
    private static final int[] SIDE_SLOTS = {10, 11, 12, 14, 15, 16};

    public static void start(JavaPlugin plugin, CoinflipTicket ticket, Player creator, Player acceptor) {
        // Both players open the animation inventory
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);
        buildStaticElements(inv, ticket);

        if (creator != null && creator.isOnline()) creator.openInventory(inv);
        if (acceptor != null && acceptor.isOnline()) acceptor.openInventory(inv);

        // Animation schedule: fast ticks slowing down
        // Tick pattern: 2,2,2,2,3,3,4,5,6,8,10 then hold on winner for 40 ticks
        int[] tickDelays = {2, 2, 2, 2, 2, 3, 3, 4, 5, 6, 8, 10};
        UUID[] participants = {ticket.getCreatorUuid(), ticket.getAcceptorUuid()};
        String[] names = {ticket.getCreatorName(), ticket.getAcceptorName()};

        // Determine winner before animation
        UUID winnerUuid = participants[new Random().nextInt(2)];

        long[] cumulative = {0};
        for (int i = 0; i < tickDelays.length; i++) {
            cumulative[0] += tickDelays[i];
            final boolean showCreator = (i % 2 == 0);
            final long delay = cumulative[0];
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                UUID showing = showCreator ? participants[0] : participants[1];
                String showingName = showCreator ? names[0] : names[1];
                ItemStack head = makeHead(showingName, "<!italic><white>" + showingName);
                updateInv(inv, head, creator, acceptor, TITLE);
                // Play sound for both
                Sounds.nav(creator);
                if (acceptor != null) Sounds.nav(acceptor);
            }, delay);
        }

        // Final reveal
        long revealDelay = cumulative[0] + 20;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            String winnerName = winnerUuid.equals(ticket.getCreatorUuid())
                ? ticket.getCreatorName() : ticket.getAcceptorName();

            ItemStack winHead = makeHead(winnerName,
                "<!italic><green>⭐ " + winnerName,
                "<!italic><green>WINNER!",
                "<!italic><aqua>✦ <gray>Wins: <gold>$" + String.format("%,d", ticket.getAmount() * 2));
            updateInv(inv, winHead, creator, acceptor, TITLE);

            Sounds.win(creator);
            if (acceptor != null) Sounds.win(acceptor);

            // Resolve
            CoinflipManager.getInstance().resolve(ticket, winnerUuid);

            // Send title to both
            net.kyori.adventure.title.Title titleMsg = net.kyori.adventure.title.Title.title(
                MM.deserialize("<green>⭐ " + winnerName + " wins!"),
                MM.deserialize("<gray>$" + String.format("%,d", ticket.getAmount() * 2)),
                net.kyori.adventure.title.Title.Times.times(
                    java.time.Duration.ofMillis(200),
                    java.time.Duration.ofMillis(2000),
                    java.time.Duration.ofMillis(500)
                )
            );
            if (creator  != null && creator.isOnline())  creator.showTitle(titleMsg);
            if (acceptor != null && acceptor.isOnline()) acceptor.showTitle(titleMsg);

            // Close inventory after 3 more seconds
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (creator  != null && creator.isOnline()  && TITLE.equals(creator.getOpenInventory().title()))  creator.closeInventory();
                if (acceptor != null && acceptor.isOnline() && TITLE.equals(acceptor.getOpenInventory().title())) acceptor.closeInventory();
            }, 60L);
        }, revealDelay);
    }

    private static void buildStaticElements(Inventory inv, CoinflipTicket ticket) {
        inv.setItem(4, Gui.make(Material.GOLD_INGOT,
            "<!italic><aqua>Coinflip",
            "<!italic><aqua>✦ <gray>Bet: <gold>$" + String.format("%,d", ticket.getAmount()) + "<gray> each",
            "<!italic><aqua>✦ <gray>Prize: <green>$" + String.format("%,d", ticket.getAmount() * 2)));
    }

    private static void updateInv(Inventory inv, ItemStack head, Player creator, Player acceptor, Component title) {
        inv.setItem(SLOT_CENTER, head);
        if (creator  != null && creator.isOnline()  && title.equals(creator.getOpenInventory().title()))
            creator.updateInventory();
        if (acceptor != null && acceptor.isOnline() && title.equals(acceptor.getOpenInventory().title()))
            acceptor.updateInventory();
    }

    private static ItemStack makeHead(String playerName, String... loreLines) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(playerName));
        meta.displayName(MM.deserialize(loreLines[0]));
        if (loreLines.length > 1) {
            List<Component> lore = new ArrayList<>();
            for (int i = 1; i < loreLines.length; i++)
                lore.add(MM.deserialize(loreLines[i]));
            meta.lore(lore);
        }
        skull.setItemMeta(meta);
        return skull;
    }
}
