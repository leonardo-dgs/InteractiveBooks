package net.leonardo_dgs.interactivebooks;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.HelpCommand;
import co.aikar.commands.annotation.Subcommand;
import net.leonardo_dgs.interactivebooks.util.BooksUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Iterator;

@CommandAlias("interactivebooks|ibooks|ib")
@CommandPermission("interactivebooks.command")
@Description("Manage books")
public final class CommandIBooks extends BaseCommand {
    @HelpCommand
    public void onHelp(CommandSender sender) {
        sender.sendMessage(
                "§6InteractiveBooks §7- §6Commands\n"
                        + "§e/ibooks list\n"
                        + "§e/ibooks open <book-id> [player]\n"
                        + "§e/ibooks get <book-id>\n"
                        + "§e/ibooks give <book-id> <player>\n"
                        + "§e/ibooks create <book-id> <name> <title> <author> [generation]\n"
                        + "§e/ibooks reload"
        );
    }

    @Subcommand("list")
    @CommandPermission("interactivebooks.command.list")
    public void onList(CommandSender sender) {
        StringBuilder sb = new StringBuilder();
        Iterator<String> iterator = InteractiveBooks.getBooks().keySet().iterator();
        boolean hasNext = iterator.hasNext();
        while (hasNext) {
            String bookId = iterator.next();
            sb.append("§6");
            sb.append(bookId);
            hasNext = iterator.hasNext();
            if (hasNext)
                sb.append("§7, ");
        }
        sender.sendMessage("§eBooks:\n" + sb);
    }

    @Subcommand("open")
    @CommandPermission("interactivebooks.command.open")
    @CommandCompletion("@ibooks @players @nothing")
    public void onOpen(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§cUsage: §7/ibooks open <book-id> [player]");
            return;
        }
        if (args.length == 1 && !(sender instanceof Player)) {
            sender.sendMessage("§cIf you execute this command by the console, you need to specify the player's name.");
            return;
        }
        Player playerToOpen = args.length == 1 ? (Player) sender : Bukkit.getPlayer(args[1]);
        String bookIdToOpen = BooksUtils.setPlaceholders(args[0], playerToOpen);
        IBook book = InteractiveBooks.getBook(bookIdToOpen);
        if (book == null) {
            sender.sendMessage("§cThat book doesn't exists.");
            return;
        }
        if (playerToOpen == null) {
            sender.sendMessage("§cThat player isn't connected.");
            return;
        }
        book.open(playerToOpen);
        if (!playerToOpen.equals(sender))
            sender.sendMessage(String.format("§aBook §6%s §aopened to §6%s§a.", bookIdToOpen, playerToOpen.getName()));
    }

    @Subcommand("get")
    @CommandPermission("interactivebooks.command.get")
    @CommandCompletion("@ibooks @nothing")
    public void onGet(Player player, String[] args) {
        if (args.length == 0) {
            player.sendMessage("§cUsage: §7/ibooks get <book-id>");
            return;
        }
        String bookIdToGet = BooksUtils.setPlaceholders(args[0], player);
        if (InteractiveBooks.getBook(bookIdToGet) == null) {
            player.sendMessage("§cThat book doesn't exists.");
            return;
        }
        player.getInventory().addItem(InteractiveBooks.getBook(bookIdToGet).getItem(player));
        player.sendMessage("§aYou have received the book §6%book_id%§a.".replace("%book_id%", bookIdToGet));
    }

    @Subcommand("give")
    @CommandPermission("interactivebooks.command.give")
    @CommandCompletion("@ibooks @players @nothing")
    public void onGive(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: §7/ibooks give <book-id> <player>");
            return;
        }
        Player targetPlayer = Bukkit.getPlayer(args[1]);
        String targetBookId = BooksUtils.setPlaceholders(args[0], targetPlayer);
        IBook book = InteractiveBooks.getBook(targetBookId);
        if (book == null) {
            sender.sendMessage("§cThat book doesn't exists.");
            return;
        }
        if (targetPlayer == null) {
            sender.sendMessage("§cThat player isn't connected.");
            return;
        }

        targetPlayer.getInventory().addItem(book.getItem(targetPlayer));
        sender.sendMessage("§aBook §6%book_id% §agiven to §6%player%§a.".replace("%book_id%", targetBookId).replace("%player%", args[1]));
        targetPlayer.sendMessage("§aYou have received the book §6%book_id%§a.".replace("%book_id%", targetBookId));
    }

    @Subcommand("create")
    @CommandPermission("interactivebooks.command.create")
    @CommandCompletion("@nothing @nothing @nothing @players @book_generations @nothing")
    public void onCreate(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cUsage: §7/ibooks create <book-id> <name> <title> <author> [generation]");
            return;
        }
        if (InteractiveBooks.getBook(args[0]) != null) {
            sender.sendMessage("§cA book with that id already exists");
            return;
        }

        String bookId = args[0];
        String bookName = args[1];
        String bookTitle = args[2];
        String bookAuthor = args[3];
        String bookGeneration = "ORIGINAL";
        if (args.length > 4)
            bookGeneration = args[4].toUpperCase();
        if (BooksUtils.isBookGenerationSupported()) {
            switch (bookGeneration) {
                case "ORIGINAL":
                case "COPY_OF_ORIGINAL":
                case "COPY_OF_COPY":
                case "TATTERED":
                    break;
                default:
                    sender.sendMessage("§cThe argument supplied as book generation is not valid, possible values: ORIGINAL, COPY_OF_ORIGINAL, COPY_OF_COPY, TATTERED");
                    return;
            }
        }

        IBook createdBook = new IBook(bookId, bookName, bookTitle, bookAuthor, bookGeneration, new ArrayList<>(), new ArrayList<>());
        createdBook.save();
        InteractiveBooks.registerBook(createdBook);
        sender.sendMessage("§aBook successfully created.");
    }

    @Subcommand("reload")
    @CommandPermission("interactivebooks.command.reload")
    public void onReload(CommandSender sender) {
        ConfigManager.loadAll();
        sender.sendMessage("§aConfig reloaded!");
    }
}
