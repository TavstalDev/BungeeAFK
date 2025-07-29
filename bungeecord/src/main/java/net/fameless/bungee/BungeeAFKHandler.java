package net.fameless.bungee;

import net.fameless.core.handling.AFKHandler;
import net.fameless.core.handling.AFKState;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class BungeeAFKHandler extends AFKHandler implements Listener {

    @Override
    public void init() {
        BungeePlatform.proxyServer().registerChannel("bungee:bungeeafk");
        BungeePlatform.proxyServer().getPluginManager().registerListener(BungeePlatform.get(), this);
    }

    @EventHandler
    public void onPostLogin(@NotNull PostLoginEvent event) {
        BungeePlayer bungeePlayer = BungeePlayer.adapt(event.getPlayer());
        bungeePlayer.setTimeSinceLastAction(0);
        bungeePlayer.setAfkState(AFKState.ACTIVE);
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
            BungeePlayer bungeePlayer = BungeePlayer.adapt(playerUUID).orElse(null);
            if (bungeePlayer == null) return;
            bungeePlayer.setTimeSinceLastAction(0);
            bungeePlayer.setAfkState(AFKState.ACTIVE);
        }
    }
}
