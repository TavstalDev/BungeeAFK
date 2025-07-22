package net.fameless.spigot;

import net.fameless.core.BungeeAFK;
import net.fameless.core.BungeeAFKPlatform;
import net.fameless.core.handling.AFKHandler;
import net.fameless.core.handling.Action;
import net.kyori.adventure.text.Component;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class SpigotPlatform extends JavaPlugin implements BungeeAFKPlatform {

    private static SpigotPlatform instance;

    public static SpigotPlatform get() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        Action.setActionExcluded(Action.CONNECT, true);

        BungeeAFK.initCore(new SpigotModule());

        SpigotCommandHandler commandHandler = new SpigotCommandHandler();
        getCommand("bungeeafk").setExecutor(commandHandler);
        getCommand("bungeeafk").setTabCompleter(commandHandler);

        new Metrics(this, 25575);
    }

    @Override
    public void onDisable() {
        BungeeAFK.handleShutdown();
    }

    @Override
    public void shutDown(String message) {
        Bukkit.getPluginManager().disablePlugin(this);
    }

    @Override
    public void broadcast(Component message) {
        SpigotUtil.BUKKIT_AUDIENCES.all().sendMessage(message);
    }

    @Override
    public boolean doesServerExist(String serverName) {
        return false;
    }
}
