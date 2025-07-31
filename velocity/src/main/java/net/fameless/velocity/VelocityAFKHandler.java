package net.fameless.velocity;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import net.fameless.core.BungeeAFK;
import net.fameless.core.handling.AFKHandler;
import net.fameless.core.handling.AFKState;
import net.fameless.core.messaging.RequestType;
import net.fameless.core.player.GameMode;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class VelocityAFKHandler extends AFKHandler {

    @Override
    public void init() {
        VelocityPlatform.getProxy().getChannelRegistrar().register(MinecraftChannelIdentifier.create("bungee", "bungeeafk"));
        VelocityPlatform.getProxy().getEventManager().register(VelocityPlatform.get(), this);
    }

    @Subscribe
    public void onConnect(@NotNull ServerPostConnectEvent event) {
        if (event.getPreviousServer() != null) return;
        handleJoin(VelocityPlayer.adapt(event.getPlayer()));
    }

    @Subscribe
    public void onPluginMessage(@NotNull PluginMessageEvent event) {
        if (!event.getIdentifier().getId().equals("bungee:bungeeafk")) return;
        String data = new String(event.getData());
        String[] parts = data.split(";");
        if (RequestType.ACTION_CAUGHT.matches(parts[0])) {
            if (parts.length < 2) return;
            try {
                UUID playerUUID = UUID.fromString(parts[1]);
                VelocityPlayer velocityPlayer = VelocityPlayer.adapt(playerUUID).orElse(null);
                if (velocityPlayer == null) return;
                velocityPlayer.setTimeSinceLastAction(0);
                velocityPlayer.setAfkState(AFKState.ACTIVE);
                BungeeAFK.getAFKHandler().handleAction(velocityPlayer);
            } catch (Exception e) {
                LOGGER.error("Invalid data received: {} stacktrace: {}", data, e.getMessage());
            }
            return;
        }
        if (RequestType.GAMEMODE_CHANGE.matches(parts[0])) {
            if (parts.length < 3) return;
            try {
                UUID playerUUID = UUID.fromString(parts[1]);
                GameMode gameMode = GameMode.valueOf(parts[2].toUpperCase(Locale.ROOT));
                VelocityPlayer velocityPlayer = VelocityPlayer.adapt(playerUUID).orElse(null);
                if (velocityPlayer == null) return;
                velocityPlayer.setGameMode(gameMode);
            } catch (Exception e) {
                LOGGER.error("Invalid game mode data received: {} stacktrace: {}", data, e.getMessage());
            }
            return;
        }
        if (RequestType.LOCATION_CHANGE.matches(parts[0])) {
            if (parts.length < 8) return;
            try {
                UUID uuid = UUID.fromString(parts[1]);
                String worldName = parts[2];
                double x = Double.parseDouble(parts[3]);
                double y = Double.parseDouble(parts[4]);
                double z = Double.parseDouble(parts[5]);
                float yaw = Float.parseFloat(parts[6]);
                float pitch = Float.parseFloat(parts[7]);
                VelocityPlayer velocityPlayer = VelocityPlayer.adapt(uuid).orElse(null);
                if (velocityPlayer == null) return;
                velocityPlayer.setLocation(new net.fameless.core.location.Location(worldName, x, y, z, pitch, yaw));
            } catch (Exception e) {
                LOGGER.error("Invalid location data received: {} stacktrace: {}", data, e.getMessage());
            }
        }
    }
}
