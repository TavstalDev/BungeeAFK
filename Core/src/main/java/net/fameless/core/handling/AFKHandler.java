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

import java.util.ArrayList;
import java.util.List;
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

    protected final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> scheduledTask;

    public AFKHandler() {
        this.scheduledTask = scheduler.scheduleAtFixedRate(() -> {
            try {
                List<BAFKPlayer<?>> players = new ArrayList<>(BAFKPlayer.getOnlinePlayers());
                for (BAFKPlayer<?> player : players) {
                    updateTimeSinceLastAction(player);
                    handleWarning(player);
                    handleAction(player);
                    handleAfkStatus(player);
                    sendActionBar(player);
                }
            } catch (Exception e) {
                LOGGER.error("Error during AFK check task", e);
                scheduledTask.cancel(false);
            }
        }, 0, 500, TimeUnit.MILLISECONDS);
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
        long timeSinceLastAction = player.getTimeSinceLastAction();
        if (timeSinceLastAction < 0) {
            timeSinceLastAction = 0;
        }
        player.setTimeSinceLastAction(timeSinceLastAction + 500);
    }

    protected void handleWarning(@NotNull BAFKPlayer<?> player) {
        if (player.getAfkState() != AFKState.ACTIVE) return;
        long timeSinceLastAction = player.getTimeSinceLastAction();
        if (timeSinceLastAction >= warnDelay) {
            player.sendMessage(Caption.of("notification.afk_warning"));
            player.setAfkState(AFKState.WARNED);
        }
    }

    protected void handleAfkStatus(@NotNull BAFKPlayer<?> player) {
        if (player.getAfkState() != AFKState.WARNED) return;
        long timeSinceLastAction = player.getTimeSinceLastAction();

        if (timeSinceLastAction >= afkDelay) {
            player.setAfkState(AFKState.AFK);
            long timeUntilAction = Math.max(0, actionDelay - afkDelay);
            player.sendMessage(Caption.of(
                    action.getMessageKey(),
                    TagResolver.resolver("action-delay", Tag.inserting(Component.text(Format.formatTime((int) (timeUntilAction / 1000)))))
            ));
            LOGGER.info("{} is now AFK.", player.getName());
        }
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

    public abstract void init();

    public abstract void shutdown();
}
