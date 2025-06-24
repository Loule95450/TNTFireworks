package me.loule.tntfireworks;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CommandTabCompleter implements TabCompleter {

    private final List<String> subCommands = Arrays.asList("reload", "update", "check");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("tntfireworks")) {
            if (args.length == 1) {
                // Filter subcommands based on permissions and what the user has typed so far
                return subCommands.stream()
                        .filter(subCmd -> subCmd.startsWith(args[0].toLowerCase()))
                        .filter(subCmd ->
                            (subCmd.equals("reload") && sender.hasPermission("tntfireworks.reload")) ||
                            ((subCmd.equals("update") || subCmd.equals("check")) && sender.hasPermission("tntfireworks.update"))
                        )
                        .collect(Collectors.toList());
            } else if (args.length == 2 && args[0].equalsIgnoreCase("update")) {
                if ("confirm".startsWith(args[1].toLowerCase()) && sender.hasPermission("tntfireworks.update")) {
                    return Arrays.asList("confirm");
                }
            }
        }
        return new ArrayList<>();
    }
}
