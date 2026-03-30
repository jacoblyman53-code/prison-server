package com.prison.coinflip;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * CoinflipCommand — handles /coinflip, /cf, /flip.
 *
 * Subcommands:
 *   /coinflip                    — open browser GUI
 *   /coinflip create <amount>    — create a flip for the given amount
 *   /coinflip cancel             — cancel your open flip and refund
 *   /coinflip accept <player>    — accept a specific player's open flip
 */
public class CoinflipCommand {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final CoinflipPlugin plugin;

    public CoinflipCommand(CoinflipPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command must be run in-game.");
            return true;
        }

        if (args.length == 0) {
            CoinflipBrowserGUI.open(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> {
                if (args.length < 2) {
                    player.sendMessage(MM.deserialize("<red>Usage: /coinflip create <amount>"));
                    return true;
                }
                long amount;
                try {
                    amount = Long.parseLong(args[1].replace(",", "").replace("_", ""));
                } catch (NumberFormatException e) {
                    player.sendMessage(MM.deserialize("<red>Invalid amount: <white>" + args[1]));
                    return true;
                }
                String error = CoinflipManager.getInstance().createTicket(player, amount);
                if (error != null) {
                    player.sendMessage(MM.deserialize("<red>" + error));
                }
            }
            case "cancel" -> {
                String error = CoinflipManager.getInstance().cancelTicket(player.getUniqueId());
                if (error != null) {
                    player.sendMessage(MM.deserialize("<red>" + error));
                } else {
                    player.sendMessage(MM.deserialize("<green>Your coinflip has been cancelled and your $ refunded."));
                }
            }
            case "accept" -> {
                if (args.length < 2) {
                    player.sendMessage(MM.deserialize("<red>Usage: /coinflip accept <player>"));
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    player.sendMessage(MM.deserialize("<red>Player not found or not online: <white>" + args[1]));
                    return true;
                }
                CoinflipManager mgr = CoinflipManager.getInstance();
                if (!mgr.hasOpenTicket(target.getUniqueId())) {
                    player.sendMessage(MM.deserialize("<red>" + target.getName() + " does not have an active coinflip."));
                    return true;
                }
                CoinflipTicket ticket = mgr.getTicketOf(target.getUniqueId());
                if (ticket == null) return true;
                CoinflipAcceptGUI.open(player, ticket);
            }
            default -> player.sendMessage(MM.deserialize(
                "<red>Unknown subcommand. Usage: /coinflip [create <amount>|cancel|accept <player>]"));
        }
        return true;
    }
}
