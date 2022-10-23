package net.leonardo_dgs.interactivebooks.util;

import lombok.Getter;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import net.kyori.adventure.platform.bukkit.MinecraftComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.BookMeta.Generation;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class BooksUtils {

    @Getter
    private static final boolean isBookGenerationSupported = MinecraftVersion.getRunningVersion().isAfterOrEqual(MinecraftVersion.parse("1.10"));

    private static final MiniMessage MINI_MESSAGE_LEGACY_TAGS;
    private static final String NMS_VERSION = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
    private static final boolean OLD_PAGES_METHODS = MinecraftVersion.getRunningVersion().isBefore(MinecraftVersion.parse("1.12.2"));
    private static final boolean OLD_ITEM_IN_HAND_METHODS = NMS_VERSION.equals("v1_8_R3");
    private static final Field FIELD_PAGES;

    static {
        Field fieldPages = null;
        if (OLD_PAGES_METHODS) {
            try {
                fieldPages = Class.forName("org.bukkit.craftbukkit." + NMS_VERSION + ".inventory.CraftMetaBook").getDeclaredField("pages");
            } catch (ClassNotFoundException | NoSuchFieldException e) {
                e.printStackTrace();
            }
        }
        FIELD_PAGES = fieldPages;

        HashSet<String> runCommandNames = new HashSet<>(Arrays.asList("run_command", "command", "cmd"));
        HashSet<String> openUrlNames = new HashSet<>(Arrays.asList("url", "link"));
        MINI_MESSAGE_LEGACY_TAGS = MiniMessage.builder().editTags(adder -> {
                    adder.resolver(TagResolver.resolver("tooltip", (argumentQueue, context) ->
                            Tag.styling(HoverEvent.showText(context.deserialize(argumentQueue.pop().value())))));
                    adder.resolver(TagResolver.resolver(runCommandNames, (argumentQueue, context) ->
                            Tag.styling(ClickEvent.runCommand(argumentQueue.pop().value()))));
                    adder.resolver(TagResolver.resolver(openUrlNames, (argumentQueue, context) ->
                            Tag.styling(ClickEvent.openUrl(argumentQueue.pop().value()))));
                }
        ).build();
    }

    @SuppressWarnings({"unchecked", "UnstableApiUsage"})
    public static BookMeta getBookMeta(BookMeta meta, List<String> rawPages, Player player) {
        BookMeta bookMeta = meta.clone();
        setPlaceholders(bookMeta, player);
        if (OLD_PAGES_METHODS) {
            try {
                List<Object> pages = (List<Object>) FIELD_PAGES.get(bookMeta);
                rawPages.forEach(page -> pages.add(MinecraftComponentSerializer.get().serialize(getPage(page, player))));
            } catch (IllegalAccessException e) {
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
        return MINI_MESSAGE_LEGACY_TAGS.deserialize(PAPIUtil.setPlaceholders(player, page));
    }

    private static void setPlaceholders(BookMeta meta, Player player) {
        meta.setDisplayName(BukkitComponentSerializer.legacy().serialize(MiniMessage.miniMessage().deserialize(PAPIUtil.setPlaceholders(player, meta.getDisplayName()))));
        if (meta.getTitle() != null)
            meta.setTitle(BukkitComponentSerializer.legacy().serialize(MiniMessage.miniMessage().deserialize(PAPIUtil.setPlaceholders(player, meta.getTitle()))));
        if (meta.getAuthor() != null)
            meta.setAuthor(BukkitComponentSerializer.legacy().serialize(MiniMessage.miniMessage().deserialize(PAPIUtil.setPlaceholders(player, meta.getAuthor()))));
        if (meta.getLore() != null)
            meta.setLore(getColoredLore(meta.getLore(), player));
    }

    private static List<String> getColoredLore(List<String> lore, Player player) {
        lore.replaceAll(text -> BukkitComponentSerializer.legacy().serialize(MiniMessage.miniMessage().deserialize(PAPIUtil.setPlaceholders(player, text))));
        return lore;
    }

    public static Generation getBookGeneration(String generation) {
        return generation == null ? Generation.ORIGINAL : Generation.valueOf(generation.toUpperCase());
    }

    @SuppressWarnings("deprecation")
    public static ItemStack getItemInMainHand(Player player) {
        if (OLD_ITEM_IN_HAND_METHODS)
            return player.getInventory().getItemInHand();
        else
            return player.getInventory().getItemInMainHand();
    }

    @SuppressWarnings("deprecation")
    public static void setItemInMainHand(Player player, ItemStack item) {
        if (OLD_ITEM_IN_HAND_METHODS)
            player.getInventory().setItemInHand(item);
        else
            player.getInventory().setItemInMainHand(item);
    }
}
