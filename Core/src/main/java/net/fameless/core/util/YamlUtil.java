package net.fameless.core.util;

import net.fameless.core.caption.Caption;
import net.fameless.core.config.PluginConfig;
import org.jetbrains.annotations.NotNull;

public class YamlUtil {

    public static @NotNull String generateConfig() {
        StringBuilder yamlContent = new StringBuilder();
        yamlContent.append("lang: ").append(Caption.getCurrentLanguage().getIdentifier()).append("\n");
        yamlContent.append("\n");

        yamlContent.append("# Delay after which the warning message is sent to the player (seconds) | Lang entry: \"notification.afk_warning\"\n");
        yamlContent.append("# e.g., if set to 60, the player will receive a warning message after 1 minute of inactivity\n");
        yamlContent.append("warning-delay: ").append(PluginConfig.get().getInt("warning-delay", 60)).append("\n");
        yamlContent.append("\n");

        yamlContent.append("# Delay after which a player is marked as AFK (seconds)\n");
        yamlContent.append("# e.g., if set to 600, the player will be marked as AFK after 10 minutes of inactivity\n");
        yamlContent.append("afk-delay: ").append(PluginConfig.get().getInt("afk-delay", 600)).append("\n");
        yamlContent.append("\n");

        yamlContent.append("# Delay after which the action is executed (seconds)\n");
        yamlContent.append("# e.g., if set to 630, the player will be kicked or connected after 10 minutes and 30 seconds of inactivity\n");
        yamlContent.append("action-delay: ").append(PluginConfig.get().getInt("action-delay", 630)).append("\n");
        yamlContent.append("\n");

        yamlContent.append("# Action to be performed after action delay is reached. Possible values: \"kick\", \"connect\", \"nothing\".\n");
        yamlContent.append("# \"kick\" - player is kicked from the server\n");
        yamlContent.append("# \"connect\" - player is connected to the server specified in the \"afk-server-name\" option\n");
        yamlContent.append("# \"nothing\" - nothing happens\n");
        yamlContent.append("action: \"").append(PluginConfig.get().getString("action", "kick")).append("\"\n");
        yamlContent.append("\n");

        yamlContent.append("# Name of the server to which the player will be connected if the action is set to \"connect\"\n");
        yamlContent.append("# If the server does not exist, the action will default to \"kick\"\n");
        yamlContent.append("# !!! Only available for BungeeCord and Velocity !!!").append("\n");
        yamlContent.append("afk-server-name: ").append(PluginConfig.get().getString("afk-server-name", "")).append("\n");
        yamlContent.append("\n");

        yamlContent.append("# Whether to allow bypass of AFK detection for players with the \"afk.bypass\" permission\n");
        yamlContent.append("allow-bypass: ").append(PluginConfig.get().getBoolean("allow-bypass", true)).append("\n");
        return yamlContent.toString();
    }

}
