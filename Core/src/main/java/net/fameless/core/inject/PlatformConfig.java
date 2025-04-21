package net.fameless.core.inject;

public interface PlatformConfig {

    String getString(String path);
    boolean getBoolean(String path);
    int getInt(String path);
    long getLong(String path);

    String getString(String path, String def);
    boolean getBoolean(String path, boolean def);
    int getInt(String path, int def);
    long getLong(String path, long def);

    boolean contains(String path);

}
