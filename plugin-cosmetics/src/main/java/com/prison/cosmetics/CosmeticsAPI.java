package com.prison.cosmetics;

import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

/**
 * CosmeticsAPI — public static API surface for other plugins that need
 * to query or modify a player's cosmetics without depending on internals.
 *
 * Usage:
 * <pre>{@code
 *   CosmeticsAPI api = CosmeticsAPI.getInstance();
 *   if (api.hasTag(player.getUniqueId(), "crown")) { ... }
 * }</pre>
 *
 * All methods are thread-safe (backed by ConcurrentHashMaps in CosmeticsManager).
 */
public class CosmeticsAPI {

    private static CosmeticsAPI instance;

    private final CosmeticsManager manager;

    CosmeticsAPI(CosmeticsManager manager) {
        this.manager = manager;
        instance = this;
    }

    // ----------------------------------------------------------------
    // Singleton access
    // ----------------------------------------------------------------

    /**
     * Returns the API instance, or {@code null} if PrisonCosmetics is not loaded.
     * Always null-check the result when calling from a soft-depending plugin.
     */
    public static CosmeticsAPI getInstance() {
        return instance;
    }

    static void reset() {
        instance = null;
    }

    // ----------------------------------------------------------------
    // Ownership queries
    // ----------------------------------------------------------------

    /**
     * Returns {@code true} if the player owns the given chat tag.
     * Thread-safe; reads from the in-memory cache.
     */
    public boolean hasTag(UUID uuid, String tagId) {
        return manager.hasTag(uuid, tagId);
    }

    /**
     * Returns an unmodifiable view of all tag IDs the player currently owns.
     * Returns an empty set if the player is offline or their data isn't loaded yet.
     */
    public Set<String> getOwnedTags(UUID uuid) {
        Set<String> owned = manager.getOwnedTags(uuid);
        return owned != null ? Collections.unmodifiableSet(owned) : Collections.emptySet();
    }

    // ----------------------------------------------------------------
    // Equipped tag
    // ----------------------------------------------------------------

    /**
     * Returns the ID of the tag the player currently has equipped,
     * or {@code null} if no tag is equipped.
     */
    public String getEquippedTag(UUID uuid) {
        return manager.getEquippedTag(uuid);
    }

    /**
     * Returns the fully-rendered MiniMessage {@link ChatTag#display()} string
     * of the player's equipped tag, or {@code null} if nothing is equipped
     * or the tag definition no longer exists.
     */
    public String getEquippedTagDisplay(UUID uuid) {
        String id = manager.getEquippedTag(uuid);
        if (id == null) return null;
        ChatTag tag = manager.getTagDefinition(id);
        return tag != null ? tag.display() : null;
    }

    // ----------------------------------------------------------------
    // Granting / revoking
    // ----------------------------------------------------------------

    /**
     * Grants a chat tag to a player.
     * Persists to the database and updates the in-memory cache immediately.
     * Safe to call from any thread.
     *
     * @param uuid  Target player UUID (may be offline — data will be written to DB).
     * @param tagId The tag ID as defined in config.yml.
     * @return {@code true} if the tag was newly granted; {@code false} if they already owned it
     *         or the tag ID is unknown.
     */
    public boolean grantTag(UUID uuid, String tagId) {
        return manager.grantTag(uuid, tagId);
    }

    /**
     * Revokes a chat tag from a player.
     * If the tag was equipped, it is also unequipped and the player's display name
     * is updated (if they are online).
     * Persists to the database and updates the in-memory cache immediately.
     * Safe to call from any thread.
     *
     * @param uuid  Target player UUID.
     * @param tagId The tag ID to remove.
     * @return {@code true} if the tag was removed; {@code false} if they didn't own it
     *         or the tag ID is unknown.
     */
    public boolean revokeTag(UUID uuid, String tagId) {
        return manager.revokeTag(uuid, tagId);
    }

    // ----------------------------------------------------------------
    // Tag definitions
    // ----------------------------------------------------------------

    /**
     * Returns the {@link ChatTag} definition for the given ID, or {@code null}
     * if no tag with that ID is defined in config.yml.
     */
    public ChatTag getTagDefinition(String tagId) {
        return manager.getTagDefinition(tagId);
    }

    /**
     * Returns an unmodifiable collection of all configured {@link ChatTag} definitions.
     */
    public Collection<ChatTag> getAllTagDefinitions() {
        return Collections.unmodifiableCollection(manager.getAllTagDefinitions());
    }

    // ----------------------------------------------------------------
    // Equip helpers (convenience wrappers for online players)
    // ----------------------------------------------------------------

    /**
     * Equips a tag for an online player, updates their display name, and persists
     * the change to the database.
     *
     * @param player The online player.
     * @param tagId  Tag to equip — must be owned by the player.
     * @return {@code true} on success; {@code false} if the player doesn't own
     *         the tag or the tag ID is unknown.
     */
    public boolean equipTag(Player player, String tagId) {
        return manager.equipTag(player, tagId);
    }

    /**
     * Unequips the currently-equipped tag (if any) for an online player,
     * restores their plain display name, and persists the change.
     *
     * @param player The online player.
     */
    public void unequipTag(Player player) {
        manager.unequipTag(player);
    }
}
