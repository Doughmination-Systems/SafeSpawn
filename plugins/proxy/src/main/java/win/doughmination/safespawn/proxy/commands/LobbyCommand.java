// Copyright (c) 2026 Clove Twilight
// Licensed under the ESAL-1.3 License

package win.doughmination.safespawn.proxy.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import win.doughmination.safespawn.proxy.Main;

import java.util.Optional;

public class LobbyCommand implements SimpleCommand {

    private final Main plugin;

    public LobbyCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player player)) {
            invocation.source().sendPlainMessage("This command can only be used by players.");
            return;
        }

        Optional<RegisteredServer> lobby = plugin.getServer().getServer("lobby");

        if (lobby.isEmpty()) {
            player.sendPlainMessage("Lobby server not found!");
            return;
        }

        // If already on lobby, tell them
        player.getCurrentServer().ifPresent(conn -> {
            if (conn.getServerInfo().getName().equalsIgnoreCase("lobby")) {
                return;
            }
        });

        player.createConnectionRequest(lobby.get()).fireAndForget();
    }
}