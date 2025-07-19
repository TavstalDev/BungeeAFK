package net.fameless.core.player;

import net.fameless.core.BungeeAFK;
import net.fameless.core.caption.Caption;
import net.fameless.core.command.framework.CommandCaller;
import net.fameless.core.handling.AFKHandler;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public abstract class BAFKPlayer<PlatformPlayer> implements CommandCaller {

    public static final List<BAFKPlayer<?>> PLAYERS = new ArrayList<>();

    protected String name;
    private final UUID uuid;
    private boolean afk;
    private long whenToAfk;
    private long timeSinceLastAction = 0;

    public BAFKPlayer(UUID uuid) {
        this.uuid = uuid;
        updateWhenToAfk();
        PLAYERS.add(this);
    }

    public static @NotNull Optional<BAFKPlayer<?>> of(String name) {
        for (BAFKPlayer<?> battlePlayer : PLAYERS) {
            if (battlePlayer.getName().equalsIgnoreCase(name)) {
                return Optional.of(battlePlayer);
            }
        }
        return Optional.empty();
    }

    public static @NotNull Optional<BAFKPlayer<?>> of(UUID uuid) {
        for (BAFKPlayer<?> player : PLAYERS) {
            if (player.getUniqueId().equals(uuid)) {
                return Optional.of(player);
            }
        }
        return Optional.empty();
    }

    public static @NotNull List<BAFKPlayer<?>> getOnlinePlayers() {
        List<BAFKPlayer<?>> onlinePlayers = new ArrayList<>();
        for (BAFKPlayer<?> battlePlayer : PLAYERS) {
            if (battlePlayer.isOffline()) {
                continue;
            }
            onlinePlayers.add(battlePlayer);
        }
        return onlinePlayers;
    }

    public void updateWhenToAfk() {
        this.whenToAfk = System.currentTimeMillis() + BungeeAFK.injector().getInstance(AFKHandler.class).getAfkDelayMillis();
    }

    public boolean isAfk() {
        return afk;
    }

    public void setAfk(boolean afk) {
        this.afk = afk;
        if (afk) {
            sendActionbar(Caption.of("actionbar.afk"));
        }
    }

    public long getWhenToAfk() {
        return whenToAfk;
    }

    public UUID getUniqueId() {
        return uuid;
    }

    public long getTimeSinceLastAction() {
        return timeSinceLastAction;
    }

    public void setTimeSinceLastAction(long timeSinceLastAction) {
        this.timeSinceLastAction = timeSinceLastAction;
    }

    public void sendMessage(Component message) {
        getAudience().sendMessage(message);
    }

    public void sendActionbar(Component message) {
        getAudience().sendActionBar(message);
    }

    public void playSound(Sound sound) {
        getAudience().playSound(sound);
    }

    public abstract String getName();

    public abstract Audience getAudience();

    public abstract Optional<PlatformPlayer> getPlatformPlayer();

    public abstract boolean isOffline();

    public abstract void connect(String serverName);

    public abstract boolean hasPermission(String permission);
}
