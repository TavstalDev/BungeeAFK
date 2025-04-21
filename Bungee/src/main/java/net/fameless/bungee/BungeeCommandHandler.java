package net.fameless.bungee;

import net.fameless.core.command.framework.CommandCaller;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.TabExecutor;

public class BungeeCommandHandler extends net.md_5.bungee.api.plugin.Command implements TabExecutor {

    public BungeeCommandHandler(String name) {
        super(name);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        CommandCaller caller = createCaller(sender);
        net.fameless.core.command.framework.Command.execute(getName(), caller, args);
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        CommandCaller caller = createCaller(sender);
        return net.fameless.core.command.framework.Command.tabComplete(getName(), caller, args);
    }

    private CommandCaller createCaller(CommandSender sender) {
        if (sender instanceof net.md_5.bungee.api.connection.ProxiedPlayer player) {
            return BungeePlayer.adapt(player);
        } else {
            return BungeeConsoleCommandCaller.get();
        }
    }

}
