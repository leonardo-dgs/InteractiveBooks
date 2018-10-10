package net.leomixer17.interactivebooks;

import net.leomixer17.interactivebooks.lib.nbtapi.NBTItem;
import net.leomixer17.interactivebooks.nms.IBooksUtils;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.BookMeta.Generation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class IBook {
    
    private static final String bookIdKey = "InteractiveBooks|Book-Id";
    
    private String id;
    private BookMeta bookMeta;
    private List<String> pages;
    
    private final Set<String> openCommands = new HashSet<>();
    
    public IBook(final String id, final FileConfiguration bookConfig)
    {
        this(id, bookConfig.getString("name"), bookConfig.getString("title"), bookConfig.getString("author"), bookConfig.getString("generation"),
                bookConfig.getStringList("lore"), mergeLines(bookConfig.getConfigurationSection("pages")),
                (bookConfig.getString("open_command") == null || bookConfig.getString("open_command").equals("") ? null : bookConfig.getString("open_command").split(" ")));
    }
    
    public IBook(final String id, final ItemStack book)
    {
        this(id, (BookMeta) book.getItemMeta());
    }
    
    public IBook(final String id, final BookMeta bookMeta)
    {
        this.id = id;
        this.bookMeta = bookMeta;
        this.setPages(IBooksUtils.getPages(bookMeta));
    }
    
    public IBook(final String id, final String displayName, final String title, final String author, List<String> lore, List<String> pages, final String... openCommands)
    {
        this.id = id;
        final BookMeta bookMeta = (BookMeta) new ItemStack(Material.WRITTEN_BOOK).getItemMeta();
        if (lore == null)
            lore = new ArrayList<>();
        if (pages == null)
            pages = new ArrayList<>();
        bookMeta.setDisplayName(displayName);
        bookMeta.setTitle(title);
        bookMeta.setAuthor(author);
        bookMeta.setLore(lore);
        this.bookMeta = bookMeta;
        this.pages = pages;
        if (openCommands != null)
            for (final String command : openCommands)
                this.openCommands.add(command.toLowerCase());
    }
    
    public IBook(final String id, final String displayName, final String title, final String author, final String generation, List<String> lore, List<String> pages, final String... openCommands)
    {
        this(id, displayName, title, author, lore, pages, openCommands);
        if (generation != null && IBooksUtils.hasBookGenerationSupport())
            this.bookMeta.setGeneration(IBooksUtils.getBookGeneration(generation));
    }
    
    public IBook(final String id, final String displayName, final String title, final String author, final Generation generation, List<String> lore, List<String> pages, final String... openCommands)
    {
        this(id, displayName, title, author, lore, pages, openCommands);
        if (generation != null)
            this.bookMeta.setGeneration(generation);
    }
    
    public String getId()
    {
        return this.id;
    }
    
    public void open(final Player player)
    {
        InteractiveBooks.getIBooksUtils().openBook(this.getItem(player), player);
    }
    
    public ItemStack getItem()
    {
        return this.getItem(null);
    }
    
    public ItemStack getItem(final Player player)
    {
        final ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        book.setItemMeta(this.getBookMeta(player));
        final NBTItem nbti = new NBTItem(book);
        nbti.setString(bookIdKey, this.getId());
        return nbti.getItem();
    }
    
    public BookMeta getBookMeta()
    {
        return this.getBookMeta(null);
    }
    
    public BookMeta getBookMeta(final Player player)
    {
        return InteractiveBooks.getIBooksUtils().getBookMeta(this.bookMeta, this.getPages(), player);
    }
    
    public void setBookMeta(final BookMeta bookMeta)
    {
        this.bookMeta = bookMeta;
    }
    
    public List<String> getPages()
    {
        return this.pages;
    }
    
    public void setPages(final List<String> pages)
    {
        this.pages = pages;
    }
    
    public Set<String> getOpenCommands()
    {
        return this.openCommands;
    }
    
    public void save()
    {
        final File file = new File(new File(InteractiveBooks.getPlugin().getDataFolder(), "books"), this.getId() + ".yml");
        final BookMeta meta = this.bookMeta;
        try
        {
            if (!file.exists())
                file.createNewFile();
            final YamlConfiguration bookConfig = YamlConfiguration.loadConfiguration(file);
            bookConfig.set("name", meta.getDisplayName());
            bookConfig.set("title", meta.getTitle());
            bookConfig.set("author", meta.getAuthor());
            if (IBooksUtils.hasBookGenerationSupport())
                bookConfig.set("generation", meta.getGeneration().toString());
            bookConfig.set("lore", meta.getLore());
            bookConfig.set("open_command", String.join(" ", this.getOpenCommands()));
            if (this.getPages().isEmpty())
            {
                final List<String> tempPages = new ArrayList<>();
                tempPages.add("");
                bookConfig.set("pages.1", tempPages);
            }
            for (int i = 0; i < this.getPages().size(); i++)
                bookConfig.set("pages." + String.valueOf(i + 1), this.getPages().get(i).split("\n"));
            
            bookConfig.save(file);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    @Override
    public boolean equals(final Object obj)
    {
        if (!(obj instanceof IBook))
            return false;
        return ((IBook) obj).getId().equals(this.getId());
    }
    
    private static List<String> mergeLines(final ConfigurationSection section)
    {
        final List<String> pages = new ArrayList<>();
        if (section == null)
            return pages;
        section.getKeys(false).forEach(key -> {
            final StringBuilder sb = new StringBuilder();
            section.getStringList(key).forEach(line -> sb.append("\n" + line));
            pages.add(sb.toString().replaceFirst("\n", ""));
        });
        return pages;
    }
    
}
