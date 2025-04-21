package net.fameless.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import net.fameless.core.inject.PlatformConfig;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class VelocityConfig implements PlatformConfig {

    private final Path configFile;
    private final YAMLConfigurationLoader loader;
    private ConfigurationNode configNode;

    @Inject
    public VelocityConfig(@DataDirectory @NotNull Path dataDirectory) {
        this.configFile = dataDirectory.resolve("config.yml");
        this.loader = YAMLConfigurationLoader.builder().setPath(configFile).build();
        loadConfig();
    }

    public void loadConfig() {
        try {
            if (!Files.exists(configFile)) {
                Files.createDirectories(configFile.getParent());
                Files.copy(getClass().getResourceAsStream("/config.yml"), configFile);
            }
            configNode = loader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getString(@NotNull String path) {
        return configNode.getNode((Object[]) path.split("\\.")).getString();
    }

    @Override
    public boolean getBoolean(@NotNull String path) {
        return configNode.getNode((Object[]) path.split("\\.")).getBoolean();
    }

    @Override
    public int getInt(@NotNull String path) {
        return configNode.getNode((Object[]) path.split("\\.")).getInt();
    }

    @Override
    public long getLong(@NotNull String path) {
        return configNode.getNode((Object[]) path.split("\\.")).getLong();
    }

    @Override
    public String getString(@NotNull String path, String def) {
        return configNode.getNode((Object[]) path.split("\\.")).getString(def);
    }

    @Override
    public boolean getBoolean(@NotNull String path, boolean def) {
        return configNode.getNode((Object[]) path.split("\\.")).getBoolean(def);
    }

    @Override
    public int getInt(@NotNull String path, int def) {
        return configNode.getNode((Object[]) path.split("\\.")).getInt(def);
    }

    @Override
    public long getLong(@NotNull String path, long def) {
        return configNode.getNode((Object[]) path.split("\\.")).getLong(def);
    }

    @Override
    public boolean contains(@NotNull String path) {
        return configNode.getNode((Object[]) path.split("\\.")).isVirtual();
    }
}
