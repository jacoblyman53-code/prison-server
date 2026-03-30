package com.prison.quests;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.logging.Logger;

/**
 * QuestDefinition — immutable description of a quest loaded from config.yml.
 *
 * This is the template; PlayerQuestData holds per-player mutable state.
 */
public final class QuestDefinition {

    private final String id;
    private final String title;
    private final String description;
    private final QuestType type;
    private final QuestTier tier;
    private final long goal;
    private final Material icon;
    private final long igcReward;
    private final long tokenReward;

    public QuestDefinition(String id, String title, String description,
                           QuestType type, QuestTier tier, long goal,
                           Material icon, long igcReward, long tokenReward) {
        this.id          = id;
        this.title       = title;
        this.description = description;
        this.type        = type;
        this.tier        = tier;
        this.goal        = goal;
        this.icon        = icon;
        this.igcReward   = igcReward;
        this.tokenReward = tokenReward;
    }

    // ----------------------------------------------------------------
    // Factory
    // ----------------------------------------------------------------

    /**
     * Parse a QuestDefinition from a config section.
     * Returns null and logs a warning if any required field is missing or invalid.
     */
    public static QuestDefinition fromConfig(String id, ConfigurationSection sec, Logger log) {
        String title       = sec.getString("title");
        String description = sec.getString("description", "");
        String typeStr     = sec.getString("type");
        String tierStr     = sec.getString("tier");
        long   goal        = sec.getLong("goal", 0L);
        String iconStr     = sec.getString("icon", "STONE");
        long   igc         = sec.getLong("igc", 0L);
        long   tokens      = sec.getLong("tokens", 0L);

        if (title == null || typeStr == null || tierStr == null || goal <= 0) {
            log.warning("[Quests] Quest '" + id + "' is missing required fields (title/type/tier/goal). Skipping.");
            return null;
        }

        QuestType type;
        try {
            type = QuestType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warning("[Quests] Quest '" + id + "' has unknown type '" + typeStr + "'. Skipping.");
            return null;
        }

        QuestTier tier;
        try {
            tier = QuestTier.valueOf(tierStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warning("[Quests] Quest '" + id + "' has unknown tier '" + tierStr + "'. Skipping.");
            return null;
        }

        Material icon;
        try {
            icon = Material.valueOf(iconStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warning("[Quests] Quest '" + id + "' has unknown icon '" + iconStr + "'. Defaulting to STONE.");
            icon = Material.STONE;
        }

        return new QuestDefinition(id, title, description, type, tier, goal, icon, igc, tokens);
    }

    // ----------------------------------------------------------------
    // Accessors
    // ----------------------------------------------------------------

    public String    getId()          { return id; }
    public String    getTitle()       { return title; }
    public String    getDescription() { return description; }
    public QuestType getType()        { return type; }
    public QuestTier getTier()        { return tier; }
    public long      getGoal()        { return goal; }
    public Material  getIcon()        { return icon; }
    public long      getIgcReward()   { return igcReward; }
    public long      getTokenReward() { return tokenReward; }

    /** True if this quest gives IGC as a reward. */
    public boolean hasIgcReward() { return igcReward > 0; }

    /** True if this quest gives tokens as a reward. */
    public boolean hasTokenReward() { return tokenReward > 0; }
}
