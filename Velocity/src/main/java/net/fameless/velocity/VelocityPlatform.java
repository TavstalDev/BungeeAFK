package net.fameless.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import net.fameless.core.BungeeAFK;
import net.fameless.core.BungeeAFKPlatform;
import net.kyori.adventure.text.Component;
import org.bstats.velocity.Metrics;

import java.nio.file.Path;
import java.util.logging.Logger;

@Plugin(
    id = "bungeeafk",
    name = "BungeeAFK-Velocity",
    version = "1.0.0"
    ,description = "BungeeAFK for Velocity proxy"
    ,url = "https://github.com/Fameless9"
    ,authors = {"Fameless9"}
)
public class VelocityPlatform implements BungeeAFKPlatform {

    private static VelocityPlatform instance;

    private final Logger logger;
    private final ProxyServer proxyServer;
    private final Path dataDirectory;
    private final Metrics.Factory metricsFactory;

    @Inject
    public VelocityPlatform(ProxyServer proxyServer, Logger logger, @DataDirectory Path dataDirectory, Metrics.Factory metricsFactory) {
        this.proxyServer = proxyServer;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.metricsFactory = metricsFactory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        instance = this;

        proxyServer.getCommandManager().register("bungeeafk", new VelocityCommandHandler(), "bafk");

        BungeeAFK.initCore(new VelocityModule());

        metricsFactory.make(this, 25577);
    }

    public static Path getDataDirectory() {
        return instance.dataDirectory;
    }

    public static ProxyServer getProxy() {
        return instance.proxyServer;
    }

    public static VelocityPlatform get() {
        return instance;
    }

    @Override
    public java.util.logging.Logger getLogger() {
        return logger;
    }

    @Override
    public void shutDown(String message) {
        proxyServer.shutdown(Component.text(message));
    }

    @Override
    public void broadcast(Component message) {
        proxyServer.sendMessage(message);
    }

    @Override
    public boolean doesServerExist(String serverName) {
        return proxyServer.getServer(serverName).isPresent();
    }
}
