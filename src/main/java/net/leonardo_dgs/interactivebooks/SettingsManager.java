package net.leonardo_dgs.interactivebooks;

import de.leonhard.storage.Config;
import de.leonhard.storage.SimplixBuilder;
import de.leonhard.storage.internal.settings.ReloadSettings;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

final class SettingsManager {
    private final Config config;

    SettingsManager(File configFile, String resource) {
        config = SimplixBuilder.fromFile(configFile)
                .addInputStreamFromResource(resource)
                .setReloadSettings(ReloadSettings.INTELLIGENT)
                .createConfig();
        reload();
    }

    void reload() {
        config.forceReload();

        for (String key : new HashSet<>(InteractiveBooks.getBooks().keySet()))
            InteractiveBooks.unregisterBook(key);

        File booksFolder = new File(InteractiveBooks.getInstance().getDataFolder(), "books");
        boolean generateExampleBook = !booksFolder.exists();
        if (!booksFolder.exists() && !booksFolder.mkdirs())
            throw new RuntimeException();

        for (File bookFile : Objects.requireNonNull(booksFolder.listFiles())) {
            if (bookFile.getName().endsWith(".yml")) {
                String bookId = bookFile.getName().substring(0, bookFile.getName().length() - 4);
                loadBookConfig(bookFile, bookId).createConfig();
            }
        }
        if (generateExampleBook) {
            File exampleBookFile = new File(booksFolder, "example_book.yml");
            loadBookConfig(exampleBookFile, "example_book").addInputStreamFromResource("example_book.yml").createConfig();
        }
    }

    private SimplixBuilder loadBookConfig(File file, String bookId) {
        return SimplixBuilder.fromFile(file)
                .setReloadSettings(ReloadSettings.INTELLIGENT)
                .reloadCallback(config -> {
                    InteractiveBooks.unregisterBook(bookId);
                    InteractiveBooks.registerBook(new IBook(bookId, config));
                });
    }

    String getDefaultLanguage() {
        return config.getString("default_language");
    }

    boolean getPerPlayerLanguage() {
        return config.getBoolean("per_player_language");
    }

    boolean getUpdateBooksOnUse() {
        return config.getBoolean("update_books_on_use");
    }

    String getOpenBookOnJoin() {
        return config.getString("open_book_on_join");
    }

    String getOpenBookOnFirstJoin() {
        return config.getString("open_book_on_first_join");
    }

    List<String> getBooksOnJoin() {
        Object raw = config.get("books_on_join");
        if (raw instanceof String) {
            ArrayList<String> list = new ArrayList<>(1);
            list.add((String) raw);
            return list;
        } else {
            return config.getStringList("books_on_join");
        }
    }

    List<String> getBooksOnFirstJoin() {
        Object raw = config.get("books_on_first_join");
        if (raw instanceof String) {
            ArrayList<String> list = new ArrayList<>(1);
            list.add((String) raw);
            return list;
        } else {
            return config.getStringList("books_on_first_join");
        }
    }
}
