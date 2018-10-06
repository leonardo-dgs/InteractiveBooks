package net.leomixer17.interactivebooks.nms;

import io.netty.buffer.Unpooled;
import net.md_5.bungee.chat.ComponentSerializer;
import net.minecraft.server.v1_13_R1.*;
import net.minecraft.server.v1_13_R1.IChatBaseComponent.ChatSerializer;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_13_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_13_R1.inventory.CraftMetaBook;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.ArrayList;
import java.util.List;

final class IBooksUtils_v1_13_R1 implements IBooksUtils {
	
	@SuppressWarnings("unchecked")
	@Override
	public BookMeta getBookMeta(final BookMeta meta, final List<String> rawPages, final Player player)
	{
		final ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
		final BookMeta bookMeta = (BookMeta) book.getItemMeta();
		bookMeta.setDisplayName(meta.getDisplayName());
		bookMeta.setTitle(meta.getTitle());
		bookMeta.setAuthor(meta.getAuthor());
		bookMeta.setLore(meta.getLore());
		if(IBooksUtils.hasPlaceholderAPISupport())
			IBooksUtils.replacePlaceholders(bookMeta, player);
		else
			IBooksUtils.replaceColorCodes(bookMeta);
		List<IChatBaseComponent> pages = null;
		try {
			pages = (List<IChatBaseComponent>) CraftMetaBook.class.getDeclaredField("pages").get(bookMeta);
		} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
			e.printStackTrace();
		}
		pages.addAll(this.getPages(bookMeta, rawPages, player));
		
		return bookMeta;
	}
	
	@Override
	public List<IChatBaseComponent> getPages(final BookMeta meta, final List<String> rawPages, final Player player)
	{
		final List<IChatBaseComponent> pages = new ArrayList<IChatBaseComponent>();
		rawPages.forEach(page -> pages.add(ChatSerializer.a(ComponentSerializer.toString(IBooksUtils.getPage(page, player)))));
		return pages;
	}

	@Override
	public void openBook(final ItemStack book, final Player player)
	{
		final int slot = player.getInventory().getHeldItemSlot();
		final ItemStack old = player.getInventory().getItem(slot);
		player.getInventory().setItem(slot, book);
		final PacketPlayOutCustomPayload packet = new PacketPlayOutCustomPayload(MinecraftKey.a("minecraft:book_open"), new PacketDataSerializer(Unpooled.buffer()).a(EnumHand.MAIN_HAND));
		((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
		player.getInventory().setItem(slot, old);
	}
	
}
