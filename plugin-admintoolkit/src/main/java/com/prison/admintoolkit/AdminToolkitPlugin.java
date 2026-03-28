package com.prison.admintoolkit;

import com.prison.permissions.PermissionEngine;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AdminToolkitPlugin — main entry point for the admin toolkit.
 *
 * Provides the admin compass, routing of GUI clicks, and server-wide
 * toggles for maintenance mode and global PvP.
 */
public class AdminToolkitPlugin extends JavaPlugin implements Listener {

    private static AdminToolkitPlugin instance;

    private static final MiniMessage MM = MiniMessage.miniMessage();

    // 100ms debounce per player across all GUIs
    private final Map<UUID, Long> lastClick = new ConcurrentHashMap<>();

    private NamespacedKey compassKey;

    private boolean maintenanceMode;
    private boolean globalPvpEnabled;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        maintenanceMode  = getConfig().getBoolean("maintenance-mode", false);
        globalPvpEnabled = getConfig().getBoolean("global-pvp", true);

        compassKey = new NamespacedKey(this, "admin_compass");

        getServer().getPluginManager().registerEvents(this, this);

        getLogger().info("AdminToolkit enabled. Maintenance=" + maintenanceMode + " GlobalPvP=" + globalPvpEnabled);
    }

    @Override
    public void onDisable() {
        getLogger().info("AdminToolkit disabled.");
    }

    public static AdminToolkitPlugin getInstance() {
        return instance;
    }

    // ----------------------------------------------------------------
    // Getters / setters for server state
    // ----------------------------------------------------------------

    public boolean isMaintenanceMode() {
        return maintenanceMode;
    }

    public boolean isGlobalPvpEnabled() {
        return globalPvpEnabled;
    }

    public void setMaintenanceMode(boolean enabled) {
        this.maintenanceMode = enabled;
        getConfig().set("maintenance-mode", enabled);
        saveConfig();

        if (enabled) {
            // Kick non-admin players
            for (Player online : getServer().getOnlinePlayers()) {
                if (!PermissionEngine.getInstance().hasPermission(online, "prison.admintoolkit.use")) {
                    online.kick(MM.deserialize("<red>The server is currently in maintenance mode. Please try again later."));
                }
            }
        }
    }

    public void setGlobalPvp(boolean enabled) {
        this.globalPvpEnabled = enabled;
        getConfig().set("global-pvp", enabled);
        saveConfig();
    }

    // ----------------------------------------------------------------
    // Compass helpers
    // ----------------------------------------------------------------

    public boolean isAdminCompass(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer()
                   .has(compassKey, PersistentDataType.BYTE);
    }

    private ItemStack createAdminCompass() {
        ItemStack compass = new ItemStack(org.bukkit.Material.COMPASS);
        var meta = compass.getItemMeta();
        meta.displayName(MM.deserialize("<gold><bold>Admin Panel"));
        meta.lore(java.util.List.of(MM.deserialize("<gray>Right-click to open the admin panel.")));
        meta.getPersistentDataContainer().set(compassKey, PersistentDataType.BYTE, (byte) 1);
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
        compass.setItemMeta(meta);
        return compass;
    }

    private boolean hasAdminCompass(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isAdminCompass(item)) return true;
        }
        return false;
    }

    // ----------------------------------------------------------------
    // Events — Player Join / Quit
    // ----------------------------------------------------------------

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Maintenance mode check
        if (maintenanceMode && !PermissionEngine.getInstance().hasPermission(player, "prison.admintoolkit.use")) {
            player.kick(MM.deserialize("<red>The server is currently in maintenance mode. Please try again later."));
            return;
        }

        // Give admin compass to staff who don't already have one
        if (PermissionEngine.getInstance().hasPermission(player, "prison.admintoolkit.use")) {
            if (!hasAdminCompass(player)) {
                player.getInventory().addItem(createAdminCompass());
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        MineEditorGUI.cleanup(uuid);
        RankEditorGUI.cleanup(uuid);
        AnvilInputGUI.cleanup(uuid);
        lastClick.remove(uuid);
    }

    // ----------------------------------------------------------------
    // Events — Compass right-click
    // ----------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_AIR
         && event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!isAdminCompass(item)) return;

        if (!PermissionEngine.getInstance().hasPermission(player, "prison.admintoolkit.use")) {
            player.sendMessage(MM.deserialize("<red>You don't have permission to use the admin panel."));
            return;
        }

        event.setCancelled(true);
        AdminPanel.open(player);
    }

    // ----------------------------------------------------------------
    // Events — Inventory Click (GUI routing)
    // ----------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        var title = event.getView().title();
        String plain = PlainTextComponentSerializer.plainText().serialize(title);

        boolean isAdminGui = AdminPanel.isTitle(title)
            || MineEditorGUI.isTitle(title)
            || RankEditorGUI.isTitle(title)
            || EcoToolsGUI.isTitle(title)
            || PlayerManageGUI.isTitle(title)
            || AnnounceGUI.isTitle(title)
            || ServerToolsGUI.isTitle(title)
            || AnvilInputGUI.isAnvilSession(player);

        if (!isAdminGui) return;

        int rawSlot = event.getRawSlot();

        // Ignore outside-inventory clicks in normal GUIs
        if (rawSlot == -999) {
            event.setCancelled(true);
            return;
        }

        // Debounce check (100ms)
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = lastClick.get(uuid);
        if (last != null && now - last < 100) {
            event.setCancelled(true);
            return;
        }
        lastClick.put(uuid, now);

        // Route to correct handler
        if (AdminPanel.isTitle(title)) {
            event.setCancelled(true);
            AdminPanel.handleClick(player, rawSlot);
        } else if (MineEditorGUI.isTitle(title)) {
            event.setCancelled(true);
            MineEditorGUI.handleClick(player, rawSlot, event.getClick());
        } else if (RankEditorGUI.isTitle(title)) {
            event.setCancelled(true);
            RankEditorGUI.handleClick(player, rawSlot, title);
        } else if (EcoToolsGUI.isTitle(title)) {
            event.setCancelled(true);
            EcoToolsGUI.handleClick(player, rawSlot);
        } else if (PlayerManageGUI.isTitle(title)) {
            event.setCancelled(true);
            PlayerManageGUI.handleClick(player, rawSlot);
        } else if (AnnounceGUI.isTitle(title)) {
            event.setCancelled(true);
            AnnounceGUI.handleClick(player, rawSlot);
        } else if (ServerToolsGUI.isTitle(title)) {
            event.setCancelled(true);
            ServerToolsGUI.handleClick(player, rawSlot);
        } else if (AnvilInputGUI.isAnvilSession(player)) {
            // Do NOT cancel automatically — AnvilInputGUI decides
            AnvilInputGUI.handleClick(player, event);
        }
    }

    // ----------------------------------------------------------------
    // Events — Anvil
    // ----------------------------------------------------------------

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        if (!(event.getView().getPlayer() instanceof Player player)) return;
        if (!AnvilInputGUI.isAnvilSession(player)) return;
        AnvilInputGUI.handlePrepare(player, event);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (AnvilInputGUI.isAnvilSession(player)) {
            AnvilInputGUI.handleClose(player);
        }
    }

    // ----------------------------------------------------------------
    // Events — PvP toggle
    // ----------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (globalPvpEnabled) return;
        if (!(event.getDamager() instanceof Player)) return;
        if (!(event.getEntity() instanceof Player)) return;
        event.setCancelled(true);
    }

    // ----------------------------------------------------------------
    // Commands
    // ----------------------------------------------------------------

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command must be run by a player.");
            return true;
        }

        switch (command.getName().toLowerCase()) {
            case "adminpanel" -> {
                if (!PermissionEngine.getInstance().hasPermission(player, "prison.admintoolkit.use")) {
                    player.sendMessage(MM.deserialize("<red>No permission."));
                    return true;
                }
                AdminPanel.open(player);
            }
            case "adminmine" -> {
                if (!PermissionEngine.getInstance().hasPermission(player, "prison.admintoolkit.mines")) {
                    player.sendMessage(MM.deserialize("<red>No permission."));
                    return true;
                }
                if (args.length >= 2 && args[0].equalsIgnoreCase("edit")) {
                    MineEditorGUI.openMineEditor(player, args[1].toUpperCase());
                } else {
                    MineEditorGUI.openMineList(player);
                }
            }
            case "adminranks" -> {
                if (!PermissionEngine.getInstance().hasPermission(player, "prison.admintoolkit.ranks")) {
                    player.sendMessage(MM.deserialize("<red>No permission."));
                    return true;
                }
                RankEditorGUI.open(player);
            }
            case "ecotools" -> {
                if (!PermissionEngine.getInstance().hasPermission(player, "prison.admintoolkit.economy")) {
                    player.sendMessage(MM.deserialize("<red>No permission."));
                    return true;
                }
                if (args.length < 1) {
                    player.sendMessage(MM.deserialize("<red>Usage: /ecotools <player>"));
                    return true;
                }
                EcoToolsGUI.open(player, args[0]);
            }
            case "manage" -> {
                if (!PermissionEngine.getInstance().hasPermission(player, "prison.admintoolkit.use")) {
                    player.sendMessage(MM.deserialize("<red>No permission."));
                    return true;
                }
                if (args.length < 1) {
                    player.sendMessage(MM.deserialize("<red>Usage: /manage <player>"));
                    return true;
                }
                PlayerManageGUI.open(player, args[0]);
            }
            case "announce" -> {
                if (!PermissionEngine.getInstance().hasPermission(player, "prison.admintoolkit.use")) {
                    player.sendMessage(MM.deserialize("<red>No permission."));
                    return true;
                }
                AnnounceGUI.open(player);
            }
            case "servertools" -> {
                if (!PermissionEngine.getInstance().hasPermission(player, "prison.admintoolkit.servercontrols")) {
                    player.sendMessage(MM.deserialize("<red>No permission."));
                    return true;
                }
                ServerToolsGUI.open(player);
            }
        }
        return true;
    }
}
