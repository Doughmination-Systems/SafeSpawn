// Copyright (c) 2026 Clove Twilight
// Licensed under the ESAL-1.3 License

package win.doughmination.safespawn.playtime.expansion;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;
import win.doughmination.safespawn.playtime.Main;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NetworkExpansion extends PlaceholderExpansion implements PluginMessageListener {

    private static final String CHANNEL = "safespawn:servercount";
    private final Main plugin;
    private final Map<String, Integer> cache = new ConcurrentHashMap<>();
    private int taskId = -1;

    public NetworkExpansion(Main plugin) {
        this.plugin = plugin;
        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL);
        Bukkit.getMessenger().registerIncomingPluginChannel(plugin, CHANNEL, this);
        reloadServers();
    }

    public void reloadServers() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }

        List<String> servers = plugin.getConfig().getStringList("network.servers");
        taskId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () ->
                        servers.forEach(this::requestCount),
                20L, 20L * 10).getTaskId();
    }

    @Override public @NotNull String getIdentifier() { return "network"; }
    @Override public @NotNull String getAuthor()     { return "Doughmination"; }
    @Override public @NotNull String getVersion()    { return plugin.getDescription().getVersion(); }
    @Override public boolean persist()               { return true; }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (params.equals("online_total")) {
            return String.valueOf(cache.values().stream().mapToInt(Integer::intValue).sum());
        }
        if (params.startsWith("online_")) {
            String server = params.substring(7);
            return String.valueOf(cache.getOrDefault(server, 0));
        }
        return null;
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte[] message) {
        if (!channel.equals(CHANNEL)) return;
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(message));
            String server = in.readUTF();
            int count = in.readInt();
            cache.put(server, count);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to read server count response: " + e.getMessage());
        }
    }

    private void requestCount(String serverName) {
        Player any = Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
        if (any == null) return;
        try {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            new DataOutputStream(buf).writeUTF(serverName);
            any.sendPluginMessage(plugin, CHANNEL, buf.toByteArray());
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to send server count request: " + e.getMessage());
        }
    }
}