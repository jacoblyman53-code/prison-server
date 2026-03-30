package com.prison.menu;

import com.prison.database.DatabaseManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * PlayerSettingsManager — stores per-player UI preferences.
 *
 * Settings are loaded on join, saved async on change.
 * Backed by the `player_ui_settings` table.
 */
public class PlayerSettingsManager {

    // ----------------------------------------------------------------
    // Singleton
    // ----------------------------------------------------------------

    private static PlayerSettingsManager instance;

    public static PlayerSettingsManager getInstance() { return instance; }

    public static PlayerSettingsManager initialize(JavaPlugin plugin) {
        instance = new PlayerSettingsManager(plugin.getLogger());
        instance.ensureTable();
        return instance;
    }

    // ----------------------------------------------------------------

    public static class PlayerSettings {
        private boolean sounds        = true;
        private boolean notifications = true;
        private boolean autosellDefault = false;
        private boolean rankupAutoteleport = true;
        private boolean uiDetailMode  = true;

        public boolean isSounds()              { return sounds; }
        public boolean isNotifications()        { return notifications; }
        public boolean isAutosellDefault()      { return autosellDefault; }
        public boolean isRankupAutoteleport()   { return rankupAutoteleport; }
        public boolean isUiDetailMode()         { return uiDetailMode; }

        public void setSounds(boolean v)              { this.sounds = v; }
        public void setNotifications(boolean v)        { this.notifications = v; }
        public void setAutosellDefault(boolean v)      { this.autosellDefault = v; }
        public void setRankupAutoteleport(boolean v)   { this.rankupAutoteleport = v; }
        public void setUiDetailMode(boolean v)         { this.uiDetailMode = v; }
    }

    private final Logger logger;
    private final Map<UUID, PlayerSettings> cache = new ConcurrentHashMap<>();

    private PlayerSettingsManager(Logger logger) {
        this.logger = logger;
    }

    // ----------------------------------------------------------------

    private void ensureTable() {
        try {
            DatabaseManager.getInstance().execute(
                "CREATE TABLE IF NOT EXISTS player_ui_settings (" +
                "  player_uuid          VARCHAR(36) NOT NULL PRIMARY KEY," +
                "  sounds               TINYINT(1)  NOT NULL DEFAULT 1," +
                "  notifications        TINYINT(1)  NOT NULL DEFAULT 1," +
                "  autosell_default     TINYINT(1)  NOT NULL DEFAULT 0," +
                "  rankup_autoteleport  TINYINT(1)  NOT NULL DEFAULT 1," +
                "  ui_detail_mode       TINYINT(1)  NOT NULL DEFAULT 1" +
                ")"
            );
        } catch (SQLException e) {
            logger.severe("[Menu] Failed to create player_ui_settings table: " + e.getMessage());
        }
    }

    public void loadPlayer(UUID uuid) {
        try {
            PlayerSettings s = DatabaseManager.getInstance().query(
                "SELECT sounds, notifications, autosell_default, rankup_autoteleport, ui_detail_mode " +
                "FROM player_ui_settings WHERE player_uuid = ?",
                rs -> {
                    if (rs.next()) {
                        PlayerSettings ps = new PlayerSettings();
                        ps.setSounds(rs.getInt("sounds") == 1);
                        ps.setNotifications(rs.getInt("notifications") == 1);
                        ps.setAutosellDefault(rs.getInt("autosell_default") == 1);
                        ps.setRankupAutoteleport(rs.getInt("rankup_autoteleport") == 1);
                        ps.setUiDetailMode(rs.getInt("ui_detail_mode") == 1);
                        return ps;
                    }
                    return new PlayerSettings(); // defaults
                },
                uuid.toString()
            );
            cache.put(uuid, s);
        } catch (SQLException e) {
            logger.warning("[Menu] Failed to load settings for " + uuid + ": " + e.getMessage());
            cache.put(uuid, new PlayerSettings());
        }
    }

    public void unloadPlayer(UUID uuid) {
        cache.remove(uuid);
    }

    public PlayerSettings get(UUID uuid) {
        return cache.computeIfAbsent(uuid, k -> new PlayerSettings());
    }

    public void save(UUID uuid) {
        PlayerSettings s = cache.get(uuid);
        if (s == null) return;
        try {
            DatabaseManager.getInstance().execute(
                "INSERT INTO player_ui_settings " +
                "(player_uuid, sounds, notifications, autosell_default, rankup_autoteleport, ui_detail_mode) " +
                "VALUES (?,?,?,?,?,?) ON DUPLICATE KEY UPDATE " +
                "sounds=VALUES(sounds), notifications=VALUES(notifications), " +
                "autosell_default=VALUES(autosell_default), " +
                "rankup_autoteleport=VALUES(rankup_autoteleport), " +
                "ui_detail_mode=VALUES(ui_detail_mode)",
                uuid.toString(),
                s.isSounds()            ? 1 : 0,
                s.isNotifications()      ? 1 : 0,
                s.isAutosellDefault()    ? 1 : 0,
                s.isRankupAutoteleport() ? 1 : 0,
                s.isUiDetailMode()       ? 1 : 0
            );
        } catch (SQLException e) {
            logger.warning("[Menu] Failed to save settings for " + uuid + ": " + e.getMessage());
        }
    }
}
