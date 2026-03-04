// Copyright (c) 2026 Clove Twilight
// Licensed under the ESAL-1.3 License

package win.doughmination.safespawn.playtime.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import win.doughmination.safespawn.playtime.Main;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlaytimeListener implements Listener {

    private final Main plugin;
    private final Map<UUID, Long> sessionStart = new ConcurrentHashMap<>();

    public PlaytimeListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        sessionStart.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        Long start = sessionStart.remove(uuid);
        if (start == null) return;

        long sessionMs = System.currentTimeMillis() - start;

        // Save to PostgreSQL, then refresh the leaderboard cache
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getDatabaseManager().addPlaytime(uuid, player.getName(), sessionMs);
            plugin.getPlaytimeExpansion().refreshCache();
        });

        // Forward to Velocity proxy so proxy DB stays in sync
        sendPlaytimeToProxy(player, sessionMs);
    }

    private void sendPlaytimeToProxy(Player player, long sessionMs) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeUTF(player.getUniqueId().toString());
            out.writeUTF(player.getName());
            out.writeLong(sessionMs);
            player.sendPluginMessage(plugin, Main.CHANNEL, bytes.toByteArray());
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to send playtime to proxy for " + player.getName() + ": " + e.getMessage());
        }
    }
}