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
    private final BukkitAudiences adventure;
    private final SettingsManager settings;
    private final TranslationsManager translations;

    public CommandIBooks(BukkitAudiences adventure, SettingsManager settings, TranslationsManager translations) {
        this.adventure = adventure;
        this.settings = settings;
        this.translations = translations;
    }

    @HelpCommand
    public void onHelp(CommandSender sender) {
        String locale = sender instanceof Player ? ((Player) sender).getLocale() : settings.getDefaultLanguage();
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
    public void onList(CommandSender sender) {
        String locale = sender instanceof Player ? ((Player) sender).getLocale() : settings.getDefaultLanguage();
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
    public void onOpen(CommandSender sender, String[] args) {
        String locale = sender instanceof Player ? ((Player) sender).getLocale() : settings.getDefaultLanguage();
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
        IBook book = InteractiveBooks.getBook(bookIdToOpen);
        if (book == null) {
            adventure.sender(sender).sendMessage(translations.getBookDoesNotExists(locale));
            return;
        }
        if (playerToOpen == null) {
            adventure.sender(sender).sendMessage(translations.getPlayerNotConnected(locale));
            return;
        }
        book.open(playerToOpen);
        if (!playerToOpen.equals(sender))
            adventure.sender(sender).sendMessage(translations.getBookOpenSuccess(locale, Placeholder.unparsed("book", bookIdToOpen), Placeholder.unparsed("player", playerToOpen.getName())));
    }

    @Subcommand("get")
    @CommandPermission("interactivebooks.command.get")
    @CommandCompletion("@ibooks @nothing")
    public void onGet(Player player, String[] args) {
        if (args.length == 0) {
            adventure.sender(player).sendMessage(translations.getBookGetUsage(player.getLocale()));
            return;
        }
        String bookIdToGet = BooksUtils.setPlaceholders(args[0], player);
        IBook book = InteractiveBooks.getBook(bookIdToGet);
        if (book == null) {
            adventure.sender(player).sendMessage(translations.getBookDoesNotExists(player.getLocale()));
            return;
        }
        player.getInventory().addItem(book.getItem(player));
        adventure.sender(player).sendMessage(translations.getBookGetSuccess(player.getLocale(), Placeholder.unparsed("book_id", bookIdToGet)));
    }

    @Subcommand("give")
    @CommandPermission("interactivebooks.command.give")
    @CommandCompletion("@ibooks @players @nothing")
    public void onGive(CommandSender sender, String[] args) {
        String locale = sender instanceof Player ? ((Player) sender).getLocale() : settings.getDefaultLanguage();
        if (args.length < 2) {
            adventure.sender(sender).sendMessage(translations.getBookGiveUsage(locale));
            return;
        }
        Player targetPlayer = Bukkit.getPlayer(args[1]);
        String targetBookId = BooksUtils.setPlaceholders(args[0], targetPlayer);
        IBook book = InteractiveBooks.getBook(targetBookId);
        if (book == null) {
            adventure.sender(sender).sendMessage(translations.getBookDoesNotExists(locale));
            return;
        }
        if (targetPlayer == null) {
            adventure.sender(sender).sendMessage(translations.getPlayerNotConnected(locale));
            return;
        }

        targetPlayer.getInventory().addItem(book.getItem(targetPlayer));
        adventure.sender(sender).sendMessage(translations.getBookGiveSuccess(locale, Placeholder.unparsed("book_id", targetBookId), Placeholder.unparsed("player", args[1])));
        adventure.sender(targetPlayer).sendMessage(translations.getBookReceived(targetPlayer.getLocale(), Placeholder.unparsed("book_id", targetBookId)));
    }

    @Subcommand("create")
    @CommandPermission("interactivebooks.command.create")
    @CommandCompletion("@nothing @nothing @nothing @players @book_generations @nothing")
    public void onCreate(CommandSender sender, String[] args) {
        String locale = sender instanceof Player ? ((Player) sender).getLocale() : settings.getDefaultLanguage();
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
    public void onReload(CommandSender sender) {
        String locale = sender instanceof Player ? ((Player) sender).getLocale() : settings.getDefaultLanguage();
        settings.reload();
        adventure.sender(sender).sendMessage(translations.getReloadSuccess(locale));
    }
}
