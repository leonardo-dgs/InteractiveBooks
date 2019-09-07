package net.leomixer17.interactivebooks.nms;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.PlaceholderAPIPlugin;
import net.leomixer17.pluginlib.reflect.BukkitReflection;
import net.leomixer17.pluginlib.reflect.MinecraftVersion;
import net.md_5.bungee.api.chat.*;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.BookMeta.Generation;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class IBooksUtils {

    private static String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];

    public static void openBook(final ItemStack book, final Player player)
    {
        if (MinecraftVersion.getVersion().getVersionId() > MinecraftVersion.v1_14_R1.getVersionId())
        {
            player.openBook(book);
            return;
        }
        int slot = player.getInventory().getHeldItemSlot();
        ItemStack old = player.getInventory().getItem(slot);
        player.getInventory().setItem(slot, book);
        try
        {
            Object packet = null;
            Constructor<?> packetConstructor;
            Object enumHand;
            Object packetDataSerializer;
            Object packetDataSerializerArg;
            Object minecraftKey;
            switch (version)
            {
                case "v1_14_R1":
                    enumHand = BukkitReflection.getNMSClass("EnumHand").getField("MAIN_HAND").get(null);
                    packetConstructor = BukkitReflection.getNMSClass("PacketPlayOutOpenBook").getConstructor(BukkitReflection.getNMSClass("EnumHand"));
                    packet = packetConstructor.newInstance(enumHand);
                    break;

                case "v1_13_R2":
                case "v1_13_R1":
                    enumHand = BukkitReflection.getNMSClass("EnumHand").getField("MAIN_HAND").get(null);
                    minecraftKey = BukkitReflection.getNMSClass("MinecraftKey").getMethod("a", String.class).invoke(null, "minecraft:book_open");
                    packetDataSerializerArg = BukkitReflection.getNMSClass("PacketDataSerializer").getConstructor(ByteBuf.class).newInstance(Unpooled.buffer());
                    packetDataSerializer = BukkitReflection.getNMSClass("PacketDataSerializer").getMethod("a", Enum.class).invoke(packetDataSerializerArg, enumHand);
                    packetConstructor = BukkitReflection.getNMSClass("PacketPlayOutCustomPayload").getConstructor(BukkitReflection.getNMSClass("MinecraftKey"), BukkitReflection.getNMSClass("PacketDataSerializer"));
                    packet = packetConstructor.newInstance(minecraftKey, packetDataSerializer);
                    break;

                case "v1_9_R2":
                case "v1_9_R1":
                    enumHand = BukkitReflection.getNMSClass("EnumHand").getField("MAIN_HAND").get(null);
                    packetDataSerializerArg = BukkitReflection.getNMSClass("PacketDataSerializer").getConstructor(ByteBuf.class).newInstance(Unpooled.buffer());
                    packetDataSerializer = BukkitReflection.getNMSClass("PacketDataSerializer").getMethod("a", Enum.class).invoke(packetDataSerializerArg, enumHand);
                    packetConstructor = BukkitReflection.getNMSClass("PacketPlayOutCustomPayload").getConstructor(String.class, BukkitReflection.getNMSClass("PacketDataSerializer"));
                    packet = packetConstructor.newInstance("MC|BOpen", packetDataSerializer);
                    break;

                case "v1_8_R3":
                case "v1_8_R2":
                case "v1_8_R1":
                    packetDataSerializer = BukkitReflection.getNMSClass("PacketDataSerializer").getConstructor(ByteBuf.class).newInstance(Unpooled.buffer());
                    packetConstructor = BukkitReflection.getNMSClass("PacketPlayOutCustomPayload").getConstructor(String.class, BukkitReflection.getNMSClass("PacketDataSerializer"));
                    packet = packetConstructor.newInstance("MC|BOpen", packetDataSerializer);
                    break;
            }
            BukkitReflection.sendPacket(packet, player);
        }
        catch (IllegalAccessException | NoSuchFieldException | NoSuchMethodException | InstantiationException | InvocationTargetException e)
        {
            e.printStackTrace();
        }
        player.getInventory().setItem(slot, old);
    }

    public static BookMeta getBookMeta(final BookMeta meta, final List<String> rawPages, final Player player)
    {
        final ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        final BookMeta bookMeta = (BookMeta) book.getItemMeta();
        bookMeta.setDisplayName(meta.getDisplayName());
        bookMeta.setTitle(meta.getTitle());
        bookMeta.setAuthor(meta.getAuthor());
        bookMeta.setLore(meta.getLore());
        if (IBooksUtils.hasPlaceholderAPISupport())
            IBooksUtils.replacePlaceholders(bookMeta, player);
        else
            IBooksUtils.replaceColorCodes(bookMeta);
        try
        {
            List<?> pages = (List<?>) BukkitReflection.getOBCClass("inventory.CraftMetaBook").getDeclaredField("pages").get(bookMeta);
            pages.getClass().getMethod("addAll", Collection.class).invoke(pages, getPages(bookMeta, rawPages, player));
        }
        catch (IllegalAccessException | NoSuchFieldException | NoSuchMethodException | InvocationTargetException e)
        {
            e.printStackTrace();
        }
        return bookMeta;
    }

    public static boolean hasPlaceholderAPISupport()
    {
        return Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null && Bukkit.getPluginManager().getPlugin("PlaceholderAPI") instanceof PlaceholderAPIPlugin;
    }

    private static void replacePlaceholders(final BookMeta meta, final Player player)
    {
        meta.setDisplayName(PlaceholderAPI.setPlaceholders((OfflinePlayer) player, meta.getDisplayName()));
        meta.setTitle(PlaceholderAPI.setPlaceholders((OfflinePlayer) player, meta.getTitle()));
        meta.setAuthor(PlaceholderAPI.setPlaceholders((OfflinePlayer) player, meta.getAuthor()));
        meta.setLore(PlaceholderAPI.setPlaceholders((OfflinePlayer) player, meta.getLore()));
    }

    private static void replaceColorCodes(final BookMeta meta)
    {
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', meta.getDisplayName()));
        meta.setTitle(ChatColor.translateAlternateColorCodes('&', meta.getTitle()));
        meta.setAuthor(ChatColor.translateAlternateColorCodes('&', meta.getAuthor()));
        for (int i = 0; i < meta.getLore().size(); i++)
            meta.getLore().set(i, ChatColor.translateAlternateColorCodes('&', meta.getLore().get(i)));
    }

    public static boolean hasBookGenerationSupport()
    {
        return !version.equals("v1_8_R2") && !version.equals("v1_8_R3") && !version.equals("v1_9_R1") && !version.equals("v1_9_R2");
    }

    public static Generation getBookGeneration(final String generation)
    {
        return generation == null ? Generation.ORIGINAL : Generation.valueOf(generation.toUpperCase());
    }

    @SuppressWarnings("deprecation")
    public static ItemStack getItemInMainHand(final Player player)
    {
        if (version.equals("v1_8_R2") || version.equals("v1_8_R3"))
            return player.getInventory().getItemInHand();
        return player.getInventory().getItemInMainHand();
    }

    @SuppressWarnings("deprecation")
    public static void setItemInMainHand(final Player player, final ItemStack item)
    {
        if (version.equals("v1_8_R2") || version.equals("v1_8_R3"))
        {
            player.getInventory().setItemInHand(item);
            return;
        }
        player.getInventory().setItemInMainHand(item);
    }

    public static List<String> getPages(final BookMeta meta)
    {
        final List<String> plainPages = new ArrayList<>();
        final List<BaseComponent[]> components = meta.spigot().getPages();
        components.forEach(component -> plainPages.add(getPage(component)));
        return plainPages;
    }

    private static String getPage(final BaseComponent[] components)
    {
        final StringBuilder sb = new StringBuilder();
        for (final BaseComponent component : components)
            sb.append(BaseComponentSerializer.toString(component));
        return sb.toString();
    }

    private static List<?> getPages(final BookMeta meta, final List<String> rawPages, final Player player)
    {
        List<Object> pages = new ArrayList<>();
        Method chatSerializerA = BukkitReflection.getMethod(BukkitReflection.getNMSClass("IChatBaseComponent").getClasses()[0], "a", String.class);
        rawPages.forEach(page ->
        {
            try
            {
                pages.add(chatSerializerA.invoke(null, ComponentSerializer.toString(IBooksUtils.getPage(page, player))));
            }
            catch (IllegalAccessException | InvocationTargetException e)
            {
                e.printStackTrace();
            }
        });
        return pages;
    }

    private static TextComponent[] getPage(final String page, final Player player)
    {
        final String[] plainRows = page.split("\n");
        final TextComponentBuilder compBuilder = new TextComponentBuilder();
        for (int i = 0; i < plainRows.length; i++)
            compBuilder.add(parseRow(plainRows[i] + (i < plainRows.length - 1 ? "\n" : ""), player));
        return convertListToArray(compBuilder.getComponents());
    }

    private static TextComponentBuilder parseRow(String plainRow, final Player player)
    {
        final boolean papiSupport = hasPlaceholderAPISupport();

        plainRow = plainRow.replace("<br>", "\n");

        final TextComponentBuilder compBuilder = new TextComponentBuilder();
        final char[] chars = plainRow.toCharArray();

        String clickType = null;
        String hoverType = null;
        String clickValue = null;
        String hoverValue = null;

        TextComponent current = new TextComponent();
        StringBuilder curStr = new StringBuilder();
        for (int i = 0; i < chars.length; i++)
        {
            if (chars[i] == '<')
            {
                final StringBuilder sb = new StringBuilder();
                int j;
                for (j = i; j < chars.length; j++)
                {
                    if (chars[j] == '>')
                    {
                        final String tag = sb.toString().replaceFirst("<", "");
                        if (!tag.contains(":"))
                        {
                            if (!tag.equalsIgnoreCase("reset"))
                                break;
                            current = new TextComponent(TextComponent.fromLegacyText(setPlaceholders(player, curStr.toString(), papiSupport)));
                            compBuilder.add(current);
                            curStr = new StringBuilder();

                            compBuilder.setNextClickEvent(null);
                            compBuilder.setNextHoverEvent(null);
                            i = j;
                            break;
                        }
                        if (!isValidTag(tag))
                            break;
                        current = new TextComponent(TextComponent.fromLegacyText(setPlaceholders(player, curStr.toString(), papiSupport)));
                        compBuilder.add(current);
                        curStr = new StringBuilder();
                        final String[] tagArgs = tag.split(":", 2);
                        if (isClickEvent(tagArgs[0]))
                        {
                            clickType = tagArgs[0];
                            clickValue = tagArgs[1];
                            compBuilder.setNextClickEvent(getClickEvent(clickType, setPlaceholders(player, clickValue, papiSupport)));
                        }
                        else
                        {
                            hoverType = tagArgs[0];
                            hoverValue = tagArgs[1];
                            compBuilder.setNextHoverEvent(getHoverEvent(hoverType, setPlaceholders(player, hoverValue, papiSupport)));
                        }
                        i = j;
                        break;
                    }
                    else if (chars[j] == '&')
                    {
                        final StringBuilder specialCharSb = new StringBuilder();
                        int k;
                        for (k = j + 1; k < chars.length && chars[k] != ';'; k++)
                            specialCharSb.append(chars[k]);
                        Character specialChar = getEscapedChar(specialCharSb.toString());
                        if (specialChar != null)
                        {
                            sb.append(specialChar);
                            j = k;
                            continue;
                        }
                    }
                    sb.append(chars[j]);
                }
                if (j == chars.length)
                    curStr.append(sb);
            }
            else if (chars[i] == '&')
            {
                final StringBuilder specialCharSb = new StringBuilder();
                int k;
                for (k = i + 1; k < chars.length && chars[k] != ';'; k++)
                    specialCharSb.append(chars[k]);
                Character specialChar = getEscapedChar(specialCharSb.toString());
                if (specialChar != null)
                {
                    curStr.append(specialChar);
                    i = k;
                    continue;
                }
                curStr.append(chars[i]);
            }
            else
                curStr.append(chars[i]);
        }

        if (!curStr.toString().isEmpty())
        {
            current = new TextComponent(TextComponent.fromLegacyText(setPlaceholders(player, curStr.toString(), papiSupport)));
            compBuilder.add(current);
        }

        return compBuilder;
    }

    private static Character getEscapedChar(final String escapeCode)
    {
        switch (escapeCode)
        {
            case "amp":
                return '&';
            case "lt":
                return '<';
            case "gt":
                return '>';
        }
        return null;
    }

    private static String setPlaceholders(final Player player, final String text, final boolean papi)
    {
        if (papi)
            return PlaceholderAPI.setPlaceholders(player, text);
        else
            return ChatColor.translateAlternateColorCodes('&', text);
    }

    private static ClickEvent getClickEvent(final String type, final String value)
    {
        if (type == null)
            return null;

        ClickEvent.Action action = null;
        switch (type.toLowerCase())
        {
            case "link":
            case "open url":
            case "url":
                action = ClickEvent.Action.OPEN_URL;
                break;
            case "run command":
            case "command":
            case "cmd":
                action = ClickEvent.Action.RUN_COMMAND;
                break;
            case "suggest command":
            case "suggest cmd":
            case "suggest":
                action = ClickEvent.Action.SUGGEST_COMMAND;
                break;
            case "change page":
                action = ClickEvent.Action.CHANGE_PAGE;
                break;
        }

        return new ClickEvent(action, value);
    }

    private static HoverEvent getHoverEvent(final String type, final String value)
    {
        if (type == null)
            return null;

        HoverEvent.Action action = null;
        switch (type.toLowerCase())
        {
            case "tooltip":
            case "show text":
                action = HoverEvent.Action.SHOW_TEXT;
                break;
        }
        return new HoverEvent(action, new ComponentBuilder(value).create());
    }

    private static boolean isValidTag(final String tag)
    {
        switch (tag.split(":", 2)[0].toLowerCase())
        {
            case "link":
            case "open url":
            case "url":
            case "run command":
            case "command":
            case "cmd":
            case "suggest command":
            case "suggest cmd":
            case "suggest":
            case "change page":
            case "tooltip":
            case "show text":
                return true;
        }
        return false;
    }

    private static boolean isClickEvent(final String type)
    {
        switch (type.toLowerCase())
        {
            case "tooltip":
            case "show text":
                return false;
        }
        return true;
    }

    private static TextComponent[] convertListToArray(final List<TextComponent> list)
    {
        final TextComponent[] objects = new TextComponent[list.size()];
        for (int i = 0; i < list.size(); i++)
            objects[i] = list.get(i);
        return objects;
    }

}
