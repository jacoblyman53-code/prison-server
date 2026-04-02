package com.prison.warps;

import com.prison.permissions.PermissionEngine;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class WarpsPlugin extends JavaPlugin implements Listener {

    private WarpAPI api;
    private final MiniMessage mm = MiniMessage.miniMessage();

    // GUI title constants — used to identify our inventories in click events
    private static final String WARPS_TITLE_STR = "<dark_gray><bold>Warps</bold></dark_gray>";

    @Override
    public void onEnable() {
        if (PermissionEngine.getInstance() == null) {
            getLogger().severe("PrisonPermissions must be loaded before PrisonWarps!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();
        api = new WarpAPI(getLogger());
        api.loadFromDatabase(); // async — cache ready within a tick

        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("Warp and navigation system enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Warp system disabled.");
    }

    // ----------------------------------------------------------------
    // Commands
    // ----------------------------------------------------------------

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        switch (command.getName().toLowerCase()) {
            case "spawn"  -> handleSpawn(sender, args);
            case "warp"   -> handleWarp(sender, args);
            case "warps"  -> handleWarpsGui(sender);
            default -> { return false; }
        }
        return true;
    }

    // /spawn [set]
    private void handleSpawn(CommandSender sender, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("set")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("This command must be run by a player."); return;
            }
            if (!PermissionEngine.getInstance().hasPermission(player, "prison.admin.*")) {
                player.sendMessage(mm.deserialize("<red>No permission.")); return;
            }
            Location loc = player.getLocation();
            getConfig().set("spawn.world", loc.getWorld().getName());
            getConfig().set("spawn.x",     loc.getX());
            getConfig().set("spawn.y",     loc.getY());
            getConfig().set("spawn.z",     loc.getZ());
            getConfig().set("spawn.yaw",   (double) loc.getYaw());
            getConfig().set("spawn.pitch", (double) loc.getPitch());
            saveConfig();
            player.sendMessage(mm.deserialize("<green>Spawn set to your current location."));
            return;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command must be run by a player."); return;
        }
        Location spawn = getSpawnLocation();
        if (spawn == null) {
            player.sendMessage(mm.deserialize("<red>Spawn has not been set. Use /spawn set."));
            return;
        }
        player.teleport(spawn);
        player.sendMessage(mm.deserialize("<green>Teleported to spawn."));
    }

    // /warp <name> | /warp create <name> | /warp delete <name> |
    // /warp list   | /warp setperm <name> <node>
    private void handleWarp(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command must be run by a player."); return;
        }

        if (args.length == 0) {
            handleWarpsGui(sender);
            return;
        }

        boolean isAdmin = PermissionEngine.getInstance().hasPermission(player, "prison.admin.*");

        switch (args[0].toLowerCase()) {
            case "create" -> {
                if (!isAdmin) { player.sendMessage(mm.deserialize("<red>No permission.")); return; }
                if (args.length < 2) { player.sendMessage(mm.deserialize("<red>Usage: /warp create <name>")); return; }
                String name = args[1].toLowerCase();
                if (api.getWarp(name) != null) {
                    player.sendMessage(mm.deserialize("<red>A warp named '" + name + "' already exists.")); return;
                }
                Location loc = player.getLocation();
                api.createWarp(name, loc.getWorld().getName(),
                    loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch(),
                    null, player.getUniqueId().toString())
                    .thenAccept(ok -> getServer().getScheduler().runTask(this, () -> {
                        if (ok) player.sendMessage(mm.deserialize("<green>Warp <white>" + name + "<green> created."));
                        else    player.sendMessage(mm.deserialize("<red>Failed to create warp."));
                    }));
            }
            case "delete" -> {
                if (!isAdmin) { player.sendMessage(mm.deserialize("<red>No permission.")); return; }
                if (args.length < 2) { player.sendMessage(mm.deserialize("<red>Usage: /warp delete <name>")); return; }
                String name = args[1].toLowerCase();
                if (api.getWarp(name) == null) {
                    player.sendMessage(mm.deserialize("<red>Warp '" + name + "' not found.")); return;
                }
                api.deleteWarp(name).thenAccept(ok -> getServer().getScheduler().runTask(this, () -> {
                    if (ok) player.sendMessage(mm.deserialize("<green>Warp <white>" + name + "<green> deleted."));
                    else    player.sendMessage(mm.deserialize("<red>Failed to delete warp."));
                }));
            }
            case "setperm" -> {
                if (!isAdmin) { player.sendMessage(mm.deserialize("<red>No permission.")); return; }
                if (args.length < 3) { player.sendMessage(mm.deserialize("<red>Usage: /warp setperm <name> <node>")); return; }
                String name = args[1].toLowerCase();
                String node = args[2];
                if (api.getWarp(name) == null) {
                    player.sendMessage(mm.deserialize("<red>Warp '" + name + "' not found.")); return;
                }
                api.setPermission(name, node).thenAccept(ok -> getServer().getScheduler().runTask(this, () -> {
                    if (ok) player.sendMessage(mm.deserialize("<green>Permission set for warp <white>" + name + "<green>."));
                    else    player.sendMessage(mm.deserialize("<red>Failed to update warp permission."));
                }));
            }
            case "list" -> {
                List<WarpData> warps = api.getAccessibleWarps(player);
                player.sendMessage(mm.deserialize("<gold><bold>Warps:"));
                if (warps.isEmpty()) { player.sendMessage(mm.deserialize("<gray>No warps available.")); return; }
                for (WarpData w : warps) {
                    String perm = w.permissionNode() == null ? "<dark_gray>public" : "<dark_gray>" + w.permissionNode();
                    player.sendMessage(mm.deserialize(" <gray>- <white>" + w.name() + " " + perm));
                }
            }
            default -> {
                // /warp <name>
                String name = args[0].toLowerCase();
                WarpData warp = api.getWarp(name);
                if (warp == null) {
                    player.sendMessage(mm.deserialize("<red>Warp '" + name + "' not found.")); return;
                }
                if (!api.canUseWarp(player, warp)) {
                    player.sendMessage(mm.deserialize("<red>You do not have permission to use that warp.")); return;
                }
                World world = Bukkit.getWorld(warp.world());
                if (world == null) {
                    player.sendMessage(mm.deserialize("<red>Warp world not found.")); return;
                }
                player.teleport(new Location(world, warp.x(), warp.y(), warp.z(), warp.yaw(), warp.pitch()));
                player.sendMessage(mm.deserialize("<green>Teleported to <white>" + warp.name() + "<green>."));
            }
        }
    }

    private void handleWarpsGui(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command must be run by a player."); return;
        }
        openWarpsGui(player);
    }

    // ----------------------------------------------------------------
    // Warps GUI
    // ----------------------------------------------------------------

    private void openWarpsGui(Player player) {
        List<WarpData> warps = api.getAccessibleWarps(player);

        int rows = warps.isEmpty() ? 1 : Math.min(6, (warps.size() + 8) / 9);
        int size = rows * 9;
        Inventory inv = Bukkit.createInventory(null, size,
            mm.deserialize(WARPS_TITLE_STR));

        for (int i = 0; i < warps.size() && i < size; i++) {
            WarpData warp = warps.get(i);
            String permLine = warp.permissionNode() == null
                ? "<dark_gray>Public warp"
                : "<dark_gray>Requires: " + warp.permissionNode();
            inv.setItem(i, makeItem(Material.ENDER_PEARL,
                "<aqua>" + warp.name(), permLine, "<gray>Click to teleport"));
        }

        if (warps.isEmpty()) {
            inv.setItem(4, makeItem(Material.GRAY_STAINED_GLASS_PANE,
                "<gray>No warps available"));
        }

        player.openInventory(inv);
    }

    // ----------------------------------------------------------------
    // GUI Click Handler
    // ----------------------------------------------------------------

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Component title = event.getView().title();
        Component warpsTitle = mm.deserialize(WARPS_TITLE_STR);

        if (title.equals(warpsTitle)) {
            event.setCancelled(true);
            handleWarpsClick(player, event.getRawSlot(),
                api.getAccessibleWarps(player));
        }
    }

    private void handleWarpsClick(Player player, int slot, List<WarpData> warps) {
        if (slot < 0 || slot >= warps.size()) return;
        WarpData warp = warps.get(slot);

        player.closeInventory();
        getServer().getScheduler().runTaskLater(this, () -> {
            World world = Bukkit.getWorld(warp.world());
            if (world == null) {
                player.sendMessage(mm.deserialize("<red>Warp world not found.")); return;
            }
            player.teleport(new Location(world, warp.x(), warp.y(), warp.z(),
                warp.yaw(), warp.pitch()));
            player.sendMessage(mm.deserialize("<green>Teleported to <white>" + warp.name() + "<green>."));
        }, 1L);
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private Location getSpawnLocation() {
        String world = getConfig().getString("spawn.world", "world");
        World w = Bukkit.getWorld(world);
        if (w == null) return null;
        return new Location(w,
            getConfig().getDouble("spawn.x",     0.5),
            getConfig().getDouble("spawn.y",     64.0),
            getConfig().getDouble("spawn.z",     0.5),
            (float) getConfig().getDouble("spawn.yaw",   0.0),
            (float) getConfig().getDouble("spawn.pitch", 0.0));
    }

    private ItemStack makeItem(Material mat, String name, String... loreParts) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(mm.deserialize(name));
        if (loreParts.length > 0) {
            List<Component> lore = new ArrayList<>();
            for (String line : loreParts) lore.add(mm.deserialize(line));
            meta.lore(lore);
        }
        item.setItemMeta(meta);
        return item;
    }
}
