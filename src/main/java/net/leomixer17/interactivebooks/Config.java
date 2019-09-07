package net.leomixer17.interactivebooks;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

final class Config {

    static final void loadAll()
    {
        InteractiveBooks.getPlugin().saveDefaultConfig();
        final File f = new File(InteractiveBooks.getPlugin().getDataFolder(), "books");
        if (!f.exists())
        {
            f.mkdirs();
            try
            {
                Files.copy(InteractiveBooks.getPlugin().getResource("resources/examplebook.yml"), new File(f, "resources/examplebook.yml").toPath());
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        loadBookConfigs();
    }

    static final void loadBookConfigs()
    {
        InteractiveBooks.getBooks().keySet().forEach(id -> InteractiveBooks.unregisterBook(id));
        final File booksFolder = new File(InteractiveBooks.getPlugin().getDataFolder(), "books");
        for (final File f : booksFolder.listFiles())
            if (f.getName().endsWith(".yml"))
                InteractiveBooks.registerBook(new IBook(f.getName().substring(0, f.getName().length() - 4), YamlConfiguration.loadConfiguration(f)));
    }
}
