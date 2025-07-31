package net.fameless.bungee;

import net.fameless.core.BungeeAFK;
import net.fameless.core.handling.AFKHandler;
import net.fameless.core.handling.AFKState;
import net.fameless.core.messaging.RequestType;
import net.fameless.core.player.GameMode;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class BungeeAFKHandler extends AFKHandler implements Listener {

    @Override
    public void init() {
        BungeePlatform.proxyServer().registerChannel("bungee:bungeeafk");
        BungeePlatform.proxyServer().getPluginManager().registerListener(BungeePlatform.get(), this);
    }

    @EventHandler
    public void onPostLogin(@NotNull PostLoginEvent event) {
        BungeePlayer bungeePlayer = BungeePlayer.adapt(event.getPlayer());
        awaitConnectionAndHandleJoin(bungeePlayer, 0);
    }

    private void awaitConnectionAndHandleJoin(BungeePlayer bungeePlayer, int attempt) {
        final int maxAttempts = 50; // 50 * 100 ms = 5 second timeout

        BungeePlatform.get().getProxy().getScheduler().schedule(BungeePlatform.get(), () -> {
            Optional<ProxiedPlayer> playerOpt = bungeePlayer.getPlatformPlayer();
            if (playerOpt.isPresent() && playerOpt.get().getServer() != null) {
                handleJoin(bungeePlayer);
            } else if (attempt < maxAttempts) {
                awaitConnectionAndHandleJoin(bungeePlayer, attempt + 1);
            } else {
                LOGGER.error("Timeout while waiting for player {} to have a valid server connection. Previous states cannot be reverted.", bungeePlayer.getUniqueId());
            }
        }, 100, TimeUnit.MILLISECONDS);
    }

    @EventHandler
    public void onPluginMessage(@NotNull PluginMessageEvent event) {
        if (!event.getTag().equals("bungee:bungeeafk")) return;
        String data = new String(event.getData());
        String[] parts = data.split(";");
        if (RequestType.ACTION_CAUGHT.matches(parts[0])) {
            if (parts.length != 2) return;
            try {
                UUID playerUUID = UUID.fromString(parts[1]);
                BungeePlayer bungeePlayer = BungeePlayer.adapt(playerUUID).orElse(null);
                if (bungeePlayer == null) return;
                bungeePlayer.setTimeSinceLastAction(0);
                bungeePlayer.setAfkState(AFKState.ACTIVE);
                BungeeAFK.getAFKHandler().handleAction(bungeePlayer);
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
                BungeePlayer bungeePlayer = BungeePlayer.adapt(playerUUID).orElse(null);
                if (bungeePlayer == null) return;
                bungeePlayer.setGameMode(gameMode);
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
                BungeePlayer bungeePlayer = BungeePlayer.adapt(uuid).orElse(null);
                if (bungeePlayer == null) return;
                bungeePlayer.setLocation(new net.fameless.core.location.Location(worldName, x, y, z, pitch, yaw));
            } catch (Exception e) {
                LOGGER.error("Invalid location data received: {} stacktrace: {}", data, e.getMessage());
            }
        }
    }
}
