// Copyright (c) 2026 Clove Twilight
// Licensed under the ESAL-1.3 License

package win.doughmination.safespawn.playtime.expansion;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import win.doughmination.safespawn.playtime.Main;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PlaytimeExpansion extends PlaceholderExpansion {

    private static final int  TOP_SIZE    = 10;
    private static final long CACHE_TICKS = 20L * 60 * 5; // 5 minutes

    private final Main plugin;
    private final List<String> cachedTop = new ArrayList<>();

    public PlaytimeExpansion(Main plugin) {
        this.plugin = plugin;
        for (int i = 1; i <= TOP_SIZE; i++) cachedTop.add("#" + i + " ...");

        // Fetch immediately on startup, then every 5 minutes
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::refreshCache);
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::refreshCache, CACHE_TICKS, CACHE_TICKS);
    }

    @Override public @NotNull String getIdentifier() { return "safespawn"; }
    @Override public @NotNull String getAuthor()     { return "Doughmination"; }
    @Override public @NotNull String getVersion()    { return plugin.getDescription().getVersion(); }
    @Override public boolean persist()               { return true; }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        // %safespawn_top_1% through %safespawn_top_10%
        if (params.startsWith("top_")) {
            try {
                int rank = Integer.parseInt(params.substring(4));
                if (rank >= 1 && rank <= TOP_SIZE) {
                    synchronized (cachedTop) {
                        return cachedTop.size() >= rank ? cachedTop.get(rank - 1) : "#" + rank + " ...";
                    }
                }
            } catch (NumberFormatException ignored) {}
            return null;
        }

        // %safespawn_my_rank%
        if (params.equalsIgnoreCase("my_rank")) {
            if (player == null) return "#? ... 0s";
            return getPersonalRank(player);
        }

        return null;
    }

    /**
     * Call this after a player's playtime is saved so the leaderboard
     * updates immediately rather than waiting up to 5 minutes.
     */
    public void refreshCache() {
        List<String> fresh = fetchTop();
        synchronized (cachedTop) {
            cachedTop.clear();
            cachedTop.addAll(fresh);
        }
    }

    // -----------------------------------------------------------------
    // Leaderboard fetch
    // -----------------------------------------------------------------

    private List<String> fetchTop() {
        List<String> lines = new ArrayList<>();
        String sql = "SELECT username, playtime_ms FROM playtime ORDER BY playtime_ms DESC LIMIT ?";

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, TOP_SIZE);
            ResultSet rs = stmt.executeQuery();

            int rank = 1;
            while (rs.next()) {
                lines.add("#" + rank + " " + rs.getString("username") + " ... " + format(rs.getLong("playtime_ms")));
                rank++;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to fetch leaderboard: " + e.getMessage());
        }

        while (lines.size() < TOP_SIZE) lines.add("#" + (lines.size() + 1) + " ...");
        return lines;
    }

    // -----------------------------------------------------------------
    // Personal rank (live query)
    // -----------------------------------------------------------------

    private String getPersonalRank(OfflinePlayer player) {
        String timeSql = "SELECT playtime_ms FROM playtime WHERE uuid = ?";
        String rankSql = """
                SELECT COUNT(*) + 1 AS rank
                FROM playtime
                WHERE playtime_ms > (
                    SELECT COALESCE(playtime_ms, -1) FROM playtime WHERE uuid = ?
                )
                """;
        String uuid = player.getUniqueId().toString();

        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            long ms;
            try (PreparedStatement stmt = conn.prepareStatement(timeSql)) {
                stmt.setString(1, uuid);
                ResultSet rs = stmt.executeQuery();
                if (!rs.next()) return "#? " + player.getName() + " ... 0s";
                ms = rs.getLong("playtime_ms");
            }

            long rank = 1;
            try (PreparedStatement stmt = conn.prepareStatement(rankSql)) {
                stmt.setString(1, uuid);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) rank = rs.getLong("rank");
            }

            return "#" + rank + " " + player.getName() + " ... " + format(ms);

        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to fetch rank for " + player.getName() + ": " + e.getMessage());
        }

        return "#? " + player.getName() + " ... 0s";
    }

    // -----------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------

    private String format(long ms) {
        long seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(ms) % 60;
        long hours   = TimeUnit.MILLISECONDS.toHours(ms)   % 24;
        long days    = TimeUnit.MILLISECONDS.toDays(ms);

        if (days > 0)    return days + "d " + hours + "h " + minutes + "m " + seconds + "s";
        if (hours > 0)   return hours + "h " + minutes + "m " + seconds + "s";
        if (minutes > 0) return minutes + "m " + seconds + "s";
        return seconds + "s";
    }
}