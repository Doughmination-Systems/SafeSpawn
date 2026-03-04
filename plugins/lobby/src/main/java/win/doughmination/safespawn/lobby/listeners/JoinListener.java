// Copyright (c) 2026 Clove Twilight
// Licensed under the ESAL-1.3 License

package win.doughmination.safespawn.lobby.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import win.doughmination.safespawn.lobby.Main;

public class JoinListener implements Listener {

    private final Main plugin;
    private final String worldName;
    private final double x, y, z;
    private final float yaw, pitch;

    public JoinListener(Main plugin, String worldName, double x, double y, double z, float yaw, float pitch) {
        this.plugin    = plugin;
        this.worldName = worldName;
        this.x         = x;
        this.y         = y;
        this.z         = z;
        this.yaw       = yaw;
        this.pitch     = pitch;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            plugin.getLogger().warning("Spawn world '" + worldName + "' not found! Check config.yml.");
            return;
        }

        Location spawn = new Location(world, x, y, z, yaw, pitch);

        // Teleport on the next tick so the player is fully loaded first
        Bukkit.getScheduler().runTask(plugin, () -> player.teleport(spawn));
    }
}
