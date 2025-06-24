package me.loule.tntfireworks;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CommandTabCompleter implements TabCompleter {

    private final List<String> subCommands = Arrays.asList("reload");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("tntfireworks")) {
            if (args.length == 1) {
                // Filter subcommands based on permissions and what the user has typed so far
                return subCommands.stream()
                        .filter(subCmd -> subCmd.startsWith(args[0].toLowerCase()))
                        .filter(subCmd -> !subCmd.equals("reload") || sender.hasPermission("tntfireworks.reload"))
                        .collect(Collectors.toList());
            }
        }
        return new ArrayList<>();
    }
}
