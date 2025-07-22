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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class VelocityAFKHandler extends AFKHandler {

    private final Map<BAFKPlayer<?>, String> playerLastServerMap = new HashMap<>();

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> scheduledTask;

    @Override
    public void init() {
        updateConfigValues();

        // try-catch block to handle exceptions that would otherwise silently cancel the task
        this.scheduledTask = scheduler.scheduleAtFixedRate(() -> {
            try {
                List<BAFKPlayer<?>> players = new ArrayList<>(VelocityPlayer.getOnlinePlayers());

                for (BAFKPlayer<?> player : players) {
                    if (!(player instanceof VelocityPlayer velocityPlayer)) continue;

                    updateTimeSinceLastAction(velocityPlayer);
                    handleWarning(velocityPlayer);
                    handleAction(velocityPlayer);
                    handleAfkStatus(velocityPlayer);
                    sendActionBar(velocityPlayer);
                }
            } catch (Exception e) {
                LOGGER.error("Error during AFK check task", e);
                scheduledTask.cancel(false);
            }
        }, 0, 500, TimeUnit.MILLISECONDS);

        VelocityPlatform.getProxy().getChannelRegistrar().register(MinecraftChannelIdentifier.create("bungee", "bungeeafk"));
        VelocityPlatform.getProxy().getEventManager().register(VelocityPlatform.get(), this);
    }

    @Override
    public void shutdown() {
        if (scheduledTask != null && !scheduledTask.isCancelled()) {
            scheduledTask.cancel(true);
        }
        scheduler.shutdownNow();
        LOGGER.info("AFK handler successfully shutdown.");
    }

    @Override
    protected void handleAction(@NotNull BAFKPlayer<?> bafkPlayer) {
        if (!(bafkPlayer instanceof VelocityPlayer velocityPlayer)) return;

        long timeSinceLastAction = velocityPlayer.getTimeSinceLastAction();

        Optional<Player> platformPlayerOptional = velocityPlayer.getPlatformPlayer();
        if (platformPlayerOptional.isEmpty()) return;
        Player platformPlayer = platformPlayerOptional.get();

        Optional<ServerConnection> playerServerOptional = platformPlayer.getCurrentServer();
        if (playerServerOptional.isEmpty()) return;
        ServerConnection playerServer = playerServerOptional.get();

        if (velocityPlayer.getAfkState() != AFKState.ACTION_TAKEN && playerServer.getServerInfo().getName().equalsIgnoreCase(PluginConfig.get().getString("afk-server-name", ""))) {
            velocityPlayer.connect(playerLastServerMap.getOrDefault(velocityPlayer, "lobby"));
            return;
        }

        if (action.equals(Action.NOTHING)) return;
        if (velocityPlayer.getAfkState() != AFKState.AFK) return;
        if (timeSinceLastAction < actionDelay) return;

        switch (action) {
            case CONNECT -> {
                if (!Action.isAfkServerConfigured()) {
                    LOGGER.warn("AFK server not found. Defaulting to KICK.");

                    this.action = Action.KICK;
                    platformPlayer.disconnect(Caption.of("notification.afk_kick"));
                    LOGGER.info("Kicked {} for being AFK.", velocityPlayer.getName());
                    return;
                }

                playerLastServerMap.put(velocityPlayer, playerServer.getServerInfo().getName());
                velocityPlayer.connect(PluginConfig.get().getString("afk-server-name"));
                velocityPlayer.sendMessage(Caption.of("notification.afk_disconnect"));
                LOGGER.info("Moved {} to AFK server.", velocityPlayer.getName());
            }
            case KICK -> {
                platformPlayer.disconnect(Caption.of("notification.afk_kick"));
                LOGGER.info("Kicked {} for being AFK.", velocityPlayer.getName());
            }
        }
        velocityPlayer.setAfkState(AFKState.ACTION_TAKEN);
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

            if (velocityPlayer.getAfkState() == AFKState.ACTION_TAKEN || velocityPlayer.getAfkState() == AFKState.AFK) {
                velocityPlayer.sendMessage(Caption.of("notification.afk_return"));
            }
            velocityPlayer.setTimeSinceLastAction(0);
            velocityPlayer.setAfkState(AFKState.ACTIVE);
        }
    }
}
