package net.leonardo_dgs.interactivebooks;

import net.leonardo_dgs.interactivebooks.util.BooksUtils;
import net.leomixer17.pluginlib.nbt.NBTItem;
import net.leomixer17.pluginlib.reflect.MinecraftVersion;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Objects;

public final class PlayerListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        String openBookId;
        List<String> booksToGiveIds;
        if (event.getPlayer().hasPlayedBefore())
        {
            openBookId = InteractiveBooks.getInstance().getConfig().getString("open_book_on_join");
            booksToGiveIds = InteractiveBooks.getInstance().getConfig().getStringList("books_on_join");
        }
        else
        {
            openBookId = InteractiveBooks.getInstance().getConfig().getString("open_book_on_first_join");
            booksToGiveIds = InteractiveBooks.getInstance().getConfig().getStringList("books_on_first_join");
        }
        if (!Objects.equals(openBookId, "") && InteractiveBooks.getBook(openBookId) != null)
        {
            if (MinecraftVersion.getVersion().getId() < MinecraftVersion.v1_14_R1.getId())
                Bukkit.getScheduler().runTask(InteractiveBooks.getInstance(), () -> InteractiveBooks.getBook(openBookId).open(event.getPlayer()));
            else
                InteractiveBooks.getBook(openBookId).open(event.getPlayer());
        }

        booksToGiveIds.forEach(id ->
        {
            if (InteractiveBooks.getBook(id) != null)
                event.getPlayer().getInventory().addItem(InteractiveBooks.getBook(id).getItem(event.getPlayer()));
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event)
    {
        if (event.useItemInHand().equals(Event.Result.DENY))
            return;
        if (!event.getAction().equals(Action.RIGHT_CLICK_AIR) && !event.getAction().equals(Action.RIGHT_CLICK_BLOCK))
            return;
        if (!InteractiveBooks.getInstance().getConfig().getBoolean("update_books_on_use"))
            return;
        if (!BooksUtils.getItemInMainHand(event.getPlayer()).getType().equals(Material.WRITTEN_BOOK))
            return;
        NBTItem nbti = new NBTItem(BooksUtils.getItemInMainHand(event.getPlayer()));
        if (!nbti.hasKey("InteractiveBooks|Book-Id"))
            return;
        IBook book = InteractiveBooks.getBook(nbti.getString("InteractiveBooks|Book-Id"));
        if (book == null)
            return;
        ItemStack bookItem = book.getItem(event.getPlayer());
        bookItem.setAmount(BooksUtils.getItemInMainHand(event.getPlayer()).getAmount());
        BooksUtils.setItemInMainHand(event.getPlayer(), bookItem);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event)
    {
        String command = event.getMessage().split(" ", 2)[0].replaceFirst("/", "").toLowerCase();
        IBook iBook = null;
        for (IBook book : InteractiveBooks.getBooks().values())
            if (book.getOpenCommands().contains(command))
            {
                iBook = book;
                break;
            }
        if (iBook == null)
            return;
        if (event.getPlayer().hasPermission("interactivebooks.open." + iBook.getId()))
            iBook.open(event.getPlayer());
        else
            event.getPlayer().sendMessage("§cYou don't have permission to open this book.");

        event.setCancelled(true);
    }

}
