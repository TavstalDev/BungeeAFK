package net.fameless.bungee;

import net.fameless.core.caption.Caption;
import net.fameless.core.config.PluginConfig;
import net.fameless.core.handling.AFKHandler;
import net.fameless.core.handling.AFKState;
import net.fameless.core.handling.Action;
import net.fameless.core.player.BAFKPlayer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class BungeeAFKHandler extends AFKHandler implements Listener {

    private final Map<BAFKPlayer<?>, String> playerLastServerMap = new HashMap<>();

    @Override
    public void init() {
        BungeePlatform.proxyServer().registerChannel("bungee:bungeeafk");
        BungeePlatform.proxyServer().getPluginManager().registerListener(BungeePlatform.get(), this);
    }

    @Override
    protected void handleAction(@NotNull BAFKPlayer<?> bafkPlayer) {
        if (!(bafkPlayer instanceof BungeePlayer bungeePlayer)) return;

        long timeSinceLastAction = bungeePlayer.getTimeSinceLastAction();

        Optional<ProxiedPlayer> platformPlayerOptional = bungeePlayer.getPlatformPlayer();
        if (platformPlayerOptional.isEmpty()) return;
        ProxiedPlayer platformPlayer = platformPlayerOptional.get();

        Server playerServer = platformPlayer.getServer();
        if (playerServer == null) return;

        if (bungeePlayer.getAfkState() != AFKState.ACTION_TAKEN && playerServer.getInfo().getName().equalsIgnoreCase(PluginConfig.get().getString("afk-server-name", ""))) {
            bungeePlayer.connect(playerLastServerMap.getOrDefault(bungeePlayer, "lobby"));
            return;
        }

        if (action.equals(Action.NOTHING)) return;
        if (bungeePlayer.getAfkState() != AFKState.AFK) return;
        if (timeSinceLastAction < actionDelay) return;

        switch (action) {
            case CONNECT -> {
                if (!Action.isAfkServerConfigured()) {
                    LOGGER.warn("AFK server not found. Defaulting to KICK.");

                    this.action = Action.KICK;
                    bungeePlayer.kick(Caption.of("notification.afk_kick"));
                    LOGGER.info("Kicked {} for being AFK.", bungeePlayer.getName());
                    return;
                }

                playerLastServerMap.put(bungeePlayer, playerServer.getInfo().getName());
                bungeePlayer.connect(PluginConfig.get().getString("afk-server-name"));
                bungeePlayer.sendMessage(Caption.of("notification.afk_disconnect"));
                LOGGER.info("Moved {} to AFK server.", bungeePlayer.getName());
            }
            case KICK -> {
                bungeePlayer.kick(Caption.of("notification.afk_kick"));
                LOGGER.info("Kicked {} for being AFK.", bungeePlayer.getName());
            }
        }
        bungeePlayer.setAfkState(AFKState.ACTION_TAKEN);
    }

    @EventHandler
    public void onPostLogin(@NotNull PostLoginEvent event) {
        BungeePlayer bungeePlayer = BungeePlayer.adapt(event.getPlayer());
        bungeePlayer.setTimeSinceLastAction(0);
        bungeePlayer.setAfkState(AFKState.ACTIVE);
    }

    @EventHandler
    public void onPluginMessage(@NotNull PluginMessageEvent event) {
        if (!event.getTag().equals("bungee:bungeeafk")) return;
        String data = new String(event.getData());
        String[] parts = data.split(";");

        if (parts.length != 2) return;

        UUID playerUUID = UUID.fromString(parts[0]);
        String status = parts[1];

        if (status.equals("action_caught")) {
            BungeePlayer bungeePlayer = BungeePlayer.adapt(playerUUID).orElse(null);
            if (bungeePlayer == null) return;

            if (bungeePlayer.getAfkState() == AFKState.ACTION_TAKEN || bungeePlayer.getAfkState() == AFKState.AFK) {
                bungeePlayer.sendMessage(Caption.of("notification.afk_return"));
            }
            bungeePlayer.setTimeSinceLastAction(0);
            bungeePlayer.setAfkState(AFKState.ACTIVE);
        }
    }
}
