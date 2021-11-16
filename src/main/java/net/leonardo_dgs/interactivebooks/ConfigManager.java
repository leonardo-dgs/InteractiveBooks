package net.leonardo_dgs.interactivebooks;

import de.leonhard.storage.Config;
import de.leonhard.storage.LightningBuilder;
import de.leonhard.storage.internal.FlatFile;
import de.leonhard.storage.internal.settings.ReloadSettings;
import de.leonhard.storage.sections.FlatFileSection;
import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import static net.leonardo_dgs.interactivebooks.InteractiveBooks.getInstance;

final class ConfigManager {

    @Getter
    private static Config config;

    static void loadAll() {
        config = LightningBuilder.fromFile(new File(getInstance().getDataFolder().getPath(), "config.yml"))
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
                Config bookConfig = LightningBuilder.fromFile(bookFile)
                        .setReloadSettings(ReloadSettings.INTELLIGENT)
                        .createConfig();
                registerBook(bookId, bookConfig);
            }
        }
    }

    private static void registerBook(String id, FlatFile bookConfig) {
        InteractiveBooks.registerBook(new IBook(id, bookConfig.getString("name"), bookConfig.getString("title"), bookConfig.getString("author"),
                bookConfig.getString("generation"), bookConfig.getStringList("lore"), mergeLines(bookConfig.getSection("pages")),
                (bookConfig.getString("open_command") == null || bookConfig.getString("open_command").equals("")) ? null : bookConfig.getString("open_command").split(" ")));
    }

    private static List<String> mergeLines(FlatFileSection section) {
        List<String> pages = new ArrayList<>();
        if (section != null) {
            section.singleLayerKeySet().forEach(key -> {
                StringBuilder sb = new StringBuilder();
                section.getStringList(key).forEach(line -> sb.append("\n").append(line));
                pages.add(sb.toString().replaceFirst("\n", ""));
            });
        }
        return pages;
    }
}
