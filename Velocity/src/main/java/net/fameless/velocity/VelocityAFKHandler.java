package net.fameless.velocity;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import net.fameless.core.handling.AFKHandler;
import net.fameless.core.handling.AFKState;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class VelocityAFKHandler extends AFKHandler {

    @Override
    public void init() {
        VelocityPlatform.getProxy().getChannelRegistrar().register(MinecraftChannelIdentifier.create("bungee", "bungeeafk"));
        VelocityPlatform.getProxy().getEventManager().register(VelocityPlatform.get(), this);
    }

    @Subscribe
    public void onPostLogin(@NotNull PostLoginEvent event) {
        VelocityPlayer velocityPlayer = VelocityPlayer.adapt(event.getPlayer());
        velocityPlayer.setTimeSinceLastAction(0);
        velocityPlayer.setAfkState(AFKState.ACTIVE);
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
            VelocityPlayer velocityPlayer = VelocityPlayer.adapt(playerUUID).orElse(null);
            if (velocityPlayer == null) return;
            velocityPlayer.setTimeSinceLastAction(0);
            velocityPlayer.setAfkState(AFKState.ACTIVE);
        }
    }
}
