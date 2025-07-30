package net.fameless.core.messaging;

public enum RequestType {

    ACTION_CAUGHT,
    TELEPORT_PLAYER;

    public boolean matches(String name) {
        return this.name().equalsIgnoreCase(name);
    }
}
