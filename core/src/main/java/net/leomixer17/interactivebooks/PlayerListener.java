package net.leomixer17.interactivebooks;

import net.leomixer17.interactivebooks.lib.nbtapi.NBTItem;
import net.leomixer17.interactivebooks.nms.IBooksUtils;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

public final class PlayerListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(final PlayerJoinEvent event)
    {
        if (event.getPlayer().hasPlayedBefore())
        {
            InteractiveBooks.getPlugin().getConfig().getStringList("books_on_join").forEach(id -> {
                if (InteractiveBooks.getBook(id) != null)
                    event.getPlayer().getInventory().addItem(InteractiveBooks.getBook(id).getItem(event.getPlayer()));
            });
        }
        else
        {
            InteractiveBooks.getPlugin().getConfig().getStringList("books_on_first_join").forEach(id -> {
                if (InteractiveBooks.getBook(id) != null)
                    event.getPlayer().getInventory().addItem(InteractiveBooks.getBook(id).getItem(event.getPlayer()));
            });
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(final PlayerInteractEvent e)
    {
        if (!e.getAction().equals(Action.RIGHT_CLICK_AIR) && !e.getAction().equals(Action.RIGHT_CLICK_BLOCK))
            return;
        if (!InteractiveBooks.getPlugin().getConfig().getBoolean("update_books_on_use"))
            return;
        if (!IBooksUtils.getItemInMainHand(e.getPlayer()).getType().equals(Material.WRITTEN_BOOK))
            return;
        final NBTItem nbti = new NBTItem(IBooksUtils.getItemInMainHand(e.getPlayer()));
        if (!nbti.hasKey("InteractiveBooks|Book-Id"))
            return;
        final IBook book = InteractiveBooks.getBook(nbti.getString("InteractiveBooks|Book-Id"));
        if (book == null)
            return;
        final ItemStack bookItem = book.getItem(e.getPlayer());
        bookItem.setAmount(IBooksUtils.getItemInMainHand(e.getPlayer()).getAmount());
        IBooksUtils.setItemInMainHand(e.getPlayer(), bookItem);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerCommandPreprocess(final PlayerCommandPreprocessEvent event)
    {
        final String command = event.getMessage().split(" ", 2)[0].replaceFirst("/", "").toLowerCase();
        IBook iBook = null;
        for (final IBook book : InteractiveBooks.getBooks().values())
            if (book.getOpenCommands().contains(command))
            {
                iBook = book;
                break;
            }
        if (iBook == null)
            return;
        if (!event.getPlayer().hasPermission("interactivebooks.open." + iBook.getId()))
        {
            event.getPlayer().sendMessage("§cYou don't have permission to open this book.");
            return;
        }

        iBook.open(event.getPlayer());
        event.setCancelled(true);
    }

}
