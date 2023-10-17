package net.leonardo_dgs.interactivebooks;

import de.tr7zw.changeme.nbtapi.NBTItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class PlayerListener implements Listener {
    private static final boolean MC_VERSION_AFTER_1_14;
    private final SettingsManager settings;

    static {
        Pattern versionPattern = Pattern.compile("^([0-9]+\\.[0-9]+)");
        Matcher matcher = versionPattern.matcher(Bukkit.getBukkitVersion());
        if (matcher.find()) {
            String[] version = matcher.group().split("\\.");
            MC_VERSION_AFTER_1_14 = Integer.parseInt(version[1]) >= 14;
        } else {
            MC_VERSION_AFTER_1_14 = false;
        }
    }

    PlayerListener(SettingsManager settings) {
        this.settings = settings;
    }

    @EventHandler
    void onPlayerJoin(PlayerJoinEvent event) {
        String openBookId;
        List<String> booksToGiveIds;
        if (event.getPlayer().hasPlayedBefore()) {
            openBookId = settings.getOpenBookOnJoin();
            booksToGiveIds = settings.getBooksOnJoin();
        } else {
            openBookId = settings.getOpenBookOnFirstJoin();
            booksToGiveIds = settings.getBooksOnFirstJoin();
        }
        if (openBookId != null && !openBookId.isEmpty()) {
            IBook book = InteractiveBooks.getBook(openBookId);
            if (book != null) {
                if (MC_VERSION_AFTER_1_14)
                    book.open(event.getPlayer());
                else
                    Bukkit.getScheduler().runTask(InteractiveBooks.getInstance(), () -> book.open(event.getPlayer()));
            }
        }

        booksToGiveIds.forEach(id -> {
            IBook book = InteractiveBooks.getBook(id);
            if (book != null)
                event.getPlayer().getInventory().addItem(book.getItem(event.getPlayer()));
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    void onPlayerInteract(PlayerInteractEvent event) {
        if (event.useItemInHand() == Event.Result.DENY)
            return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;
        if (!settings.getUpdateBooksOnUse())
            return;

        EquipmentSlot hand = BooksUtils.isOffHandSupported() ? event.getHand() : EquipmentSlot.HAND;
        ItemStack itemInHand = BooksUtils.getItemInHand(event.getPlayer(), hand);
        if (itemInHand.getType() != Material.WRITTEN_BOOK)
            return;

        NBTItem nbtItem = new NBTItem(itemInHand);
        if (!nbtItem.hasTag("InteractiveBooks|Book-Id"))
            return;

        IBook book = InteractiveBooks.getBook(nbtItem.getString("InteractiveBooks|Book-Id"));
        if (book == null)
            return;

        ItemStack bookItem = book.getItem(event.getPlayer());
        bookItem.setAmount(itemInHand.getAmount());
        BooksUtils.setItemInHand(event.getPlayer(), bookItem, hand);
    }
}
