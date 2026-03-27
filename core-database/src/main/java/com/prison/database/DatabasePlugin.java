package com.prison.database;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * DatabasePlugin — the main class for the core-database module.
 *
 * This plugin must be loaded before every other prison plugin.
 * It initializes the shared HikariCP connection pool and creates
 * all database tables on first run.
 *
 * Other plugins access the database through:
 *   DatabaseManager.getInstance().getConnection()
 *   DatabaseManager.getInstance().query(...)
 *   DatabaseManager.getInstance().execute(...)
 *   DatabaseManager.getInstance().queueWrite(...)
 */
public class DatabasePlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        // Save the default config.yml if it doesn't exist yet
        saveDefaultConfig();

        // Initialize the shared database connection pool
        try {
            DatabaseManager.initialize(this);
            getLogger().info("Database module enabled successfully.");
        } catch (Exception e) {
            getLogger().severe("Failed to initialize database: " + e.getMessage());
            getLogger().severe("Check your config.yml database settings and make sure MySQL is running.");
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        // Flush remaining writes and close the pool cleanly
        if (DatabaseManager.getInstance() != null) {
            DatabaseManager.getInstance().shutdown();
        }
        getLogger().info("Database module disabled.");
    }
}
