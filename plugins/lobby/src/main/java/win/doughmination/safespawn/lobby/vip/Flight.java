// Copyright (c) 2026 Clove Twilight
// Licensed under the ESAL-1.3 License

package win.doughmination.safespawn.lobby.vip;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import win.doughmination.safespawn.lobby.Main;

public class Flight implements Listener {

    // Flight zone bounds
    private static final double MIN_Y = -10;
    private static final double MAX_Y = 300;
    private static final double MIN_X = -160;
    private static final double MAX_X = 80;
    private static final double MIN_Z = -110;
    private static final double MAX_Z = 120;

    private static final String FLIGHT_PERMISSION = "lobby.flight";

    private final Main plugin;

    public Flight(Main plugin) {
        this.plugin = plugin;
    }

    /** True if the given coordinates are inside the flight zone. */
    private boolean inFlightZone(double x, double y, double z) {
        return x >= MIN_X && x <= MAX_X
                && y >= MIN_Y && y <= MAX_Y
                && z >= MIN_Z && z <= MAX_Z;
    }

    /** Grant or revoke flight based on the player's current position and permission. */
    private void updateFlight(Player player) {
        if (!player.hasPermission(FLIGHT_PERMISSION)) {
            if (player.getAllowFlight()) {
                player.setAllowFlight(false);
                player.setFlying(false);
            }
            return;
        }

        boolean inZone = inFlightZone(
                player.getLocation().getX(),
                player.getLocation().getY(),
                player.getLocation().getZ()
        );

        if (inZone && !player.getAllowFlight()) {
            player.setAllowFlight(true);
        } else if (!inZone && player.getAllowFlight()) {
            player.setAllowFlight(false);
            player.setFlying(false);
        }
    }

    // Enable flight when a VIP joins (if they spawn inside the zone)
    @EventHandler(priority = EventPriority.NORMAL)
    public void onJoin(PlayerJoinEvent event) {
        // Run one tick later so the teleport from JoinListener resolves first
        plugin.getServer().getScheduler().runTask(plugin, () -> updateFlight(event.getPlayer()));
    }

    // Revoke flight cleanly on quit so the flag isn't stored
    @EventHandler(priority = EventPriority.NORMAL)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        player.setAllowFlight(false);
        player.setFlying(false);
    }

    // Check zone boundary on every move
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        // Only react when the player crosses a block boundary (cheaper than every micro-move)
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        updateFlight(event.getPlayer());
    }
}