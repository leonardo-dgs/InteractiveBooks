package net.leomixer17.interactivebooks;

import me.clip.placeholderapi.PlaceholderAPI;
import net.leomixer17.interactivebooks.util.BooksUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;

public final class CommandIBooks implements CommandExecutor {

    private static final String helpMessage =
            "§6InteractiveBooks §7- §6Commands\n"
                    + "§e/ibooks list\n"
                    + "§e/ibooks open <book-id> [player]\n"
                    + "§e/ibooks get <book-id>\n"
                    + "§e/ibooks give <book-id> <player>\n"
                    + "§e/ibooks create <book-id> <name> <title> <author> [generation]\n"
                    // WIP: Book importing
                    // + "§e/ibooks import <book-id>\n"
                    + "§e/ibooks reload";

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args)
    {
        if (args.length == 0)
        {
            sender.sendMessage(helpMessage);
            return false;
        }

        switch (args[0])
        {
            case "list":
                if (!sender.hasPermission("interactivebooks.command.list"))
                {
                    sender.sendMessage("§4You don't have permission to execute this action.");
                    return false;
                }
                StringBuilder sb = new StringBuilder();
                for (IBook book : InteractiveBooks.getBooks().values())
                    sb.append("§6%book_id%§7, ".replace("%book_id%", book.getId()));
                sender.sendMessage("§eBooks:\n" + (sb.toString().equals("") ? "" : sb.toString().substring(0, sb.toString().length() - 2)));
                break;

            case "open":
                if (!sender.hasPermission("interactivebooks.command.open"))
                {
                    sender.sendMessage("§4You don't have permission to execute this action.");
                    return false;
                }
                if (args.length == 1)
                {
                    sender.sendMessage("§cUsage: §7/ibooks open <book-id> [player]");
                    return false;
                }
                if (args.length == 2 && !(sender instanceof Player))
                {
                    sender.sendMessage("§cIf you execute this command by the console, you need to specify the player's name.");
                    return false;
                }
                Player playerToOpen = args.length == 2 ? (Player) sender : Bukkit.getPlayer(args[2]);
                String bookIdToOpen = BooksUtils.hasPlaceholderAPISupport() ? PlaceholderAPI.setPlaceholders(playerToOpen, args[1]) : args[1];
                if (InteractiveBooks.getBook(bookIdToOpen) == null)
                {
                    sender.sendMessage("§cThat book doesn't exists.");
                    return false;
                }
                if (playerToOpen == null)
                {
                    sender.sendMessage("§cThat player isn't connected.");
                    return false;
                }
                InteractiveBooks.getBook(bookIdToOpen).open(playerToOpen);
                if (!playerToOpen.equals(sender))
                    sender.sendMessage("§aBook §6%book_id% §aopened to §6%player%§a.".replace("%book_id%", bookIdToOpen).replace("%player%", args[2]));
                break;

            case "get":
                if (!sender.hasPermission("interactivebooks.command.get"))
                {
                    sender.sendMessage("§4You don't have permission to execute this action.");
                    return false;
                }
                if (!(sender instanceof Player))
                {
                    sender.sendMessage("§cThat command can only be executed by players.");
                    return false;
                }
                if (args.length == 1)
                {
                    sender.sendMessage("§cUsage: §7/ibooks get <book-id>");
                    return false;
                }
                Player playerToGet = (Player) sender;
                String bookIdToGet = BooksUtils.hasPlaceholderAPISupport() ? PlaceholderAPI.setPlaceholders(playerToGet, args[1]) : args[1];
                if (InteractiveBooks.getBook(bookIdToGet) == null)
                {
                    sender.sendMessage("§cThat book doesn't exists.");
                    return false;
                }
                playerToGet.getInventory().addItem(InteractiveBooks.getBook(bookIdToGet).getItem(playerToGet));
                sender.sendMessage("§aYou have received the book §6%book_id%§a.".replace("%book_id%", bookIdToGet));
                break;

            case "give":
                if (!sender.hasPermission("interactivebooks.command.give"))
                {
                    sender.sendMessage("§4You don't have permission to execute this action.");
                    return false;
                }
                if (args.length < 3)
                {
                    sender.sendMessage("§cUsage: §7/ibooks give <book-id> <player>");
                    return false;
                }
                Player playerToGive = Bukkit.getPlayer(args[2]);
                String bookIdToGive = BooksUtils.hasPlaceholderAPISupport() ? PlaceholderAPI.setPlaceholders(playerToGive, args[1]) : args[1];
                if (InteractiveBooks.getBook(bookIdToGive) == null)
                {
                    sender.sendMessage("§cThat book doesn't exists.");
                    return false;
                }
                if (playerToGive == null)
                {
                    sender.sendMessage("§cThat player isn't connected.");
                    return false;
                }
                playerToGive.getInventory().addItem(InteractiveBooks.getBook(bookIdToGive).getItem(playerToGive));
                sender.sendMessage("§aBook §6%book_id% §agiven to §6%player%§a.".replace("%book_id%", bookIdToGive).replace("%player%", args[2]));
                playerToGive.sendMessage("§aYou have received the book §6%book_id%§a.".replace("%book_id%", bookIdToGive));
                break;

            case "create":
                if (!sender.hasPermission("interactivebooks.command.create"))
                {
                    sender.sendMessage("§4You don't have permission to execute this action.");
                    return false;
                }
                if (args.length < 5)
                {
                    sender.sendMessage("§cUsage: §7/ibooks create <book-id> <name> <title> <author> [generation]");
                    return false;
                }
                if (InteractiveBooks.getBook(args[1]) != null)
                {
                    sender.sendMessage("§cA book with that id already exists");
                    return false;
                }

                String bookId = args[1];
                String bookName = args[2];
                String bookTitle = args[3];
                String bookAuthor = args[4];
                String bookGeneration = "ORIGINAL";
                if (args.length > 5)
                    bookGeneration = args[5].toUpperCase();
                if (BooksUtils.hasBookGenerationSupport() && !bookGeneration.equals("ORIGINAL") && !bookGeneration.equals("COPY_OF_ORIGINAL") && !bookGeneration.equals("COPY_OF_COPY") && !bookGeneration.equals("TATTERED"))
                {
                    sender.sendMessage("§cThe argument supplied as book generation is not valid, possible values: ORIGINAL, COPY_OF_ORIGINAL, COPY_OF_COPY, TATTERED");
                    return false;
                }

                IBook createdBook = new IBook(bookId, bookName, bookTitle, bookAuthor, bookGeneration, new ArrayList<>(), new ArrayList<>());
                createdBook.save();
                InteractiveBooks.registerBook(createdBook);
                sender.sendMessage("§aBook successfully created.");
                break;
            // WIP: Book importing
		/*
		case "import":
			if(!sender.hasPermission("interactivebooks.command.import")) {
				sender.sendMessage("§4You don't have permission to execute this action.");
				return false;
			}
			if(args.length < 2) {
				sender.sendMessage("§cUsage: §7/ibooks import <book-id>");
				return false;
			}
			if(InteractiveBooks.getBook(args[1]) != null) {
				sender.sendMessage("§cA book with that id already exists.");
				return false;
			}
			if(!IBooksUtils.getItemInMainHand((Player) sender).getType().equals(Material.WRITTEN_BOOK)) {
				sender.sendMessage("§cIn order to import a book, you must have it in your hand.");
				return false;
			}
			
			ItemStack itemToImport = IBooksUtils.getItemInMainHand((Player) sender);
			IBook importedBook = new IBook(args[1], (BookMeta) itemToImport.getItemMeta());
			importedBook.save();
			InteractiveBooks.registerBook(importedBook);
			sender.sendMessage("§aBook successfully imported.");
			break;
		*/
            case "reload":
                if (!sender.hasPermission("interactivebooks.command.reload"))
                {
                    sender.sendMessage("§4You don't have permission to execute this action.");
                    return false;
                }
                Config.loadAll();
                sender.sendMessage("§aConfig reloaded!");
                break;

            default:
                sender.sendMessage(helpMessage);
        }

        return false;
    }

}
