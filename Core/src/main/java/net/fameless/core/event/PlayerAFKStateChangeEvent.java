package net.fameless.core.event;

import net.fameless.core.handling.AFKState;
import net.fameless.core.player.BAFKPlayer;

public class PlayerAFKStateChangeEvent {

    private final BAFKPlayer<?> player;
    private final AFKState oldState;
    private AFKState newState;

    public PlayerAFKStateChangeEvent(BAFKPlayer<?> player, AFKState oldState, AFKState newState) {
        this.player = player;
        this.oldState = oldState;
        this.newState = newState;
    }

    public BAFKPlayer<?> getPlayer() {
        return player;
    }

    public AFKState getOldState() {
        return oldState;
    }

    public AFKState getNewState() {
        return newState;
    }

    public void setNewState(AFKState newState) {
        this.newState = newState;
    }
}
