package net.fameless.spigot;

import net.fameless.core.BungeeAFK;
import net.fameless.core.BungeeAFKPlatform;
import net.fameless.core.handling.Action;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SpigotPlatform extends JavaPlugin implements BungeeAFKPlatform {

    private static final Logger LOGGER = LoggerFactory.getLogger("BungeeAFK/" + SpigotPlatform.class.getSimpleName());
    private static SpigotPlatform instance;

    public static SpigotPlatform get() {
        return instance;
    }

    @Override
    public void onEnable() {
        long startTime = System.currentTimeMillis();
        instance = this;

        Action.setActionExcluded(Action.CONNECT, true);

        BungeeAFK.initCore(new SpigotModule());

        SpigotCommandHandler commandHandler = new SpigotCommandHandler();
        getCommand("bungeeafk").setExecutor(commandHandler);
        getCommand("bungeeafk").setTabCompleter(commandHandler);
        getCommand("afk").setExecutor(commandHandler);
        getCommand("afk").setTabCompleter(commandHandler);

        new Metrics(this, 25575);
        long duration = System.currentTimeMillis() - startTime;
        LOGGER.info("Successfully enabled. (took {}ms)", duration);
    }

    @Override
    public void onDisable() {
        BungeeAFK.handleShutdown();
    }

    @Override
    public boolean doesServerExist(String serverName) {
        return false;
    }
}
