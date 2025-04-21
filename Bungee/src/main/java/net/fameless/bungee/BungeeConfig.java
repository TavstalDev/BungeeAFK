package net.fameless.bungee;

import net.fameless.core.inject.PlatformConfig;

public class BungeeConfig implements PlatformConfig {

    @Override
    public String getString(String path) {
        return BungeePlatform.get().getConfig().getString(path);
    }

    @Override
    public boolean getBoolean(String path) {
        return BungeePlatform.get().getConfig().getBoolean(path);
    }

    @Override
    public int getInt(String path) {
        return BungeePlatform.get().getConfig().getInt(path);
    }

    @Override
    public long getLong(String path) {
        return BungeePlatform.get().getConfig().getLong(path);
    }

    @Override
    public String getString(String path, String def) {
        return BungeePlatform.get().getConfig().getString(path, def);
    }

    @Override
    public boolean getBoolean(String path, boolean def) {
        return BungeePlatform.get().getConfig().getBoolean(path, def);
    }

    @Override
    public int getInt(String path, int def) {
        return BungeePlatform.get().getConfig().getInt(path, def);
    }

    @Override
    public long getLong(String path, long def) {
        return BungeePlatform.get().getConfig().getLong(path, def);
    }

    @Override
    public boolean contains(String path) {
        return BungeePlatform.get().getConfig().contains(path);
    }
}
