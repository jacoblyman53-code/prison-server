package com.prison.mines;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

/**
 * MineManager — owns all mine data and state, drives the reset cycle.
 *
 * Reset flow:
 *   1. Mark mine as resetting, teleport players out (main thread)
 *   2. Build full block list async (weighted random material for every position)
 *   3. Apply blocks in batches of batchSize per tick (main thread)
 *   4. On completion: mark reset done, broadcast
 */
public class MineManager {

    private static MineManager instance;

    private final JavaPlugin plugin;
    private final Logger logger;
    private final MiniMessage mm = MiniMessage.miniMessage();

    private final LinkedHashMap<String, MineData>  mines  = new LinkedHashMap<>();
    private final HashMap<String, MineState>       states = new HashMap<>();

    private int batchSize = 1000;
    private String warningMsg = "<gold>[Mine {mine}]</gold> <gray>Mine is resetting — stand clear!";
    private String doneMsg    = "<gold>[Mine {mine}]</gold> <green>Reset complete.";

    MineManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        instance = this;
    }

    public static MineManager getInstance() { return instance; }

    // ----------------------------------------------------------------
    // Config Loading
    // ----------------------------------------------------------------

    public void loadFromConfig(FileConfiguration config) {
        mines.clear();
        states.clear();

        batchSize  = config.getInt("reset-batch-size", 1000);
        warningMsg = config.getString("reset-warning-message",
            "<gold>[Mine {mine}]</gold> <gray>Mine is resetting — stand clear!");
        doneMsg    = config.getString("reset-done-message",
            "<gold>[Mine {mine}]</gold> <green>Reset complete.");

        ConfigurationSection minesSection = config.getConfigurationSection("mines");
        if (minesSection == null) {
            logger.info("[Mines] No mines defined in config.");
            return;
        }

        int loaded = 0;
        for (String id : minesSection.getKeys(false)) {
            ConfigurationSection s = minesSection.getConfigurationSection(id);
            if (s == null) continue;

            if (!s.getBoolean("enabled", false)) {
                // Register state for disabled mines too (so /mine info shows them)
                states.put(id.toUpperCase(), new MineState());
                continue;
            }

            MineData mine = parseMine(id.toUpperCase(), s);
            if (mine == null) continue;

            mines.put(mine.id(), mine);
            states.put(mine.id(), new MineState());
            loaded++;
        }

        logger.info("[Mines] Loaded " + loaded + " enabled mines.");
    }

    @SuppressWarnings("unchecked")
    private MineData parseMine(String id, ConfigurationSection s) {
        String display = s.getString("display", id);
        String world   = s.getString("world", "world");

        List<Integer> c1 = (List<Integer>) s.getList("corner1", List.of(0, 64, 0));
        List<Integer> c2 = (List<Integer>) s.getList("corner2", List.of(0, 64, 0));

        // Normalise so x1 ≤ x2, y1 ≤ y2, z1 ≤ z2
        int x1 = Math.min(c1.get(0), c2.get(0)), x2 = Math.max(c1.get(0), c2.get(0));
        int y1 = Math.min(c1.get(1), c2.get(1)), y2 = Math.max(c1.get(1), c2.get(1));
        int z1 = Math.min(c1.get(2), c2.get(2)), z2 = Math.max(c1.get(2), c2.get(2));

        double spawnX   = s.getDouble("spawn-x", 0.5);
        double spawnY   = s.getDouble("spawn-y", 66.0);
        double spawnZ   = s.getDouble("spawn-z", 0.5);
        float  spawnYaw = (float) s.getDouble("spawn-yaw", 0.0);
        float  spawnPit = (float) s.getDouble("spawn-pitch", 0.0);

        // Composition
        Map<Material, Double> composition = new LinkedHashMap<>();
        ConfigurationSection compSec = s.getConfigurationSection("composition");
        if (compSec != null) {
            for (String matName : compSec.getKeys(false)) {
                try {
                    Material mat = Material.valueOf(matName.toUpperCase());
                    composition.put(mat, compSec.getDouble(matName));
                } catch (IllegalArgumentException e) {
                    logger.warning("[Mines] Unknown material in mine " + id + ": " + matName);
                }
            }
        }
        if (composition.isEmpty()) {
            logger.warning("[Mines] Mine " + id + " has no composition — using STONE.");
            composition.put(Material.STONE, 100.0);
        }

        // Per-mine sell prices (optional)
        Map<Material, Long> sellPrices = new LinkedHashMap<>();
        ConfigurationSection priceSec = s.getConfigurationSection("sell-prices");
        if (priceSec != null) {
            for (String matName : priceSec.getKeys(false)) {
                try {
                    Material mat = Material.valueOf(matName.toUpperCase());
                    sellPrices.put(mat, priceSec.getLong(matName));
                } catch (IllegalArgumentException e) {
                    logger.warning("[Mines] Unknown material in sell-prices for mine " + id + ": " + matName);
                }
            }
        }

        return new MineData(
            id, display, world,
            x1, y1, z1, x2, y2, z2,
            spawnX, spawnY, spawnZ, spawnYaw, spawnPit,
            Collections.unmodifiableMap(composition),
            Collections.unmodifiableMap(sellPrices),
            s.getInt("reset-timer-mins", 15),
            s.getDouble("reset-threshold", 0.80),
            s.getString("permission-node", "prison.mine." + id.toLowerCase()),
            s.getString("mine-type", "STANDARD"),
            s.getInt("prestige-required", 0)
        );
    }

    // ----------------------------------------------------------------
    // Config Saving (admin commands)
    // ----------------------------------------------------------------

    public void saveMineToConfig(FileConfiguration config, String id,
                                  String field, Object value) {
        config.set("mines." + id + "." + field, value);
        plugin.saveConfig();
    }

    public void createMineInConfig(FileConfiguration config, String id) {
        String path = "mines." + id;
        config.set(path + ".enabled",           false);
        config.set(path + ".display",            "<gray>[<white>" + id + "</white>]</gray>");
        config.set(path + ".world",              "world");
        config.set(path + ".corner1",            List.of(0, 64, 0));
        config.set(path + ".corner2",            List.of(0, 64, 0));
        config.set(path + ".spawn-x",            0.5);
        config.set(path + ".spawn-y",            66.0);
        config.set(path + ".spawn-z",            0.5);
        config.set(path + ".spawn-yaw",          0.0);
        config.set(path + ".spawn-pitch",        0.0);
        config.set(path + ".composition.STONE",  100.0);
        config.set(path + ".sell-prices",        new LinkedHashMap<>());
        config.set(path + ".reset-timer-mins",   15);
        config.set(path + ".reset-threshold",    0.80);
        config.set(path + ".permission-node",    "prison.mine." + id.toLowerCase());
        config.set(path + ".mine-type",          "STANDARD");
        config.set(path + ".prestige-required",  0);
        plugin.saveConfig();
    }

    public void deleteMineFromConfig(FileConfiguration config, String id) {
        config.set("mines." + id, null);
        plugin.saveConfig();
    }

    // ----------------------------------------------------------------
    // Lookups
    // ----------------------------------------------------------------

    public Map<String, MineData>  getMines()              { return Collections.unmodifiableMap(mines); }
    public MineData               getMine(String id)      { return mines.get(id.toUpperCase()); }
    public MineState              getState(String id)     { return states.get(id.toUpperCase()); }

    /** Returns the mine containing this world position, or null if none. */
    public MineData getMineAt(String worldName, int x, int y, int z) {
        for (MineData mine : mines.values()) {
            if (mine.world().equals(worldName) && mine.contains(x, y, z)) return mine;
        }
        return null;
    }

    /** Returns the standard mine for a given rank letter (e.g. "A" → mine A). */
    public MineData getMineForRank(String rank) {
        return mines.get(rank.toUpperCase());
    }

    // ----------------------------------------------------------------
    // Reset
    // ----------------------------------------------------------------

    /**
     * Trigger an async mine reset.
     * Safe to call from any thread; teleports and block changes are
     * scheduled back onto the main thread.
     */
    public void triggerReset(String mineId) {
        MineData mine = getMine(mineId);
        if (mine == null) {
            logger.warning("[Mines] triggerReset called for unknown mine: " + mineId);
            return;
        }
        MineState state = getState(mineId);
        if (state.isResetting()) return; // already in progress
        state.setResetting(true);

        // Step 1 — teleport players and broadcast (main thread)
        Bukkit.getScheduler().runTask(plugin, () -> {
            broadcastInMine(mine, warningMsg);
            teleportPlayersOut(mine);

            // Step 2 — build block list async, then apply on main thread
            CompletableFuture.supplyAsync(() -> buildBlockList(mine))
                .thenAccept(blocks ->
                    Bukkit.getScheduler().runTask(plugin, () ->
                        applyBatch(mine, state, blocks, 0)));
        });
    }

    /** Build the full list of (blockIndex → material) for the mine. Runs async. */
    private List<Material> buildBlockList(MineData mine) {
        // Pre-compute cumulative weight array for O(log n) weighted selection
        List<Material> materials = new ArrayList<>(mine.composition().keySet());
        double[] cumulative = new double[materials.size()];
        double total = 0;
        for (int i = 0; i < materials.size(); i++) {
            total += mine.composition().get(materials.get(i));
            cumulative[i] = total;
        }

        int count = mine.totalBlocks();
        List<Material> result = new ArrayList<>(count);
        ThreadLocalRandom rng = ThreadLocalRandom.current();

        for (int i = 0; i < count; i++) {
            double pick = rng.nextDouble(total);
            Material chosen = materials.get(materials.size() - 1); // fallback
            for (int j = 0; j < cumulative.length; j++) {
                if (pick < cumulative[j]) { chosen = materials.get(j); break; }
            }
            result.add(chosen);
        }
        return result;
    }

    /** Apply one batch of block changes, then schedule the next batch. */
    private void applyBatch(MineData mine, MineState state,
                             List<Material> blocks, int offset) {
        World world = Bukkit.getWorld(mine.world());
        if (world == null) {
            logger.warning("[Mines] World '" + mine.world() + "' not found during reset of mine " + mine.id());
            state.setResetting(false);
            return;
        }

        int idx   = offset;
        int end   = Math.min(offset + batchSize, blocks.size());
        int blockIdx = 0;

        outer:
        for (int y = mine.y1(); y <= mine.y2(); y++) {
            for (int z = mine.z1(); z <= mine.z2(); z++) {
                for (int x = mine.x1(); x <= mine.x2(); x++) {
                    if (blockIdx < idx)   { blockIdx++; continue; }
                    if (blockIdx >= end)  break outer;
                    // applyPhysics=false prevents cascading updates (critical for perf)
                    world.getBlockAt(x, y, z).setType(blocks.get(blockIdx), false);
                    blockIdx++;
                }
            }
        }

        if (end < blocks.size()) {
            // More batches to go — schedule next tick
            Bukkit.getScheduler().runTaskLater(plugin,
                () -> applyBatch(mine, state, blocks, end), 1L);
        } else {
            // All done
            state.markReset();
            state.setResetting(false);
            broadcastInMine(mine, doneMsg);
            logger.info("[Mines] Mine " + mine.id() + " reset complete (" +
                mine.totalBlocks() + " blocks).");
        }
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private void teleportPlayersOut(MineData mine) {
        World world = Bukkit.getWorld(mine.world());
        if (world == null) return;

        Location spawn = new Location(world,
            mine.spawnX(), mine.spawnY(), mine.spawnZ(),
            mine.spawnYaw(), mine.spawnPitch());

        for (Player player : world.getPlayers()) {
            Location loc = player.getLocation();
            if (mine.contains(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ())) {
                player.teleport(spawn);
                player.sendMessage(mm.deserialize(
                    "<gray>You were moved for mine reset."));
            }
        }
    }

    private void broadcastInMine(MineData mine, String template) {
        World world = Bukkit.getWorld(mine.world());
        if (world == null) return;

        String msg = template.replace("{mine}", mine.id());
        var component = mm.deserialize(msg);

        // Broadcast to everyone in the same world (mine resets are a world event)
        for (Player player : world.getPlayers()) {
            player.sendMessage(component);
        }
    }
}
