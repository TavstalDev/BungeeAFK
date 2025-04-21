package net.fameless.velocity;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import net.fameless.core.BungeeAFK;
import net.fameless.core.caption.Caption;
import net.fameless.core.handling.AFKHandler;
import net.fameless.core.handling.Action;
import net.fameless.core.player.BAFKPlayer;
import net.fameless.core.util.Format;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class VelocityAFKHandler implements AFKHandler {

    private final long warningTime = 60 * 1000L;
    private final List<BAFKPlayer<?>> WARNED = new ArrayList<>();
    private final Map<BAFKPlayer<?>, Long> playerAfkTimeMap = new HashMap<>();
    private final Map<BAFKPlayer<?>, String> playerLastServerMap = new HashMap<>();

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

        if (action.equals(Action.CONNECT)) {
            if (!BungeeAFK.getConfig().contains("afk-server-name")) {
                BungeeAFK.getLogger().warning("AFK server not found. Defaulting to KICK.");
                this.action = Action.KICK;
            }

            String serverName = BungeeAFK.getConfig().getString("afk-server-name");
            if (!checkServerAvailable(serverName)) {
                BungeeAFK.getLogger().warning("AFK server not found. Defaulting to KICK.");
                this.action = Action.KICK;
            }
        }

        scheduler.scheduleAtFixedRate(() -> {
            for (BAFKPlayer<?> player : VelocityPlayer.getOnlinePlayers()) {
                if (!(player instanceof VelocityPlayer velocityPlayer)) continue;
                long timeUntilAfk = calculateTimeUntilAfk(velocityPlayer);

                handleWarning(velocityPlayer, timeUntilAfk);
                handleAfkStatus(velocityPlayer, timeUntilAfk);
                handleAction(velocityPlayer);
            }
        }, 0, 500, TimeUnit.MILLISECONDS);

        VelocityPlatform.getProxy().getChannelRegistrar().register(MinecraftChannelIdentifier.create("bungee", "bungeeafk"));
        VelocityPlatform.getProxy().getEventManager().register(VelocityPlatform.get(), this);
    }

    @Override
    public void setAction(Action action) {
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

    private long calculateTimeUntilAfk(@NotNull VelocityPlayer velocityPlayer) {
        return velocityPlayer.getWhenToAfk() - System.currentTimeMillis();
    }

    private void handleWarning(VelocityPlayer velocityPlayer, long timeUntilAfk) {
        if (timeUntilAfk <= warningTime && timeUntilAfk > 0 && !WARNED.contains(velocityPlayer)) {
            velocityPlayer.sendMessage(Caption.of("notification.afk_warning"));
            WARNED.add(velocityPlayer);
        }
    }

    private void handleAfkStatus(@NotNull VelocityPlayer velocityPlayer, long timeUntilAfk) {
        boolean newAfk = timeUntilAfk <= 0;

        if (velocityPlayer.isAfk() && !newAfk) {
            playerAfkTimeMap.remove(velocityPlayer);
            velocityPlayer.sendMessage(Caption.of("notification.afk_return"));
        } else if (!velocityPlayer.isAfk() && newAfk) {
            playerAfkTimeMap.put(velocityPlayer, System.currentTimeMillis());
            velocityPlayer.sendMessage(Caption.of(
                    action.getMessageKey(),
                    TagResolver.resolver("action-delay", Tag.inserting(Component.text(Format.formatTime((int) (actionDelay / 1000)))))));
        }

        velocityPlayer.setAfk(newAfk);
        if (newAfk) {
            WARNED.remove(velocityPlayer);
        }
    }

    private void handleAction(@NotNull VelocityPlayer velocityPlayer) {
        if (action.equals(Action.NOTHING)) return;

        Optional<Player> platformPlayerOptional = velocityPlayer.getPlatformPlayer();
        if (platformPlayerOptional.isEmpty()) return;
        Player platformPlayer = platformPlayerOptional.get();

        Optional<ServerConnection> playerServerOptional = platformPlayer.getCurrentServer();
        if (playerServerOptional.isEmpty()) return;
        ServerConnection playerServer = playerServerOptional.get();

        if (velocityPlayer.isAfk()) {
            long timeSinceAfk = System.currentTimeMillis() - playerAfkTimeMap.get(velocityPlayer);
            switch (this.action) {
                case CONNECT -> {
                    if (!Action.isAfkServerConfigured()) {
                        BungeeAFK.getLogger().warning("AFK server not found. Defaulting to KICK.");

                        this.action = Action.KICK;
                        if (timeSinceAfk > actionDelay) {
                            platformPlayer.disconnect(Caption.of("notification.afk_kick"));
                            BungeeAFK.getLogger().info("Kicked " + velocityPlayer.getName() + " for being AFK.");
                        }
                        return;
                    }

                    if (timeSinceAfk > actionDelay && !playerServer.getServerInfo().getName().equals(BungeeAFK.getConfig().getString("afk-server-name"))) {
                        playerLastServerMap.put(velocityPlayer, playerServer.getServerInfo().getName());
                        velocityPlayer.connect(BungeeAFK.getConfig().getString("afk-server-name"));
                        velocityPlayer.sendMessage(Caption.of("notification.afk_disconnect"));
                        BungeeAFK.getLogger().info("Moved " + velocityPlayer.getName() + " to AFK server.");
                    }
                }
                case KICK -> {
                    if (timeSinceAfk > actionDelay) {
                        platformPlayer.disconnect(Caption.of("notification.afk_kick"));
                        BungeeAFK.getLogger().info("Kicked " + velocityPlayer.getName() + " for being AFK.");
                    }
                }
            }
        } else if (!velocityPlayer.isAfk() && playerServer.getServerInfo().getName().equalsIgnoreCase(BungeeAFK.getConfig().getString("afk-server-name"))) {
            velocityPlayer.connect(playerLastServerMap.getOrDefault(velocityPlayer, "lobby"));
        }
    }

    private boolean checkServerAvailable(String serverName) {
        return VelocityPlatform.getProxy().getServer(serverName).isPresent();
    }

    @Subscribe
    public void onPostLogin(@NotNull PostLoginEvent event) {
        VelocityPlayer.adapt(event.getPlayer()).updateWhenToAfk();
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
            Optional<VelocityPlayer> player = VelocityPlayer.adapt(playerUUID);
            player.ifPresent(BAFKPlayer::updateWhenToAfk);
        }
    }
}
