package net.fameless.bungee;

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
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class BungeeAFKHandler implements AFKHandler, Listener {

    private static final Logger LOGGER = LoggerFactory.getLogger("BungeeAFK/" + BungeeAFKHandler.class.getSimpleName());
    private final Map<BAFKPlayer<?>, String> playerLastServerMap = new HashMap<>();

    private Action action;
    private long warnDelay;
    private long actionDelay;
    private long afkDelay;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> scheduledTask;

    @Override
    public void init() {
        updateConfigValues();

        // try-catch block to handle exceptions that would otherwise silently cancel the task
        scheduledTask = scheduler.scheduleAtFixedRate(() -> {
            try {
                for (BAFKPlayer<?> player : BungeePlayer.getOnlinePlayers()) {
                    if (!(player instanceof BungeePlayer bungeePlayer)) continue;

                    updateTimeSinceLastAction(bungeePlayer);
                    handleWarning(bungeePlayer);
                    handleAction(bungeePlayer);
                    handleAfkStatus(bungeePlayer);
                    sendActionBar(bungeePlayer);
                }
            } catch (Exception e) {
                LOGGER.error("Error in AFK check task: ", e);
                scheduledTask.cancel(false);
            }
        }, 0, 500, TimeUnit.MILLISECONDS);

        BungeePlatform.proxyServer().registerChannel("bungee:bungeeafk");
        BungeePlatform.proxyServer().getPluginManager().registerListener(BungeePlatform.get(), this);
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

    private void updateTimeSinceLastAction(@NotNull BungeePlayer player) {
        long timeSinceLastAction = player.getTimeSinceLastAction();
        if (timeSinceLastAction < 0) {
            timeSinceLastAction = 0;
        }
        player.setTimeSinceLastAction(timeSinceLastAction + 500);
    }

    private void handleWarning(@NotNull BungeePlayer bungeePlayer) {
        if (bungeePlayer.getAfkState() != AFKState.ACTIVE) return;
        long timeSinceLastAction = bungeePlayer.getTimeSinceLastAction();
        if (timeSinceLastAction >= warnDelay) {
            bungeePlayer.sendMessage(Caption.of("notification.afk_warning"));
            bungeePlayer.setAfkState(AFKState.WARNED);
        }
    }

    private void handleAfkStatus(@NotNull BungeePlayer bungeePlayer) {
        if (bungeePlayer.getAfkState() != AFKState.WARNED) return;
        long timeSinceLastAction = bungeePlayer.getTimeSinceLastAction();

        if (timeSinceLastAction >= afkDelay) {
            bungeePlayer.setAfkState(AFKState.AFK);

            long timeUntilAction = Math.max(0, actionDelay - afkDelay);
            bungeePlayer.sendMessage(Caption.of(
                    action.getMessageKey(),
                    TagResolver.resolver("action-delay", Tag.inserting(Component.text(Format.formatTime((int) (timeUntilAction / 1000)))))
            ));
            LOGGER.info("Player {} is now AFK.", bungeePlayer.getName());
        }
    }

    private void handleAction(@NotNull BungeePlayer bungeePlayer) {
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
                    platformPlayer.disconnect(BungeeComponentSerializer.get().serialize(Caption.of("notification.afk_kick")));
                    LOGGER.info("Kicked {} for being AFK.", bungeePlayer.getName());
                    return;
                }

                playerLastServerMap.put(bungeePlayer, playerServer.getInfo().getName());
                bungeePlayer.connect(PluginConfig.get().getString("afk-server-name"));
                bungeePlayer.sendMessage(Caption.of("notification.afk_disconnect"));
                LOGGER.info("Moved {} to AFK server.", bungeePlayer.getName());
            }
            case KICK -> {
                platformPlayer.disconnect(BungeeComponentSerializer.get().serialize(Caption.of("notification.afk_kick")));
                LOGGER.info("Kicked {} for being AFK.", bungeePlayer.getName());
            }
        }
        bungeePlayer.setAfkState(AFKState.ACTION_TAKEN);
    }

    private void sendActionBar(@NotNull BungeePlayer bungeePlayer) {
        if (bungeePlayer.getAfkState() == AFKState.AFK || bungeePlayer.getAfkState() == AFKState.ACTION_TAKEN) {
            bungeePlayer.sendActionbar(Caption.of("actionbar.afk"));
        }
    }

    private boolean checkServerAvailable(String serverName) {
        return BungeePlatform.proxyServer().getServerInfo(serverName) != null;
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
