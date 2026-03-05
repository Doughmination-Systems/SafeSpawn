package win.doughmination.safespawn.proxy.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import win.doughmination.safespawn.proxy.Main;

import java.io.*;

public class ServerCountListener {

    private final Main plugin;

    public ServerCountListener(Main plugin) { this.plugin = plugin; }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!(event.getSource() instanceof ServerConnection source)) return;
        if (!event.getIdentifier().getId().equals(Main.COUNT_CHANNEL)) return;

        event.setResult(PluginMessageEvent.ForwardResult.handled());

        try {
            String serverName = new DataInputStream(new ByteArrayInputStream(event.getData())).readUTF();
            int count = plugin.getServer().getServer(serverName)
                    .map(s -> s.getPlayersConnected().size())
                    .orElse(-1);

            // Send count back to the requesting server
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(buf);
            out.writeUTF(serverName);
            out.writeInt(count);
            source.sendPluginMessage(MinecraftChannelIdentifier.from(Main.COUNT_CHANNEL), buf.toByteArray());

        } catch (IOException e) {
            plugin.getLogger().error("Failed to handle server count request: {}", e.getMessage());
        }
    }
}