package net.fameless.core.messaging;

public enum RequestType {

    ACTION_CAUGHT,
    TELEPORT_PLAYER,
    ENABLE_COLLISION,
    DISABLE_COLLISION,
    SET_GAMEMODE,
    GAMEMODE_CHANGE,
    LOCATION_CHANGE;

    public boolean matches(String name) {
        return this.name().equalsIgnoreCase(name);
    }
}
