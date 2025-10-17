package net.fameless.api.event;

import net.fameless.api.model.Player;

import java.util.function.Consumer;

public class PlayerAutoClickerDetectedEvent {

    private final Player player;
    private Consumer<Player> action;

    public PlayerAutoClickerDetectedEvent(Player player, Consumer<Player> action) {
        this.player = player;
        this.action = action;
    }

    public Player getPlayer() {
        return player;
    }

    public Consumer<Player> getAction() {
        return action;
    }

    public void setAction(Consumer<Player> action) {
        if (action == null) {
            action = player -> {};
        }
        this.action = action;
    }
}
