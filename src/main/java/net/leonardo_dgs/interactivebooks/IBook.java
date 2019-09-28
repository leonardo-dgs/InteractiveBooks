package net.leonardo_dgs.interactivebooks;

import net.leonardo_dgs.interactivebooks.util.BooksUtils;
import net.leomixer17.pluginlib.nbt.NBTItem;
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
import java.util.*;

public class IBook {

    private static final String bookIdKey = "InteractiveBooks|Book-Id";

    private String id;
    private BookMeta bookMeta;
    private List<String> pages;

    private final Set<String> openCommands = new HashSet<>();

    /**
     * Constructor for {@link IBook} that takes data from the supplied configuration.
     *
     * @param id         the id of the book
     * @param bookConfig configuration from which to take data to crete the book
     */
    public IBook(String id, FileConfiguration bookConfig)
    {
        this(id, bookConfig.getString("name"), bookConfig.getString("title"), bookConfig.getString("author"), bookConfig.getString("generation"),
                bookConfig.getStringList("lore"), mergeLines(bookConfig.getConfigurationSection("pages")),
                (((bookConfig.getString("open_command") == null) || Objects.equals(bookConfig.getString("open_command"), "")) ? null : Objects.requireNonNull(bookConfig.getString("open_command")).split(" ")));
    }

    /**
     * Constructor for {@link IBook} that takes data from the supplied book item.
     *
     * @param id   the id of the book
     * @param book {@link ItemStack} book item from which to take data to crete the book
     */
    public IBook(String id, ItemStack book)
    {
        this(id, (BookMeta) book.getItemMeta());
    }

    /**
     * Constructor for {@link IBook} that takes data from the supplied {@link BookMeta}.
     *
     * @param id       the id of the book
     * @param bookMeta {@link BookMeta} from which to take information to crete the book
     */
    public IBook(String id, BookMeta bookMeta)
    {
        this.id = id;
        this.bookMeta = bookMeta;
        this.setPages(BooksUtils.getPages(bookMeta));
    }

    /**
     * Constructor for {@link IBook} with the supplied data.
     *
     * @param id           the id of the book
     * @param displayName  the display name of the book item
     * @param title        the title of the book item
     * @param author       the author of the book item
     * @param lore         the lore of the book item
     * @param pages        the pages that will be converted to the book item pages
     * @param openCommands the commands that will open the book
     */
    public IBook(String id, String displayName, String title, String author, List<String> lore, List<String> pages, String... openCommands)
    {
        this.id = id;
        BookMeta bookMeta = (BookMeta) new ItemStack(Material.WRITTEN_BOOK).getItemMeta();
        if (lore == null)
            lore = new ArrayList<>();
        if (pages == null)
            pages = new ArrayList<>();
        Objects.requireNonNull(bookMeta).setDisplayName(displayName);
        bookMeta.setTitle(title);
        bookMeta.setAuthor(author);
        bookMeta.setLore(lore);
        this.bookMeta = bookMeta;
        this.pages = pages;
        if (openCommands != null)
            for (String command : openCommands)
                this.openCommands.add(command.toLowerCase());
    }

    /**
     * Constructor for {@link IBook} with the supplied data.
     *
     * @param id           the id of the book
     * @param displayName  the display name of the book item
     * @param title        the title of the book item
     * @param author       the author of the book item
     * @param generation   a string that represents generation of the book item
     * @param lore         the lore of the book item
     * @param pages        the pages that will be converted to the book item pages
     * @param openCommands the commands that will open the book
     */
    public IBook(String id, String displayName, String title, String author, String generation, List<String> lore, List<String> pages, String... openCommands)
    {
        this(id, displayName, title, author, lore, pages, openCommands);
        if (generation != null && BooksUtils.hasBookGenerationSupport())
            bookMeta.setGeneration(BooksUtils.getBookGeneration(generation));
    }

    /**
     * Constructor for {@link IBook} with the supplied data.
     *
     * @param id           the id of the book
     * @param displayName  the display name of the book item
     * @param title        the title of the book item
     * @param author       the author of the book item
     * @param generation   the generation of the book item
     * @param lore         the lore of the book item
     * @param pages        the pages that will be converted to the book item pages
     * @param openCommands the commands that will open the book
     */
    public IBook(String id, String displayName, String title, String author, Generation generation, List<String> lore, List<String> pages, String... openCommands)
    {
        this(id, displayName, title, author, lore, pages, openCommands);
        if (generation != null)
            this.bookMeta.setGeneration(generation);
    }

    /**
     * Gets the id of the book.
     *
     * @return the book id.
     */
    public String getId()
    {
        return this.id;
    }

    /**
     * Opens the book to the specified player.
     *
     * @param player the player to which open the book
     */
    public void open(Player player)
    {
        BooksUtils.openBook(this.getItem(player), player);
    }

    /**
     * Gets the book item without replacing placeholders.
     *
     * @return the book item
     */
    public ItemStack getItem()
    {
        return this.getItem(null);
    }

    /**
     * Gets the book item replacing its placeholders with the specified player data.
     *
     * @param player the player to get the data from for replacing placeholders
     * @return the book item with placeholders replaced with the specified player data
     */
    public ItemStack getItem(Player player)
    {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        book.setItemMeta(this.getBookMeta(player));
        NBTItem nbti = new NBTItem(book);
        nbti.setString(bookIdKey, this.getId());
        return nbti.getItem();
    }

    /**
     * Gets the {@link BookMeta} without replacing placeholders.
     *
     * @return the {@link BookMeta} of the book
     */
    public BookMeta getBookMeta()
    {
        return this.getBookMeta(null);
    }

    /**
     * Gets the {@link BookMeta} replacing its placeholders with the specified player data.
     *
     * @param player the player to get the data from for replacing placeholders
     * @return the {@link BookMeta} with placeholders replaced with the specified player data
     */
    public BookMeta getBookMeta(Player player)
    {
        return BooksUtils.getBookMeta(bookMeta, this.getPages(), player);
    }

    /**
     * Sets the {@link BookMeta} of this book to the specified one.
     *
     * @param bookMeta the {@link BookMeta} to set
     */
    public void setBookMeta(BookMeta bookMeta)
    {
        this.bookMeta = bookMeta;
    }

    /**
     * Gets the list of pages of this book.
     *
     * @return the {@link List} containing all pages of this book
     */
    public List<String> getPages()
    {
        return pages;
    }

    /**
     * Sets the book pages to the specified ones.
     *
     * @param pages the {@link List} of the book pages to set.
     */
    public void setPages(List<String> pages)
    {
        this.pages = pages;
    }

    /**
     * Gets the commands that can be used to open this book.
     *
     * @return a {@link Set} containing the commands that can be used to open this book
     */
    public Set<String> getOpenCommands()
    {
        return this.openCommands;
    }

    /**
     * Saves this book to his config file.
     */
    public void save()
    {
        File file = new File(new File(InteractiveBooks.getInstance().getDataFolder(), "books"), getId() + ".yml");
        BookMeta meta = bookMeta;
        try
        {
            if (!file.exists())
                if (file.createNewFile())
                    throw new IOException();
            YamlConfiguration bookConfig = YamlConfiguration.loadConfiguration(file);
            bookConfig.set("name", meta.getDisplayName());
            bookConfig.set("title", meta.getTitle());
            bookConfig.set("author", meta.getAuthor());
            if (BooksUtils.hasBookGenerationSupport())
                bookConfig.set("generation", Optional.ofNullable(meta.getGeneration()).orElse(Generation.ORIGINAL));
            bookConfig.set("lore", meta.getLore());
            bookConfig.set("open_command", String.join(" ", this.getOpenCommands()));
            if (this.getPages().isEmpty())
            {
                List<String> tempPages = new ArrayList<>();
                tempPages.add("");
                bookConfig.set("pages.1", tempPages);
            }
            for (int i = 0; i < this.getPages().size(); i++)
                bookConfig.set("pages." + (i + 1), this.getPages().get(i).split("\n"));

            bookConfig.save(file);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof IBook))
            return false;
        return ((IBook) obj).getId().equals(this.getId());
    }

    private static List<String> mergeLines(ConfigurationSection section)
    {
        List<String> pages = new ArrayList<>();
        if (section == null)
            return pages;
        section.getKeys(false).forEach(key ->
        {
            StringBuilder sb = new StringBuilder();
            section.getStringList(key).forEach(line -> sb.append("\n").append(line));
            pages.add(sb.toString().replaceFirst("\n", ""));
        });
        return pages;
    }

}
