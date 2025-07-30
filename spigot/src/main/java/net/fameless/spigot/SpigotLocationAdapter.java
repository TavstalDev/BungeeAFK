package net.fameless.spigot;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class SpigotLocationAdapter {

    @Contract("_ -> new")
    public static @NotNull Location adapt(@NotNull net.fameless.core.location.Location location) {
        World world = Bukkit.getWorld(location.getWorldName());
        if (world == null) {
            throw new IllegalArgumentException("World '" + location.getWorldName() + "' not found for location conversion");
        }
        return new Location(
            world,
            location.getX(),
            location.getY(),
            location.getZ(),
            location.getYaw(),
            location.getPitch()
        );
    }

}
