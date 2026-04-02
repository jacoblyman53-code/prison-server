package com.prison.database;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;

/**
 * MessagingAPI — thin wrapper around the BungeeCord plugin messaging channel.
 *
 * Provides typed helpers for the most common proxy operations.
 * Every method requires at least one online player to act as the
 * delivery vehicle (BungeeCord's requirement for forwarding messages).
 *
 * Usage from any plugin:
 *   MessagingAPI.connectToServer(player, "hub");
 *   String server = MessagingAPI.getCurrentServerName(); // after GetServer response
 *
 * To add a new sub-channel, add a static helper following the same pattern:
 *   build a ByteArrayOutputStream, write UTF strings, call sendRaw().
 *
 * Reference: https://www.spigotmc.org/wiki/bukkit-bungee-plugin-messaging-channel/
 */
public final class MessagingAPI {

    private MessagingAPI() {}

    /** The name of this server as reported by BungeeCord (updated on GetServer response). */
    private static volatile String currentServerName = "unknown";

    public static String getCurrentServerName() { return currentServerName; }

    /** Called by DatabasePlugin when a GetServer response is received. */
    static void setCurrentServerName(String name) { currentServerName = name; }

    // ----------------------------------------------------------------
    // Outgoing helpers
    // ----------------------------------------------------------------

    /**
     * Connect a player to a different server on the proxy network.
     * Equivalent to BungeeCord's "Connect" sub-channel.
     */
    public static void connectToServer(Player player, String serverName) {
        byte[] payload = build(out -> {
            out.writeUTF("Connect");
            out.writeUTF(serverName);
        });
        if (payload != null) sendRaw(player, payload);
    }

    /**
     * Request the name of the server this player is currently on.
     * The response arrives asynchronously via DatabasePlugin.onPluginMessageReceived
     * and updates MessagingAPI.getCurrentServerName().
     */
    public static void requestServerName(Player player) {
        byte[] payload = build(out -> out.writeUTF("GetServer"));
        if (payload != null) sendRaw(player, payload);
    }

    /**
     * Forward a custom plugin message to all players on the given server.
     * Uses BungeeCord's "Forward" sub-channel.
     *
     * @param server   target server name, or "ALL" for every server
     * @param channel  custom channel name (must be registered on receiving end)
     * @param data     arbitrary byte payload
     */
    public static void forward(Player player, String server, String channel, byte[] data) {
        byte[] payload = build(out -> {
            out.writeUTF("Forward");
            out.writeUTF(server);
            out.writeUTF(channel);
            out.writeShort(data.length);
            out.write(data);
        });
        if (payload != null) sendRaw(player, payload);
    }

    // ----------------------------------------------------------------
    // Internal helpers
    // ----------------------------------------------------------------

    @FunctionalInterface
    private interface PayloadWriter {
        void write(DataOutputStream out) throws IOException;
    }

    private static byte[] build(PayloadWriter writer) {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(bytes)) {
            writer.write(out);
            return bytes.toByteArray();
        } catch (IOException e) {
            Bukkit.getLogger().warning("[MessagingAPI] Failed to build payload: " + e.getMessage());
            return null;
        }
    }

    /**
     * Send a raw BungeeCord payload via the first available online player.
     * BungeeCord requires a real player to be used as the message vehicle.
     */
    private static void sendRaw(Player player, byte[] payload) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("PrisonDatabase");
        if (plugin == null || !plugin.isEnabled()) return;
        player.sendPluginMessage(plugin, "BungeeCord", payload);
    }
}
