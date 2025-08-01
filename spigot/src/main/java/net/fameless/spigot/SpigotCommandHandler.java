package net.fameless.spigot;

import net.fameless.core.command.framework.CommandCaller;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SpigotCommandHandler implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        CommandCaller caller = createCaller(sender);
        net.fameless.core.command.framework.Command.execute(command.getName(), caller, args);
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        CommandCaller caller = createCaller(sender);
        return net.fameless.core.command.framework.Command.tabComplete(command.getName(), caller, args);
    }

    private @NotNull CommandCaller createCaller(CommandSender sender) {
        if (sender instanceof Player player) {
            return SpigotPlayer.adapt(player);
        } else if (sender instanceof ConsoleCommandSender) {
            return SpigotConsoleCommandCaller.get();
        }
        throw new RuntimeException("Unknown sender type: " + sender.getClass());
    }

}
