package net.leonardo_dgs.interactivebooks;

import de.leonhard.storage.Config;
import de.leonhard.storage.SimplixBuilder;
import de.leonhard.storage.internal.settings.ReloadSettings;
import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Objects;

import static net.leonardo_dgs.interactivebooks.InteractiveBooks.getInstance;

final class ConfigManager {
    @Getter
    private static Config config;

    static void loadAll() {
        config = SimplixBuilder.fromFile(new File(getInstance().getDataFolder().getPath(), "config.yml"))
                .setReloadSettings(ReloadSettings.INTELLIGENT)
                .addInputStreamFromResource("config.yml")
                .createConfig();
        loadBookConfigs();
    }

    private static void loadBookConfigs() {
        for (String key : new HashSet<>(InteractiveBooks.getBooks().keySet()))
            InteractiveBooks.unregisterBook(key);

        File booksFolder = new File(getInstance().getDataFolder(), "books");
        if (!booksFolder.exists()) {
            try {
                if (!booksFolder.mkdirs())
                    throw new IOException();
                Files.copy(Objects.requireNonNull(getInstance().getResource("example_book.yml")), new File(booksFolder, "example_book.yml").toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        for (File bookFile : Objects.requireNonNull(booksFolder.listFiles())) {
            if (bookFile.getName().endsWith(".yml")) {
                String bookId = bookFile.getName().substring(0, bookFile.getName().length() - 4);
                SimplixBuilder.fromFile(bookFile)
                        .setReloadSettings(ReloadSettings.INTELLIGENT)
                        .reloadCallback(flatFile -> {
                            InteractiveBooks.unregisterBook(bookId);
                            InteractiveBooks.registerBook(new IBook(bookId, flatFile));
                        })
                        .createConfig();
            }
        }
    }

    private ConfigManager() {

    }
}
