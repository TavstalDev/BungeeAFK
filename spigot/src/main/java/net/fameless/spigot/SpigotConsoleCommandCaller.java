package net.fameless.spigot;

import net.fameless.core.command.framework.ConsoleCommandCaller;
import net.kyori.adventure.text.Component;

public class SpigotConsoleCommandCaller extends ConsoleCommandCaller {

    private static SpigotConsoleCommandCaller instance;

    public static SpigotConsoleCommandCaller get() {
        if (instance == null) {
            instance = new SpigotConsoleCommandCaller();
        }
        return instance;
    }

    @Override
    public void sendMessage(Component component) {
        SpigotUtil.BUKKIT_AUDIENCES.console().sendMessage(component);
    }
}
