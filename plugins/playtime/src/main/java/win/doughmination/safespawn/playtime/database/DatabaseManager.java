// Copyright (c) 2026 Clove Twilight
// Licensed under the ESAL-1.3 License

package win.doughmination.safespawn.playtime.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import win.doughmination.safespawn.playtime.Main;

import java.sql.*;
import java.util.UUID;

public class DatabaseManager {

    private final Main plugin;
    private HikariDataSource dataSource;

    public DatabaseManager(Main plugin) {
        this.plugin = plugin;
    }

    public void connect(String host, int port, String database, String user, String password) throws SQLException {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://" + host + ":" + port + "/" + database);
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(10000);
        config.setIdleTimeout(60000);
        config.setMaxLifetime(1800000);
        config.setDriverClassName("org.postgresql.Driver");

        dataSource = new HikariDataSource(config);
        createTables();
        plugin.getLogger().info("Connected to PostgreSQL.");
    }

    private void createTables() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS playtime (
                    uuid        VARCHAR(36) PRIMARY KEY,
                    username    VARCHAR(16) NOT NULL,
                    playtime_ms BIGINT      NOT NULL DEFAULT 0
                );
                """;
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public long getPlaytime(UUID uuid) {
        String sql = "SELECT playtime_ms FROM playtime WHERE uuid = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getLong("playtime_ms");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to fetch playtime for " + uuid + ": " + e.getMessage());
        }
        return 0;
    }

    public void addPlaytime(UUID uuid, String username, long sessionMs) {
        String sql = """
                INSERT INTO playtime (uuid, username, playtime_ms)
                VALUES (?, ?, ?)
                ON CONFLICT (uuid) DO UPDATE
                    SET playtime_ms = playtime.playtime_ms + EXCLUDED.playtime_ms,
                        username    = EXCLUDED.username;
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, username);
            stmt.setLong(3, sessionMs);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save playtime for " + uuid + ": " + e.getMessage());
        }
    }

    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("Disconnected from PostgreSQL.");
        }
    }
}