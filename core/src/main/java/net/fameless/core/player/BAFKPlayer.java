package net.fameless.core.player;

import net.fameless.core.caption.Caption;
import net.fameless.core.command.framework.CommandCaller;
import net.fameless.core.config.PluginConfig;
import net.fameless.core.event.EventDispatcher;
import net.fameless.core.event.PlayerAFKStateChangeEvent;
import net.fameless.core.handling.AFKState;
import net.fameless.core.util.PlayerFilters;
import net.fameless.core.util.PluginMessage;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class BAFKPlayer<PlatformPlayer> implements CommandCaller {

    protected static final Logger LOGGER = LoggerFactory.getLogger("BungeeAFK/Player");
    public static final List<BAFKPlayer<?>> PLAYERS = new CopyOnWriteArrayList<>();

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

    public UUID getUniqueId() {
        return uuid;
    }

    public long getTimeSinceLastAction() {
        return timeSinceLastAction;
    }

    public void setTimeSinceLastAction(long timeSinceLastAction) {
        this.timeSinceLastAction = timeSinceLastAction;
    }

    public void increaseTimeSinceLastAction(long increment) {
        if (afkState == AFKState.BYPASS) return;
        this.timeSinceLastAction += increment;
    }

    public AFKState getAfkState() {
        if (PluginConfig.get().getBoolean("allow-bypass") && hasPermission("bungeeafk.bypass")) {
            return AFKState.BYPASS;
        }
        return afkState;
    }

    public void setAfkState(AFKState afkState) {
        if (this.afkState == afkState) return;

        PlayerAFKStateChangeEvent event = new PlayerAFKStateChangeEvent(this, this.afkState, afkState);
        EventDispatcher.post(event);

        if ((this.afkState == AFKState.AFK || this.afkState == AFKState.ACTION_TAKEN) && event.getNewState() == AFKState.ACTIVE) {
            sendMessage(Caption.of("notification.afk_return"));
            PluginMessage.broadcastMessageToFiltered(
                    Caption.of("notification.afk_return_broadcast",
                    TagResolver.resolver("player", Tag.inserting(Component.text(getName())))),
                    PlayerFilters.matches(this).negate()
            );
        }
        this.afkState = event.getNewState();
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

    public abstract void kick(Component reason);

    public abstract boolean hasPermission(String permission);

    public abstract String getCurrentServerName();
}
