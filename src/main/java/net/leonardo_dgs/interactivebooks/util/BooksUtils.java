package net.leonardo_dgs.interactivebooks.util;

import lombok.Getter;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import net.kyori.adventure.platform.bukkit.MinecraftComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.BookMeta.Generation;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BooksUtils {

    @Getter
    private static final boolean isBookGenerationSupported = MinecraftVersion.getRunningVersion().isAfterOrEqual(MinecraftVersion.parse("1.10"));

    private static final boolean OLD_PAGES_METHODS = MinecraftVersion.getRunningVersion().isBefore(MinecraftVersion.parse("1.12.2"));
    private static final boolean OLD_ITEMINHAND_METHOD = ReflectionUtil.getNmsVersion().equals("v1_8_R3");
    private static final Field FIELD_PAGES;
    private static final Pattern OLD_TRANSFORMATIONS_PATTERN = Pattern.compile("(<(show text|tooltip|run command|command|cmd|suggest command|suggest cmd|suggest|open url|url|link|change page):[^>]*>)");

    static {
        Field fieldPages = null;
        if (OLD_PAGES_METHODS) {
            try {
                fieldPages = ReflectionUtil.obcClass("inventory.CraftMetaBook").getDeclaredField("pages");
            } catch (ClassNotFoundException | NoSuchFieldException e) {
                e.printStackTrace();
            }
        }
        FIELD_PAGES = fieldPages;
    }

    public static BookMeta getBookMeta(BookMeta meta, List<String> rawPages, Player player) {
        BookMeta bookMeta = meta.clone();
        setPlaceholders(bookMeta, player);
        if (OLD_PAGES_METHODS) {
            try {
                List<?> pages = (List<?>) FIELD_PAGES.get(bookMeta);
                Method methodAdd = pages.getClass().getMethod("add", Object.class);
                for (String page : rawPages)
                    methodAdd.invoke(pages, MinecraftComponentSerializer.get().serialize(getPage(page, player)));
            } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                e.printStackTrace();
            }
        } else {
            rawPages.forEach(page -> bookMeta.spigot().addPage(BungeeComponentSerializer.get().serialize(getPage(page, player))));
        }
        return bookMeta;
    }

    public static List<String> getPages(BookMeta meta) {
        List<String> plainPages = new ArrayList<>();
        List<BaseComponent[]> components = meta.spigot().getPages();
        components.forEach(component -> plainPages.add(MiniMessage.miniMessage().serialize(BungeeComponentSerializer.get().deserialize(component))));
        return plainPages;
    }

    public static Component getPage(String page, Player player) {
        StringBuilder sb = new StringBuilder(PAPIUtil.setPlaceholders(player, page).replace("<br>", "\n"));
        Matcher matcher = OLD_TRANSFORMATIONS_PATTERN.matcher(sb);
        while (matcher.find()) {
            String occurrence = matcher.group();
            String replacement = null;
            if (occurrence.startsWith("<show text:")) {
                replacement = "<hover:show_text:\"" + occurrence.substring(0, occurrence.length() - 1).replaceFirst("<show text:", "") + "\">";
            } else if (occurrence.startsWith("<tooltip:")) {
                replacement = "<hover:show_text:\"" + occurrence.substring(0, occurrence.length() - 1).replaceFirst("<tooltip:", "") + "\">";
            } else if (occurrence.startsWith("<run command:")) {
                replacement = "<click:run_command:\"" + occurrence.substring(0, occurrence.length() - 1).replaceFirst("<run command:", "") + "\">";
            } else if (occurrence.startsWith("<command:")) {
                replacement = "<click:run_command:\"" + occurrence.substring(0, occurrence.length() - 1).replaceFirst("<command:", "") + "\">";
            } else if (occurrence.startsWith("<cmd:")) {
                replacement = "<click:run_command:\"" + occurrence.substring(0, occurrence.length() - 1).replaceFirst("<cmd:", "") + "\">";
            } else if (occurrence.startsWith("<suggest command:")) {
                replacement = "<click:suggest_command:\"" + occurrence.substring(0, occurrence.length() - 1).replaceFirst("<suggest command:", "") + "\">";
            } else if (occurrence.startsWith("<suggest cmd:")) {
                replacement = "<click:suggest_command:\"" + occurrence.substring(0, occurrence.length() - 1).replaceFirst("<suggest cmd:", "") + "\">";
            } else if (occurrence.startsWith("<suggest:")) {
                replacement = "<click:suggest_command:\"" + occurrence.substring(0, occurrence.length() - 1).replaceFirst("<suggest:", "") + "\">";
            } else if (occurrence.startsWith("<open url:")) {
                replacement = "<click:open_url:\"" + occurrence.substring(0, occurrence.length() - 1).replaceFirst("<open url:", "") + "\">";
            } else if (occurrence.startsWith("<url:")) {
                replacement = "<click:open_url:\"" + occurrence.substring(0, occurrence.length() - 1).replaceFirst("<url:", "") + "\">";
            } else if (occurrence.startsWith("<link:")) {
                replacement = "<click:open_url:\"" + occurrence.substring(0, occurrence.length() - 1).replaceFirst("<link:", "") + "\">";
            } else if (occurrence.startsWith("<change page:")) {
                replacement = "<click:change_page:\"" + occurrence.substring(0, occurrence.length() - 1).replaceFirst("<change page:", "") + "\">";
            }

            if (replacement != null)
                sb.replace(matcher.start(), matcher.end(), replacement);
        }
        return MiniMessage.miniMessage().parse(sb.toString());
    }

    private static void setPlaceholders(BookMeta meta, Player player) {
        meta.setDisplayName(BukkitComponentSerializer.legacy().serialize(MiniMessage.miniMessage().parse(PAPIUtil.setPlaceholders(player, meta.getDisplayName()))));
        if (meta.getTitle() != null)
            meta.setTitle(BukkitComponentSerializer.legacy().serialize(MiniMessage.miniMessage().parse(PAPIUtil.setPlaceholders(player, meta.getTitle()))));
        if (meta.getAuthor() != null)
            meta.setAuthor(BukkitComponentSerializer.legacy().serialize(MiniMessage.miniMessage().parse(PAPIUtil.setPlaceholders(player, meta.getAuthor()))));
        if (meta.getLore() != null)
            meta.setLore(getColoredLore(meta.getLore(), player));
    }

    private static List<String> getColoredLore(List<String> lore, Player player) {
        for (int i = 0; i < lore.size(); i++)
            lore.set(i, BukkitComponentSerializer.legacy().serialize(MiniMessage.miniMessage().parse(PAPIUtil.setPlaceholders(player, lore.get(i)))));
        return lore;
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
}
