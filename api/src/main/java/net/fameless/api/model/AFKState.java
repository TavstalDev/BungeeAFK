package net.fameless.api.model;

/**
 * Represents the different states a player can be in regarding AFK (Away From Keyboard) status.
 * This enum is used to track the player's AFK state in the BungeeAFK system.
 */
public enum AFKState {

    BYPASS,
    ACTIVE,
    WARNED,
    AFK,
    ACTION_TAKEN

}
