// Copyright (c) 2026 Clove Twilight
// Licensed under the ESAL-1.3 License

package win.doughmination.safespawn.proxy.database;

import win.doughmination.safespawn.proxy.Main;

import java.sql.*;
import java.util.UUID;

public class DatabaseManager {

    private final Main plugin;
    private Connection connection;

    public DatabaseManager(Main plugin) {
        this.plugin = plugin;
    }

    public void connect(String host, int port, String database, String user, String password) throws SQLException {
        // Explicitly load the driver — Velocity's classloader isolation prevents
        // DriverManager from finding it automatically via ServiceLoader.
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("PostgreSQL JDBC driver not found in jar!", e);
        }

        String url = "jdbc:postgresql://" + host + ":" + port + "/" + database;
        connection = DriverManager.getConnection(url, user, password);
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
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    /**
     * Returns total playtime in milliseconds for the given player, or 0 if not found.
     */
    public long getPlaytime(UUID uuid) {
        String sql = "SELECT playtime_ms FROM playtime WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getLong("playtime_ms");
        } catch (SQLException e) {
            plugin.getLogger().error("Failed to fetch playtime for {}: {}", uuid, e.getMessage());
        }
        return 0;
    }

    /**
     * Upserts the player's playtime, adding sessionMs to any existing value.
     */
    public void addPlaytime(UUID uuid, String username, long sessionMs) {
        String sql = """
                INSERT INTO playtime (uuid, username, playtime_ms)
                VALUES (?, ?, ?)
                ON CONFLICT (uuid) DO UPDATE
                    SET playtime_ms = playtime.playtime_ms + EXCLUDED.playtime_ms,
                        username    = EXCLUDED.username;
                """;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, username);
            stmt.setLong(3, sessionMs);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().error("Failed to save playtime for {}: {}", uuid, e.getMessage());
        }
    }

    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("Disconnected from PostgreSQL.");
            }
        } catch (SQLException e) {
            plugin.getLogger().error("Error closing database connection: {}", e.getMessage());
        }
    }
}