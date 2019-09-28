package net.leonardo_dgs.interactivebooks;

import net.leonardo_dgs.interactivebooks.util.BooksUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.BookMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class TabCompleterIBooks implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args)
    {
        if (args.length == 0)
            return null;

        Player player = sender instanceof Player ? (Player) sender : null;
        List<String> completions = new ArrayList<>();
        if ("list".startsWith(args[0].toLowerCase()) && sender.hasPermission("interactivebooks.command.list"))
        {
            if (args.length == 1)
                completions.add("list");
        }
        if ("open".startsWith(args[0].toLowerCase()) && sender.hasPermission("interactivebooks.command.open"))
        {
            if (args.length == 1)
            {
                completions.add("open");
            }
            else if (args.length == 2)
            {
                Set<String> bookCompletions = InteractiveBooks.getBooks().keySet().stream().filter(bookId -> bookId.toLowerCase().startsWith(args[1].toLowerCase())).collect(Collectors.toSet());
                completions.addAll(bookCompletions);
            }
            else if (args.length == 3)
            {
                Set<Player> players = Bukkit.getOnlinePlayers().stream().filter(completionPlayer -> completionPlayer.getName().toLowerCase().startsWith(args[2].toLowerCase())).collect(Collectors.toSet());
                players.forEach(completionPlayer ->
                {
                    if (player == null || player.canSee(completionPlayer))
                        completions.add(completionPlayer.getName());
                });
            }
        }
        if ("get".startsWith(args[0].toLowerCase()) && sender.hasPermission("interactivebooks.command.get"))
        {
            if (args.length == 1)
            {
                completions.add("get");
            }
            else if (args.length == 2)
            {
                Set<String> bookCompletions = InteractiveBooks.getBooks().keySet().stream().filter(bookId -> bookId.toLowerCase().startsWith(args[1].toLowerCase())).collect(Collectors.toSet());
                completions.addAll(bookCompletions);
            }
        }
        if ("give".startsWith(args[0].toLowerCase()) && sender.hasPermission("interactivebooks.command.give"))
        {
            if (args.length == 1)
            {
                completions.add("give");
            }
            else if (args.length == 2)
            {
                Set<String> bookCompletions = InteractiveBooks.getBooks().keySet().stream().filter(bookId -> bookId.toLowerCase().startsWith(args[1].toLowerCase())).collect(Collectors.toSet());
                completions.addAll(bookCompletions);
            }
            else if (args.length == 3)
            {
                Set<Player> players = Bukkit.getOnlinePlayers().stream().filter(completionPlayer -> completionPlayer.getName().toLowerCase().startsWith(args[2].toLowerCase())).collect(Collectors.toSet());
                players.forEach(completionPlayer ->
                {
                    if (player == null || player.canSee(completionPlayer))
                        completions.add(completionPlayer.getName());
                });
            }
        }
        if ("create".startsWith(args[0].toLowerCase()) && sender.hasPermission("interactivebooks.command.create"))
        {
            if (args.length == 1)
            {
                completions.add("create");
            }
            else if (args.length == 5 && BooksUtils.hasBookGenerationSupport())
            {
                for (BookMeta.Generation bookGeneration : BookMeta.Generation.values())
                {
                    if (bookGeneration.toString().toLowerCase().startsWith(args[4].toLowerCase()))
                        completions.add(bookGeneration.toString());
                }
            }
        }
        if ("reload".startsWith(args[0].toLowerCase()) && sender.hasPermission("interactivebooks.command.reload"))
        {
            if (args.length == 1)
                completions.add("reload");
        }

        return completions;
    }

}
