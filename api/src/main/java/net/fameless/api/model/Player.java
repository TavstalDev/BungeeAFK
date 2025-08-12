package net.fameless.api.model;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Represents a player in the BungeeAFK API.
 * This class encapsulates the player's unique identifier (UUID).
 * <br>
 * BungeeAFK uses the same UUID for players across different platforms,
 * ensuring consistency in player identification. E.g., calling getUniqueId() on spigot's
 * player will return the same UUID as calling getUniqueId() on BungeeAFK player representing them
 */
public record Player(UUID uuid) {

    @Contract(value = "_ -> new", pure = true)
    public static @NotNull Player of(UUID uuid) {
        return new Player(uuid);
    }
}
