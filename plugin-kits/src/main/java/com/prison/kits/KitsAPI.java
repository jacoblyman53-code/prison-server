package com.prison.kits;

import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * KitsAPI — public interface for other plugins to query kit state.
 *
 * Usage:
 *   KitsAPI api = KitsAPI.getInstance();
 *   List<KitData> available = api.getAccessibleKits(player);
 *   long remaining = api.getRemainingCooldownMs(player.getUniqueId(), "daily");
 */
public class KitsAPI {

    private static KitsAPI instance;

    KitsAPI() { instance = this; }

    public static KitsAPI getInstance() { return instance; }

    /** Get a kit definition by id, or null if not found. */
    public KitData getKit(String id) {
        return KitsManager.getInstance().getKit(id);
    }

    /** All configured kits. */
    public Collection<KitData> getAllKits() {
        return KitsManager.getInstance().getAllKits();
    }

    /** Kits the player has permission to claim (type + rank requirements met). */
    public List<KitData> getAccessibleKits(Player player) {
        return KitsManager.getInstance().getAccessibleKits(player);
    }

    /**
     * Ms until this kit can be claimed again. 0 = claimable now.
     * Long.MAX_VALUE = one-time kit already claimed.
     */
    public long getRemainingCooldownMs(UUID uuid, String kitId) {
        KitData kit = KitsManager.getInstance().getKit(kitId);
        if (kit == null) return 0;
        return KitsManager.getInstance().getRemainingMs(uuid, kit);
    }
}
