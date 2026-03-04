// Copyright (c) 2026 Clove Twilight
// Licensed under the ESAL-1.3 License

package win.doughmination.safespawn.proxy.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.ServerConnection;
import win.doughmination.safespawn.proxy.Main;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.UUID;

public class PluginMessageListener {

    private final Main plugin;

    public PluginMessageListener(Main plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        // Only accept messages from backend servers, not clients
        if (!(event.getSource() instanceof ServerConnection)) return;
        if (!event.getIdentifier().getId().equals(Main.CHANNEL)) return;

        event.setResult(PluginMessageEvent.ForwardResult.handled());

        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(event.getData()));
            UUID uuid      = UUID.fromString(in.readUTF());
            String username = in.readUTF();
            long sessionMs  = in.readLong();

            // Save async so we don't block the event thread
            plugin.getServer().getScheduler()
                    .buildTask(plugin, () -> plugin.getDatabaseManager().addPlaytime(uuid, username, sessionMs))
                    .schedule();

        } catch (IOException e) {
            plugin.getLogger().error("Failed to parse playtime plugin message: {}", e.getMessage());
        }
    }
}
