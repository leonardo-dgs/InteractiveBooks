package net.leonardo_dgs.interactivebooks;

import co.aikar.commands.CommandReplacements;
import co.aikar.commands.PaperCommandManager;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.leonardo_dgs.interactivebooks.util.MinecraftVersion;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public final class InteractiveBooks extends JavaPlugin {
    private static InteractiveBooks instance;
    private static final Map<String, IBook> books = new HashMap<>();
    private BukkitAudiences adventure;
    private static SettingsManager settings;
    private static TranslationsManager translations;
    private static PaperCommandManager commandManager;

    @Override
    public void onEnable() {
        instance = this;
        if (MinecraftVersion.getRunningVersion().isBefore(MinecraftVersion.parse("1.8.8"))) {
            getLogger().log(Level.WARNING, "This Minecraft version is not supported, please use 1.8.8 or newer");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        adventure = BukkitAudiences.create(this);
        initCommandManager();
        settings = new SettingsManager(new File(getDataFolder(), "config.yml"), "config.yml");
        translations = new TranslationsManager(new File(getDataFolder(), "translations"), settings);
        commandManager.registerCommand(new CommandIBooks(adventure, settings, translations));
        de.tr7zw.changeme.nbtapi.utils.MinecraftVersion.replaceLogger(getLogger());
        de.tr7zw.changeme.nbtapi.utils.MinecraftVersion.disableUpdateCheck();
        de.tr7zw.changeme.nbtapi.utils.MinecraftVersion.getVersion();
        Bukkit.getPluginManager().registerEvents(new PlayerListener(), this);
        new Metrics(this, 5483);
    }

    @Override
    public void onDisable() {
        if (this.adventure != null) {
            this.adventure.close();
            this.adventure = null;
        }
        instance = null;
    }

    /**
     * Gets the instance of this plugin.
     *
     * @return an instance of the plugin
     */
    public static InteractiveBooks getInstance() {
        return instance;
    }

    /**
     * Gets the registered books.
     *
     * @return a {@link Map} with book ids as keys and the registered books ({@link IBook}) as values
     */
    public static Map<String, IBook> getBooks() {
        return Collections.unmodifiableMap(books);
    }

    /**
     * Gets an {@link IBook} by its id.
     *
     * @param id the id of the book to get
     * @return the book with the specified id if it's registered, or null if not found
     * @see #registerBook(IBook)
     */
    public static IBook getBook(String id) {
        return books.get(id);
    }

    /**
     * Registers a book.
     *
     * @param book the book id to register
     */
    public static void registerBook(IBook book) {
        if (!book.getOpenCommands().isEmpty()) {
            CommandReplacements replacements = commandManager.getCommandReplacements();
            replacements.addReplacement("openbook", String.join("|", book.getOpenCommands()));
            replacements.addReplacement("interactivebooks.open", "interactivebooks.open." + book.getId());
            CommandOpenBook command = new CommandOpenBook(book);
            commandManager.registerCommand(command);
            book.setCommandExecutor(command);
        }
        books.put(book.getId(), book);
    }

    /**
     * Unregisters a book by its id.
     *
     * @param id the book id to unregister
     */
    public static void unregisterBook(String id) {
        IBook book = getBook(id);
        if (book != null) {
            if (book.getCommandExecutor() != null)
                commandManager.unregisterCommand(book.getCommandExecutor());
            books.remove(id);
        }
    }

    public BukkitAudiences adventure() {
        return adventure;
    }

    static SettingsManager getSettings() {
        return settings;
    }

    static TranslationsManager getTranslations() {
        return translations;
    }

    private void initCommandManager() {
        commandManager = new PaperCommandManager(this);
        commandManager.getCommandCompletions().registerCompletion("ibooks", handler -> getBooks().keySet());
        commandManager.getCommandCompletions().registerStaticCompletion("book_generations", new String[]{"original", "copy_of_original", "copy_of_copy", "tattered"});
    }
}
