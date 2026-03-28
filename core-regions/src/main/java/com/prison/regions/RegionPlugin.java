package com.prison.regions;

import com.prison.database.DatabaseManager;
import com.google.gson.Gson;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * RegionPlugin — main class for the core-regions module.
 *
 * On startup: loads all regions from the database into the engine.
 * Commands:
 *   /rg tool            — gives admin the region wand
 *   /rg create <name>   — creates region from current wand selection
 *   /rg delete <name>   — removes a region
 *   /rg list            — lists all regions
 *   /rg reload          — reloads all regions from database
 *   /rg flag <name> <flag> <allow|deny|clear> — set a flag
 *   /rg info <name>     — show region details
 */
public class RegionPlugin extends JavaPlugin implements Listener {

    private RegionEngine engine;
    private RegionWand   wand;
    private RegionListener regionListener;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final Gson gson = new Gson();

    @Override
    public void onEnable() {
        if (DatabaseManager.getInstance() == null) {
            getLogger().severe("core-database must be loaded before core-regions!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        engine         = RegionEngine.initialize(getLogger());
        wand           = new RegionWand(this);
        regionListener = new RegionListener(engine);

        try {
            engine.loadFromDatabase();
        } catch (SQLException e) {
            getLogger().severe("[Regions] Failed to load regions from database: " + e.getMessage());
        }

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(regionListener, this);

        getLogger().info("Region engine enabled — " + engine.getAllRegions().size() + " regions loaded.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Region engine disabled.");
    }

    // ----------------------------------------------------------------
    // Wand interaction — handle left/right click with the wand item
    // ----------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH)
    public void onWandUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("prison.admintoolkit.regions")) return;

        var item = player.getInventory().getItemInMainHand();
        if (!wand.isWand(item)) return;

        Action action = event.getAction();
        if (action == Action.LEFT_CLICK_BLOCK && event.getClickedBlock() != null) {
            event.setCancelled(true);
            wand.setCorner1(player, event.getClickedBlock().getLocation());
        } else if (action == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            event.setCancelled(true);
            wand.setCorner2(player, event.getClickedBlock().getLocation());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        wand.clearSelection(event.getPlayer());
        regionListener.untrack(event.getPlayer().getUniqueId());
    }

    // ----------------------------------------------------------------
    // Commands
    // ----------------------------------------------------------------

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("rg")) return false;

        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command must be run by a player.");
            return true;
        }

        if (!player.hasPermission("prison.admintoolkit.regions")) {
            player.sendMessage(mm.deserialize("<red>You don't have permission to manage regions."));
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "tool"   -> cmdTool(player);
            case "create" -> cmdCreate(player, args);
            case "delete" -> cmdDelete(player, args);
            case "list"   -> cmdList(player);
            case "info"   -> cmdInfo(player, args);
            case "flag"   -> cmdFlag(player, args);
            case "reload" -> cmdReload(player);
            default       -> sendHelp(player);
        }
        return true;
    }

    private void cmdTool(Player player) {
        player.getInventory().addItem(wand.createWand());
        player.sendMessage(mm.deserialize("<green>Region wand given. Left-click = corner 1, right-click = corner 2."));
    }

    private void cmdCreate(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(mm.deserialize("<red>Usage: /rg create <name>"));
            return;
        }
        if (!wand.hasBothCorners(player)) {
            player.sendMessage(mm.deserialize("<red>Select both corners first using the region wand (/rg tool)."));
            return;
        }

        String name = args[1].toLowerCase();
        if (engine.getRegion(name) != null) {
            player.sendMessage(mm.deserialize("<red>A region named '" + name + "' already exists."));
            return;
        }

        var c1 = wand.getCorner1(player);
        var c2 = wand.getCorner2(player);

        if (!c1.getWorld().equals(c2.getWorld())) {
            player.sendMessage(mm.deserialize("<red>Both corners must be in the same world."));
            return;
        }

        try {
            DatabaseManager.getInstance().execute(
                "INSERT INTO regions (name, world, x1, y1, z1, x2, y2, z2, priority) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0)",
                name,
                c1.getWorld().getName(),
                c1.getBlockX(), c1.getBlockY(), c1.getBlockZ(),
                c2.getBlockX(), c2.getBlockY(), c2.getBlockZ()
            );

            // Reload this region into cache
            engine.loadFromDatabase();
            wand.clearSelection(player);

            player.sendMessage(mm.deserialize("<green>Region <white>" + name + "</white> created. Use <white>/rg flag " + name + " <flag> <allow|deny></white> to configure it."));
        } catch (SQLException e) {
            getLogger().severe("[Regions] Failed to create region: " + e.getMessage());
            player.sendMessage(mm.deserialize("<red>Database error — check server console."));
        }
    }

    private void cmdDelete(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(mm.deserialize("<red>Usage: /rg delete <name>"));
            return;
        }
        String name = args[1].toLowerCase();
        if (engine.getRegion(name) == null) {
            player.sendMessage(mm.deserialize("<red>No region named '" + name + "' exists."));
            return;
        }

        try {
            DatabaseManager.getInstance().execute("DELETE FROM regions WHERE name = ?", name);
            engine.removeRegion(name);
            player.sendMessage(mm.deserialize("<green>Region <white>" + name + "</white> deleted."));
        } catch (SQLException e) {
            getLogger().severe("[Regions] Failed to delete region: " + e.getMessage());
            player.sendMessage(mm.deserialize("<red>Database error — check server console."));
        }
    }

    private void cmdList(Player player) {
        var names = engine.getRegionNames();
        if (names.isEmpty()) {
            player.sendMessage(mm.deserialize("<yellow>No regions defined."));
            return;
        }
        player.sendMessage(mm.deserialize("<gold>Regions (" + names.size() + "):"));
        for (String name : names) {
            Region r = engine.getRegion(name);
            player.sendMessage(mm.deserialize("  <white>" + name
                + " <gray>| priority: " + r.getPriority()
                + " | world: " + r.getWorld()));
        }
    }

    private void cmdInfo(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(mm.deserialize("<red>Usage: /rg info <name>"));
            return;
        }
        String name = args[1].toLowerCase();
        Region r = engine.getRegion(name);
        if (r == null) {
            player.sendMessage(mm.deserialize("<red>No region named '" + name + "' exists."));
            return;
        }
        player.sendMessage(mm.deserialize("<gold>Region: <white>" + r.getName()));
        player.sendMessage(mm.deserialize("<gray>World: <white>" + r.getWorld()));
        player.sendMessage(mm.deserialize("<gray>Corner 1: <white>" + r.getX1() + ", " + r.getY1() + ", " + r.getZ1()));
        player.sendMessage(mm.deserialize("<gray>Corner 2: <white>" + r.getX2() + ", " + r.getY2() + ", " + r.getZ2()));
        player.sendMessage(mm.deserialize("<gray>Priority: <white>" + r.getPriority()));
        player.sendMessage(mm.deserialize("<gray>Flags: <white>" + (r.getFlags().isEmpty() ? "none" : r.getFlags().toString())));
        if (r.getEntryMessage() != null) player.sendMessage(mm.deserialize("<gray>Entry: <white>" + r.getEntryMessage()));
        if (r.getExitMessage()  != null) player.sendMessage(mm.deserialize("<gray>Exit: <white>"  + r.getExitMessage()));
    }

    private void cmdFlag(Player player, String[] args) {
        // /rg flag <name> <flag> <allow|deny|clear>
        if (args.length < 4) {
            player.sendMessage(mm.deserialize("<red>Usage: /rg flag <name> <pvp|build|entry|mob-spawning> <allow|deny|clear>"));
            return;
        }
        String name  = args[1].toLowerCase();
        String flag  = args[2].toLowerCase();
        String value = args[3].toLowerCase();

        Region r = engine.getRegion(name);
        if (r == null) {
            player.sendMessage(mm.deserialize("<red>No region named '" + name + "' exists."));
            return;
        }

        // Validate
        if (!flag.equals(Region.FLAG_PVP) && !flag.equals(Region.FLAG_BUILD)
         && !flag.equals(Region.FLAG_ENTRY) && !flag.equals(Region.FLAG_MOB_SPAWNING)
         && !flag.equals("entry-message") && !flag.equals("exit-message") && !flag.equals("priority")) {
            player.sendMessage(mm.deserialize("<red>Unknown flag. Valid: pvp, build, entry, mob-spawning, entry-message, exit-message, priority"));
            return;
        }

        try {
            if (flag.equals("entry-message") || flag.equals("exit-message")) {
                String msg = String.join(" ", java.util.Arrays.copyOfRange(args, 3, args.length));
                String col  = flag.equals("entry-message") ? "entry_message" : "exit_message";
                DatabaseManager.getInstance().execute(
                    "UPDATE regions SET " + col + " = ? WHERE name = ?",
                    value.equals("clear") ? null : msg, name
                );
            } else if (flag.equals("priority")) {
                int prio = Integer.parseInt(value);
                DatabaseManager.getInstance().execute(
                    "UPDATE regions SET priority = ? WHERE name = ?", prio, name
                );
            } else {
                // Build updated flags map
                Map<String, String> flags = new HashMap<>(r.getFlags());
                if (value.equals("clear")) {
                    flags.remove(flag);
                } else {
                    if (!value.equals(Region.ALLOW) && !value.equals(Region.DENY)) {
                        player.sendMessage(mm.deserialize("<red>Value must be 'allow', 'deny', or 'clear'."));
                        return;
                    }
                    flags.put(flag, value);
                }
                DatabaseManager.getInstance().execute(
                    "UPDATE regions SET flags = ? WHERE name = ?",
                    flags.isEmpty() ? null : gson.toJson(flags), name
                );
            }

            engine.loadFromDatabase();
            player.sendMessage(mm.deserialize("<green>Flag <white>" + flag + "</white> set to <white>" + value + "</white> on region <white>" + name + "</white>."));
        } catch (NumberFormatException e) {
            player.sendMessage(mm.deserialize("<red>Priority must be a number."));
        } catch (SQLException e) {
            getLogger().severe("[Regions] Failed to set flag: " + e.getMessage());
            player.sendMessage(mm.deserialize("<red>Database error — check server console."));
        }
    }

    private void cmdReload(Player player) {
        try {
            engine.loadFromDatabase();
            player.sendMessage(mm.deserialize("<green>Regions reloaded — " + engine.getAllRegions().size() + " loaded."));
        } catch (SQLException e) {
            player.sendMessage(mm.deserialize("<red>Failed to reload: " + e.getMessage()));
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage(mm.deserialize("<gold>Region Commands:"));
        player.sendMessage(mm.deserialize("<gray>/rg tool                           <white>Get the region wand"));
        player.sendMessage(mm.deserialize("<gray>/rg create <name>                  <white>Create region from wand selection"));
        player.sendMessage(mm.deserialize("<gray>/rg delete <name>                  <white>Delete a region"));
        player.sendMessage(mm.deserialize("<gray>/rg flag <name> <flag> <val>       <white>Set a region flag"));
        player.sendMessage(mm.deserialize("<gray>/rg info <name>                    <white>Show region details"));
        player.sendMessage(mm.deserialize("<gray>/rg list                           <white>List all regions"));
        player.sendMessage(mm.deserialize("<gray>/rg reload                         <white>Reload from database"));
    }
}
