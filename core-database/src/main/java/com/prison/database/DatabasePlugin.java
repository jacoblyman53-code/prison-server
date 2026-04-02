package com.prison.database;

import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

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
 *
 * BungeeCord / Velocity plugin messaging:
 *   The "BungeeCord" channel is registered here as the foundation for
 *   future proxy expansion. Use MessagingAPI to send cross-server messages.
 *   See: https://www.spigotmc.org/wiki/bukkit-bungee-plugin-messaging-channel/
 */
public class DatabasePlugin extends JavaPlugin implements PluginMessageListener {

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

        // Register BungeeCord plugin messaging channels.
        // Outgoing: used to send sub-channel messages to the proxy (e.g. Connect, GetServer).
        // Incoming: used to receive responses from the proxy.
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", this);
        getLogger().info("BungeeCord plugin messaging channels registered.");
    }

    @Override
    public void onDisable() {
        // Flush remaining writes and close the pool cleanly
        if (DatabaseManager.getInstance() != null) {
            DatabaseManager.getInstance().shutdown();
        }
        // Unregister messaging channels
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
        getServer().getMessenger().unregisterIncomingPluginChannel(this);
        getLogger().info("Database module disabled.");
    }

    // ----------------------------------------------------------------
    // BungeeCord plugin messaging — incoming handler
    // ----------------------------------------------------------------

    /**
     * Handles incoming messages from the BungeeCord proxy on the "BungeeCord" channel.
     *
     * Sub-channels currently handled:
     *   GetServer — logs the server name returned by the proxy (useful for debugging).
     *
     * To extend: add cases to the switch below for other sub-channels your proxy sends.
     * MessagingAPI.sendOutgoing() handles all outgoing traffic.
     */
    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!"BungeeCord".equals(channel)) return;

        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(message))) {
            String subChannel = in.readUTF();
            switch (subChannel) {
                case "GetServer" -> {
                    String serverName = in.readUTF();
                    MessagingAPI.setCurrentServerName(serverName);
                    getLogger().fine("[BungeeCord] Server name: " + serverName);
                }
                // Future sub-channels go here
                default -> { /* ignore unrecognised sub-channels */ }
            }
        } catch (IOException e) {
            getLogger().warning("[BungeeCord] Failed to read plugin message: " + e.getMessage());
        }
    }
}
