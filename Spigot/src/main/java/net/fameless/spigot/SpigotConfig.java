package net.fameless.spigot;

import net.fameless.core.inject.PlatformConfig;

public class SpigotConfig implements PlatformConfig {

    @Override
    public String getString(String path) {
        return SpigotPlatform.get().getConfig().getString(path);
    }

    @Override
    public boolean getBoolean(String path) {
        return SpigotPlatform.get().getConfig().getBoolean(path);
    }

    @Override
    public int getInt(String path) {
        return SpigotPlatform.get().getConfig().getInt(path);
    }

    @Override
    public long getLong(String path) {
        return SpigotPlatform.get().getConfig().getLong(path);
    }

    @Override
    public String getString(String path, String def) {
        return SpigotPlatform.get().getConfig().getString(path, def);
    }

    @Override
    public boolean getBoolean(String path, boolean def) {
        return SpigotPlatform.get().getConfig().getBoolean(path, def);
    }

    @Override
    public int getInt(String path, int def) {
        return SpigotPlatform.get().getConfig().getInt(path, def);
    }

    @Override
    public long getLong(String path, long def) {
        return SpigotPlatform.get().getConfig().getLong(path, def);
    }

    @Override
    public boolean contains(String path) {
        return SpigotPlatform.get().getConfig().contains(path);
    }
}
