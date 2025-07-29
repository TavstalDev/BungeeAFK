package net.fameless.bungee;

import net.fameless.core.command.framework.CallerType;
import net.fameless.core.event.EventDispatcher;
import net.fameless.core.event.PlayerKickEvent;
import net.fameless.core.player.BAFKPlayer;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class BungeePlayer extends BAFKPlayer<ProxiedPlayer> {

    public static final List<BungeePlayer> BUNGEE_PLAYERS = new ArrayList<>();

    public BungeePlayer(@NotNull ProxiedPlayer player) {
        super(player.getUniqueId());
        this.name = player.getName();
        BUNGEE_PLAYERS.add(this);
    }

    public static @NotNull Optional<BungeePlayer> adapt(@NotNull String name) {
        for (BungeePlayer bungeePlayer : BUNGEE_PLAYERS) {
            if (bungeePlayer.getName().equals(name)) {
                return Optional.of(bungeePlayer);
            }
        }
        return Optional.empty();
    }

    public static @NotNull Optional<BungeePlayer> adapt(@NotNull UUID uuid) {
        for (BungeePlayer bungeePlayer : BUNGEE_PLAYERS) {
            if (bungeePlayer.getUniqueId().equals(uuid)) {
                return Optional.of(bungeePlayer);
            }
        }
        return Optional.empty();
    }

    public static @NotNull BungeePlayer adapt(@NotNull ProxiedPlayer object) {
        for (BungeePlayer bungeePlayer : BUNGEE_PLAYERS) {
            Optional<ProxiedPlayer> platformPlayerOptional = bungeePlayer.getPlatformPlayer();
            if (platformPlayerOptional.isEmpty()) continue;

            if (platformPlayerOptional.get().getUniqueId().equals(object.getUniqueId())) {
                return bungeePlayer;
            }
        }
        return new BungeePlayer(object);
    }

    public static @NotNull Optional<BungeePlayer> adapt(@NotNull BAFKPlayer<?> battlePlayer) {
        Optional<?> playerOptional = battlePlayer.getPlatformPlayer();
        if (playerOptional.isEmpty()) {
            return Optional.empty();
        }

        if (playerOptional.get() instanceof ProxiedPlayer player) {
            return Optional.of(BungeePlayer.adapt(player));
        }
        return Optional.empty();
    }

    @Override
    public @NotNull CallerType callerType() {
        return CallerType.PLAYER;
    }

    @Override
    public @NotNull String getName() {
        if (this.name == null) {
            Optional<ProxiedPlayer> platformPlayer = getPlatformPlayer();
            if (platformPlayer.isEmpty()) {
                return "N/A";
            }
            this.name = platformPlayer.get().getName();
        }
        return this.name;
    }

    @Override
    public @NotNull Audience getAudience() {
        Optional<ProxiedPlayer> platformPlayerOptional = getPlatformPlayer();
        return platformPlayerOptional.map(proxiedPlayer -> BungeeUtil.BUNGEE_AUDIENCES.player(proxiedPlayer)).orElseGet(Audience::empty);
    }

    @Override
    public Optional<ProxiedPlayer> getPlatformPlayer() {
        return Optional.ofNullable(BungeePlatform.get().getProxy().getPlayer(getUniqueId()));
    }

    @Override
    public boolean isOffline() {
        Optional<ProxiedPlayer> platformPlayerOptional = getPlatformPlayer();
        return platformPlayerOptional.isEmpty() || !platformPlayerOptional.get().isConnected();
    }

    @Override
    public void connect(String serverName) {
        Optional<ProxiedPlayer> platformPlayer = getPlatformPlayer();
        platformPlayer.ifPresent(player -> player.connect(BungeePlatform.get().getProxy().getServerInfo(serverName)));
    }

    @Override
    public void kick(Component reason) {
        ProxiedPlayer player = getPlatformPlayer().orElse(null);
        if (player == null) return;

        PlayerKickEvent event = new PlayerKickEvent(this, reason);
        EventDispatcher.post(event);

        if (event.isCancelled()) {
            LOGGER.info("PlayerKickEvent was cancelled for player: {}", getName());
            return;
        }

        player.disconnect(BungeeComponentSerializer.get().serialize(event.getReason()));
    }

    @Override
    public boolean hasPermission(@NotNull String permission) {
        Optional<ProxiedPlayer> platformPlayerOptional = getPlatformPlayer();
        return platformPlayerOptional.isPresent() && platformPlayerOptional.get().hasPermission(permission);
    }

    @Override
    public String getCurrentServerName() {
        ProxiedPlayer player = getPlatformPlayer().orElse(null);
        if (player == null) {
            return "N/A";
        }
        Server playerServer = player.getServer();
        return playerServer != null ? playerServer.getInfo().getName() : "N/A";
    }
}
