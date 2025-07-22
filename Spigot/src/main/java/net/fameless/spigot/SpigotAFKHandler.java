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

public class SpigotAFKHandler extends AFKHandler implements Listener {

    @Override
    public void init() {
        Bukkit.getPluginManager().registerEvents(this, SpigotPlatform.get());
    }

    @Override
    protected void handleAction(@NotNull BAFKPlayer<?> bafkPlayer) {
        if (!action.equals(Action.KICK)) return;
        if (!(bafkPlayer instanceof SpigotPlayer spigotPlayer)) return;
        if (spigotPlayer.getAfkState() != AFKState.AFK) return;
        if (spigotPlayer.getTimeSinceLastAction() < actionDelay) return;

        spigotPlayer.kick(Caption.of("notification.afk_kick"));
        LOGGER.info("Kicked {} for being AFK.", spigotPlayer.getName());
    }

    private void actionCaught(@NotNull SpigotPlayer spigotPlayer) {
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
