package net.fameless.core.event;

import net.fameless.core.player.BAFKPlayer;
import net.kyori.adventure.text.Component;

public class PlayerKickEvent implements CancellableEvent {

    private final BAFKPlayer<?> player;
    private Component reason;
    private boolean cancelled;

    public PlayerKickEvent(BAFKPlayer<?> player, Component reason) {
        this.player = player;
        this.reason = reason;
    }

    public BAFKPlayer<?> getPlayer() {
        return player;
    }

    public Component getReason() {
        return reason;
    }

    public void setReason(Component reason) {
        this.reason = reason;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
