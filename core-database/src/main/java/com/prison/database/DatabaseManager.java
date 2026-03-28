package com.prison.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DatabaseManager — the single connection pool shared by every plugin.
 *
 * Every plugin gets a connection from this pool. No plugin ever opens
 * its own raw connection. This keeps the number of open connections
 * predictable and prevents the server from overwhelming MySQL.
 */
public class DatabaseManager {

    private static DatabaseManager instance;

    private HikariDataSource dataSource;
    private final Logger logger;

    // Write batching — queued SQL writes are flushed every 500ms
    // instead of hitting the database on every single event.
    private final List<BatchEntry> writeQueue = new ArrayList<>();
    private final ScheduledExecutorService batchScheduler = Executors.newSingleThreadScheduledExecutor();

    private DatabaseManager(Logger logger) {
        this.logger = logger;
    }

    // ----------------------------------------------------------------
    // Initialization
    // ----------------------------------------------------------------

    public static DatabaseManager getInstance() {
        return instance;
    }

    /**
     * Called once on server startup by the DatabasePlugin main class.
     * Sets up the HikariCP connection pool using values from config.yml.
     */
    public static DatabaseManager initialize(JavaPlugin plugin) {
        if (instance != null) {
            throw new IllegalStateException("DatabaseManager is already initialized");
        }
        instance = new DatabaseManager(plugin.getLogger());
        instance.setupPool(plugin);
        instance.startBatchFlusher();
        instance.createTables();
        return instance;
    }

    private void setupPool(JavaPlugin plugin) {
        HikariConfig config = new HikariConfig();

        String host     = plugin.getConfig().getString("database.host", "localhost");
        int    port     = plugin.getConfig().getInt("database.port", 3306);
        String database = plugin.getConfig().getString("database.name", "prison");
        String username = plugin.getConfig().getString("database.username", "root");
        String password = plugin.getConfig().getString("database.password", "");
        int    minPool  = plugin.getConfig().getInt("database.pool.minimum", 10);
        int    maxPool  = plugin.getConfig().getInt("database.pool.maximum", 20);

        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=false&autoReconnect=true&characterEncoding=utf8");
        config.setUsername(username);
        config.setPassword(password);
        config.setMinimumIdle(minPool);
        config.setMaximumPoolSize(maxPool);
        config.setConnectionTimeout(30_000);
        config.setIdleTimeout(600_000);
        config.setMaxLifetime(1_800_000);
        config.setPoolName("PrisonDB");

        // These driver properties improve performance on MySQL
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");

        dataSource = new HikariDataSource(config);
        logger.info("[Database] Connected to MySQL — pool size: " + minPool + "-" + maxPool);
    }

    // ----------------------------------------------------------------
    // Public API — used by all other plugins
    // ----------------------------------------------------------------

    /**
     * Get a connection from the pool.
     * Always use try-with-resources so the connection is returned to the pool.
     *
     * Example:
     *   try (Connection conn = DatabaseManager.getInstance().getConnection()) { ... }
     */
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * Run a SELECT query. The handler receives the ResultSet and returns a value.
     * This is async-safe — call it from an async thread.
     *
     * Example:
     *   String name = DatabaseManager.getInstance().query(
     *       "SELECT username FROM players WHERE uuid = ?",
     *       rs -> rs.next() ? rs.getString("username") : null,
     *       uuid.toString()
     *   );
     */
    public <T> T query(String sql, ResultSetHandler<T> handler, Object... params) throws SQLException {
        // Async only — never call this on the main server thread
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            setParams(stmt, params);

            try (ResultSet rs = stmt.executeQuery()) {
                return handler.handle(rs);
            }
        }
    }

    /**
     * Run an INSERT, UPDATE, or DELETE immediately.
     * Use queueWrite() instead if the result doesn't need to be instant.
     */
    public int execute(String sql, Object... params) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            setParams(stmt, params);
            return stmt.executeUpdate();
        }
    }

    /**
     * Run an INSERT and return the auto-generated primary key.
     * Returns -1 if no key was generated (e.g. table has no auto-increment).
     */
    public long executeAndGetId(String sql, Object... params) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {

            setParams(stmt, params);
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : -1L;
            }
        }
    }

    /**
     * Queue a write to be flushed in the next batch (every 500ms).
     * Use this for high-frequency writes like block break events or token drops
     * where a slight delay is acceptable and you want to minimize DB pressure.
     */
    public synchronized void queueWrite(String sql, Object... params) {
        writeQueue.add(new BatchEntry(sql, params));
    }

    // ----------------------------------------------------------------
    // Batch flusher
    // ----------------------------------------------------------------

    /**
     * Starts the background scheduler that flushes queued writes every 500ms.
     * Runs on a dedicated thread — never touches the main server thread.
     */
    private void startBatchFlusher() {
        batchScheduler.scheduleAtFixedRate(this::flushBatch, 500, 500, TimeUnit.MILLISECONDS);
        logger.info("[Database] Batch write flusher started (500ms interval)");
    }

    private synchronized void flushBatch() {
        if (writeQueue.isEmpty()) return;

        List<BatchEntry> toFlush = new ArrayList<>(writeQueue);
        writeQueue.clear();

        try (Connection conn = getConnection()) {
            // Wrap all queued writes in a single transaction for efficiency
            conn.setAutoCommit(false);

            for (BatchEntry entry : toFlush) {
                try (PreparedStatement stmt = conn.prepareStatement(entry.sql)) {
                    setParams(stmt, entry.params);
                    stmt.executeUpdate();
                }
            }

            conn.commit();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "[Database] Batch flush failed — " + toFlush.size() + " writes lost", e);
        }
    }

    // ----------------------------------------------------------------
    // Table creation
    // ----------------------------------------------------------------

    /**
     * Creates all database tables on startup if they do not already exist.
     * Safe to run repeatedly — uses IF NOT EXISTS on every table.
     */
    private void createTables() {
        logger.info("[Database] Running table creation SQL...");

        String[] tables = {

            // Players — core player data
            """
            CREATE TABLE IF NOT EXISTS players (
                id            BIGINT AUTO_INCREMENT PRIMARY KEY,
                uuid          VARCHAR(36)  NOT NULL UNIQUE,
                username      VARCHAR(16)  NOT NULL,
                mine_rank     CHAR(1)      NOT NULL DEFAULT 'A',
                prestige      INT          NOT NULL DEFAULT 0,
                donor_rank    VARCHAR(32)  DEFAULT NULL,
                igc_balance   BIGINT       NOT NULL DEFAULT 0,
                token_balance BIGINT       NOT NULL DEFAULT 0,
                blocks_mined  BIGINT       NOT NULL DEFAULT 0,
                playtime      BIGINT       NOT NULL DEFAULT 0,
                first_join    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                last_seen     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                INDEX idx_uuid (uuid),
                INDEX idx_username (username),
                INDEX idx_mine_rank (mine_rank)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """,

            // Username history — tracks name changes
            """
            CREATE TABLE IF NOT EXISTS username_history (
                id          BIGINT AUTO_INCREMENT PRIMARY KEY,
                player_uuid VARCHAR(36) NOT NULL,
                username    VARCHAR(16) NOT NULL,
                recorded_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
                INDEX idx_uuid (player_uuid)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """,

            // IP history — needed for IP bans and alt detection
            """
            CREATE TABLE IF NOT EXISTS ip_history (
                id          BIGINT AUTO_INCREMENT PRIMARY KEY,
                player_uuid VARCHAR(36)  NOT NULL,
                ip_address  VARCHAR(45)  NOT NULL,
                last_seen   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                UNIQUE KEY unique_player_ip (player_uuid, ip_address),
                INDEX idx_uuid (player_uuid),
                INDEX idx_ip (ip_address)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """,

            // Transactions — every IGC and Token movement is logged here
            """
            CREATE TABLE IF NOT EXISTS transactions (
                id            BIGINT AUTO_INCREMENT PRIMARY KEY,
                player_uuid   VARCHAR(36)  NOT NULL,
                currency_type ENUM('IGC','TOKEN') NOT NULL,
                type          VARCHAR(64)  NOT NULL,
                amount        BIGINT       NOT NULL,
                balance_after BIGINT       NOT NULL,
                timestamp     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                INDEX idx_uuid (player_uuid),
                INDEX idx_timestamp (timestamp),
                INDEX idx_type (type)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """,

            // Pickaxe enchants — per pickaxe UUID, not per player
            """
            CREATE TABLE IF NOT EXISTS pickaxe_enchants (
                pickaxe_uuid VARCHAR(36)  NOT NULL,
                owner_uuid   VARCHAR(36)  NOT NULL,
                enchant_id   VARCHAR(64)  NOT NULL,
                level        INT          NOT NULL DEFAULT 1,
                last_updated TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                PRIMARY KEY (pickaxe_uuid, enchant_id),
                INDEX idx_owner (owner_uuid)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """,

            // Gangs
            """
            CREATE TABLE IF NOT EXISTS gangs (
                id           BIGINT AUTO_INCREMENT PRIMARY KEY,
                name         VARCHAR(32)  NOT NULL UNIQUE,
                tag          VARCHAR(8)   NOT NULL UNIQUE,
                level        INT          NOT NULL DEFAULT 1,
                bank_balance BIGINT       NOT NULL DEFAULT 0,
                created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """,

            // Gang members
            """
            CREATE TABLE IF NOT EXISTS gang_members (
                gang_id     BIGINT      NOT NULL,
                player_uuid VARCHAR(36) NOT NULL,
                role        ENUM('LEADER','OFFICER','MEMBER') NOT NULL DEFAULT 'MEMBER',
                joined_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (player_uuid),
                INDEX idx_gang (gang_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """,

            // Crate keys — how many keys each player holds per tier
            """
            CREATE TABLE IF NOT EXISTS crate_keys (
                id           BIGINT AUTO_INCREMENT PRIMARY KEY,
                player_uuid  VARCHAR(36) NOT NULL,
                crate_tier   VARCHAR(32) NOT NULL,
                quantity     INT         NOT NULL DEFAULT 0,
                last_updated TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                UNIQUE KEY unique_player_crate (player_uuid, crate_tier),
                INDEX idx_uuid (player_uuid)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """,

            // Crate open logs
            """
            CREATE TABLE IF NOT EXISTS crate_logs (
                id          BIGINT AUTO_INCREMENT PRIMARY KEY,
                player_uuid VARCHAR(36)  NOT NULL,
                crate_tier  VARCHAR(32)  NOT NULL,
                reward      VARCHAR(255) NOT NULL,
                timestamp   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                INDEX idx_uuid (player_uuid),
                INDEX idx_timestamp (timestamp)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """,

            // Auction listings
            """
            CREATE TABLE IF NOT EXISTS auction_listings (
                id          BIGINT AUTO_INCREMENT PRIMARY KEY,
                seller_uuid VARCHAR(36)   NOT NULL,
                item_data   MEDIUMBLOB    NOT NULL,
                price_igc   BIGINT        NOT NULL,
                listed_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                expires_at  TIMESTAMP     NOT NULL,
                status      ENUM('ACTIVE','SOLD','EXPIRED','REMOVED') NOT NULL DEFAULT 'ACTIVE',
                INDEX idx_seller (seller_uuid),
                INDEX idx_status (status),
                INDEX idx_expires (expires_at)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """,

            // Auction transactions
            """
            CREATE TABLE IF NOT EXISTS auction_transactions (
                id          BIGINT AUTO_INCREMENT PRIMARY KEY,
                listing_id  BIGINT      NOT NULL,
                seller_uuid VARCHAR(36) NOT NULL,
                buyer_uuid  VARCHAR(36) NOT NULL,
                price_igc   BIGINT      NOT NULL,
                timestamp   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
                INDEX idx_seller (seller_uuid),
                INDEX idx_buyer (buyer_uuid)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """,

            // IGC shop purchases
            """
            CREATE TABLE IF NOT EXISTS igc_shop_purchases (
                id          BIGINT AUTO_INCREMENT PRIMARY KEY,
                player_uuid VARCHAR(36)  NOT NULL,
                item_id     VARCHAR(64)  NOT NULL,
                price_igc   BIGINT       NOT NULL,
                timestamp   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                INDEX idx_uuid (player_uuid)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """,

            // Kit cooldowns
            """
            CREATE TABLE IF NOT EXISTS kit_cooldowns (
                player_uuid       VARCHAR(36) NOT NULL,
                kit_id            VARCHAR(64) NOT NULL,
                last_claimed      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
                cooldown_duration BIGINT      NOT NULL,
                PRIMARY KEY (player_uuid, kit_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """,

            // Kit claim logs
            """
            CREATE TABLE IF NOT EXISTS kit_logs (
                id          BIGINT AUTO_INCREMENT PRIMARY KEY,
                player_uuid VARCHAR(36) NOT NULL,
                kit_id      VARCHAR(64) NOT NULL,
                timestamp   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
                INDEX idx_uuid (player_uuid)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """,

            // Staff actions — everything staff do is logged here
            """
            CREATE TABLE IF NOT EXISTS staff_actions (
                id          BIGINT AUTO_INCREMENT PRIMARY KEY,
                actor_uuid  VARCHAR(36)  NOT NULL,
                target_uuid VARCHAR(36)  DEFAULT NULL,
                action_type VARCHAR(64)  NOT NULL,
                details     JSON         DEFAULT NULL,
                timestamp   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                INDEX idx_actor (actor_uuid),
                INDEX idx_target (target_uuid),
                INDEX idx_timestamp (timestamp)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """,

            // Punishments — bans, mutes, kicks
            """
            CREATE TABLE IF NOT EXISTS punishments (
                id            BIGINT AUTO_INCREMENT PRIMARY KEY,
                player_uuid   VARCHAR(36)  NOT NULL,
                ip_address    VARCHAR(45)  DEFAULT NULL,
                type          ENUM('BAN','TEMPBAN','IPBAN','MUTE','TEMPMUTE','KICK') NOT NULL,
                reason        VARCHAR(512) NOT NULL,
                issued_by_uuid VARCHAR(36) NOT NULL,
                issued_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                expires_at    TIMESTAMP    DEFAULT NULL,
                active        BOOLEAN      NOT NULL DEFAULT TRUE,
                INDEX idx_uuid (player_uuid),
                INDEX idx_ip (ip_address),
                INDEX idx_active (active)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """,

            // Player reports
            """
            CREATE TABLE IF NOT EXISTS reports (
                id              BIGINT AUTO_INCREMENT PRIMARY KEY,
                reporter_uuid   VARCHAR(36)  NOT NULL,
                reported_uuid   VARCHAR(36)  NOT NULL,
                reason          VARCHAR(512) NOT NULL,
                status          ENUM('PENDING','REVIEWED','CLOSED') NOT NULL DEFAULT 'PENDING',
                resolution_note VARCHAR(512) DEFAULT NULL,
                assigned_to_uuid VARCHAR(36) DEFAULT NULL,
                created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                INDEX idx_reported (reported_uuid),
                INDEX idx_status (status)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """,

            // Regions — custom region engine data
            """
            CREATE TABLE IF NOT EXISTS regions (
                id            BIGINT AUTO_INCREMENT PRIMARY KEY,
                name          VARCHAR(64)  NOT NULL UNIQUE,
                world         VARCHAR(64)  NOT NULL,
                x1            INT NOT NULL, y1 INT NOT NULL, z1 INT NOT NULL,
                x2            INT NOT NULL, y2 INT NOT NULL, z2 INT NOT NULL,
                priority      INT          NOT NULL DEFAULT 0,
                flags         JSON         DEFAULT NULL,
                entry_message VARCHAR(256) DEFAULT NULL,
                exit_message  VARCHAR(256) DEFAULT NULL,
                created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """,

            // Warps
            """
            CREATE TABLE IF NOT EXISTS warps (
                id               BIGINT AUTO_INCREMENT PRIMARY KEY,
                name             VARCHAR(64)  NOT NULL UNIQUE,
                world            VARCHAR(64)  NOT NULL,
                x                DOUBLE       NOT NULL,
                y                DOUBLE       NOT NULL,
                z                DOUBLE       NOT NULL,
                yaw              FLOAT        NOT NULL DEFAULT 0,
                pitch            FLOAT        NOT NULL DEFAULT 0,
                permission_node  VARCHAR(128) DEFAULT NULL,
                created_by_uuid  VARCHAR(36)  NOT NULL,
                created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """,

            // Tebex deliveries — idempotent delivery tracking
            """
            CREATE TABLE IF NOT EXISTS tebex_deliveries (
                id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
                player_uuid          VARCHAR(36)  NOT NULL,
                player_username      VARCHAR(16)  NOT NULL,
                product_id           VARCHAR(64)  NOT NULL,
                product_type         VARCHAR(64)  NOT NULL,
                delivered_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                tebex_transaction_id VARCHAR(128) NOT NULL UNIQUE,
                INDEX idx_uuid (player_uuid)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """,

            // Anti-cheat violation flags
            """
            CREATE TABLE IF NOT EXISTS anticheat_flags (
                id          BIGINT AUTO_INCREMENT PRIMARY KEY,
                player_uuid VARCHAR(36)  NOT NULL,
                flag_type   VARCHAR(64)  NOT NULL,
                details     JSON         DEFAULT NULL,
                timestamp   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                INDEX idx_uuid (player_uuid),
                INDEX idx_timestamp (timestamp)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """
        };

        // Run every table creation statement
        try (Connection conn = getConnection()) {
            for (String sql : tables) {
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.executeUpdate();
                }
            }
            logger.info("[Database] All tables verified.");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "[Database] Table creation failed!", e);
        }
    }

    // ----------------------------------------------------------------
    // Shutdown
    // ----------------------------------------------------------------

    /**
     * Called on server shutdown. Flushes any remaining queued writes
     * and closes the connection pool cleanly.
     */
    public void shutdown() {
        logger.info("[Database] Flushing remaining writes...");
        batchScheduler.shutdown();
        flushBatch(); // One final flush before closing

        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("[Database] Connection pool closed.");
        }
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private void setParams(PreparedStatement stmt, Object[] params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            stmt.setObject(i + 1, params[i]);
        }
    }

    // Functional interface for handling ResultSets
    @FunctionalInterface
    public interface ResultSetHandler<T> {
        T handle(ResultSet rs) throws SQLException;
    }

    // Container for a queued write
    private record BatchEntry(String sql, Object[] params) {}
}
