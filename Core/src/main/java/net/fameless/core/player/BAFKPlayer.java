package net.fameless.core.player;

import net.fameless.core.command.framework.CommandCaller;
import net.fameless.core.config.PluginConfig;
import net.fameless.core.handling.AFKState;
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
    private long timeSinceLastAction = 0;
    private AFKState afkState;

    public BAFKPlayer(UUID uuid) {
        this.uuid = uuid;
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
        for (BAFKPlayer<?> bafkPlayer : PLAYERS) {
            if (bafkPlayer.isOffline()) {
                continue;
            }
            onlinePlayers.add(bafkPlayer);
        }
        return onlinePlayers;
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

    public AFKState getAfkState() {
        if (PluginConfig.get().getBoolean("allow-bypass") && hasPermission("bungeeafk.bypass")) {
            return AFKState.BYPASS;
        }
        return afkState;
    }

    public void setAfkState(AFKState afkState) {
        this.afkState = afkState;
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
