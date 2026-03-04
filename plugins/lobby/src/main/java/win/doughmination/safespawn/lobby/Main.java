// Copyright (c) 2026 Clove Twilight
// Licensed under the ESAL-1.3 License

package win.doughmination.safespawn.lobby;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import win.doughmination.safespawn.lobby.listeners.JoinListener;

public class Main extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        FileConfiguration config = getConfig();

        double x         = config.getDouble("spawn.x", 0);
        double y         = config.getDouble("spawn.y", 64);
        double z         = config.getDouble("spawn.z", 0);
        float  yaw       = (float) config.getDouble("spawn.yaw", 0);
        float  pitch     = (float) config.getDouble("spawn.pitch", 0);
        String worldName = config.getString("spawn.world", "world");
        double voidY     = config.getDouble("void-rescue.trigger-y", -9);

        getServer().getPluginManager().registerEvents(
                new JoinListener(this, worldName, x, y, z, yaw, pitch), this);

        // Check every 10 ticks (0.5s) if any player has fallen into the void
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            World world = Bukkit.getWorld(worldName);
            if (world == null) return;

            Location spawn = new Location(world, x, y, z, yaw, pitch);

            for (Player player : world.getPlayers()) {
                if (player.getLocation().getY() <= voidY) {
                    player.teleport(spawn);
                    player.sendMessage("§cYou fell into the void! Teleporting you back to spawn.");
                }
            }
        }, 0L, 10L);

        getLogger().info("SafeSpawnLobby enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("SafeSpawnLobby disabled.");
    }
}