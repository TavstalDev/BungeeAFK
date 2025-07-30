package net.fameless.bungeeAFKTracking;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;

import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;


public class BungeeAFKTracking extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "bungee:bungeeafk");
        this.getServer().getMessenger().registerIncomingPluginChannel(this, "bungee:bungeeafk", this::handleIncomingMessage);
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        this.getServer().getMessenger().unregisterOutgoingPluginChannel(this);
    }

    @EventHandler
    public void onMove(@NotNull PlayerMoveEvent event) {
        if (!event.getFrom().equals(event.getTo())) {
            sendBungeeMessage(event.getPlayer());
        }
    }

    @EventHandler
    public void onChat(@NotNull AsyncPlayerChatEvent event) {
        sendBungeeMessage(event.getPlayer());
    }

    @EventHandler
    public void onInteract(@NotNull PlayerInteractEvent event) {
        sendBungeeMessage(event.getPlayer());
    }

    private void sendBungeeMessage(@NotNull Player player) {
        byte[] data = ("action_caught;" + player.getUniqueId()).getBytes();
        player.sendPluginMessage(this, "bungee:bungeeafk", data);
    }

    private void handleIncomingMessage(String channel, Player player, byte[] message) {
        String messageString = new String(message);
        String[] parts = messageString.split(";");
        if (parts.length < 1) return;
        if (!parts[0].equalsIgnoreCase("teleport_player")) return;
        try {
            Player targetPlayer = Bukkit.getPlayer(UUID.fromString(parts[1]));
            World world = Bukkit.getWorld(parts[2]);
            double x = Double.parseDouble(parts[3]);
            double y = Double.parseDouble(parts[4]);
            double z = Double.parseDouble(parts[5]);
            float yaw = Float.parseFloat(parts[6]);
            float pitch = Float.parseFloat(parts[7]);

            if (targetPlayer == null || world == null) {
                getLogger().severe("Invalid teleport data received: " + messageString);
                return;
            }

            player.teleport(new Location(world, x, y, z, yaw, pitch));
        } catch (Exception e) {
            getLogger().severe("Invalid teleport data received: " + e.getMessage());
        }
    }
}
