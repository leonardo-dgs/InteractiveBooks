package net.leonardo_dgs.interactivebooks;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Objects;

final class Config {

    static void loadAll() {
        InteractiveBooks.getInstance().saveDefaultConfig();
        InteractiveBooks.getInstance().reloadConfig();
        File f = new File(InteractiveBooks.getInstance().getDataFolder(), "books");
        if (!f.exists()) {
            try {
                if (!f.mkdirs())
                    throw new IOException();
                Files.copy(Objects.requireNonNull(InteractiveBooks.getInstance().getResource("examplebook.yml")), new File(f, "examplebook.yml").toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        loadBookConfigs();
    }

    private static void loadBookConfigs() {
        for(String key : new HashSet<>(InteractiveBooks.getBooks().keySet()))
            InteractiveBooks.unregisterBook(key);
        File booksFolder = new File(InteractiveBooks.getInstance().getDataFolder(), "books");
        for (File f : Objects.requireNonNull(booksFolder.listFiles()))
            if (f.getName().endsWith(".yml"))
                InteractiveBooks.registerBook(new IBook(f.getName().substring(0, f.getName().length() - 4), YamlConfiguration.loadConfiguration(f)));
    }
}
