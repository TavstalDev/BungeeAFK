package net.fameless.velocity;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.RawCommand;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import net.fameless.core.command.framework.Command;
import net.fameless.core.command.framework.CommandCaller;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class VelocityCommandHandler implements RawCommand {

    @Override
    public void execute(@NotNull Invocation invocation) {
        CommandCaller caller = createCaller(invocation.source());
        String commandName = invocation.alias();
        String[] args = invocation.arguments().trim().isEmpty() ? new String[0] : invocation.arguments().split(" ");
        Command.execute(commandName, caller, args);
    }

    @Override
    public List<String> suggest(@NotNull Invocation invocation) {
        CommandCaller caller = createCaller(invocation.source());
        String rawArgs = invocation.arguments();
        String[] args = rawArgs.isEmpty() ? new String[]{""} : rawArgs.split(" ", -1);
        return Command.tabComplete(invocation.alias(), caller, args);
    }

    private @NotNull CommandCaller createCaller(CommandSource sender) {
        if (sender instanceof Player player) {
            return VelocityPlayer.adapt(player);
        } else if (sender instanceof ConsoleCommandSource) {
            return VelocityConsoleCommandCaller.get();
        }
        throw new RuntimeException("Unknown sender type: " + sender.getClass());
    }

}
