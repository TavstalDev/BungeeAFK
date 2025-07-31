package net.fameless.core.handling;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fameless.core.BungeeAFK;
import net.fameless.core.caption.Caption;
import net.fameless.core.config.PluginConfig;
import net.fameless.core.location.Location;
import net.fameless.core.player.BAFKPlayer;
import net.fameless.core.player.GameMode;
import net.fameless.core.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
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
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private final Map<BAFKPlayer<?>, String> playerPreviousServerMap = new ConcurrentHashMap<>();
    private final Map<BAFKPlayer<?>, Location> playerPreviousLocationMap = new ConcurrentHashMap<>();
    private final Map<BAFKPlayer<?>, GameMode> playerPreviousGameModeMap = new ConcurrentHashMap<>();
    private static final long UPDATE_PERIOD_MILLIS = 500L;

    private Action action;
    private long warnDelay;
    private long afkDelay;
    private long actionDelay;
    private final ScheduledFuture<?> scheduledTask;

    private JsonObject locationObject;
    private JsonObject gameModeObject;
    private JsonObject serverObject;

    public AFKHandler() {
        if (BungeeAFK.getAFKHandler() != null) throw new IllegalStateException("AFKHandler is already initialized.");
        fetchConfigValues();
        this.scheduledTask = SCHEDULER.scheduleAtFixedRate(this::checkAFKPlayers, 0, UPDATE_PERIOD_MILLIS, TimeUnit.MILLISECONDS);
        initJsonObjects();
        init();
    }

    private void initJsonObjects() {
        File playerStatesFile = createPersistedStatesFileIfNotExists();
        JsonObject root;
        try (FileReader reader = new FileReader(playerStatesFile)) {
            root = GSON.fromJson(reader, JsonObject.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (locationObject == null) locationObject = root.has("location") ? root.getAsJsonObject("location") : new JsonObject();
        if (gameModeObject == null) gameModeObject = root.has("game_mode") ? root.getAsJsonObject("game_mode") : new JsonObject();
        if (serverObject == null) serverObject = root.has("server") ? root.getAsJsonObject("server") : new JsonObject();
    }

    private void checkAFKPlayers() {
        try {
            BAFKPlayer.PLAYERS.stream()
                    .filter(PlayerFilters.isOnline())
                    .forEach(player -> {
                        player.increaseTimeSinceLastAction(UPDATE_PERIOD_MILLIS);
                        switch (player.getAfkState()) {
                            case ACTIVE -> handleWarning(player);
                            case WARNED -> handleAfkStatus(player);
                        }
                        handleAction(player);
                        updatePlayerStatus(player);
                        sendActionBar(player);
                    });
        } catch (Exception e) {
            LOGGER.error("Error during AFK check task", e);
            scheduledTask.cancel(false);
        }
    }

    public void fetchPreviousPlayerState(@NotNull BAFKPlayer<?> player) {
        if (locationObject.has(player.getUniqueId().toString())) {
            JsonObject playerLocation = locationObject.getAsJsonObject(player.getUniqueId().toString());
            playerPreviousLocationMap.put(player, new Location(
                    playerLocation.get("worldName").getAsString(),
                    playerLocation.get("x").getAsDouble(),
                    playerLocation.get("y").getAsDouble(),
                    playerLocation.get("z").getAsDouble(),
                    playerLocation.get("pitch").getAsFloat(),
                    playerLocation.get("yaw").getAsFloat()
            ));
        }

        if (gameModeObject.has(player.getUniqueId().toString())) {
            String gameModeStr = gameModeObject.get(player.getUniqueId().toString()).getAsString();
            try {
                GameMode gameMode = GameMode.valueOf(gameModeStr.toUpperCase());
                playerPreviousGameModeMap.put(player, gameMode);
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Invalid game mode for player {}: {}", player.getName(), gameModeStr);
            }
        }

        if (serverObject.has(player.getUniqueId().toString())) {
            String previousServer = serverObject.get(player.getUniqueId().toString()).getAsString();
            playerPreviousServerMap.put(player, previousServer);
        }

        locationObject.remove(player.getUniqueId().toString());
        gameModeObject.remove(player.getUniqueId().toString());
        serverObject.remove(player.getUniqueId().toString());
    }

    private @NotNull File createPersistedStatesFileIfNotExists() {
        File playerStatesFile = PluginPaths.getPersistedStatesFile();
        if (!playerStatesFile.exists()) {
            ResourceUtil.extractResourceIfMissing("persisted_player_states.json", playerStatesFile);
        }
        return playerStatesFile;
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
            if (!BungeeAFK.getPlatform().doesServerExist(serverName)) {
                LOGGER.warn("AFK server not found. Defaulting to KICK.");
                this.action = Action.KICK;
            }
        }
    }

    public void shutdown() {
        if (scheduledTask != null && !scheduledTask.isCancelled()) {
            scheduledTask.cancel(true);
        }

        for (Map.Entry<BAFKPlayer<?>, String> entry : playerPreviousServerMap.entrySet()) {
            serverObject.addProperty(entry.getKey().getUniqueId().toString(), entry.getValue());
        }
        for (Map.Entry<BAFKPlayer<?>, GameMode> entry : playerPreviousGameModeMap.entrySet()) {
            gameModeObject.addProperty(entry.getKey().getUniqueId().toString(), entry.getValue().name());
        }
        for (Map.Entry<BAFKPlayer<?>, Location> entry : playerPreviousLocationMap.entrySet()) {
            locationObject.add(entry.getKey().getUniqueId().toString(), entry.getValue().getAsJsonObject());
        }

        JsonObject root = new JsonObject();
        root.add("location", locationObject);
        root.add("game_mode", gameModeObject);
        root.add("server", serverObject);

        File playerStatesFile = createPersistedStatesFileIfNotExists();
        try (FileWriter writer = new FileWriter(playerStatesFile)) {
            GSON.toJson(root, writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
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
            MessageBroadcaster.broadcastMessageToFiltered(
                    Caption.of("notification.afk_broadcast",
                            TagResolver.resolver("player", Tag.inserting(Component.text(player.getName())))),
                    PlayerFilters.matches(player).negate()
            );
            LOGGER.info("{} is now AFK.", player.getName());
        }
    }

    public void handleAction(@NotNull BAFKPlayer<?> player) {
        if (player.getAfkState() == AFKState.ACTION_TAKEN) return;
        revertPreviousState(player);

        if (action == Action.NOTHING || player.getAfkState() != AFKState.AFK || player.getTimeSinceLastAction() < actionDelay)
            return;

        switch (action) {
            case CONNECT -> {
                if (!Action.isAfkServerConfigured()) {
                    LOGGER.warn("AFK server not found. Defaulting to KICK.");
                    this.action = Action.KICK;
                    player.kick(Caption.of("notification.afk_kick"));
                    MessageBroadcaster.broadcastMessageToFiltered(
                            Caption.of("notification.afk_kick_broadcast",
                                    TagResolver.resolver("player", Tag.inserting(Component.text(player.getName())))),
                            PlayerFilters.matches(player).negate()
                    );
                    LOGGER.info("Kicked {} for being AFK.", player.getName());
                    return;
                }
                String currentServerName = player.getCurrentServerName();
                String afkServerName = PluginConfig.get().getString("afk-server-name", "");
                playerPreviousServerMap.put(player, currentServerName);
                player.connect(afkServerName);
                player.sendMessage(Caption.of("notification.afk_disconnect"));
                MessageBroadcaster.broadcastMessageToFiltered(
                        Caption.of("notification.afk_disconnect_broadcast",
                                TagResolver.resolver("player", Tag.inserting(Component.text(player.getName())))),
                        PlayerFilters.matches(player).negate()
                );
                LOGGER.info("Moved {} to AFK server.", player.getName());
            }
            case KICK -> {
                player.kick(Caption.of("notification.afk_kick"));
                MessageBroadcaster.broadcastMessageToFiltered(
                        Caption.of("notification.afk_kick_broadcast",
                                TagResolver.resolver("player", Tag.inserting(Component.text(player.getName())))));
                LOGGER.info("Kicked {} for being AFK.", player.getName());
            }
            case TELEPORT -> {
                playerPreviousLocationMap.put(player, player.getLocation());
                playerPreviousGameModeMap.put(player, player.getGameMode());
                player.updateGameMode(GameMode.SPECTATOR);
                player.teleport(Location.getConfiguredAfkZone());
            }
        }
    }

    public void revertPreviousState(@NotNull BAFKPlayer<?> player) {
        String afkServerName = PluginConfig.get().getString("afk-server-name", "");
        if (player.getCurrentServerName().equalsIgnoreCase(afkServerName)) {
            player.connect(playerPreviousServerMap.getOrDefault(player, "lobby"));
        }

        if (playerPreviousLocationMap.containsKey(player) && playerPreviousGameModeMap.containsKey(player)) {
            player.teleport(playerPreviousLocationMap.get(player));
            player.updateGameMode(playerPreviousGameModeMap.get(player));
            playerPreviousLocationMap.remove(player);
            playerPreviousGameModeMap.remove(player);
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

    public void handleJoin(@NotNull BAFKPlayer<?> player) {
        player.setTimeSinceLastAction(0);
        player.setAfkState(AFKState.ACTIVE);
        fetchPreviousPlayerState(player);
        revertPreviousState(player);
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
