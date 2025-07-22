package net.fameless.velocity;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import net.fameless.core.caption.Caption;
import net.fameless.core.config.PluginConfig;
import net.fameless.core.handling.AFKHandler;
import net.fameless.core.handling.AFKState;
import net.fameless.core.handling.Action;
import net.fameless.core.player.BAFKPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class VelocityAFKHandler extends AFKHandler {

    private final Map<BAFKPlayer<?>, String> playerLastServerMap = new ConcurrentHashMap<>();

    @Override
    public void init() {
        VelocityPlatform.getProxy().getChannelRegistrar().register(MinecraftChannelIdentifier.create("bungee", "bungeeafk"));
        VelocityPlatform.getProxy().getEventManager().register(VelocityPlatform.get(), this);
    }

    @Override
    protected void handleAction(@NotNull BAFKPlayer<?> bafkPlayer) {
        if (!(bafkPlayer instanceof VelocityPlayer velocityPlayer)) return;

        Player platformPlayer = velocityPlayer.getPlatformPlayer().orElse(null);
        if (platformPlayer == null) return;

        ServerConnection playerServer = platformPlayer.getCurrentServer().orElse(null);
        if (playerServer == null) return;

        String afkServerName = PluginConfig.get().getString("afk-server-name", "");
        if (velocityPlayer.getAfkState() != AFKState.ACTION_TAKEN && playerServer.getServerInfo().getName().equalsIgnoreCase(afkServerName)) {
            velocityPlayer.connect(playerLastServerMap.getOrDefault(velocityPlayer, "lobby"));
            return;
        }

        if (action == Action.NOTHING) return;
        if (velocityPlayer.getAfkState() != AFKState.AFK) return;
        if (velocityPlayer.getTimeSinceLastAction() < actionDelay) return;

        switch (action) {
            case CONNECT -> {
                if (!Action.isAfkServerConfigured()) {
                    LOGGER.warn("AFK server not found. Defaulting to KICK.");
                    this.action = Action.KICK;
                    velocityPlayer.kick(Caption.of("notification.afk_kick"));
                    LOGGER.info("Kicked {} for being AFK.", velocityPlayer.getName());
                    return;
                }
                playerLastServerMap.put(velocityPlayer, playerServer.getServerInfo().getName());
                velocityPlayer.connect(afkServerName);
                velocityPlayer.sendMessage(Caption.of("notification.afk_disconnect"));
                LOGGER.info("Moved {} to AFK server.", velocityPlayer.getName());
            }
            case KICK -> {
                velocityPlayer.kick(Caption.of("notification.afk_kick"));
                LOGGER.info("Kicked {} for being AFK.", velocityPlayer.getName());
            }
        }
    }

    @Subscribe
    public void onPostLogin(@NotNull PostLoginEvent event) {
        VelocityPlayer velocityPlayer = VelocityPlayer.adapt(event.getPlayer());
        velocityPlayer.setTimeSinceLastAction(0);
        velocityPlayer.setAfkState(AFKState.ACTIVE);
    }

    @Subscribe
    public void onPluginMessage(@NotNull PluginMessageEvent event) {
        if (!event.getIdentifier().getId().equals("bungee:bungeeafk")) return;
        String data = new String(event.getData());
        String[] parts = data.split(";");

        if (parts.length != 2) return;

        UUID playerUUID = UUID.fromString(parts[0]);
        String status = parts[1];

        if (status.equals("action_caught")) {
            VelocityPlayer velocityPlayer = VelocityPlayer.adapt(playerUUID).orElse(null);
            if (velocityPlayer == null) return;
            velocityPlayer.setTimeSinceLastAction(0);
            velocityPlayer.setAfkState(AFKState.ACTIVE);
        }
    }
}
