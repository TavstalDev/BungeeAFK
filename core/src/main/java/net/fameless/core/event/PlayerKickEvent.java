package net.fameless.core.event;

import net.fameless.core.player.BAFKPlayer;
import net.kyori.adventure.text.Component;

/**
 * Event triggered only when a player is kicked from the server <b>by BungeeAFK</b>.
 * <br>
 * Will not be triggered if the player is kicked by the server itself or other plugins.
 * <br>
 * Event can be cancelled using {@link PlayerKickEvent#setCancelled} to prevent the kick from happening.
 */
public class PlayerKickEvent implements CancellableEvent {

    private final BAFKPlayer<?> player;
    private Component reason;
    private boolean cancelled;

    public PlayerKickEvent(BAFKPlayer<?> player, Component reason) {
        this.player = player;
        this.reason = reason;
    }

    /**
     * Gets the player who is being kicked.
     *
     * @return the player being kicked
     */
    public BAFKPlayer<?> getPlayer() {
        return player;
    }

    /**
     * Gets the reason for the kick.
     *
     * @return the reason for the kick
     */
    public Component getReason() {
        return reason;
    }

    /**
     * Sets the reason for the kick.
     *
     * @param reason the reason to set
     */
    public void setReason(Component reason) {
        this.reason = reason;
    }

    /**
     * Checks if the kick event is cancelled.
     *
     * @return true if the event is cancelled, false otherwise
     */
    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Sets whether the kick event should be cancelled.
     *
     * @param cancelled true to cancel the event, false to allow it to proceed
     */
    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
