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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public abstract class AFKHandler {

    protected static final Logger LOGGER = LoggerFactory.getLogger("BungeeAFK/" + AFKHandler.class.getSimpleName());

    protected Action action;
    protected long warnDelay;
    protected long afkDelay;
    protected long actionDelay;

    protected static final ScheduledExecutorService SCHEDULER =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "AFKHandler-Scheduler");
                t.setDaemon(true);
                return t;
            });
    private ScheduledFuture<?> scheduledTask;

    private final long UPDATE_PERIOD_MILLIS = 500L;

    public AFKHandler() {
        if (BungeeAFK.getAFKHandler() != null) throw new IllegalStateException("AFKHandler is already initialized.");
        updateConfigValues();

        // try-catch block to handle exceptions that would otherwise silently halt the task
        this.scheduledTask = SCHEDULER.scheduleAtFixedRate(() -> {
            try {
                for (BAFKPlayer<?> player : BAFKPlayer.PLAYERS) {
                    if (player.isOffline()) continue;
                    updateTimeSinceLastAction(player);
                    handleWarning(player);
                    handleAction(player);
                    handleAfkStatus(player);
                    sendActionBar(player);
                    updatePlayerStatus(player);
                }
            } catch (Exception e) {
                LOGGER.error("Error during AFK check task", e);
                scheduledTask.cancel(false);
            }
        }, 0, UPDATE_PERIOD_MILLIS, TimeUnit.MILLISECONDS);

        init();
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

    protected void updatePlayerStatus(@NotNull BAFKPlayer<?> player) {
        if (player.getAfkState() == AFKState.BYPASS) {
            player.setTimeSinceLastAction(0);
            return;
        }

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

            if (!BungeeAFK.platform().doesServerExist(serverName)) {
                LOGGER.warn("AFK server not found. Defaulting to KICK.");
                this.action = Action.KICK;
            }
        }
    }

    protected void updateTimeSinceLastAction(@NotNull BAFKPlayer<?> player) {
        player.setTimeSinceLastAction(player.getTimeSinceLastAction() + UPDATE_PERIOD_MILLIS);
    }

    protected void handleWarning(@NotNull BAFKPlayer<?> player) {
        if (player.getAfkState() != AFKState.ACTIVE) return;
        if (player.getTimeSinceLastAction() < warnDelay) return;
        player.sendMessage(Caption.of("notification.afk_warning"));
    }

    protected void handleAfkStatus(@NotNull BAFKPlayer<?> player) {
        if (player.getAfkState() != AFKState.WARNED) return;
        if (player.getTimeSinceLastAction() < afkDelay) return;

        long timeUntilAction = Math.max(0, actionDelay - afkDelay);
        player.sendMessage(Caption.of(
                action.getMessageKey(),
                TagResolver.resolver("action-delay", Tag.inserting(Component.text(Format.formatTime((int) (timeUntilAction / 1000)))))
        ));
        LOGGER.info("{} is now AFK.", player.getName());
    }

    protected void sendActionBar(@NotNull BAFKPlayer<?> player) {
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

    protected abstract void handleAction(@NotNull BAFKPlayer<?> player);

    protected abstract void init();
}
