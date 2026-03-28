package com.prison.permissions;

import com.prison.database.DatabaseManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * PermissionsPlugin — main class for the core-permissions module.
 *
 * On startup: loads group definitions from config, initializes the engine.
 * On player join: loads their permissions into memory.
 * On player leave: removes them from memory.
 */
public class PermissionsPlugin extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        // Database must be loaded first
        if (DatabaseManager.getInstance() == null) {
            getLogger().severe("core-database must be loaded before core-permissions!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();

        // Add staff_rank column if it doesn't exist yet
        // (safe to run every startup — ignores error if column exists)
        ensureStaffRankColumn();

        // Initialize the engine
        PermissionEngine engine = PermissionEngine.initialize(getLogger());

        // Load group definitions from config.yml
        loadGroupsFromConfig(engine);

        // Register player join/leave listeners
        getServer().getPluginManager().registerEvents(this, this);

        // Load permissions for any players already online (e.g. after a reload)
        for (Player player : getServer().getOnlinePlayers()) {
            engine.loadPlayer(player.getUniqueId());
        }

        getLogger().info("Permission engine enabled — " + getServer().getOnlinePlayers().size() + " players loaded.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Permission engine disabled.");
    }

    // ----------------------------------------------------------------
    // Player Events
    // ----------------------------------------------------------------

    /**
     * Load permissions before the player fully joins.
     * Using LOWEST priority so permissions are ready before other plugins need them.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        UUID uuid = event.getUniqueId();
        String name = event.getName();

        try {
            // Ensure the player exists in the database
            ensurePlayerExists(uuid, name);

            // Load their permission cache (this is the one blocking DB read we allow on join)
            PermissionEngine.getInstance().loadPlayer(uuid).join();

        } catch (Exception e) {
            getLogger().severe("[Permissions] Failed to load player " + name + ": " + e.getMessage());
        }
    }

    /**
     * Record IP address and update last seen on login.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        String ip = event.getAddress().getHostAddress();

        // Update username and last seen async
        getServer().getAsyncScheduler().runNow(this, task -> {
            try {
                DatabaseManager.getInstance().execute(
                    "UPDATE players SET username = ?, last_seen = CURRENT_TIMESTAMP WHERE uuid = ?",
                    player.getName(), player.getUniqueId().toString()
                );

                // Record username history
                DatabaseManager.getInstance().execute(
                    "INSERT INTO username_history (player_uuid, username) VALUES (?, ?) " +
                    "ON DUPLICATE KEY UPDATE recorded_at = CURRENT_TIMESTAMP",
                    player.getUniqueId().toString(), player.getName()
                );

                // Record IP history
                DatabaseManager.getInstance().execute(
                    "INSERT INTO ip_history (player_uuid, ip_address) VALUES (?, ?) " +
                    "ON DUPLICATE KEY UPDATE last_seen = CURRENT_TIMESTAMP",
                    player.getUniqueId().toString(), ip
                );
            } catch (SQLException e) {
                getLogger().warning("[Permissions] Failed to update login data for " + player.getName());
            }
        });
    }

    /**
     * Remove player from permission cache when they disconnect.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        PermissionEngine.getInstance().unloadPlayer(event.getPlayer().getUniqueId());
    }

    // ----------------------------------------------------------------
    // Setup Helpers
    // ----------------------------------------------------------------

    /**
     * Insert a new player row if this is their first time joining.
     */
    private void ensurePlayerExists(UUID uuid, String username) throws SQLException {
        DatabaseManager.getInstance().execute(
            "INSERT IGNORE INTO players (uuid, username) VALUES (?, ?)",
            uuid.toString(), username
        );
    }

    /**
     * Add the staff_rank column to the players table if it doesn't exist.
     * This handles the case where the database was created before this column was added.
     */
    private void ensureStaffRankColumn() {
        try {
            DatabaseManager.getInstance().execute(
                "ALTER TABLE players ADD COLUMN IF NOT EXISTS staff_rank VARCHAR(32) DEFAULT NULL"
            );
        } catch (SQLException e) {
            // Column likely already exists — safe to ignore
        }
    }

    /**
     * Load rank group definitions from config.yml.
     *
     * Uses SnakeYAML directly instead of Bukkit's ConfigurationSection.
     * Bukkit's MemorySection always splits keys on dots, so a key like
     * "prison.rank.a" gets stored as nested sections prison → rank → a
     * rather than as a single key — breaking iteration with getKeys(false).
     * Reading the file ourselves preserves dotted keys as-is.
     *
     * Config format:
     *   groups:
     *     prison.rank.a:
     *       - prison.mine.a
     *       - prison.sell
     */
    /**
     * Load rank group definitions from config.yml.
     *
     * Uses SnakeYAML directly instead of Bukkit's ConfigurationSection.
     * Bukkit's MemorySection always splits keys on dots, so "prison.rank.a"
     * would be stored as nested sections prison → rank → a rather than a
     * single key. Reading the raw file preserves dotted keys as-is.
     */
    @SuppressWarnings("unchecked")
    private void loadGroupsFromConfig(PermissionEngine engine) {
        saveDefaultConfig();

        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            getLogger().warning("[Permissions] config.yml not found — using defaults.");
            loadDefaultGroups(engine);
            return;
        }

        try {
            FileInputStream fis = new FileInputStream(configFile);
            Map<String, Object> root = new Yaml().load(fis);
            fis.close();

            if (root == null || !root.containsKey("groups")) {
                getLogger().warning("[Permissions] No groups defined in config.yml — using defaults.");
                loadDefaultGroups(engine);
                return;
            }

            Map<String, Object> groups = (Map<String, Object>) root.get("groups");
            int count = 0;
            for (Map.Entry<String, Object> entry : groups.entrySet()) {
                Object val = entry.getValue();
                if (!(val instanceof List)) continue;
                List<String> perms = (List<String>) val;
                if (!perms.isEmpty()) {
                    engine.registerGroup(entry.getKey(), new HashSet<>(perms));
                    count++;
                }
            }
            getLogger().info("[Permissions] Loaded " + count + " permission groups.");

        } catch (Throwable e) {
            getLogger().severe("[Permissions] Failed to load config: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            loadDefaultGroups(engine);
        }
    }

    /**
     * Default group definitions used if config.yml has no groups section.
     * These are sensible defaults — configure properly in config.yml.
     */
    private void loadDefaultGroups(PermissionEngine engine) {
        // Mine ranks — each grants access to sell and use their mine
        String[] ranks = {"a","b","c","d","e","f","g","h","i","j","k","l","m",
                          "n","o","p","q","r","s","t","u","v","w","x","y","z"};
        for (String rank : ranks) {
            engine.registerGroup("prison.rank." + rank, Set.of(
                "prison.mine." + rank,
                "prison.sell",
                "prison.kit.starter",
                "prison.kit.daily"
            ));
        }

        // Donor ranks
        engine.registerGroup("prison.donor.donor", Set.of(
            "prison.autosell",
            "prison.kit.donor",
            "prison.donor.mines"
        ));
        engine.registerGroup("prison.donor.donorplus", Set.of(
            "prison.autosell",
            "prison.kit.donorplus",
            "prison.donor.mines"
        ));
        engine.registerGroup("prison.donor.elite", Set.of(
            "prison.autosell",
            "prison.kit.elite",
            "prison.donor.mines"
        ));
        engine.registerGroup("prison.donor.eliteplus", Set.of(
            "prison.autosell",
            "prison.kit.eliteplus",
            "prison.donor.mines"
        ));

        // Staff ranks
        engine.registerGroup("prison.staff.helper", Set.of(
            "prison.staff.helper",
            "prison.mute",
            "prison.slowchat",
            "prison.tp.players"
        ));
        engine.registerGroup("prison.staff.moderator", Set.of(
            "prison.kick",
            "prison.tempban",
            "prison.freeze",
            "prison.invsee",
            "prison.tp.anywhere",
            "prison.reports"
        ));
        engine.registerGroup("prison.staff.seniormod", Set.of(
            "prison.ban",
            "prison.ipban",
            "prison.transactions.view"
        ));
        engine.registerGroup("prison.staff.admin", Set.of(
            "prison.admintoolkit.use",
            "prison.admintoolkit.economy",
            "prison.admintoolkit.ranks",
            "prison.admintoolkit.mines",
            "prison.admintoolkit.crates",
            "prison.admintoolkit.regions",
            "prison.admintoolkit.servercontrols",
            "prison.admin.*"
        ));
        engine.registerGroup("prison.staff.senioradmin", Set.of(
            "prison.staff.manage.admin"
        ));
        engine.registerGroup("prison.staff.owner", Set.of(
            "*"
        ));

        getLogger().info("[Permissions] Loaded default permission groups.");
    }
}
