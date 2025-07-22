package net.fameless.spigot;

import net.fameless.core.caption.Caption;
import net.fameless.core.handling.AFKHandler;
import net.fameless.core.handling.AFKState;
import net.fameless.core.handling.Action;
import net.fameless.core.player.BAFKPlayer;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class SpigotAFKHandler extends AFKHandler implements Listener {

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
    protected void handleAction(@NotNull BAFKPlayer<?> bafkPlayer) {
        if (!(bafkPlayer instanceof SpigotPlayer spigotPlayer)) return;

        if (spigotPlayer.getAfkState() != AFKState.AFK) return;
        if (!action.equals(Action.KICK)) return;

        long timeSinceLastAction = spigotPlayer.getTimeSinceLastAction();
        if (timeSinceLastAction < actionDelay) return;

        Bukkit.getScheduler().runTask(SpigotPlatform.get(), () -> spigotPlayer.kick(Caption.of("notification.afk_kick")));
        LOGGER.info("Kicked {} for being AFK.", spigotPlayer.getName());
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
