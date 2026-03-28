package com.prison.mines;

import com.prison.economy.EconomyAPI;
import com.prison.permissions.PermissionEngine;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

public class MinesPlugin extends JavaPlugin implements Listener {

    private MineManager manager;
    private MinesAPI    api;
    private final MiniMessage mm = MiniMessage.miniMessage();

    @Override
    public void onEnable() {
        if (PermissionEngine.getInstance() == null) {
            getLogger().severe("PrisonPermissions must be loaded before PrisonMines!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();
        manager = new MineManager(this);
        manager.loadFromConfig(getConfig());

        api = new MinesAPI(manager);

        // Register as the sell price provider if the economy plugin is loaded
        if (EconomyAPI.getInstance() != null) {
            EconomyAPI.getInstance().setSellPriceProvider(api);
            getLogger().info("[Mines] Registered mine-aware sell price provider.");
        }

        getServer().getPluginManager().registerEvents(this, this);

        // 60-second auto-reset check task
        getServer().getScheduler().runTaskTimerAsynchronously(this,
            this::checkAutoResets, 20 * 60L, 20 * 60L);

        getLogger().info("Mines system enabled — " + manager.getMines().size() + " mines active.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Mines system disabled.");
    }

    // ----------------------------------------------------------------
    // Auto-reset Timer Check
    // ----------------------------------------------------------------

    private void checkAutoResets() {
        long now = System.currentTimeMillis();
        for (Map.Entry<String, MineData> entry : manager.getMines().entrySet()) {
            MineData mine   = entry.getValue();
            MineState state = manager.getState(mine.id());
            if (state.isResetting()) continue;

            if (mine.resetTimerMins() > 0) {
                long elapsedMins = (now - state.getLastResetMs()) / 60_000L;
                if (elapsedMins >= mine.resetTimerMins()) {
                    manager.triggerReset(mine.id());
                }
            }
        }
    }

    // ----------------------------------------------------------------
    // Block Break Event
    // ----------------------------------------------------------------

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        var block = event.getBlock();

        MineData mine = manager.getMineAt(
            block.getWorld().getName(),
            block.getX(), block.getY(), block.getZ()
        );

        if (mine == null) return; // not in a mine — don't interfere

        // Permission check
        if (!PermissionEngine.getInstance().hasPermission(player, mine.permissionNode())
                && !PermissionEngine.getInstance().hasPermission(player, "prison.admin.*")) {
            event.setCancelled(true);
            player.sendMessage(mm.deserialize(
                "<red>You do not have access to this mine. Rank up to unlock it!"));
            return;
        }

        // Track blocks broken and check reset threshold
        MineState state = manager.getState(mine.id());
        long broken = state.incrementBlocksBroken();

        if (mine.resetThreshold() > 0 && !state.isResetting()) {
            double ratio = (double) broken / mine.totalBlocks();
            if (ratio >= mine.resetThreshold()) {
                manager.triggerReset(mine.id());
            }
        }
    }

    // ----------------------------------------------------------------
    // Commands
    // ----------------------------------------------------------------

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("mine")) return false;

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        // /mine tp is a player command — check it before the admin guard
        if (args[0].equalsIgnoreCase("tp")) {
            cmdTeleportToMine(sender, args);
            return true;
        }

        // All other subcommands require admin permission
        if (sender instanceof Player player
                && !PermissionEngine.getInstance().hasPermission(player, "prison.admin.*")) {
            player.sendMessage(mm.deserialize("<red>You do not have permission to use this command."));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "list"           -> cmdList(sender);
            case "info"           -> cmdInfo(sender, args);
            case "reset"          -> cmdReset(sender, args);
            case "reload"         -> cmdReload(sender);
            case "create"         -> cmdCreate(sender, args);
            case "delete"         -> cmdDelete(sender, args);
            case "enable"         -> cmdSetEnabled(sender, args, true);
            case "disable"        -> cmdSetEnabled(sender, args, false);
            case "setcorner1"     -> cmdSetCorner(sender, args, true);
            case "setcorner2"     -> cmdSetCorner(sender, args, false);
            case "setspawn"       -> cmdSetSpawn(sender, args);
            case "setcomposition" -> cmdSetComposition(sender, args);
            case "setprice"       -> cmdSetPrice(sender, args);
            default -> sendHelp(sender);
        }
        return true;
    }

    // ----------------------------------------------------------------
    // Command Handlers
    // ----------------------------------------------------------------

    private void cmdList(CommandSender sender) {
        sender.sendMessage(mm.deserialize("<gold><bold>═══ Mines ═══"));
        if (manager.getMines().isEmpty()) {
            sender.sendMessage(mm.deserialize("<gray>No enabled mines. Use /mine create <id> to add one."));
            return;
        }
        for (MineData mine : manager.getMines().values()) {
            MineState state = manager.getState(mine.id());
            String status = state.isResetting() ? "<yellow>RESETTING" : "<green>ACTIVE";
            int total = mine.totalBlocks();
            long broken = state.getBlocksBroken();
            int pct = total > 0 ? (int) (100.0 * broken / total) : 0;
            sender.sendMessage(mm.deserialize(
                " <gray>- " + mine.display() + " <dark_gray>| " + status +
                " <dark_gray>| <white>" + pct + "% mined <dark_gray>| " +
                "<white>" + mine.world()));
        }
    }

    private void cmdInfo(CommandSender sender, String[] args) {
        if (args.length < 2) { sender.sendMessage(mm.deserialize("<red>Usage: /mine info <id>")); return; }
        MineData mine = manager.getMine(args[1]);
        if (mine == null) { sender.sendMessage(mm.deserialize("<red>Mine '" + args[1] + "' not found.")); return; }

        MineState state = manager.getState(mine.id());
        sender.sendMessage(mm.deserialize("<gold><bold>═══ Mine " + mine.id() + " ═══"));
        sender.sendMessage(mm.deserialize("<gray>Display: " + mine.display()));
        sender.sendMessage(mm.deserialize("<gray>World: <white>" + mine.world()));
        sender.sendMessage(mm.deserialize("<gray>Corner 1: <white>" + mine.x1() + ", " + mine.y1() + ", " + mine.z1()));
        sender.sendMessage(mm.deserialize("<gray>Corner 2: <white>" + mine.x2() + ", " + mine.y2() + ", " + mine.z2()));
        sender.sendMessage(mm.deserialize("<gray>Spawn: <white>" + mine.spawnX() + ", " + mine.spawnY() + ", " + mine.spawnZ()));
        sender.sendMessage(mm.deserialize("<gray>Total blocks: <white>" + mine.totalBlocks()));
        sender.sendMessage(mm.deserialize("<gray>Blocks broken: <white>" + state.getBlocksBroken() +
            " <gray>(" + (mine.totalBlocks() > 0 ? (int)(100.0*state.getBlocksBroken()/mine.totalBlocks()) : 0) + "%)"));
        sender.sendMessage(mm.deserialize("<gray>Permission: <white>" + mine.permissionNode()));
        sender.sendMessage(mm.deserialize("<gray>Reset timer: <white>" + mine.resetTimerMins() + " min" +
            " <gray>| Threshold: <white>" + (int)(mine.resetThreshold()*100) + "%"));
        sender.sendMessage(mm.deserialize("<gray>Composition: <white>" + formatComposition(mine)));
    }

    private void cmdReset(CommandSender sender, String[] args) {
        if (args.length < 2) { sender.sendMessage(mm.deserialize("<red>Usage: /mine reset <id>")); return; }
        MineData mine = manager.getMine(args[1]);
        if (mine == null) { sender.sendMessage(mm.deserialize("<red>Mine '" + args[1] + "' not found.")); return; }
        manager.triggerReset(mine.id());
        sender.sendMessage(mm.deserialize("<green>Reset triggered for mine " + mine.id() + "."));
    }

    private void cmdReload(CommandSender sender) {
        reloadConfig();
        manager.loadFromConfig(getConfig());
        // Re-register sell price provider after reload
        if (EconomyAPI.getInstance() != null) {
            EconomyAPI.getInstance().setSellPriceProvider(api);
        }
        sender.sendMessage(mm.deserialize("<green>Mine config reloaded — " +
            manager.getMines().size() + " mines active."));
    }

    private void cmdCreate(CommandSender sender, String[] args) {
        if (args.length < 2) { sender.sendMessage(mm.deserialize("<red>Usage: /mine create <id>")); return; }
        String id = args[1].toUpperCase();
        if (getConfig().contains("mines." + id)) {
            sender.sendMessage(mm.deserialize("<red>Mine '" + id + "' already exists."));
            return;
        }
        manager.createMineInConfig(getConfig(), id);
        sender.sendMessage(mm.deserialize(
            "<green>Mine <white>" + id + "<green> created. Now use:" +
            "\n  <yellow>/mine setcorner1 " + id +
            "\n  <yellow>/mine setcorner2 " + id +
            "\n  <yellow>/mine setspawn " + id +
            "\n  <yellow>/mine enable " + id));
    }

    private void cmdDelete(CommandSender sender, String[] args) {
        if (args.length < 2) { sender.sendMessage(mm.deserialize("<red>Usage: /mine delete <id>")); return; }
        String id = args[1].toUpperCase();
        if (!getConfig().contains("mines." + id)) {
            sender.sendMessage(mm.deserialize("<red>Mine '" + id + "' not found.")); return;
        }
        manager.deleteMineFromConfig(getConfig(), id);
        manager.loadFromConfig(getConfig());
        sender.sendMessage(mm.deserialize("<green>Mine <white>" + id + "<green> deleted."));
    }

    private void cmdSetEnabled(CommandSender sender, String[] args, boolean enabled) {
        if (args.length < 2) {
            sender.sendMessage(mm.deserialize("<red>Usage: /mine " + (enabled ? "enable" : "disable") + " <id>"));
            return;
        }
        String id = args[1].toUpperCase();
        if (!getConfig().contains("mines." + id)) {
            sender.sendMessage(mm.deserialize("<red>Mine '" + id + "' not found.")); return;
        }
        manager.saveMineToConfig(getConfig(), id, "enabled", enabled);
        manager.loadFromConfig(getConfig());
        sender.sendMessage(mm.deserialize("<green>Mine <white>" + id + "<green> " +
            (enabled ? "enabled" : "disabled") + ". " + manager.getMines().size() + " mines active."));
    }

    private void cmdSetCorner(CommandSender sender, String[] args, boolean corner1) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command must be run by a player."); return;
        }
        if (args.length < 2) {
            sender.sendMessage(mm.deserialize("<red>Usage: /mine " +
                (corner1 ? "setcorner1" : "setcorner2") + " <id>")); return;
        }
        String id = args[1].toUpperCase();
        if (!getConfig().contains("mines." + id)) {
            player.sendMessage(mm.deserialize("<red>Mine '" + id + "' not found.")); return;
        }
        Location loc = player.getLocation();
        String field = corner1 ? "corner1" : "corner2";
        manager.saveMineToConfig(getConfig(), id, field,
            java.util.List.of(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
        player.sendMessage(mm.deserialize(
            "<green>Set " + field + " of mine <white>" + id + "<green> to " +
            loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + "."));
    }

    private void cmdSetSpawn(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command must be run by a player."); return;
        }
        if (args.length < 2) {
            sender.sendMessage(mm.deserialize("<red>Usage: /mine setspawn <id>")); return;
        }
        String id = args[1].toUpperCase();
        if (!getConfig().contains("mines." + id)) {
            player.sendMessage(mm.deserialize("<red>Mine '" + id + "' not found.")); return;
        }
        Location loc = player.getLocation();
        manager.saveMineToConfig(getConfig(), id, "spawn-x", loc.getX());
        manager.saveMineToConfig(getConfig(), id, "spawn-y", loc.getY());
        manager.saveMineToConfig(getConfig(), id, "spawn-z", loc.getZ());
        manager.saveMineToConfig(getConfig(), id, "spawn-yaw",   (double) loc.getYaw());
        manager.saveMineToConfig(getConfig(), id, "spawn-pitch", (double) loc.getPitch());
        player.sendMessage(mm.deserialize(
            "<green>Spawn of mine <white>" + id + "<green> set to your location."));
    }

    private void cmdSetComposition(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(mm.deserialize("<red>Usage: /mine setcomposition <id> <MATERIAL> <percent>"));
            return;
        }
        String id = args[1].toUpperCase();
        if (!getConfig().contains("mines." + id)) {
            sender.sendMessage(mm.deserialize("<red>Mine '" + id + "' not found.")); return;
        }
        try {
            org.bukkit.Material.valueOf(args[2].toUpperCase()); // validate
            double pct = Double.parseDouble(args[3]);
            manager.saveMineToConfig(getConfig(), id,
                "composition." + args[2].toUpperCase(), pct);
            sender.sendMessage(mm.deserialize(
                "<green>Set <white>" + args[2].toUpperCase() + "<green> to <white>" +
                pct + "%<green> in mine <white>" + id + "<green>."));
        } catch (IllegalArgumentException e) {
            sender.sendMessage(mm.deserialize("<red>Invalid material: " + args[2]));
        }
    }

    private void cmdSetPrice(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(mm.deserialize("<red>Usage: /mine setprice <id> <MATERIAL> <price>"));
            return;
        }
        String id = args[1].toUpperCase();
        if (!getConfig().contains("mines." + id)) {
            sender.sendMessage(mm.deserialize("<red>Mine '" + id + "' not found.")); return;
        }
        try {
            org.bukkit.Material.valueOf(args[2].toUpperCase()); // validate
            long price = Long.parseLong(args[3]);
            manager.saveMineToConfig(getConfig(), id,
                "sell-prices." + args[2].toUpperCase(), price);
            sender.sendMessage(mm.deserialize(
                "<green>Set sell price of <white>" + args[2].toUpperCase() +
                "<green> in mine <white>" + id + "<green> to <white>$" + price + "<green>."));
        } catch (NumberFormatException e) {
            sender.sendMessage(mm.deserialize("<red>Invalid price value."));
        } catch (IllegalArgumentException e) {
            sender.sendMessage(mm.deserialize("<red>Invalid material: " + args[2]));
        }
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private void cmdTeleportToMine(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command must be run by a player.");
            return;
        }
        if (args.length < 2) {
            player.sendMessage(mm.deserialize("<red>Usage: /mine tp <id>"));
            return;
        }
        String id = args[1].toUpperCase();
        MineData mine = manager.getMine(id);
        if (mine == null) {
            player.sendMessage(mm.deserialize("<red>Mine '" + id + "' not found or not enabled."));
            return;
        }
        if (!PermissionEngine.getInstance().hasPermission(player, mine.permissionNode())
                && !PermissionEngine.getInstance().hasPermission(player, "prison.admin.*")) {
            player.sendMessage(mm.deserialize("<red>You don't have access to Mine " + id + ". Rank up to unlock it!"));
            return;
        }
        World world = getServer().getWorld(mine.world());
        if (world == null) {
            player.sendMessage(mm.deserialize("<red>Mine world not found."));
            return;
        }
        player.teleport(new Location(world, mine.spawnX(), mine.spawnY(), mine.spawnZ(),
            mine.spawnYaw(), mine.spawnPitch()));
        player.sendMessage(mm.deserialize("<green>Teleported to " + mine.display() + "<green>."));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(mm.deserialize("<gold><bold>═══ /mine commands ═══"));
        sender.sendMessage(mm.deserialize("<yellow>/mine tp <id>  <gray>— teleport to a mine"));
        sender.sendMessage(mm.deserialize("<yellow>/mine list"));
        sender.sendMessage(mm.deserialize("<yellow>/mine info <id>"));
        sender.sendMessage(mm.deserialize("<yellow>/mine reset <id>"));
        sender.sendMessage(mm.deserialize("<yellow>/mine reload"));
        sender.sendMessage(mm.deserialize("<yellow>/mine create <id>"));
        sender.sendMessage(mm.deserialize("<yellow>/mine delete <id>"));
        sender.sendMessage(mm.deserialize("<yellow>/mine enable|disable <id>"));
        sender.sendMessage(mm.deserialize("<yellow>/mine setcorner1|setcorner2 <id>"));
        sender.sendMessage(mm.deserialize("<yellow>/mine setspawn <id>"));
        sender.sendMessage(mm.deserialize("<yellow>/mine setcomposition <id> <MATERIAL> <pct>"));
        sender.sendMessage(mm.deserialize("<yellow>/mine setprice <id> <MATERIAL> <price>"));
    }

    private String formatComposition(MineData mine) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<org.bukkit.Material, Double> e : mine.composition().entrySet()) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(e.getKey().name()).append(":").append(e.getValue().intValue()).append("%");
        }
        return sb.toString();
    }
}
