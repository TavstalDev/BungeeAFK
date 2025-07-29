package net.fameless.bungee;

import net.fameless.core.command.framework.ConsoleCommandCaller;
import net.kyori.adventure.text.Component;

public class BungeeConsoleCommandCaller extends ConsoleCommandCaller {

    private static BungeeConsoleCommandCaller instance;

    public static BungeeConsoleCommandCaller get() {
        if (instance == null) {
            instance = new BungeeConsoleCommandCaller();
        }
        return instance;
    }

    @Override
    public void sendMessage(Component component) {
        BungeeUtil.BUNGEE_AUDIENCES.console().sendMessage(component);
    }
}
