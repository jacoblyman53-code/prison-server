package com.prison.crates;

import com.prison.permissions.PermissionEngine;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

/**
 * CrateCommand — admin /crate management commands.
 *
 *   /crate give <player> <tier> [amount]   — give keys to a player
 *   /crate setblock <tier>                 — tag the looked-at block as a crate
 *   /crate removeblock                     — remove crate tag from looked-at block
 *   /crate simulate <tier> [count]         — simulate openings and show distribution
 *   /crate reload                          — reload config
 *   /crate list                            — list configured tiers and block count
 */
public class CrateCommand implements CommandExecutor {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private final CrateManager manager;

    public CrateCommand(CrateManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!checkPerm(sender)) return true;

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "give"        -> handleGive(sender, args);
            case "setblock"    -> handleSetBlock(sender, args);
            case "removeblock" -> handleRemoveBlock(sender);
            case "simulate"    -> handleSimulate(sender, args);
            case "reload"      -> handleReload(sender);
            case "list"        -> handleList(sender);
            default            -> sendHelp(sender);
        }
        return true;
    }

    // ----------------------------------------------------------------
    // Subcommands
    // ----------------------------------------------------------------

    private void handleGive(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(MM.deserialize("<red>Usage: /crate give <player> <tier> [amount]"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        UUID targetUuid;
        String targetName;
        if (target != null) {
            targetUuid  = target.getUniqueId();
            targetName  = target.getName();
        } else {
            org.bukkit.OfflinePlayer offline = Bukkit.getOfflinePlayer(args[1]);
            targetUuid  = offline.getUniqueId();
            targetName  = offline.getName() != null ? offline.getName() : args[1];
        }

        String tierId = args[2].toLowerCase();
        if (manager.getTier(tierId) == null) {
            sender.sendMessage(MM.deserialize("<red>Unknown crate tier: " + tierId
                + ". Valid: " + String.join(", ", manager.getTiers().keySet())));
            return;
        }

        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
                if (amount < 1) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                sender.sendMessage(MM.deserialize("<red>Invalid amount."));
                return;
            }
        }

        final int finalAmount = amount;
        manager.giveKeys(targetUuid, tierId, finalAmount).thenRun(() ->
            Bukkit.getScheduler().runTask(getCratePlugin(), () ->
                sender.sendMessage(MM.deserialize(
                    "<green>Gave <white>" + finalAmount + "x " + tierId
                    + "</white> key(s) to <white>" + targetName + "</white>."))
            )
        );
    }

    private void handleSetBlock(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Must be a player.");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(MM.deserialize("<red>Usage: /crate setblock <tier>"));
            return;
        }

        String tierId = args[1].toLowerCase();
        if (manager.getTier(tierId) == null) {
            sender.sendMessage(MM.deserialize("<red>Unknown tier: " + tierId));
            return;
        }

        Block target = player.getTargetBlockExact(5);
        if (target == null) {
            sender.sendMessage(MM.deserialize("<red>No block in range. Look at a block within 5 blocks."));
            return;
        }

        manager.addCrateBlock(target.getLocation(), tierId);
        sender.sendMessage(MM.deserialize(
            "<green>Set block at " + blockCoords(target.getLocation())
            + " as a <white>" + tierId + "</white> crate."));
    }

    private void handleRemoveBlock(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Must be a player.");
            return;
        }

        Block target = player.getTargetBlockExact(5);
        if (target == null) {
            sender.sendMessage(MM.deserialize("<red>No block in range."));
            return;
        }

        String old = manager.getTierAtBlock(target.getLocation());
        if (old == null) {
            sender.sendMessage(MM.deserialize("<red>That block is not a crate."));
            return;
        }

        manager.removeCrateBlock(target.getLocation());
        sender.sendMessage(MM.deserialize(
            "<green>Removed crate block at " + blockCoords(target.getLocation()) + "."));
    }

    private void handleSimulate(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MM.deserialize("<red>Usage: /crate simulate <tier> [count]"));
            return;
        }

        String tierId = args[1].toLowerCase();
        CrateTier tier = manager.getTier(tierId);
        if (tier == null) {
            sender.sendMessage(MM.deserialize("<red>Unknown tier: " + tierId));
            return;
        }

        int count = 100;
        if (args.length >= 3) {
            try {
                count = Integer.parseInt(args[2]);
                if (count < 1 || count > 10000) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                sender.sendMessage(MM.deserialize("<red>Count must be 1–10000."));
                return;
            }
        }

        final int finalCount = count;
        Bukkit.getScheduler().runTaskAsynchronously(getCratePlugin(), () -> {
            Map<String, Integer> results = manager.simulate(tierId, finalCount);
            Bukkit.getScheduler().runTask(getCratePlugin(), () -> {
                sender.sendMessage(MM.deserialize(
                    "<gold><bold>Simulating " + finalCount + " openings of " + tierId + "...</bold></gold>"));
                results.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .forEach(e -> {
                        double pct = (e.getValue() * 100.0) / finalCount;
                        sender.sendMessage(MM.deserialize(
                            "<gray>" + e.getKey() + ": <white>" + e.getValue()
                            + " <dark_gray>(" + String.format("%.1f", pct) + "%)"));
                    });
            });
        });
    }

    private void handleReload(CommandSender sender) {
        manager.loadConfig();
        sender.sendMessage(MM.deserialize("<green>Crate config reloaded."));
    }

    private void handleList(CommandSender sender) {
        sender.sendMessage(MM.deserialize("<gold><bold>--- Crate Tiers ---</bold></gold>"));
        manager.getTiers().forEach((id, tier) -> {
            long blockCount = manager.getCrateBlocks().values().stream()
                .filter(t -> t.equals(id)).count();
            sender.sendMessage(MM.deserialize(
                "<yellow>" + id + "</yellow><gray>: " + tier.displayName()
                + " | " + tier.rewards().size() + " rewards | " + blockCount + " blocks"));
        });
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private boolean checkPerm(CommandSender sender) {
        if (sender instanceof Player player) {
            if (!PermissionEngine.getInstance().hasPermission(player, "prison.admintoolkit.crates")) {
                player.sendMessage(MM.deserialize("<red>No permission."));
                return false;
            }
        }
        return true;
    }

    private static String blockCoords(Location loc) {
        return loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    private static CratePlugin getCratePlugin() {
        return CratePlugin.getInstance();
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(MM.deserialize(
            "<gold><bold>--- /crate ---</bold></gold>\n"
            + "<yellow>/crate give <player> <tier> [amt]</yellow><gray> — give keys\n"
            + "<yellow>/crate setblock <tier></yellow><gray> — tag looked-at block\n"
            + "<yellow>/crate removeblock</yellow><gray> — remove crate tag\n"
            + "<yellow>/crate simulate <tier> [count]</yellow><gray> — simulate openings\n"
            + "<yellow>/crate reload</yellow><gray> — reload config\n"
            + "<yellow>/crate list</yellow><gray> — list tiers and blocks"
        ));
    }
}
