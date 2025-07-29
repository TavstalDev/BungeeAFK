package net.fameless.core.util;

import net.fameless.core.caption.Caption;
import net.fameless.core.config.PluginConfig;
import org.jetbrains.annotations.NotNull;

public class YamlUtil {

    public static @NotNull String generateConfig() {
        return "lang: " + Caption.getCurrentLanguage().getIdentifier() + "\n" +
                "\n" +
                "# Delay after which the warning message is sent to the player (seconds) | Lang entry: \"notification.afk_warning\"\n" +
                "# e.g., if set to 60, the player will receive a warning message after 1 minute of inactivity\n" +
                "warning-delay: " + PluginConfig.get().getInt("warning-delay", 60) + "\n" +
                "\n" +
                "# Delay after which a player is marked as AFK (seconds)\n" +
                "# e.g., if set to 600, the player will be marked as AFK after 10 minutes of inactivity\n" +
                "afk-delay: " + PluginConfig.get().getInt("afk-delay", 600) + "\n" +
                "\n" +
                "# Delay after which the action is executed (seconds)\n" +
                "# e.g., if set to 630, the player will be kicked or connected after 10 minutes and 30 seconds of inactivity\n" +
                "action-delay: " + PluginConfig.get().getInt("action-delay", 630) + "\n" +
                "\n" +
                "# Action to be performed after action delay is reached. Possible values: \"kick\", \"connect\", \"nothing\".\n" +
                "# \"kick\" - player is kicked from the server\n" +
                "# \"connect\" - player is connected to the server specified in the \"afk-server-name\" option\n" +
                "# \"nothing\" - nothing happens\n" +
                "action: \"" + PluginConfig.get().getString("action", "kick") + "\"\n" +
                "\n" +
                "# Name of the server to which the player will be connected if the action is set to \"connect\"\n" +
                "# If the server does not exist, the action will default to \"kick\"\n" +
                "# !!! Only available for BungeeCord and Velocity !!!" + "\n" +
                "afk-server-name: " + PluginConfig.get().getString("afk-server-name", "") + "\n" +
                "\n" +
                "# Whether to allow bypass of AFK detection for players with the \"afk.bypass\" permission\n" +
                "allow-bypass: " + PluginConfig.get().getBoolean("allow-bypass", true) + "\n";
    }

}
