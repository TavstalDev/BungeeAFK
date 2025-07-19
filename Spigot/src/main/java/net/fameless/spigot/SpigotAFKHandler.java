package net.fameless.spigot;

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
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class SpigotAFKHandler implements AFKHandler, Listener {

    private static final Logger LOGGER = LoggerFactory.getLogger("BungeeAFK/" + SpigotAFKHandler.class.getSimpleName());

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
        scheduledTask = scheduler.scheduleAtFixedRate(() -> {
            try {
                for (BAFKPlayer<?> player : SpigotPlayer.getOnlinePlayers()) {
                    if (!(player instanceof SpigotPlayer spigotPlayer)) continue;

                    updateTimeSinceLastAction(spigotPlayer);
                    handleWarning(spigotPlayer);
                    handleAfkStatus(spigotPlayer);
                    handleAction(spigotPlayer);
                    sendActionBar(spigotPlayer);
                }
            } catch (Exception e) {
                LOGGER.error("Error in AFK check task: ", e);
                scheduledTask.cancel(false);
            }
        }, 0, 500, TimeUnit.MILLISECONDS);
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
            this.action = Action.KICK;
            LOGGER.warn("CONNECT action is not supported in Spigot. Defaulting to KICK.");
        }
    }

    private void updateTimeSinceLastAction(@NotNull SpigotPlayer spigotPlayer) {
        long timeSinceLastAction = spigotPlayer.getTimeSinceLastAction();
        if (timeSinceLastAction < 0) {
            timeSinceLastAction = 0;
        }
        spigotPlayer.setTimeSinceLastAction(timeSinceLastAction + 500);
    }

    private void handleWarning(@NotNull SpigotPlayer spigotPlayer) {
        if (spigotPlayer.getAfkState() != AFKState.ACTIVE) return;

        long timeSinceLastAction = spigotPlayer.getTimeSinceLastAction();
        if (timeSinceLastAction >= warnDelay) {
            spigotPlayer.sendMessage(Caption.of("notification.afk_warning"));
            spigotPlayer.setAfkState(AFKState.WARNED);
        }
    }

    private void handleAfkStatus(@NotNull SpigotPlayer spigotPlayer) {
        if (spigotPlayer.getAfkState() != AFKState.WARNED) return;
        long timeSinceLastAction = spigotPlayer.getTimeSinceLastAction();

        if (timeSinceLastAction >= afkDelay) {
            spigotPlayer.setAfkState(AFKState.AFK);
            long timeUntilAction = Math.max(0, actionDelay - afkDelay);
            spigotPlayer.sendMessage(Caption.of(
                    action.getMessageKey(),
                    TagResolver.resolver("action-delay", Tag.inserting(Component.text(Format.formatTime((int) (timeUntilAction / 1000)))))
            ));
            LOGGER.info("{} is now AFK.", spigotPlayer.getName());
        }
    }

    private void handleAction(@NotNull SpigotPlayer spigotPlayer) {
        if (spigotPlayer.getAfkState() != AFKState.AFK) return;
        if (!action.equals(Action.KICK)) return;

        long timeSinceLastAction = spigotPlayer.getTimeSinceLastAction();
        if (timeSinceLastAction < actionDelay) return;

        Bukkit.getScheduler().runTask(SpigotPlatform.get(), () -> spigotPlayer.kick(Caption.of("notification.afk_kick")));
        LOGGER.info("Kicked {} for being AFK.", spigotPlayer.getName());
    }

    private void sendActionBar(@NotNull SpigotPlayer spigotPlayer) {
        if (spigotPlayer.getAfkState() == AFKState.AFK || spigotPlayer.getAfkState() == AFKState.ACTION_TAKEN) {
            spigotPlayer.sendActionbar(Caption.of("actionbar.afk"));
        }
    }

    private void actionCaught(@NotNull SpigotPlayer spigotPlayer) {
        if (spigotPlayer.getAfkState() == AFKState.ACTION_TAKEN || spigotPlayer.getAfkState() == AFKState.AFK) {
            spigotPlayer.sendMessage(Caption.of("notification.afk_return"));
        }

        spigotPlayer.setTimeSinceLastAction(0);
        spigotPlayer.setAfkState(AFKState.ACTIVE);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        actionCaught(SpigotPlayer.adapt(event.getPlayer()));
    }

    @EventHandler
    public void onMove(@NotNull PlayerMoveEvent event) {
        if (!event.getFrom().equals(event.getTo())) {
            actionCaught(SpigotPlayer.adapt(event.getPlayer()));
        }
    }

    @EventHandler
    public void onChat(@NotNull AsyncPlayerChatEvent event) {
        actionCaught(SpigotPlayer.adapt(event.getPlayer()));
    }

    @EventHandler
    public void onInteract(@NotNull PlayerInteractEvent event) {
        actionCaught(SpigotPlayer.adapt(event.getPlayer()));
    }
}
