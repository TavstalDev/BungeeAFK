package net.fameless.bungee;

import net.fameless.core.BungeeAFK;
import net.fameless.core.BungeeAFKPlatform;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import org.bstats.bungeecord.Metrics;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public final class BungeePlatform extends Plugin implements BungeeAFKPlatform {

    private static BungeePlatform instance;
    private static ProxyServer proxyServer;

    public static BungeePlatform get() {
        return instance;
    }

    public static ProxyServer proxyServer() {
        return proxyServer;
    }

    private Configuration config;

    @Override
    public void onEnable() {
        instance = this;
        proxyServer = getProxy();

        try {
            makeFile();
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(new File(getDataFolder(), "config.yml"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        getProxy().getPluginManager().registerCommand(this, new BungeeCommandHandler("bungeeafk", new String[]{"bafk"}));
        getProxy().getPluginManager().registerCommand(this, new BungeeCommandHandler("afk", new String[]{}));

        BungeeAFK.initCore(new BungeeModule());

        proxyServer.registerChannel("bungee:bungeeafk");

        new Metrics(this, 25576);
    }

    @Override
    public void onDisable() {
        BungeeAFK.handleShutdown();
    }

    public void makeFile() throws IOException {
        if (!getDataFolder().exists()) {
            getLogger().info("Created config folder: " + getDataFolder().mkdir());
        }

        File configFile = new File(getDataFolder(), "config.yml");

        if (!configFile.exists()) {
            FileOutputStream outputStream = new FileOutputStream(configFile);
            InputStream in = getResourceAsStream("config.yml");
            in.transferTo(outputStream);
        }
    }

    public Configuration getConfig() {
        return config;
    }

    @Override
    public void shutDown(@NotNull String message) {
        proxyServer().stop(message);
    }

    @Override
    public void broadcast(Component message) {
        getProxy().broadcast(BungeeComponentSerializer.get().serialize(message));
    }

    @Override
    public boolean doesServerExist(String serverName) {
        return getProxy().getServerInfo(serverName) != null;
    }
}
