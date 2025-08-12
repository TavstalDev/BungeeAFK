package net.fameless.api.event;

import net.fameless.api.model.AFKState;
import net.fameless.api.model.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Event triggered when a player's AFK state changes.
 */
public class PlayerAFKStateChangeEvent {

    private final Player player;
    private final AFKState oldState;
    private AFKState newState;

    public PlayerAFKStateChangeEvent(Player player, AFKState oldState, AFKState newState) {
        this.player = player;
        this.oldState = oldState;
        this.newState = newState;
    }

    /**
     * Gets the player whose AFK state has changed.
     *
     * @return the player
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Gets the old AFK state of the player.
     *
     * @return the old AFK state
     */
    public AFKState getOldState() {
        return oldState;
    }

    /**
     * Gets the new AFK state of the player.
     *
     * @return the new AFK state
     */
    public AFKState getNewState() {
        return newState;
    }

    /**
     * Sets the new AFK state of the player.
     *
     * @param newState the new AFK state to set
     */
    public void setNewState(@NotNull AFKState newState) {
        this.newState = newState;
    }
}
