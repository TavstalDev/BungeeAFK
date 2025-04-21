package net.fameless.bungee;

import net.fameless.core.BungeeAFK;
import net.fameless.core.caption.Caption;
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

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BungeeAFKHandler implements net.fameless.core.handling.AFKHandler, Listener {

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
            for (BAFKPlayer<?> player : BungeePlayer.getOnlinePlayers()) {
                if (!(player instanceof BungeePlayer bungeePlayer)) continue;
                long timeUntilAfk = calculateTimeUntilAfk(bungeePlayer);

                handleWarning(bungeePlayer, timeUntilAfk);
                handleAfkStatus(bungeePlayer, timeUntilAfk);
                handleAction(bungeePlayer);
            }
        }, 0, 500, TimeUnit.MILLISECONDS);

        BungeePlatform.proxyServer().registerChannel("bungee:bungeeafk");
        BungeePlatform.proxyServer().getPluginManager().registerListener(BungeePlatform.get(), this);
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

    private long calculateTimeUntilAfk(@NotNull BungeePlayer bungeePlayer) {
        return bungeePlayer.getWhenToAfk() - System.currentTimeMillis();
    }

    private void handleWarning(BungeePlayer bungeePlayer, long timeUntilAfk) {
        if (timeUntilAfk <= warningTime && timeUntilAfk > 0 && !WARNED.contains(bungeePlayer)) {
            bungeePlayer.sendMessage(Caption.of("notification.afk_warning"));
            WARNED.add(bungeePlayer);
        }
    }

    private void handleAfkStatus(@NotNull BungeePlayer bungeePlayer, long timeUntilAfk) {
        boolean newAfk = timeUntilAfk <= 0;

        if (bungeePlayer.isAfk() && !newAfk) {
            playerAfkTimeMap.remove(bungeePlayer);
            bungeePlayer.sendMessage(Caption.of("notification.afk_return"));
        } else if (!bungeePlayer.isAfk() && newAfk) {
            playerAfkTimeMap.put(bungeePlayer, System.currentTimeMillis());
            bungeePlayer.sendMessage(Caption.of(
                    action.getMessageKey(),
                    TagResolver.resolver("action-delay", Tag.inserting(Component.text(Format.formatTime((int) (actionDelay / 1000)))))));
        }

        bungeePlayer.setAfk(newAfk);
        if (newAfk) {
            WARNED.remove(bungeePlayer);
        }
    }

    private void handleAction(@NotNull BungeePlayer bungeePlayer) {
        if (action.equals(Action.NOTHING)) return;

        Optional<ProxiedPlayer> platformPlayerOptional = bungeePlayer.getPlatformPlayer();
        if (platformPlayerOptional.isEmpty()) return;
        ProxiedPlayer platformPlayer = platformPlayerOptional.get();

        Server playerServer = platformPlayer.getServer();
        if (playerServer == null) return;
        if (bungeePlayer.isAfk()) {
            long timeSinceAfk = System.currentTimeMillis() - playerAfkTimeMap.get(bungeePlayer);
            switch (this.action) {
                case CONNECT -> {
                    if (!Action.isAfkServerConfigured()) {
                        BungeeAFK.getLogger().warning("AFK server not found. Defaulting to KICK.");

                        this.action = Action.KICK;
                        if (timeSinceAfk > actionDelay) {
                            platformPlayer.disconnect(BungeeComponentSerializer.get().serialize(Caption.of("notification.afk_kick")));
                            BungeeAFK.getLogger().info("Kicked " + bungeePlayer.getName() + " for being AFK.");
                        }
                        return;
                    }

                    if (timeSinceAfk > actionDelay && !playerServer.getInfo().getName().equals(BungeeAFK.getConfig().getString("afk-server-name"))) {
                        playerLastServerMap.put(bungeePlayer, playerServer.getInfo().getName());
                        bungeePlayer.connect(BungeeAFK.getConfig().getString("afk-server-name"));
                        bungeePlayer.sendMessage(Caption.of("notification.afk_disconnect"));
                        BungeeAFK.getLogger().info("Moved " + bungeePlayer.getName() + " to AFK server.");
                    }
                }
                case KICK -> {
                    if (timeSinceAfk > actionDelay) {
                        platformPlayer.disconnect(BungeeComponentSerializer.get().serialize(Caption.of("notification.afk_kick")));
                        BungeeAFK.getLogger().info("Kicked " + bungeePlayer.getName() + " for being AFK.");
                    }
                }
            }
        } else if (!bungeePlayer.isAfk() && playerServer.getInfo().getName().equalsIgnoreCase(BungeeAFK.getConfig().getString("afk-server-name"))) {
            bungeePlayer.connect(playerLastServerMap.getOrDefault(bungeePlayer, "lobby"));
        }
    }

    private boolean checkServerAvailable(String serverName) {
        return BungeePlatform.proxyServer().getServerInfo(serverName) != null;
    }

    @EventHandler
    public void onPostLogin(@NotNull PostLoginEvent event) {
        BungeePlayer.adapt(event.getPlayer()).updateWhenToAfk();
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
            Optional<BungeePlayer> player = BungeePlayer.adapt(playerUUID);
            player.ifPresent(BAFKPlayer::updateWhenToAfk);
        }
    }
}
