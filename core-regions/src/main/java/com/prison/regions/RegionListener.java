package com.prison.regions;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * RegionListener — blocks protected actions and fires entry/exit messages.
 *
 * Protection logic:
 *   - Block break/place: denied if any region at the block has build=deny
 *   - PvP: denied if any region at the attacker has pvp=deny
 *   - Mob spawning: denied if any region at the spawn location has mob-spawning=deny
 *   - Player entry: denied if any region has entry=deny and the player lacks bypass permission
 *   - Entry/exit messages: fired when the player's chunk-level region set changes
 *
 * Staff with prison.admin.* or * permission always bypass all protection.
 */
public class RegionListener implements Listener {

    private final RegionEngine engine;
    private final MiniMessage mm = MiniMessage.miniMessage();

    // Track which regions each player is currently inside (for entry/exit messages)
    private final Map<UUID, List<String>> playerRegions = new HashMap<>();

    public RegionListener(RegionEngine engine) {
        this.engine = engine;
    }

    // ----------------------------------------------------------------
    // Block protection
    // ----------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (isBypass(player)) return;

        if (engine.isDenied(event.getBlock().getLocation(), Region.FLAG_BUILD)) {
            event.setCancelled(true);
            player.sendMessage(mm.deserialize("<red>You cannot break blocks here."));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (isBypass(player)) return;

        if (engine.isDenied(event.getBlock().getLocation(), Region.FLAG_BUILD)) {
            event.setCancelled(true);
            player.sendMessage(mm.deserialize("<red>You cannot place blocks here."));
        }
    }

    // ----------------------------------------------------------------
    // PvP protection
    // ----------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        // Only care about player-vs-player
        if (!(event.getEntity() instanceof Player victim)) return;
        Entity attacker = event.getDamager();
        if (!(attacker instanceof Player attackerPlayer)) return;

        if (isBypass(attackerPlayer)) return;

        if (engine.isDenied(victim.getLocation(), Region.FLAG_PVP)) {
            event.setCancelled(true);
            attackerPlayer.sendMessage(mm.deserialize("<red>PvP is disabled here."));
        }
    }

    // ----------------------------------------------------------------
    // Mob spawning protection
    // ----------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        // Only block natural spawns — don't interfere with spawners or eggs
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL
         && event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.DEFAULT) {
            return;
        }

        if (engine.isDenied(event.getLocation(), Region.FLAG_MOB_SPAWNING)) {
            event.setCancelled(true);
        }
    }

    // ----------------------------------------------------------------
    // Entry protection + entry/exit messages
    // ----------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        // Only process if the player moved to a different block
        Location from = event.getFrom();
        Location to   = event.getTo();
        if (to == null) return;
        if (from.getBlockX() == to.getBlockX()
         && from.getBlockY() == to.getBlockY()
         && from.getBlockZ() == to.getBlockZ()) return;

        Player player = event.getPlayer();

        // Entry check — deny flag blocks movement into the region
        if (!isBypass(player) && !engine.canEnter(to, player)) {
            event.setCancelled(true);
            player.sendMessage(mm.deserialize("<red>You cannot enter this area."));
            return;
        }

        // Entry/exit messages — check if the player's region set changed
        fireRegionMessages(player, from, to);
    }

    // ----------------------------------------------------------------
    // Internal
    // ----------------------------------------------------------------

    private void fireRegionMessages(Player player, Location from, Location to) {
        List<String> oldRegions = playerRegions.getOrDefault(player.getUniqueId(), List.of());
        List<Region> nowIn = engine.getRegionsAt(to);
        List<String> newRegions = nowIn.stream().map(Region::getName).toList();

        playerRegions.put(player.getUniqueId(), newRegions);

        // Exit messages — regions player just left
        for (String name : oldRegions) {
            if (!newRegions.contains(name)) {
                Region r = engine.getRegion(name);
                if (r != null && r.getExitMessage() != null && !r.getExitMessage().isBlank()) {
                    player.sendMessage(mm.deserialize(r.getExitMessage()));
                }
            }
        }

        // Entry messages — regions player just entered
        for (Region r : nowIn) {
            if (!oldRegions.contains(r.getName())) {
                if (r.getEntryMessage() != null && !r.getEntryMessage().isBlank()) {
                    player.sendMessage(mm.deserialize(r.getEntryMessage()));
                }
            }
        }
    }

    /**
     * Staff bypass — anyone with prison.admin.* or * skips all protection checks.
     */
    private boolean isBypass(Player player) {
        return player.hasPermission("prison.admin.*") || player.hasPermission("*");
    }

    /**
     * Call when a player disconnects to clean up their region tracking state.
     */
    public void untrack(UUID uuid) {
        playerRegions.remove(uuid);
    }
}
