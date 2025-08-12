package net.fameless.core.adapter;

import net.fameless.api.exception.PlayerNotFoundException;
import net.fameless.api.model.Player;
import net.fameless.core.handling.AFKState;
import net.fameless.core.player.BAFKPlayer;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class APIAdapter {

    public static BAFKPlayer<?> adapt(@NotNull Player player) throws PlayerNotFoundException {
        return BAFKPlayer.of(player.uuid()).orElseThrow(() -> new PlayerNotFoundException("Player not found: " + player.uuid()));
    }

    @Contract("_ -> new")
    public static @NotNull Player adapt(@NotNull BAFKPlayer<?> player) {
        return new Player(player.getUniqueId());
    }

    public static AFKState adapt(net.fameless.api.model.@NotNull AFKState afkState) {
        return AFKState.valueOf(afkState.name());
    }

    public static net.fameless.api.model.AFKState adapt(@NotNull AFKState afkState) {
        return net.fameless.api.model.AFKState.valueOf(afkState.name());
    }

    @Contract(pure = true)
    public static @NotNull Consumer<Player> adaptModelConsumer(@NotNull Consumer<BAFKPlayer<?>> consumer) {
        return player -> {
            try {
                consumer.accept(adapt(player));
            } catch (PlayerNotFoundException e) {
                throw new RuntimeException(e);
            }
        };
    }

    @Contract(pure = true)
    public static @NotNull Consumer<BAFKPlayer<?>> adaptCoreConsumer(@NotNull Consumer<Player> consumer) {
        return player -> consumer.accept(adapt(player));
    }

}
