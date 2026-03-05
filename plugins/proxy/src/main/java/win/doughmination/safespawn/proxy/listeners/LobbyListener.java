// Copyright (c) 2026 Clove Twilight
// Licensed under the ESAL-1.3 License

package win.doughmination.safespawn.proxy.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import win.doughmination.safespawn.proxy.Main;

import java.util.Optional;

public class LobbyListener {

    private final Main plugin;

    public LobbyListener(Main plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!(event.getSource() instanceof ServerConnection connection)) return;
        if (!event.getIdentifier().getId().equals(Main.LOBBY_CHANNEL)) return;

        event.setResult(PluginMessageEvent.ForwardResult.handled());

        Player player = connection.getPlayer();
        Optional<RegisteredServer> lobby = plugin.getServer().getServer("lobby");

        if (lobby.isEmpty()) {
            plugin.getLogger().warn("Lobby server not found! Is it registered in velocity.toml?");
            return;
        }

        player.createConnectionRequest(lobby.get()).fireAndForget();
    }
}