package net.fameless.core.command;

import net.fameless.core.BungeeAFK;
import net.fameless.core.caption.Caption;
import net.fameless.core.command.framework.CallerType;
import net.fameless.core.command.framework.Command;
import net.fameless.core.command.framework.CommandCaller;
import net.fameless.core.handling.AFKHandler;
import net.fameless.core.handling.AFKState;
import net.fameless.core.player.BAFKPlayer;

import java.util.List;

public class AFK extends Command {

    private final AFKHandler afkHandler;

    public AFK() {
        super(
                "afk",
                List.of(),
                CallerType.PLAYER,
                "/afk",
                "bungeeafk.afk"
        );
        this.afkHandler = BungeeAFK.getAFKHandler();
    }

    @Override
    protected void executeCommand(CommandCaller caller, String[] args) {
        BAFKPlayer<?> player = (BAFKPlayer<?>) caller;
        switch (player.getAfkState()) {
            case BYPASS -> player.sendMessage(Caption.of("command.afk_bypass"));
            case ACTIVE -> {
                player.setAfkState(AFKState.WARNED);
                player.setTimeSinceLastAction(afkHandler.getAfkDelayMillis());
            }
            default -> player.setTimeSinceLastAction(0);
        }
    }

    @Override
    protected List<String> tabComplete(CommandCaller caller, String[] args) {
        return List.of();
    }
}
