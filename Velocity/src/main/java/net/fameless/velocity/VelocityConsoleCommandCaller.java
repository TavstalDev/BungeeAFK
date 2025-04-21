package net.fameless.velocity;

import net.fameless.core.command.framework.ConsoleCommandCaller;
import net.kyori.adventure.text.Component;

public class VelocityConsoleCommandCaller extends ConsoleCommandCaller {

    private static VelocityConsoleCommandCaller instance;

    public static VelocityConsoleCommandCaller get() {
        if (instance == null) {
            instance = new VelocityConsoleCommandCaller();
        }
        return instance;
    }

    @Override
    public void sendMessage(Component component) {
        VelocityPlatform.getProxy().getConsoleCommandSource().sendMessage(component);
    }
}
