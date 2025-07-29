package net.fameless.core.command;

import com.google.gson.JsonObject;
import net.fameless.core.BungeeAFK;
import net.fameless.core.caption.Caption;
import net.fameless.core.caption.Language;
import net.fameless.core.command.framework.CallerType;
import net.fameless.core.command.framework.Command;
import net.fameless.core.command.framework.CommandCaller;
import net.fameless.core.config.PluginConfig;
import net.fameless.core.handling.AFKHandler;
import net.fameless.core.handling.Action;
import net.fameless.core.util.StringUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainCommand extends Command {

    public MainCommand() {
        super(
                "bungeeafk",
                List.of("bafk"),
                CallerType.NONE,
                "/bungeeafk configure <afk-delay|action-delay|action> <delay|action>",
                "bungeeafk.command"
        );
    }

    @Override
    protected void executeCommand(CommandCaller caller, String @NotNull [] args) {
        if (args.length < 2) return;
        if (args[0].equalsIgnoreCase("configure")) {
            AFKHandler afkHandler = BungeeAFK.getAFKHandler();
            switch (args[1]) {
                case "allow-bypass" -> {
                    if (args.length < 3) {
                        sendUsage(caller);
                        return;
                    }
                    boolean allowBypass;
                    try {
                        allowBypass = Boolean.parseBoolean(args[2]);
                    } catch (IllegalArgumentException e) {
                        caller.sendMessage(Caption.of("command.invalid_boolean"));
                        return;
                    }

                    PluginConfig.get().set("allow-bypass", allowBypass);
                    caller.sendMessage(Caption.of("command.bypass_set",
                            TagResolver.resolver("allow", Tag.inserting(Component.text(allowBypass)))
                    ));
                }
                case "warning-delay" -> {
                    if (args.length < 3) {
                        sendUsage(caller);
                        return;
                    }

                    long delay;
                    try {
                        int delaySeconds = Integer.parseInt(args[2]);
                        delay = delaySeconds * 1000L;
                    } catch (NumberFormatException e) {
                        caller.sendMessage(Caption.of("command.invalid_number"));
                        return;
                    }
                    afkHandler.setWarnDelayMillis(delay);
                    caller.sendMessage(Caption.of(
                            "command.warn_delay_set",
                            TagResolver.resolver("delay", Tag.inserting(Component.text(delay / 1000)))
                    ));
                }
                case "afk-delay" -> {
                    if (args.length < 3) {
                        sendUsage(caller);
                        return;
                    }

                    long delay;
                    try {
                        int delaySeconds = Integer.parseInt(args[2]);
                        delay = delaySeconds * 1000L;
                    } catch (NumberFormatException e) {
                        caller.sendMessage(Caption.of("command.invalid_number"));
                        return;
                    }
                    afkHandler.setAfkDelayMillis(delay);
                    caller.sendMessage(Caption.of(
                            "command.afk_delay_set",
                            TagResolver.resolver("delay", Tag.inserting(Component.text(delay / 1000)))
                    ));
                }
                case "action-delay" -> {
                    if (args.length < 3) {
                        sendUsage(caller);
                        return;
                    }

                    long delay;
                    try {
                        int delaySeconds = Integer.parseInt(args[2]);
                        delay = delaySeconds * 1000L;
                    } catch (NumberFormatException e) {
                        caller.sendMessage(Caption.of("command.invalid_number"));
                        return;
                    }
                    afkHandler.setActionDelayMillis(delay);
                    caller.sendMessage(Caption.of(
                            "command.action_delay_set",
                            TagResolver.resolver("delay", Tag.inserting(Component.text(delay / 1000)))
                    ));
                }
                case "action" -> {
                    Action action;
                    try {
                        action = Action.fromIdentifier(args[2]);
                    } catch (IllegalArgumentException e) {
                        caller.sendMessage(Caption.of("command.invalid_action"));
                        return;
                    }

                    if (!action.isAvailable()) {
                        caller.sendMessage(Caption.of("notification.action_unavailable"));
                        return;
                    }

                    if (action == Action.CONNECT && !Action.isAfkServerConfigured()) {
                        caller.sendMessage(Caption.of("command.action_not_configured"));
                        return;
                    }

                    afkHandler.setAction(action);
                    caller.sendMessage(Caption.of(
                            "command.action_set",
                            TagResolver.resolver("action", Tag.inserting(Component.text(action.getIdentifier().toLowerCase())))
                    ));
                }
                case "caption" -> {
                    Language language = Language.ofIdentifier(args[2]);
                    if (language == null) {
                        caller.sendMessage(Caption.of("command.invalid_language"));
                        return;
                    }

                    JsonObject languageJsonObject = Caption.getLanguageJsonObject(language);
                    if (languageJsonObject == null) {
                        caller.sendMessage(Caption.of("command.language_not_loaded", TagResolver.resolver("language", Tag.inserting(Component.text(language.getIdentifier())))));
                        return;
                    }

                    String captionKey = args[3];
                    if (!Caption.hasKey(language, captionKey)) {
                        caller.sendMessage(Caption.of("command.no_such_key", TagResolver.resolver("key", Tag.inserting(Component.text(captionKey)))));
                        return;
                    }

                    languageJsonObject.addProperty(captionKey, String.join(" ", Arrays.copyOfRange(args, 4, args.length)));
                    Caption.loadLanguage(language, languageJsonObject);
                    Caption.saveToFile();

                    caller.sendMessage(Caption.of("command.caption_set",
                            TagResolver.resolver("language", Tag.inserting(Component.text(language.getIdentifier()))),
                            TagResolver.resolver("key", Tag.inserting(Component.text(captionKey))),
                            TagResolver.resolver("caption", Tag.inserting(Component.text(Caption.getString(language, captionKey))))
                    ));
                }
                case "reloadconfig" -> {
                    PluginConfig.reload();
                    afkHandler.fetchConfigValues();
                    caller.sendMessage(Caption.of("command.config_reloaded"));
                }
            }
        } else if (args[0].equalsIgnoreCase("lang")) {
            if (args[1].equalsIgnoreCase("reload")) {
                Caption.loadDefaultLanguages();
                caller.sendMessage(Caption.of("command.languages_reloaded"));
                return;
            }
            Language newLanguage = Language.ofIdentifier(args[1]);
            if (newLanguage == null) {
                caller.sendMessage(Caption.of("command.invalid_language"));
                return;
            }

            Caption.setCurrentLanguage(newLanguage);
            caller.sendMessage(Caption.of(
                    "command.language_changed",
                    TagResolver.resolver("language", Tag.inserting(Component.text(newLanguage.getFriendlyName())))
            ));
        }
    }

    @Override
    protected List<String> tabComplete(CommandCaller caller, String @NotNull [] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.addAll(Arrays.asList("configure", "lang"));
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("configure")) {
                completions.addAll(Arrays.asList(
                        "afk-delay",
                        "action-delay",
                        "action",
                        "caption",
                        "warning-delay",
                        "allow-bypass",
                        "reloadconfig"
                ));
            } else if (args[0].equalsIgnoreCase("lang")) {
                completions.add("reload");
                for (Language language : Language.values()) {
                    completions.add(language.getIdentifier());
                }
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("configure")) {
                if (args[1].equalsIgnoreCase("action")) {
                    for (Action action : Action.getAvailableActions()) {
                        completions.add(action.getIdentifier());
                    }
                } else if (args[1].equalsIgnoreCase("afk-delay")
                        || args[1].equalsIgnoreCase("action-delay")
                        || args[1].equalsIgnoreCase("warning-delay")) {
                    completions.add("<seconds>");
                } else if (args[1].equalsIgnoreCase("caption")) {
                    for (Language language : Language.values()) {
                        completions.add(language.getIdentifier());
                    }
                } else if (args[1].equalsIgnoreCase("allow-bypass")) {
                    completions.addAll(Arrays.asList("true", "false"));
                }
            }
        } else if (args.length == 4) {
            if (args[0].equalsIgnoreCase("configure") && args[1].equalsIgnoreCase("caption")) {
                Language language = Language.ofIdentifier(args[2]);
                if (language != null) {
                    JsonObject languageObj = Caption.getLanguageJsonObject(language);
                    if (languageObj != null) {
                        completions.addAll(languageObj.keySet());
                    }
                }
            }
        } else if (args.length == 5) {
            if (args[0].equalsIgnoreCase("configure") && args[1].equalsIgnoreCase("caption")) {
                completions.add("<new-caption>");
            }
        }
        return StringUtil.copyPartialMatches(args[args.length - 1], completions, new ArrayList<>());
    }
}
