package win.doughmination.safespawn.playtime.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import win.doughmination.safespawn.playtime.Main;

public class ReloadCommand implements CommandExecutor {

    private final Main plugin;

    public ReloadCommand(Main plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!sender.hasPermission("safespawn.admin")) {
            sender.sendMessage("You don't have permission to do that.");
            return true;
        }

        if (args.length == 0 || !args[0].equalsIgnoreCase("reload")) {
            sender.sendMessage("§eUsage: /safespawn reload");
            return true;
        }

        plugin.reloadConfig();
        plugin.getNetworkExpansion().reloadServers();
        sender.sendMessage("SafeSpawn config reloaded!");
        return true;
    }
}