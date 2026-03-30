package com.prison.cosmetics;

import org.bukkit.Material;

/**
 * Immutable data record for a single chat tag definition loaded from config.yml.
 *
 * @param id          Internal identifier (e.g. "lightning") — matches the config key.
 * @param display     MiniMessage string that renders as the visible tag (e.g. "&lt;yellow&gt;[⚡]").
 * @param description Short human-readable description shown in the GUI lore.
 * @param rarity      Rarity tier that controls icon material and colour in the GUI.
 */
public record ChatTag(String id, String display, String description, Rarity rarity) {

    /**
     * Returns the GUI icon material based on this tag's rarity tier.
     *
     * <ul>
     *   <li>COMMON    → WHITE_DYE</li>
     *   <li>UNCOMMON  → CYAN_DYE</li>
     *   <li>RARE      → LAPIS_LAZULI</li>
     *   <li>LEGENDARY → AMETHYST_SHARD</li>
     *   <li>PRESTIGE  → NETHER_STAR</li>
     * </ul>
     */
    public Material iconMaterial() {
        return switch (rarity) {
            case COMMON    -> Material.WHITE_DYE;
            case UNCOMMON  -> Material.CYAN_DYE;
            case RARE      -> Material.LAPIS_LAZULI;
            case LEGENDARY -> Material.AMETHYST_SHARD;
            case PRESTIGE  -> Material.NETHER_STAR;
        };
    }
}
