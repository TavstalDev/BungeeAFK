package net.fameless.core;

import net.kyori.adventure.text.Component;


public interface BungeeAFKPlatform {

    void shutDown(String message);
    void broadcast(Component message);

    boolean doesServerExist(String serverName);

}
