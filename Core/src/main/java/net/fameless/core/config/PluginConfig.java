package net.fameless.core.config;

import net.fameless.core.util.PluginPaths;
import net.fameless.core.util.ResourceUtil;
import net.fameless.core.util.YamlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

public class PluginConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("BungeeAFK/" + PluginConfig.class.getSimpleName());
    private static YamlConfig config;

    @SuppressWarnings("unchecked")
    public static void init() {
        LOGGER.info("Loading configuration...");
        File configFile = PluginPaths.getConfigFile();

        if (!configFile.exists()) {
            ResourceUtil.extractResourceIfMissing("config.yml", configFile);
        }

        String yamlContent;
        try {
            yamlContent = new String(Files.readAllBytes(Paths.get(configFile.toURI())));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        LoadSettings loadSettings = LoadSettings.builder()
                .setParseComments(true)
                .build();

        Load load = new Load(loadSettings);

        config = new YamlConfig((Map<String, Object>) load.loadFromString(yamlContent));

        LOGGER.info("\nYour BungeeAFK configuration: \n  * lang: {} \n  * warning-delay: {} \n  * afk-delay: {} \n  * action-delay: {} \n  * action: {} \n  * afk-server-name: {} \n  * allow-bypass: {}",
                config.getString("lang", "en"),
                config.getInt("warning-delay", 300),
                config.getInt("afk-delay", 600),
                config.getInt("action-delay", 630),
                config.getString("action", "kick"),
                config.getString("afk-server-name", "not configured"),
                config.getBoolean("allow-bypass", true));
    }

    public static void handleReload() {
        init();
    }

    public static void handleShutdown() {
        File configFile = PluginPaths.getConfigFile();

        String fileContent = YamlUtil.generateYamlFile();
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write(fileContent);
            writer.flush();
        } catch (IOException e) {
            LOGGER.error("Failed to write configuration file: {}", configFile.getAbsolutePath(), e);
        }
    }

    public static YamlConfig get() {
        return config;
    }
}
