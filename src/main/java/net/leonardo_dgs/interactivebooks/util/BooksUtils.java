package net.leonardo_dgs.interactivebooks.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.PlaceholderAPIPlugin;
import net.leomixer17.pluginlib.reflect.BukkitReflection;
import net.leomixer17.pluginlib.reflect.MinecraftVersion;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
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
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BooksUtils {

    private static String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
    private static final Pattern ATTRIBUTE_PATTERN = Pattern.compile("(<[a-zA-Z ]+:[^>]*>|<reset>)");

    public static void openBook(ItemStack book, Player player)
    {
        if (MinecraftVersion.getVersion().getId() > MinecraftVersion.v1_14_R1.getId())
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

                case "v1_12_R1":
                case "v1_11_R1":
                case "v1_10_R1":
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

    public static BookMeta getBookMeta(BookMeta meta, List<String> rawPages, Player player)
    {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta bookMeta = (BookMeta) book.getItemMeta();
        Objects.requireNonNull(bookMeta);
        bookMeta.setDisplayName(meta.getDisplayName());
        bookMeta.setTitle(meta.getTitle());
        bookMeta.setAuthor(meta.getAuthor());
        bookMeta.setLore(meta.getLore());
        if (hasBookGenerationSupport())
            bookMeta.setGeneration(meta.getGeneration());
        replacePlaceholders(bookMeta, player, hasPlaceholderAPISupport());
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

    private static void replacePlaceholders(BookMeta meta, Player player, boolean papi)
    {
        meta.setDisplayName(setPlaceholders(player, meta.getDisplayName(), papi));
        if (meta.getTitle() != null)
            meta.setTitle(setPlaceholders(player, meta.getTitle(), papi));
        if (meta.getAuthor() != null)
            meta.setAuthor(setPlaceholders(player, meta.getAuthor(), papi));
        if (meta.getLore() != null)
            meta.setLore(setPlaceholders(player, meta.getLore(), papi));
    }

    public static boolean hasBookGenerationSupport()
    {
        return MinecraftVersion.getVersion().getId() >= MinecraftVersion.v1_10_R1.getId();
    }

    public static Generation getBookGeneration(String generation)
    {
        return generation == null ? Generation.ORIGINAL : Generation.valueOf(generation.toUpperCase());
    }

    @SuppressWarnings("deprecation")
    public static ItemStack getItemInMainHand(Player player)
    {
        if (version.equals("v1_8_R1") || version.equals("v1_8_R2") || version.equals("v1_8_R3"))
            return player.getInventory().getItemInHand();
        return player.getInventory().getItemInMainHand();
    }

    @SuppressWarnings("deprecation")
    public static void setItemInMainHand(Player player, ItemStack item)
    {
        if (version.equals("v1_8_R1") || version.equals("v1_8_R2") || version.equals("v1_8_R3"))
        {
            player.getInventory().setItemInHand(item);
            return;
        }
        player.getInventory().setItemInMainHand(item);
    }

    public static List<String> getPages(BookMeta meta)
    {
        List<String> plainPages = new ArrayList<>();
        List<BaseComponent[]> components = meta.spigot().getPages();
        components.forEach(component -> plainPages.add(getPage(component)));
        return plainPages;
    }

    private static String getPage(BaseComponent[] components)
    {
        StringBuilder sb = new StringBuilder();
        for (BaseComponent component : components)
            sb.append(BaseComponentSerializer.toString(component));
        return sb.toString();
    }

    private static List<?> getPages(BookMeta meta, List<String> rawPages, Player player)
    {
        List<Object> pages = new ArrayList<>();
        Method chatSerializerA = BukkitReflection.getMethod(BukkitReflection.getNMSClass("IChatBaseComponent").getClasses()[0], "a", String.class);
        rawPages.forEach(page ->
        {
            try
            {
                pages.add(chatSerializerA.invoke(null, ComponentSerializer.toString(BooksUtils.getPage(page, player))));
            }
            catch (IllegalAccessException | InvocationTargetException e)
            {
                e.printStackTrace();
            }
        });
        return pages;
    }

    private static TextComponent[] getPage(String page, Player player)
    {
        return convertListToArray(parsePage(page, player).getComponents());
    }

    private static TextComponentBuilder parsePage(String plainPage, Player player)
    {
        plainPage = plainPage.replace("<br>", "\n");
        TextComponentBuilder compBuilder = new TextComponentBuilder();
        boolean papiSupport = hasPlaceholderAPISupport();
        Matcher matcher = ATTRIBUTE_PATTERN.matcher(plainPage);
        int lastIndex = 0;
        StringBuilder curStr = new StringBuilder();
        while (matcher.find())
        {
            if (matcher.start() != 0)
            {
                curStr.append(plainPage, lastIndex, matcher.start());
                TextComponent current = new TextComponent(TextComponent.fromLegacyText(setPlaceholders(player, replaceEscapedChars(curStr.toString()), papiSupport)));
                compBuilder.add(current);
                curStr.delete(0, curStr.length());
            }
            lastIndex = matcher.end();
            if (matcher.group().equals("<reset>"))
            {
                compBuilder.setNextHoverEvent(null);
                compBuilder.setNextClickEvent(null);
            }
            else
            {
                Object event = parseEvent(matcher.group());
                if (event != null)
                {
                    if (event instanceof HoverEvent)
                        compBuilder.setNextHoverEvent((HoverEvent) event);
                    else if (event instanceof ClickEvent)
                        compBuilder.setNextClickEvent((ClickEvent) event);
                }
            }
        }
        if (lastIndex < plainPage.length())
        {
            curStr.append(plainPage, lastIndex, plainPage.length());
            TextComponent current = new TextComponent(TextComponent.fromLegacyText(setPlaceholders(player, curStr.toString(), papiSupport)));
            compBuilder.add(current);
        }

        return compBuilder;
    }

    private static Object parseEvent(String attribute)
    {
        String trimmed = attribute.replaceFirst("<", "").substring(0, attribute.length() - 2);
        String[] attributes = trimmed.split(":", 2);
        BookEventActionType type = BookEventActionType.parse(attributes[0]);
        String value = attributes[1];
        if (type == null)
            return null;
        value = replaceEscapedChars(value);
        switch (type)
        {
            case SHOW_TEXT:
                return new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(value));
            case SHOW_ITEM:
            case SHOW_ENTITY:
            case SUGGEST_COMMAND:
                return null;
            case RUN_COMMAND:
                return new ClickEvent(ClickEvent.Action.RUN_COMMAND, value);
            case OPEN_URL:
                return new ClickEvent(ClickEvent.Action.OPEN_URL, value);
            case CHANGE_PAGE:
                return new ClickEvent(ClickEvent.Action.CHANGE_PAGE, value);
        }
        return null;
    }

    private static String replaceEscapedChars(String str)
    {
        return str.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">");
    }

    private static List<String> setPlaceholders(Player player, List<String> text, boolean papi)
    {
        if (papi)
            return PlaceholderAPI.setPlaceholders(player, text);
        else
        {
            List<String> coloredText = new ArrayList<>();
            for (String s : text)
                coloredText.add(ChatColor.translateAlternateColorCodes('&', s));
            return coloredText;
        }
    }

    private static String setPlaceholders(Player player, String text, boolean papi)
    {
        if (papi)
            return PlaceholderAPI.setPlaceholders(player, text);
        else
            return ChatColor.translateAlternateColorCodes('&', text);
    }

    private static TextComponent[] convertListToArray(List<TextComponent> list)
    {
        TextComponent[] objects = new TextComponent[list.size()];
        for (int i = 0; i < list.size(); i++)
            objects[i] = list.get(i);
        return objects;
    }

}
