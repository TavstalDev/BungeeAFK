package net.fameless.core;

import net.kyori.adventure.text.Component;

import java.util.logging.Logger;

public interface BungeeAFKPlatform {

    Logger getLogger();
    void shutDown(String message);
    void broadcast(Component message);

    boolean doesServerExist(String serverName);

}
