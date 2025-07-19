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
import net.fameless.core.util.Format;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class VelocityAFKHandler implements AFKHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger("BungeeAFK/" + VelocityAFKHandler.class.getSimpleName());
    private final Map<BAFKPlayer<?>, String> playerLastServerMap = new HashMap<>();

    private Action action;
    private long warnDelay;
    private long afkDelay;
    private long actionDelay;

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
    public void setAction(Action action) {
        if (!action.isAvailable()) return;
        this.action = action;
        PluginConfig.get().set("action", action.getIdentifier());
    }

    @Override
    public void setWarnDelayMillis(long delay) {
        this.warnDelay = delay;
        PluginConfig.get().set("warning-delay", (int) (delay / 1000));
    }

    @Override
    public void setActionDelayMillis(long delay) {
        this.actionDelay = delay;
        PluginConfig.get().set("action-delay", (int) (delay / 1000));
    }

    @Override
    public void setAfkDelayMillis(long delay) {
        this.afkDelay = delay;
        PluginConfig.get().set("afk-delay", (int) (delay / 1000));
    }

    @Override
    public long getWarnDelayMillis() {
        return warnDelay;
    }

    @Override
    public long getAfkDelayMillis() {
        return afkDelay;
    }

    @Override
    public long getActionDelayMillis() {
        return actionDelay;
    }

    @Override
    public void updateConfigValues() {
        this.warnDelay = PluginConfig.get().getInt("warning-delay", 300) * 1000L;
        this.afkDelay = PluginConfig.get().getInt("afk-delay", 600) * 1000L;
        this.actionDelay = PluginConfig.get().getInt("action-delay", 630) * 1000L;

        try {
            this.action = Action.fromIdentifier(PluginConfig.get().getString("action", ""));
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Invalid action identifier in config. Defaulting to KICK.");
            this.action = Action.KICK;
        }

        if (action.equals(Action.CONNECT)) {
            String serverName = PluginConfig.get().getString("afk-server-name", "");

            if (!checkServerAvailable(serverName)) {
                LOGGER.warn("AFK server not found. Defaulting to KICK.");
                this.action = Action.KICK;
            }
        }
    }

    private void updateTimeSinceLastAction(@NotNull VelocityPlayer player) {
        long timeSinceLastAction = player.getTimeSinceLastAction();
        if (timeSinceLastAction < 0) {
            timeSinceLastAction = 0;
        }
        player.setTimeSinceLastAction(timeSinceLastAction + 500);
    }

    private void handleWarning(@NotNull VelocityPlayer velocityPlayer) {
        if (velocityPlayer.getAfkState() != AFKState.ACTIVE) return;
        long timeSinceLastAction = velocityPlayer.getTimeSinceLastAction();
        if (timeSinceLastAction >= warnDelay) {
            velocityPlayer.sendMessage(Caption.of("notification.afk_warning"));
            velocityPlayer.setAfkState(AFKState.WARNED);
        }
    }

    private void handleAfkStatus(@NotNull VelocityPlayer velocityPlayer) {
        if (velocityPlayer.getAfkState() != AFKState.WARNED) return;
        long timeSinceLastAction = velocityPlayer.getTimeSinceLastAction();

        if (timeSinceLastAction >= afkDelay) {
            velocityPlayer.setAfkState(AFKState.AFK);

            long timeUntilAction = Math.max(0, actionDelay - afkDelay);
            velocityPlayer.sendMessage(Caption.of(
                    action.getMessageKey(),
                    TagResolver.resolver("action-delay", Tag.inserting(Component.text(Format.formatTime((int) (timeUntilAction / 1000)))))
            ));
            LOGGER.info("Player {} is now AFK.", velocityPlayer.getName());
        }
    }

    private void handleAction(@NotNull VelocityPlayer velocityPlayer) {
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

    private void sendActionBar(@NotNull VelocityPlayer velocityPlayer) {
        if (velocityPlayer.getAfkState() == AFKState.AFK || velocityPlayer.getAfkState() == AFKState.ACTION_TAKEN) {
            velocityPlayer.sendActionbar(Caption.of("actionbar.afk"));
        }
    }

    private boolean checkServerAvailable(String serverName) {
        return VelocityPlatform.getProxy().getServer(serverName).isPresent();
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
