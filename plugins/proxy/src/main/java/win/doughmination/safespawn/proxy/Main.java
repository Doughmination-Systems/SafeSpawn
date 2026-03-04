// Copyright (c) 2026 Clove Twilight
// Licensed under the ESAL-1.3 License

package win.doughmination.safespawn.proxy;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import org.slf4j.Logger;
import win.doughmination.safespawn.proxy.database.DatabaseManager;
import win.doughmination.safespawn.proxy.listeners.PluginMessageListener;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Properties;

@Plugin(
        id = "safespawnproxy",
        name = "SafeSpawnProxy",
        version = "1.0.0",
        description = "Proxy plugin for the Safe Spawn Server",
        authors = {"Doughmination"},
        url = "https://modding.dougmination.win/"
)
public class Main {

    public static final String CHANNEL = "safespawn:playtime";

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    private DatabaseManager databaseManager;

    @Inject
    public Main(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server        = server;
        this.logger        = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        Properties config = loadConfig();

        String dbHost     = config.getProperty("db-host",     "localhost");
        int    dbPort     = Integer.parseInt(config.getProperty("db-port", "5432"));
        String dbName     = config.getProperty("db-name",     "minecraft");
        String dbUser     = config.getProperty("db-user",     "postgres");
        String dbPassword = config.getProperty("db-password", "password");

        databaseManager = new DatabaseManager(this);
        try {
            databaseManager.connect(dbHost, dbPort, dbName, dbUser, dbPassword);
        } catch (SQLException e) {
            logger.error("Failed to connect to PostgreSQL! Plugin will not function.", e);
            return;
        }

        server.getChannelRegistrar().register(MinecraftChannelIdentifier.from(CHANNEL));
        server.getEventManager().register(this, new PluginMessageListener(this));

        logger.info("SafeSpawnProxy enabled!");
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (databaseManager != null) databaseManager.disconnect();
        logger.info("SafeSpawnProxy disabled.");
    }

    private Properties loadConfig() {
        Properties props = new Properties();
        try {
            if (!Files.exists(dataDirectory)) Files.createDirectories(dataDirectory);
            Path configPath = dataDirectory.resolve("config.properties");
            if (!Files.exists(configPath)) saveDefaultConfig(configPath);
            try (InputStream in = Files.newInputStream(configPath)) {
                props.load(in);
            }
        } catch (IOException e) {
            logger.error("Failed to load config, using defaults.", e);
        }
        return props;
    }

    private void saveDefaultConfig(Path path) throws IOException {
        Files.writeString(path, """
                # SafeSpawnProxy config
                db-host=localhost
                db-port=5432
                db-name=minecraft
                db-user=postgres
                db-password=password
                """);
        logger.info("Created default config at {}", path);
    }

    public ProxyServer getServer()              { return server; }
    public Logger getLogger()                   { return logger; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
}
