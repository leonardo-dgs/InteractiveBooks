package net.leonardo_dgs.interactivebooks;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.HelpCommand;
import co.aikar.commands.annotation.Subcommand;
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
        adventure.sender(sender).sendMessage(message);
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
        adventure.sender(sender).sendMessage(message);
    }

    @Subcommand("open")
    @CommandPermission("interactivebooks.command.open")
    @CommandCompletion("@ibooks @players @nothing")
    void onOpen(CommandSender sender, String[] args) {
        String locale = sender instanceof Player ? (BooksUtils.getLocale((Player) sender)) : settings.getDefaultLanguage();
        if (args.length == 0) {
            adventure.sender(sender).sendMessage(translations.getBookOpenUsage(locale));
            return;
        }
        if (args.length == 1 && !(sender instanceof Player)) {
            adventure.sender(sender).sendMessage(translations.getBookOpenPlayerNotSpecified(locale));
            return;
        }
        Player playerToOpen = args.length == 1 ? (Player) sender : Bukkit.getPlayer(args[1]);
        String bookIdToOpen = BooksUtils.setPlaceholders(args[0], playerToOpen);
        IBook book = getBook(sender, args[0], playerToOpen, locale);
        if (book == null || playerToOpen == null)
            return;

        book.open(playerToOpen);
        if (!playerToOpen.equals(sender))
            adventure.sender(sender).sendMessage(translations.getBookOpenSuccess(locale, Placeholder.unparsed("book", bookIdToOpen), Placeholder.unparsed("player", playerToOpen.getName())));
    }

    @Subcommand("get")
    @CommandPermission("interactivebooks.command.get")
    @CommandCompletion("@ibooks @nothing")
    void onGet(Player player, String[] args) {
        if (args.length == 0) {
            adventure.sender(player).sendMessage(translations.getBookGetUsage(BooksUtils.getLocale(player)));
            return;
        }
        String bookIdToGet = BooksUtils.setPlaceholders(args[0], player);
        IBook book = InteractiveBooks.getBook(bookIdToGet);
        if (book == null) {
            adventure.sender(player).sendMessage(translations.getBookDoesNotExists(BooksUtils.getLocale(player)));
            return;
        }
        player.getInventory().addItem(book.getItem(player));
        adventure.sender(player).sendMessage(translations.getBookGetSuccess(BooksUtils.getLocale(player), Placeholder.unparsed("book_id", bookIdToGet)));
    }

    @Subcommand("give")
    @CommandPermission("interactivebooks.command.give")
    @CommandCompletion("@ibooks @players @nothing")
    void onGive(CommandSender sender, String[] args) {
        String locale = sender instanceof Player ? (BooksUtils.getLocale((Player) sender)) : settings.getDefaultLanguage();
        if (args.length < 2) {
            adventure.sender(sender).sendMessage(translations.getBookGiveUsage(locale));
            return;
        }
        Player targetPlayer = Bukkit.getPlayer(args[1]);
        String targetBookId = BooksUtils.setPlaceholders(args[0], targetPlayer);
        IBook book = getBook(sender, targetBookId, targetPlayer, locale);
        if (book == null || targetPlayer == null)
            return;

        targetPlayer.getInventory().addItem(book.getItem(targetPlayer));
        adventure.sender(sender).sendMessage(translations.getBookGiveSuccess(locale, Placeholder.unparsed("book_id", targetBookId), Placeholder.unparsed("player", args[1])));
        adventure.sender(targetPlayer).sendMessage(translations.getBookReceived(BooksUtils.getLocale(targetPlayer), Placeholder.unparsed("book_id", targetBookId)));
    }

    @Subcommand("create")
    @CommandPermission("interactivebooks.command.create")
    @CommandCompletion("@nothing @nothing @nothing @players @book_generations @nothing")
    void onCreate(CommandSender sender, String[] args) {
        String locale = sender instanceof Player ? (BooksUtils.getLocale((Player) sender)) : settings.getDefaultLanguage();
        if (args.length < 4) {
            adventure.sender(sender).sendMessage(translations.getBookCreateUsage(locale));
            return;
        }
        if (InteractiveBooks.getBook(args[0]) != null) {
            adventure.sender(sender).sendMessage(translations.getBookAlreadyExists(locale));
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
            adventure.sender(sender).sendMessage(translations.getBookCreateInvalidGeneration(locale));
            return;
        }

        IBook createdBook = new IBook(bookId, bookName, bookTitle, bookAuthor, bookGeneration, new ArrayList<>(), new ArrayList<>());
        createdBook.save();
        InteractiveBooks.registerBook(createdBook);
        adventure.sender(sender).sendMessage(translations.getBookCreateSuccess(locale));
    }

    @Subcommand("reload")
    @CommandPermission("interactivebooks.command.reload")
    void onReload(CommandSender sender) {
        String locale = sender instanceof Player ? (BooksUtils.getLocale((Player) sender)) : settings.getDefaultLanguage();
        settings.reload();
        adventure.sender(sender).sendMessage(translations.getReloadSuccess(locale));
    }

    private IBook getBook(CommandSender sender, String bookId, Player player, String locale) {
        IBook book = InteractiveBooks.getBook(bookId);
        if (book == null) {
            adventure.sender(sender).sendMessage(translations.getBookDoesNotExists(locale));
            return null;
        }
        if (player == null) {
            adventure.sender(sender).sendMessage(translations.getPlayerNotConnected(locale));
            return null;
        }
        return book;
    }
}
