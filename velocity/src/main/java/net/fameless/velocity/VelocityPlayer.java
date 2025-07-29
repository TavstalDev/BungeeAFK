package net.fameless.velocity;

import com.velocitypowered.api.proxy.Player;
import net.fameless.core.command.framework.CallerType;
import net.fameless.core.event.EventDispatcher;
import net.fameless.core.event.PlayerKickEvent;
import net.fameless.core.player.BAFKPlayer;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class VelocityPlayer extends BAFKPlayer<Player> {

    private static final List<VelocityPlayer> VELOCITY_PLAYERS = new ArrayList<>();

    public VelocityPlayer(@NotNull Player player) {
        super(player.getUniqueId());
        this.name = player.getUsername();
        VELOCITY_PLAYERS.add(this);
    }

    public static @NotNull VelocityPlayer adapt(Player player) {
        for (VelocityPlayer velocityPlayer : VELOCITY_PLAYERS) {
            Optional<Player> platformPlayerOptional = velocityPlayer.getPlatformPlayer();
            if (platformPlayerOptional.isEmpty()) continue;

            if (velocityPlayer.getPlatformPlayer().get().getUniqueId().equals(player.getUniqueId())) {
                return velocityPlayer;
            }
        }
        return new VelocityPlayer(player);
    }

    public static @NotNull Optional<VelocityPlayer> adapt(UUID uuid) {
        for (VelocityPlayer velocityPlayer : VELOCITY_PLAYERS) {
            if (velocityPlayer.getUniqueId().equals(uuid)) {
                return Optional.of(velocityPlayer);
            }
        }
        return Optional.empty();
    }

    public static Optional<VelocityPlayer> adapt(String name) {
        for (VelocityPlayer velocityPlayer : VELOCITY_PLAYERS) {
            if (velocityPlayer.getName().equals(name)) {
                return Optional.of(velocityPlayer);
            }
        }
        return Optional.empty();
    }

    @Override
    public CallerType callerType() {
        return CallerType.PLAYER;
    }

    @Override
    public String getName() {
        if (this.name == null) {
            Optional<Player> platformPlayer = getPlatformPlayer();
            if (platformPlayer.isEmpty()) {
                return "N/A";
            }
            this.name = platformPlayer.get().getUsername();
        }
        return this.name;
    }

    @Override
    public Audience getAudience() {
        Optional<Player> platformPlayer = getPlatformPlayer();
        return platformPlayer.map(Audience::audience).orElseGet(Audience::empty);
    }

    @Override
    public Optional<Player> getPlatformPlayer() {
        return VelocityPlatform.getProxy().getPlayer(getUniqueId());
    }

    @Override
    public boolean isOffline() {
        Optional<Player> platformPlayer = getPlatformPlayer();
        return platformPlayer.isEmpty() || !platformPlayer.get().isActive();
    }

    @Override
    public void connect(String serverName) {
        Optional<Player> platformPlayer = getPlatformPlayer();
        platformPlayer.ifPresent(player -> player.createConnectionRequest(VelocityPlatform.getProxy().getServer(serverName).orElseThrow()).fireAndForget());
    }

    @Override
    public void kick(Component reason) {
        Player player = getPlatformPlayer().orElse(null);
        if (player == null) return;

        PlayerKickEvent event = new PlayerKickEvent(this, reason);
        EventDispatcher.post(event);

        if (event.isCancelled()) {
            LOGGER.info("PlayerKickEvent was cancelled for player: {}", getName());
            return;
        }

        player.disconnect(event.getReason());
    }

    @Override
    public boolean hasPermission(String permission) {
        Optional<Player> platformPlayer = getPlatformPlayer();
        return platformPlayer.isPresent() && platformPlayer.get().hasPermission(permission);
    }

    @Override
    public @NotNull String getCurrentServerName() {
        Player player = getPlatformPlayer().orElse(null);
        if (player == null) return "N/A";
        return player.getCurrentServer()
                .map(serverConnection -> serverConnection.getServerInfo().getName())
                .orElse("N/A");
    }
}
