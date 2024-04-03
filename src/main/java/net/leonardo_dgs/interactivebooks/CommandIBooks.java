package net.leonardo_dgs.interactivebooks;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.HelpCommand;
import co.aikar.commands.annotation.Subcommand;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Iterator;

@CommandAlias("interactivebooks|ibooks|ib")
@CommandPermission("interactivebooks.command")
@Description("Manage books")
final class CommandIBooks extends BaseCommand {
    private final BukkitAudiences adventure;
    private final SettingsManager settings;
    private final TranslationsManager translations;

    CommandIBooks(BukkitAudiences adventure, SettingsManager settings, TranslationsManager translations) {
        this.adventure = adventure;
        this.settings = settings;
        this.translations = translations;
    }

    @HelpCommand
    void onHelp(CommandSender sender) {
        String locale = sender instanceof Player ? (BooksUtils.getLocale((Player) sender)) : settings.getDefaultLanguage();
        String version = InteractiveBooks.getInstance().getDescription().getVersion();
        Component message = translations.getHelpHeader(locale, Placeholder.unparsed("version", version)).appendNewline();
        message = message.append(translations.getHelpList(locale)).appendNewline();
        message = message.append(translations.getHelpOpen(locale)).appendNewline();
        message = message.append(translations.getHelpGet(locale)).appendNewline();
        message = message.append(translations.getHelpGive(locale)).appendNewline();
        message = message.append(translations.getHelpCreate(locale)).appendNewline();
        message = message.append(translations.getHelpReload(locale));
        sendMessage(adventure.sender(sender), message);
    }

    @Subcommand("list")
    @CommandPermission("interactivebooks.command.list")
    void onList(CommandSender sender) {
        String locale = sender instanceof Player ? (BooksUtils.getLocale((Player) sender)) : settings.getDefaultLanguage();
        Component message = translations.getBookListHeader(locale);
        Iterator<String> iterator = InteractiveBooks.getBooks().keySet().iterator();
        boolean hasNext = iterator.hasNext();
        while (hasNext) {
            String bookId = iterator.next();
            message = message.append(translations.getBookList(locale, Placeholder.unparsed("book", bookId)));
            hasNext = iterator.hasNext();
            if (hasNext)
                message = message.append(translations.getBookListSeparator(locale));
        }
        sendMessage(adventure.sender(sender), message);
    }

    @Subcommand("open")
    @CommandPermission("interactivebooks.command.open")
    @CommandCompletion("@ibooks @players @nothing")
    void onOpen(CommandSender sender, String[] args) {
        String locale = sender instanceof Player ? (BooksUtils.getLocale((Player) sender)) : settings.getDefaultLanguage();
        if (args.length == 0) {
            sendMessage(adventure.sender(sender), translations.getBookOpenUsage(locale));
            return;
        }
        if (args.length == 1 && !(sender instanceof Player)) {
            sendMessage(adventure.sender(sender), translations.getBookOpenPlayerNotSpecified(locale));
            return;
        }
        Player playerToOpen = args.length == 1 ? (Player) sender : Bukkit.getPlayer(args[1]);
        String bookIdToOpen = BooksUtils.setPlaceholders(args[0], playerToOpen);
        IBook book = getBook(sender, args[0], playerToOpen, locale);
        if (book == null || playerToOpen == null)
            return;

        book.open(playerToOpen);
        if (!playerToOpen.equals(sender))
            sendMessage(adventure.sender(sender), translations.getBookOpenSuccess(locale, Placeholder.unparsed("book", bookIdToOpen), Placeholder.unparsed("player", playerToOpen.getName())));
    }

    @Subcommand("get")
    @CommandPermission("interactivebooks.command.get")
    @CommandCompletion("@ibooks @nothing")
    void onGet(Player player, String[] args) {
        if (args.length == 0) {
            sendMessage(adventure.sender(player), translations.getBookGetUsage(BooksUtils.getLocale(player)));
            return;
        }
        String bookIdToGet = BooksUtils.setPlaceholders(args[0], player);
        IBook book = InteractiveBooks.getBook(bookIdToGet);
        if (book == null) {
            sendMessage(adventure.sender(player), translations.getBookDoesNotExists(BooksUtils.getLocale(player)));
            return;
        }
        player.getInventory().addItem(book.getItem(player));
        sendMessage(adventure.sender(player), translations.getBookGetSuccess(BooksUtils.getLocale(player), Placeholder.unparsed("book_id", bookIdToGet)));
    }

    @Subcommand("give")
    @CommandPermission("interactivebooks.command.give")
    @CommandCompletion("@ibooks @players @nothing")
    void onGive(CommandSender sender, String[] args) {
        String locale = sender instanceof Player ? (BooksUtils.getLocale((Player) sender)) : settings.getDefaultLanguage();
        if (args.length < 2) {
            sendMessage(adventure.sender(sender), translations.getBookGiveUsage(locale));
            return;
        }
        Player targetPlayer = Bukkit.getPlayer(args[1]);
        String targetBookId = BooksUtils.setPlaceholders(args[0], targetPlayer);
        IBook book = getBook(sender, targetBookId, targetPlayer, locale);
        if (book == null || targetPlayer == null)
            return;

        targetPlayer.getInventory().addItem(book.getItem(targetPlayer));
        sendMessage(adventure.sender(sender), translations.getBookGiveSuccess(locale, Placeholder.unparsed("book_id", targetBookId), Placeholder.unparsed("player", args[1])));
        sendMessage(adventure.sender(targetPlayer), translations.getBookReceived(BooksUtils.getLocale(targetPlayer), Placeholder.unparsed("book_id", targetBookId)));
    }

    @Subcommand("create")
    @CommandPermission("interactivebooks.command.create")
    @CommandCompletion("@nothing @nothing @nothing @players @book_generations @nothing")
    void onCreate(CommandSender sender, String[] args) {
        String locale = sender instanceof Player ? (BooksUtils.getLocale((Player) sender)) : settings.getDefaultLanguage();
        if (args.length < 4) {
            sendMessage(adventure.sender(sender), translations.getBookCreateUsage(locale));
            return;
        }
        if (InteractiveBooks.getBook(args[0]) != null) {
            sendMessage(adventure.sender(sender), translations.getBookAlreadyExists(locale));
            return;
        }

        String bookId = args[0];
        String bookName = args[1];
        String bookTitle = args[2];
        String bookAuthor = args[3];
        String bookGeneration = null;
        if (args.length > 4)
            bookGeneration = args[4];
        if (BooksUtils.isBookGenerationSupported() && BooksUtils.getBookGeneration(bookGeneration) == null) {
            sendMessage(adventure.sender(sender), translations.getBookCreateInvalidGeneration(locale));
            return;
        }

        IBook createdBook = new IBook(bookId, bookName, bookTitle, bookAuthor, bookGeneration, new ArrayList<>(), new ArrayList<>());
        createdBook.save();
        InteractiveBooks.registerBook(createdBook);
        sendMessage(adventure.sender(sender), translations.getBookCreateSuccess(locale));
    }

    @Subcommand("reload")
    @CommandPermission("interactivebooks.command.reload")
    void onReload(CommandSender sender) {
        String locale = sender instanceof Player ? (BooksUtils.getLocale((Player) sender)) : settings.getDefaultLanguage();
        settings.reload();
        sendMessage(adventure.sender(sender), translations.getReloadSuccess(locale));
    }

    private IBook getBook(CommandSender sender, String bookId, Player player, String locale) {
        IBook book = InteractiveBooks.getBook(bookId);
        if (book == null) {
            sendMessage(adventure.sender(sender), translations.getBookDoesNotExists(locale));
            return null;
        }
        if (player == null) {
            sendMessage(adventure.sender(sender), translations.getPlayerNotConnected(locale));
            return null;
        }
        return book;
    }

    private void sendMessage(Audience audience, Component message) {
        if (message != null)
            audience.sendMessage(message);
    }
}
