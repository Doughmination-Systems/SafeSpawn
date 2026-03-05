// Copyright (c) 2026 Clove Twilight
// Licensed under the ESAL-1.3 License

package win.doughmination.safespawn.playtime.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import win.doughmination.safespawn.playtime.Main;

public class LobbyCommand implements CommandExecutor {

    public static final String LOBBY_CHANNEL = "safespawn:lobby";

    private final Main plugin;

    public LobbyCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        // Send an empty message on safespawn:lobby — the proxy listener handles the connect
        player.sendPluginMessage(plugin, LOBBY_CHANNEL, new byte[0]);
        return true;
    }
}