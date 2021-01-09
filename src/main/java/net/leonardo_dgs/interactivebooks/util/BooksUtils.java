package net.leonardo_dgs.interactivebooks.util;

import lombok.Getter;
import me.clip.placeholderapi.PlaceholderAPI;
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
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BooksUtils {

    @Getter
    private static final boolean isBookGenerationSupported = MinecraftVersion.getRunningVersion().isAfterOrEqual(MinecraftVersion.parse("1.10"));

    private static final boolean OLD_PAGES_METHODS = MinecraftVersion.getRunningVersion().isBefore(MinecraftVersion.parse("1.12.2"));
    private static final boolean OLD_ITEMINHAND_METHOD = ReflectionUtil.getNmsVersion().equals("v1_8_R3");
    private static final Plugin PAPIPLUGIN = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");
    private static final Pattern ATTRIBUTE_PATTERN = Pattern.compile("(<[a-zA-Z ]+:[^>]*>|<reset>)");
    private static final Method CHATSERIALIZER_A;
    private static final Field FIELD_PAGES;

    static {
        Method chatSerializerA = null;
        Field fieldPages = null;
        if (OLD_PAGES_METHODS) {
            try {
                chatSerializerA = ReflectionUtil.getMethod(ReflectionUtil.nmsClass("IChatBaseComponent").getClasses()[0], "a", String.class);
                fieldPages = ReflectionUtil.obcClass("inventory.CraftMetaBook").getDeclaredField("pages");
            } catch (ClassNotFoundException | NoSuchFieldException e) {
                e.printStackTrace();
            }
        }
        CHATSERIALIZER_A = chatSerializerA;
        FIELD_PAGES = fieldPages;
    }

    public static BookMeta getBookMeta(BookMeta meta, List<String> rawPages, Player player) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta bookMeta = (BookMeta) book.getItemMeta();
        Objects.requireNonNull(bookMeta);
        bookMeta.setDisplayName(meta.getDisplayName());
        bookMeta.setTitle(meta.getTitle());
        bookMeta.setAuthor(meta.getAuthor());
        bookMeta.setLore(meta.getLore());
        if (isBookGenerationSupported())
            bookMeta.setGeneration(meta.getGeneration());
        replacePlaceholders(bookMeta, player);
        if (OLD_PAGES_METHODS) {
            try {
                List<?> pages = (List<?>) FIELD_PAGES.get(bookMeta);
                pages.getClass().getMethod("addAll", Collection.class).invoke(pages, getPages_Before_v1_12_2(rawPages, player));
            } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                e.printStackTrace();
            }
        } else {
            getPages(rawPages, player).forEach(bookMeta.spigot()::addPage);
        }
        return bookMeta;
    }

    private static void replacePlaceholders(BookMeta meta, Player player) {
        meta.setDisplayName(PAPIUtil.setPlaceholders(player, meta.getDisplayName()));
        if (meta.getTitle() != null)
            meta.setTitle(PAPIUtil.setPlaceholders(player, meta.getTitle()));
        if (meta.getAuthor() != null)
            meta.setAuthor(PAPIUtil.setPlaceholders(player, meta.getAuthor()));
        if (meta.getLore() != null)
            meta.setLore(setPlaceholders(player, meta.getLore()));
    }

    public static Generation getBookGeneration(String generation) {
        return generation == null ? Generation.ORIGINAL : Generation.valueOf(generation.toUpperCase());
    }

    @SuppressWarnings("deprecation")
    public static ItemStack getItemInMainHand(Player player) {
        if (OLD_ITEMINHAND_METHOD)
            return player.getInventory().getItemInHand();
        else
            return player.getInventory().getItemInMainHand();
    }

    @SuppressWarnings("deprecation")
    public static void setItemInMainHand(Player player, ItemStack item) {
        if (OLD_ITEMINHAND_METHOD)
            player.getInventory().setItemInHand(item);
        else
            player.getInventory().setItemInMainHand(item);
    }

    public static List<String> getPages(BookMeta meta) {
        List<String> plainPages = new ArrayList<>();
        List<BaseComponent[]> components = meta.spigot().getPages();
        components.forEach(component -> plainPages.add(getPage(component)));
        return plainPages;
    }

    private static String getPage(BaseComponent[] components) {
        StringBuilder sb = new StringBuilder();
        for (BaseComponent component : components)
            sb.append(BaseComponentSerializer.toString(component));
        return sb.toString();
    }

    private static List<BaseComponent[]> getPages(List<String> rawPages, Player player) {
        List<BaseComponent[]> pages = new ArrayList<>();
        rawPages.forEach(page -> pages.add(BooksUtils.getPage(page, player)));
        return pages;
    }

    private static List<Object> getPages_Before_v1_12_2(List<String> rawPages, Player player) {
        List<Object> pages = new ArrayList<>();
        rawPages.forEach(page -> {
            try {
                pages.add(CHATSERIALIZER_A.invoke(null, ComponentSerializer.toString(BooksUtils.getPage(page, player))));
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        });
        return pages;
    }

    private static TextComponent[] getPage(String page, Player player) {
        return parsePage(page, player).getComponents().toArray(new TextComponent[0]);
    }

    private static TextComponentBuilder parsePage(String plainPage, Player player) {
        plainPage = plainPage.replace("<br>", "\n");
        TextComponentBuilder compBuilder = new TextComponentBuilder();
        Matcher matcher = ATTRIBUTE_PATTERN.matcher(plainPage);
        int lastIndex = 0;
        StringBuilder curStr = new StringBuilder();
        while (matcher.find()) {
            if (matcher.start() != 0) {
                curStr.append(plainPage, lastIndex, matcher.start());
                TextComponent current = new TextComponent(TextComponent.fromLegacyText(PAPIUtil.setPlaceholders(player, replaceEscapedChars(curStr.toString()))));
                compBuilder.add(current);
                curStr.delete(0, curStr.length());
            }
            lastIndex = matcher.end();
            if (matcher.group().equals("<reset>")) {
                compBuilder.setNextHoverEvent(null);
                compBuilder.setNextClickEvent(null);
            } else {
                Object event = parseEvent(matcher.group(), player);
                if (event != null) {
                    if (event instanceof HoverEvent)
                        compBuilder.setNextHoverEvent((HoverEvent) event);
                    else if (event instanceof ClickEvent)
                        compBuilder.setNextClickEvent((ClickEvent) event);
                }
            }
        }
        if (lastIndex < plainPage.length()) {
            curStr.append(plainPage, lastIndex, plainPage.length());
            TextComponent current = new TextComponent(TextComponent.fromLegacyText(PAPIUtil.setPlaceholders(player, curStr.toString())));
            compBuilder.add(current);
        }

        return compBuilder;
    }

    private static Object parseEvent(String attribute, Player player) {
        String trimmed = attribute.replaceFirst("<", "").substring(0, attribute.length() - 2);
        String[] attributes = trimmed.split(":", 2);
        BookEventActionType type = BookEventActionType.parse(attributes[0]);
        String value = attributes[1];
        if (type == null)
            return null;
        value = PAPIUtil.setPlaceholders(player, replaceEscapedChars(value));
        switch (type) {
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

    private static String replaceEscapedChars(String str) {
        return str.replace("&lt;", "<").replace("&gt;", ">");
    }

    private static List<String> setPlaceholders(Player player, List<String> text) {
        if (PAPIPLUGIN != null && PAPIPLUGIN.isEnabled())
            return PlaceholderAPI.setPlaceholders(player, text);
        else {
            List<String> coloredText = new ArrayList<>();
            for (String s : text)
                coloredText.add(ChatColor.translateAlternateColorCodes('&', s));
            return coloredText;
        }
    }

}
