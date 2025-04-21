package net.fameless.spigot;

import net.fameless.core.BungeeAFK;
import net.fameless.core.caption.Caption;
import net.fameless.core.handling.AFKHandler;
import net.fameless.core.handling.Action;
import net.fameless.core.player.BAFKPlayer;
import net.fameless.core.util.Format;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SpigotAFKHandler implements AFKHandler, Listener {

    private final long warningTime = 60 * 1000L;
    private final List<BAFKPlayer<?>> WARNED = new ArrayList<>();
    private final Map<BAFKPlayer<?>, Long> playerAfkTimeMap = new HashMap<>();

    private Action action;
    private long actionDelay;
    private long afkDelay;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Override
    public void init() {
        this.actionDelay = BungeeAFK.getConfig().getInt("action-delay", 30) * 1000L;
        this.afkDelay = BungeeAFK.getConfig().getInt("afk-delay", 600) * 1000L;

        try {
            this.action = Action.fromIdentifier(BungeeAFK.getConfig().getString("action", "kick"));
        } catch (IllegalArgumentException e) {
            BungeeAFK.getLogger().warning("Invalid action identifier in config. Defaulting to KICK.");
            this.action = Action.KICK;
        }

        scheduler.scheduleAtFixedRate(() -> {
            for (BAFKPlayer<?> player : SpigotPlayer.getOnlinePlayers()) {
                if (!(player instanceof SpigotPlayer spigotPlayer)) continue;
                long timeUntilAfk = calculateTimeUntilAfk(spigotPlayer);

                handleWarning(spigotPlayer, timeUntilAfk);
                handleAfkStatus(spigotPlayer, timeUntilAfk);
                handleAction(spigotPlayer);
            }
        }, 0, 500, TimeUnit.MILLISECONDS);
    }

    @Override
    public void setAction(Action action) {
        if (!action.isAvailable()) return;
        this.action = action;
    }

    @Override
    public void setActionDelayMillis(long delay) {
        this.actionDelay = delay;
    }

    @Override
    public void setAfkDelayMillis(long delay) {
        this.afkDelay = delay;
    }

    @Override
    public long getAfkDelayMillis() {
        return afkDelay;
    }

    private long calculateTimeUntilAfk(@NotNull SpigotPlayer spigotPlayer) {
        return spigotPlayer.getWhenToAfk() - System.currentTimeMillis();
    }

    private void handleWarning(SpigotPlayer spigotPlayer, long timeUntilAfk) {
        if (timeUntilAfk <= warningTime && timeUntilAfk > 0 && !WARNED.contains(spigotPlayer)) {
            spigotPlayer.sendMessage(Caption.of("notification.afk_warning"));
            WARNED.add(spigotPlayer);
        }
    }

    private void handleAfkStatus(@NotNull SpigotPlayer spigotPlayer, long timeUntilAfk) {
        boolean newAfk = timeUntilAfk <= 0;

        if (spigotPlayer.isAfk() && !newAfk) {
            playerAfkTimeMap.remove(spigotPlayer);
            spigotPlayer.sendMessage(Caption.of("notification.afk_return"));
        } else if (!spigotPlayer.isAfk() && newAfk) {
            playerAfkTimeMap.put(spigotPlayer, System.currentTimeMillis());
            spigotPlayer.sendMessage(Caption.of(
                    action.getMessageKey(),
                    TagResolver.resolver("action-delay", Tag.inserting(Component.text(Format.formatTime((int) (actionDelay / 1000)))))));
        }

        spigotPlayer.setAfk(newAfk);
        if (newAfk) {
            WARNED.remove(spigotPlayer);
        }
    }

    private void handleAction(@NotNull SpigotPlayer spigotPlayer) {
        if (!action.equals(Action.KICK)) return;
        if (!spigotPlayer.isAfk()) return;

        long timeSinceAfk = System.currentTimeMillis() - playerAfkTimeMap.get(spigotPlayer);
        if (timeSinceAfk > actionDelay) {
            spigotPlayer.kick(Caption.of("notification.afk_kick"));
            BungeeAFK.getLogger().info("Kicked " + spigotPlayer.getName() + " for being AFK.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        SpigotPlayer.adapt(event.getPlayer());
    }

    @EventHandler
    public void onMove(@NotNull PlayerMoveEvent event) {
        if (!event.getFrom().equals(event.getTo())) {
            SpigotPlayer.adapt(event.getPlayer()).updateWhenToAfk();
        }
    }

    @EventHandler
    public void onChat(@NotNull AsyncPlayerChatEvent event) {
        SpigotPlayer.adapt(event.getPlayer()).updateWhenToAfk();
    }

    @EventHandler
    public void onInteract(@NotNull PlayerInteractEvent event) {
        SpigotPlayer.adapt(event.getPlayer()).updateWhenToAfk();
    }

}
