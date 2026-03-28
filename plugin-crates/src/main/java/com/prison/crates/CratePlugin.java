package com.prison.crates;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * CratePlugin — main entry point for the crate and key system.
 */
public class CratePlugin extends JavaPlugin implements Listener {

    private static CratePlugin instance;
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private CrateManager manager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        // Init PDC key
        CrateKey.init(this);

        // Init manager
        manager = CrateManager.initialize(this, getLogger());
        manager.loadConfig();

        // Register events and commands
        getServer().getPluginManager().registerEvents(this, this);

        var crateCmd = getCommand("crate");
        if (crateCmd != null) crateCmd.setExecutor(new CrateCommand(manager));

        getLogger().info("CratePlugin enabled — " + manager.getTiers().size() + " tiers loaded.");
    }

    @Override
    public void onDisable() {
        // Cancel all active sessions — deliver rewards immediately so nothing is lost
        for (CrateOpeningSession session : CrateOpeningSession.activeSessions.values()) {
            session.cancel(true);
        }
        CrateOpeningSession.activeSessions.clear();
        CrateManager.reset();
        instance = null;
        getLogger().info("CratePlugin disabled.");
    }

    public static CratePlugin getInstance() { return instance; }

    // ----------------------------------------------------------------
    // Events
    // ----------------------------------------------------------------

    /** Deliver any offline-queued keys when a player joins. */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        manager.deliverPendingKeys(event.getPlayer());
    }

    /** Cancel active animation on disconnect — deliver reward so it's not lost. */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        CrateOpeningSession session = CrateOpeningSession.activeSessions.get(event.getPlayer().getUniqueId());
        if (session != null) session.cancel(true);
    }

    /** Intercept right-clicks on crate blocks. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        Location loc = block.getLocation();
        if (manager.getTierAtBlock(loc) == null) return;

        // It's a crate block
        event.setCancelled(true);

        manager.tryOpenCrate(event.getPlayer(), loc);
    }

    /** Prevent players from taking items out of the crate opening GUI. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof org.bukkit.entity.Player player)) return;
        if (!CrateOpeningSession.isOpening(player.getUniqueId())) return;
        event.setCancelled(true);
    }

    /** If the player closes the GUI during animation, cancel and deliver. */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof org.bukkit.entity.Player player)) return;
        CrateOpeningSession session = CrateOpeningSession.activeSessions.get(player.getUniqueId());
        if (session == null) return;
        // Deliver reward even if closed early — items must never be lost
        session.cancel(true);
    }
}
