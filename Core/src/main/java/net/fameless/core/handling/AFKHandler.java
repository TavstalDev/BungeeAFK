package net.fameless.core.handling;

import net.fameless.core.BungeeAFK;
import net.fameless.core.caption.Caption;
import net.fameless.core.config.PluginConfig;
import net.fameless.core.player.BAFKPlayer;
import net.fameless.core.util.Format;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.*;

public abstract class AFKHandler {

    protected static final Logger LOGGER = LoggerFactory.getLogger("BungeeAFK/" + AFKHandler.class.getSimpleName());
    private static final ScheduledExecutorService SCHEDULER =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "AFKHandler-Scheduler");
                t.setDaemon(true);
                return t;
            });

    private final Map<BAFKPlayer<?>, String> playerLastServerMap = new ConcurrentHashMap<>();
    private final long UPDATE_PERIOD_MILLIS = 500L;

    private Action action;
    private long warnDelay;
    private long afkDelay;
    private long actionDelay;
    private final ScheduledFuture<?> scheduledTask;

    public AFKHandler() {
        if (BungeeAFK.getAFKHandler() != null) throw new IllegalStateException("AFKHandler is already initialized.");
        fetchConfigValues();

        // try-catch block to handle exceptions that would otherwise silently halt the task
        this.scheduledTask = SCHEDULER.scheduleAtFixedRate(this::checkAFKPlayers, 0, UPDATE_PERIOD_MILLIS, TimeUnit.MILLISECONDS);
        init();
    }

    private void checkAFKPlayers() {
        try {
            for (BAFKPlayer<?> player : BAFKPlayer.PLAYERS) {
                if (player.isOffline()) continue;
                AFKState afkState = player.getAfkState();
                if (afkState == AFKState.BYPASS) continue;
                player.increaseTimeSinceLastAction(UPDATE_PERIOD_MILLIS);
                switch (afkState) {
                    case ACTIVE -> handleWarning(player);
                    case WARNED -> handleAfkStatus(player);
                }

                handleAction(player);
                updatePlayerStatus(player);
                sendActionBar(player);
            }
        } catch (Exception e) {
            LOGGER.error("Error during AFK check task", e);
            scheduledTask.cancel(false);
        }
    }

    public void fetchConfigValues() {
        this.warnDelay = PluginConfig.get().getInt("warning-delay", 300) * 1000L;
        this.afkDelay = PluginConfig.get().getInt("afk-delay", 600) * 1000L;
        this.actionDelay = PluginConfig.get().getInt("action-delay", 630) * 1000L;

        try {
            this.action = Action.fromIdentifier(PluginConfig.get().getString("action", ""));
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Invalid action identifier in config. Defaulting to KICK.");
            this.action = Action.KICK;
        }

        if (action == Action.CONNECT) {
            String serverName = PluginConfig.get().getString("afk-server-name", "");

            if (!BungeeAFK.platform().doesServerExist(serverName)) {
                LOGGER.warn("AFK server not found. Defaulting to KICK.");
                this.action = Action.KICK;
            }
        }
    }

    public void shutdown() {
        if (scheduledTask != null && !scheduledTask.isCancelled()) {
            scheduledTask.cancel(true);
        }
        SCHEDULER.shutdownNow();
        LOGGER.info("AFK handler successfully shutdown.");
    }

    public void setAction(@NotNull Action action) {
        if (!action.isAvailable()) return;
        this.action = action;
        PluginConfig.get().set("action", action.getIdentifier());
    }

    public void setWarnDelayMillis(long delay) {
        this.warnDelay = delay;
        PluginConfig.get().set("warning-delay", (int) (delay / 1000));
    }

    public void setActionDelayMillis(long delay) {
        this.actionDelay = delay;
        PluginConfig.get().set("action-delay", (int) (delay / 1000));
    }

    public void setAfkDelayMillis(long delay) {
        this.afkDelay = delay;
        PluginConfig.get().set("afk-delay", (int) (delay / 1000));
    }

    private void handleWarning(@NotNull BAFKPlayer<?> player) {
        if (player.getTimeSinceLastAction() >= warnDelay) {
            player.sendMessage(Caption.of("notification.afk_warning"));
        }
    }

    private void handleAfkStatus(@NotNull BAFKPlayer<?> player) {
        if (player.getTimeSinceLastAction() >= afkDelay) {
            long timeUntilAction = Math.max(0, actionDelay - afkDelay);
            player.sendMessage(Caption.of(
                    action.getMessageKey(),
                    TagResolver.resolver("action-delay", Tag.inserting(Component.text(Format.formatTime((int) (timeUntilAction / 1000)))))
            ));
            BungeeAFK.platform().broadcast(Caption.of("notification.afk_broadcast",
                    TagResolver.resolver("player", Tag.inserting(Component.text(player.getName())))));
            LOGGER.info("{} is now AFK.", player.getName());
        }
    }

    private void handleAction(@NotNull BAFKPlayer<?> player) {
        if (player.getAfkState() == AFKState.ACTION_TAKEN) return;

        String afkServerName = PluginConfig.get().getString("afk-server-name", "");
        if (player.getCurrentServerName().equalsIgnoreCase(afkServerName)) {
            player.connect(playerLastServerMap.getOrDefault(player, "lobby"));
        }

        if (action == Action.NOTHING) return;
        if (player.getAfkState() != AFKState.AFK) return;
        if (player.getTimeSinceLastAction() < actionDelay) return;

        switch (action) {
            case CONNECT -> {
                if (!Action.isAfkServerConfigured()) {
                    LOGGER.warn("AFK server not found. Defaulting to KICK.");
                    this.action = Action.KICK;
                    player.kick(Caption.of("notification.afk_kick"));
                    BungeeAFK.platform().broadcast(Caption.of("notification.afk_kick_broadcast",
                            TagResolver.resolver("player", Tag.inserting(Component.text(player.getName())))));
                    LOGGER.info("Kicked {} for being AFK.", player.getName());
                    return;
                }
                String currentServerName = player.getCurrentServerName();
                playerLastServerMap.put(player, currentServerName);
                player.connect(afkServerName);
                player.sendMessage(Caption.of("notification.afk_disconnect"));
                BungeeAFK.platform().broadcast(Caption.of("notification.afk_disconnect_broadcast",
                        TagResolver.resolver("player", Tag.inserting(Component.text(player.getName())))));
                LOGGER.info("Moved {} to AFK server.", player.getName());
            }
            case KICK -> {
                player.kick(Caption.of("notification.afk_kick"));
                BungeeAFK.platform().broadcast(Caption.of("notification.afk_kick_broadcast",
                        TagResolver.resolver("player", Tag.inserting(Component.text(player.getName())))));
                LOGGER.info("Kicked {} for being AFK.", player.getName());
            }
        }
    }

    private void updatePlayerStatus(@NotNull BAFKPlayer<?> player) {
        if (player.getAfkState() == AFKState.BYPASS) return;
        long timeSinceLastAction = player.getTimeSinceLastAction();

        if (timeSinceLastAction < warnDelay) {
            player.setAfkState(AFKState.ACTIVE);
        } else if (timeSinceLastAction < afkDelay) {
            player.setAfkState(AFKState.WARNED);
        } else if (timeSinceLastAction < actionDelay) {
            player.setAfkState(AFKState.AFK);
        } else {
            player.setAfkState(AFKState.ACTION_TAKEN);
        }
    }

    private void sendActionBar(@NotNull BAFKPlayer<?> player) {
        if (player.getAfkState() == AFKState.AFK || player.getAfkState() == AFKState.ACTION_TAKEN) {
            player.sendActionbar(Caption.of("actionbar.afk"));
        }
    }

    public long getWarnDelayMillis() {
        return warnDelay;
    }

    public long getAfkDelayMillis() {
        return afkDelay;
    }

    public long getActionDelayMillis() {
        return actionDelay;
    }

    protected abstract void init();
}
