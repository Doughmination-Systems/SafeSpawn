// Copyright (c) 2026 Clove Twilight
// Licensed under the ESAL-1.3 License

package win.doughmination.safespawn.playtime;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import win.doughmination.safespawn.playtime.commands.*;
import win.doughmination.safespawn.playtime.database.*;
import win.doughmination.safespawn.playtime.expansion.*;
import win.doughmination.safespawn.playtime.listeners.*;

import java.sql.SQLException;

public class Main extends JavaPlugin {

    public static final String CHANNEL = "safespawn:playtime";

    private DatabaseManager databaseManager;
    private PlaytimeExpansion playtimeExpansion;
    private NetworkExpansion networkExpansion;

    @Override
    public void onEnable() {
        getCommand("safespawn").setExecutor(new ReloadCommand(this));
        saveDefaultConfig();
        FileConfiguration config = getConfig();

        String dbHost     = config.getString("database.host",     "localhost");
        int    dbPort     = config.getInt(   "database.port",     5432);
        String dbName     = config.getString("database.name",     "minecraft");
        String dbUser     = config.getString("database.user",     "postgres");
        String dbPassword = config.getString("database.password", "password");

        databaseManager = new DatabaseManager(this);
        try {
            databaseManager.connect(dbHost, dbPort, dbName, dbUser, dbPassword);
        } catch (SQLException e) {
            getLogger().severe("Failed to connect to PostgreSQL! Disabling plugin.");
            getLogger().severe(e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getServer().getMessenger().registerOutgoingPluginChannel(this, CHANNEL);

        getServer().getPluginManager().registerEvents(new PlaytimeListener(this), this);

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            playtimeExpansion = new PlaytimeExpansion(this);
            playtimeExpansion.register();
            getLogger().info("Hooked into PlaceholderAPI — %safespawn_top_1% and %safespawn_my_rank% are ready!");

            networkExpansion = new NetworkExpansion(this);
            networkExpansion.register();
            getLogger().info("Hooked into PlaceholderAPI — %network_online_survival% is ready!");
        } else {
            getLogger().warning("PlaceholderAPI not found! Placeholders will not be available.");
        }

        getLogger().info("SafeSpawnPlaytime enabled!");
    }

    @Override
    public void onDisable() {
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
        if (databaseManager != null) databaseManager.disconnect();
        getLogger().info("SafeSpawnPlaytime disabled.");
    }

    public DatabaseManager getDatabaseManager()     { return databaseManager; }
    public PlaytimeExpansion getPlaytimeExpansion() { return playtimeExpansion; }
    public NetworkExpansion getNetworkExpansion()   { return networkExpansion; }
}