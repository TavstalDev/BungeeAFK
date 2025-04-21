package net.fameless.core.command;

import net.fameless.core.BungeeAFK;
import net.fameless.core.caption.Caption;
import net.fameless.core.caption.Language;
import net.fameless.core.command.framework.CallerType;
import net.fameless.core.command.framework.Command;
import net.fameless.core.command.framework.CommandCaller;
import net.fameless.core.handling.AFKHandler;
import net.fameless.core.handling.Action;
import net.fameless.core.util.StringUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

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
    protected void executeCommand(CommandCaller caller, String[] args) {
        if (args.length < 2) return;
        if (args[0].equalsIgnoreCase("configure")) {
            AFKHandler afkHandler = BungeeAFK.injector().getInstance(AFKHandler.class);
            switch (args[1]) {
                case "afk-delay" -> {
                    if (args.length < 3) return;
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
                    if (args.length < 3) return;
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
            }
        } else if (args[0].equalsIgnoreCase("lang")) {
            Language newLanguage;
            try {
                newLanguage = Language.ofIdentifier(args[1]);
                Caption.setCurrentLanguage(newLanguage);

                String message = newLanguage.getUpdateMessage();
                message = message.replace("<prefix>", Caption.getString("prefix"));

                caller.sendMessage(MiniMessage.miniMessage().deserialize(message));
            } catch (IllegalArgumentException e) {
                caller.sendMessage(Caption.of("command.invalid_language"));
            }
        }
    }

    @Override
    protected List<String> tabComplete(CommandCaller caller, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.addAll(Arrays.asList("configure", "lang"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("configure")) {
            completions.addAll(Arrays.asList("afk-delay", "action-delay", "action"));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("configure")) {
            if (args[1].equalsIgnoreCase("action")) {
                for (Action action : Action.values()) {
                    completions.add(action.getIdentifier());
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("lang")) {
            for (Language language : Language.values()) {
                completions.add(language.getIdentifier());
            }
        }
        return StringUtil.copyPartialMatches(args[args.length - 1], completions, new ArrayList<>());
    }
}
