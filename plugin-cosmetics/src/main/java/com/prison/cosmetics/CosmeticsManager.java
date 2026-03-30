package com.prison.cosmetics;

import com.prison.database.DatabaseManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * CosmeticsManager — owns all runtime state for the cosmetics system.
 *
 * <p>Two caches are maintained:
 * <ul>
 *   <li>{@code ownedTags}    — UUID → set of tag IDs the player owns.</li>
 *   <li>{@code equippedTag}  — UUID → single tag ID currently equipped (absent = none).</li>
 * </ul>
 *
 * <p>All cache reads are instant (no DB). DB writes are batched via
 * {@link DatabaseManager#queueWrite} for equip/unequip, and executed
 * immediately via {@link DatabaseManager#execute} for grant/revoke so
 * that ownership changes are durable before the method returns.
 */
public class CosmeticsManager {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final JavaPlugin plugin;
    private final Logger logger;

    // Tag definitions loaded from config — immutable after onEnable
    private final Map<String, ChatTag> tagDefinitions = new ConcurrentHashMap<>();

    // Per-player caches — populated on join, cleared on quit
    private final ConcurrentHashMap<UUID, Set<String>> ownedTags   = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, String>      equippedTag = new ConcurrentHashMap<>();

    CosmeticsManager(JavaPlugin plugin, Logger logger) {
        this.plugin = plugin;
        this.logger = logger;
    }

    // ----------------------------------------------------------------
    // Tag definition management
    // ----------------------------------------------------------------

    /** Register a tag definition (called during config load at startup). */
    void registerTag(ChatTag tag) {
        tagDefinitions.put(tag.id(), tag);
    }

    /** Returns the definition for the given ID, or null if not found. */
    public ChatTag getTagDefinition(String id) {
        return tagDefinitions.get(id);
    }

    /** Returns all registered tag definitions. */
    public Collection<ChatTag> getAllTagDefinitions() {
        return Collections.unmodifiableCollection(tagDefinitions.values());
    }

    // ----------------------------------------------------------------
    // Player data — load / unload
    // ----------------------------------------------------------------

    /**
     * Asynchronously loads a player's owned/equipped tags from the database,
     * then applies the display-name update on the main thread.
     * Called from PlayerJoinEvent.
     */
    public void loadPlayerAsync(Player player) {
        UUID uuid = player.getUniqueId();

        CompletableFuture.runAsync(() -> {
            Set<String> owned   = ConcurrentHashMap.newKeySet();
            String[]    equip   = { null };   // single-element array for lambda capture

            try {
                DatabaseManager.getInstance().query(
                    "SELECT cosmetic_id, equipped FROM player_cosmetics " +
                    "WHERE player_uuid = ? AND cosmetic_type = 'CHAT_TAG'",
                    rs -> {
                        while (rs.next()) {
                            String id       = rs.getString("cosmetic_id");
                            boolean isEquip = rs.getInt("equipped") == 1;
                            owned.add(id);
                            if (isEquip) equip[0] = id;
                        }
                        return null;
                    },
                    uuid.toString()
                );
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "[Cosmetics] Failed to load data for " + uuid, e);
            }

            // Write to cache from this async thread — ConcurrentHashMap is safe
            ownedTags.put(uuid, owned);
            if (equip[0] != null) {
                equippedTag.put(uuid, equip[0]);
            }

            // Apply display name on the main thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                // Player may have disconnected during the async load
                Player online = Bukkit.getPlayer(uuid);
                if (online != null) {
                    applyDisplayName(online);
                }
            });
        });
    }

    /**
     * Clears the in-memory cache for a player when they disconnect.
     * Called from PlayerQuitEvent.
     */
    public void unloadPlayer(UUID uuid) {
        ownedTags.remove(uuid);
        equippedTag.remove(uuid);
    }

    // ----------------------------------------------------------------
    // Ownership queries
    // ----------------------------------------------------------------

    public boolean hasTag(UUID uuid, String tagId) {
        Set<String> owned = ownedTags.get(uuid);
        return owned != null && owned.contains(tagId);
    }

    public Set<String> getOwnedTags(UUID uuid) {
        return ownedTags.get(uuid);
    }

    public String getEquippedTag(UUID uuid) {
        return equippedTag.get(uuid);
    }

    // ----------------------------------------------------------------
    // Grant / revoke
    // ----------------------------------------------------------------

    /**
     * Grants a tag to a player. Updates DB immediately (execute, not queueWrite)
     * so ownership is durable. Returns {@code false} if the tag is unknown or
     * the player already owns it.
     *
     * <p>If the player is offline, the DB is still written but the in-memory
     * cache is only updated if they happen to be online.
     */
    public boolean grantTag(UUID uuid, String tagId) {
        if (!tagDefinitions.containsKey(tagId)) return false;

        // If we have a cache entry (player online) and they already own it, short-circuit
        Set<String> owned = ownedTags.get(uuid);
        if (owned != null && owned.contains(tagId)) return false;

        try {
            // INSERT IGNORE means a second grant of the same tag is silently ignored at DB level
            DatabaseManager.getInstance().execute(
                "INSERT IGNORE INTO player_cosmetics (player_uuid, cosmetic_id, cosmetic_type, equipped) " +
                "VALUES (?, ?, 'CHAT_TAG', 0)",
                uuid.toString(), tagId
            );
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "[Cosmetics] Failed to grant tag " + tagId + " to " + uuid, e);
            return false;
        }

        // Update the in-memory cache if the player is online
        if (owned != null) {
            owned.add(tagId);
        }
        return true;
    }

    /**
     * Revokes a tag from a player. If the tag was equipped it is unequipped first.
     * Persists to DB immediately. Returns {@code false} if the tag is unknown or
     * the player doesn't own it.
     */
    public boolean revokeTag(UUID uuid, String tagId) {
        if (!tagDefinitions.containsKey(tagId)) return false;

        Set<String> owned = ownedTags.get(uuid);
        if (owned != null && !owned.contains(tagId)) return false;

        // If this tag is equipped, unequip it first
        String current = equippedTag.get(uuid);
        if (tagId.equals(current)) {
            equippedTag.remove(uuid);
            // Update display name if online
            Player online = Bukkit.getPlayer(uuid);
            if (online != null) applyDisplayName(online);
        }

        try {
            DatabaseManager.getInstance().execute(
                "DELETE FROM player_cosmetics WHERE player_uuid = ? AND cosmetic_id = ? AND cosmetic_type = 'CHAT_TAG'",
                uuid.toString(), tagId
            );
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "[Cosmetics] Failed to revoke tag " + tagId + " from " + uuid, e);
            return false;
        }

        if (owned != null) {
            owned.remove(tagId);
        }
        return true;
    }

    // ----------------------------------------------------------------
    // Equip / unequip
    // ----------------------------------------------------------------

    /**
     * Equips a tag for an online player. The previously-equipped tag (if any)
     * is unequipped atomically. Persists via {@code queueWrite} — a short delay
     * is acceptable here since the in-memory state is already updated instantly.
     *
     * @return {@code true} on success; {@code false} if the player doesn't own
     *         the tag or the tag ID is unknown.
     */
    public boolean equipTag(Player player, String tagId) {
        UUID uuid = player.getUniqueId();
        if (!tagDefinitions.containsKey(tagId)) return false;
        if (!hasTag(uuid, tagId)) return false;

        String prev = equippedTag.put(uuid, tagId);

        // Persist: clear old equipped flag then set new one
        if (prev != null && !prev.equals(tagId)) {
            DatabaseManager.getInstance().queueWrite(
                "UPDATE player_cosmetics SET equipped = 0 " +
                "WHERE player_uuid = ? AND cosmetic_id = ? AND cosmetic_type = 'CHAT_TAG'",
                uuid.toString(), prev
            );
        }
        DatabaseManager.getInstance().queueWrite(
            "UPDATE player_cosmetics SET equipped = 1 " +
            "WHERE player_uuid = ? AND cosmetic_id = ? AND cosmetic_type = 'CHAT_TAG'",
            uuid.toString(), tagId
        );

        applyDisplayName(player);
        return true;
    }

    /**
     * Unequips the currently-equipped tag for an online player.
     * If no tag is equipped, this is a no-op.
     */
    public void unequipTag(Player player) {
        UUID uuid = player.getUniqueId();
        String prev = equippedTag.remove(uuid);
        if (prev == null) return;

        DatabaseManager.getInstance().queueWrite(
            "UPDATE player_cosmetics SET equipped = 0 " +
            "WHERE player_uuid = ? AND cosmetic_id = ? AND cosmetic_type = 'CHAT_TAG'",
            uuid.toString(), prev
        );

        applyDisplayName(player);
    }

    // ----------------------------------------------------------------
    // Display-name application
    // ----------------------------------------------------------------

    /**
     * Builds and applies the player's Adventure display name so that
     * plugin-chat (and the tab list) picks up the equipped tag.
     *
     * Format: {@code <tag> <playerName>} — or just {@code <playerName>} when no
     * tag is equipped.
     *
     * Must be called on the main thread.
     */
    public void applyDisplayName(Player player) {
        UUID uuid = player.getUniqueId();
        String tagId = equippedTag.get(uuid);

        Component nameComponent;
        if (tagId != null) {
            ChatTag tag = tagDefinitions.get(tagId);
            if (tag != null) {
                // e.g.  <yellow>[⚡]</yellow> PlayerName
                nameComponent = MM.deserialize(tag.display() + " <white>" + escapePlayerName(player.getName()));
            } else {
                // Tag was deleted from config — treat as no tag
                nameComponent = Component.text(player.getName());
            }
        } else {
            nameComponent = Component.text(player.getName());
        }

        player.displayName(nameComponent);
        // Keep the tab-list name in sync as well
        player.playerListName(nameComponent);
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    /**
     * Escapes MiniMessage tags in a player name so a name like
     * {@code "<red>Hacker"} doesn't inject formatting.
     */
    private String escapePlayerName(String name) {
        return MM.escapeTags(name);
    }
}
