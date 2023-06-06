package net.leonardo_dgs.interactivebooks.util;

import lombok.Getter;
import me.clip.placeholderapi.PlaceholderAPI;
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
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.BookMeta.Generation;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public final class BooksUtils {
    @Getter
    private static final boolean isBookGenerationSupported = MinecraftVersion.getRunningVersion().isAfterOrEqual(MinecraftVersion.parse("1.10"));

    private static final MiniMessage MINI_MESSAGE;
    private static final Plugin PAPI_PLUGIN = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");
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
        MINI_MESSAGE = MiniMessage.builder().editTags(adder -> {
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
                rawPages.forEach(page -> pages.add(MinecraftComponentSerializer.get().serialize(deserialize(page, player))));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        } else {
            rawPages.forEach(page -> bookMeta.spigot().addPage(BungeeComponentSerializer.get().serialize(deserialize(page, player))));
        }
        return bookMeta;
    }

    public static List<String> getPages(BookMeta meta) {
        List<String> plainPages = new ArrayList<>();
        List<BaseComponent[]> components = meta.spigot().getPages();
        components.forEach(component -> plainPages.add(MiniMessage.miniMessage().serialize(BungeeComponentSerializer.get().deserialize(component))));
        return plainPages;
    }

    public static Component deserialize(String text, Player player, TagResolver... tagResolvers) {
        if (text == null)
            return Component.text("");
        char[] chars = text.toCharArray();
        List<String> replacements = Arrays.asList("<black>", "<dark_blue>", "<dark_green>", "<dark_aqua>", "<dark_red>", "<dark_purple>", "<gold>", "<gray>", "<dark_gray>", "<blue>", "<green>", "<aqua>", "<red>", "<light_purple>", "<yellow>", "<white>", "<obf>", "<b>", "<st>", "<u>", "<i>", "<reset>");

        StringBuilder sb = new StringBuilder(chars.length);
        for (int i = 0; i < chars.length; i++) {
            int index;
            if ((chars[i] == '&' || chars[i] == '§') && (index = "0123456789abcdefklmnorx".indexOf(chars[i + 1])) > -1) {
                if (chars[i + 1] == 'x') {
                    sb.append("<#").append(text, i + 2, i + 8).append('>');
                    i += 7;
                } else {
                    if (index <= 15 || index == 22)
                        sb.append("<reset>");
                    sb.append(replacements.get(index));
                    i++;
                }
            } else {
                sb.append(chars[i]);
            }
        }
        return MINI_MESSAGE.deserialize(setPlaceholders(sb.toString(), player), tagResolvers);
    }

    public static String setPlaceholders(String text, CommandSender sender) {
        if (PAPI_PLUGIN != null && PAPI_PLUGIN.isEnabled())
            return PlaceholderAPI.setPlaceholders(sender instanceof OfflinePlayer ? (OfflinePlayer) sender : null, text);
        else
            return text;
    }

    private static void setPlaceholders(BookMeta meta, Player player) {
        meta.setDisplayName(BukkitComponentSerializer.legacy().serialize(deserialize(meta.getDisplayName(), player)));
        if (meta.getTitle() != null)
            meta.setTitle(BukkitComponentSerializer.legacy().serialize(deserialize(meta.getTitle(), player)));
        if (meta.getAuthor() != null)
            meta.setAuthor(BukkitComponentSerializer.legacy().serialize(deserialize(meta.getAuthor(), player)));
        if (meta.getLore() != null) {
            List<String> lore = meta.getLore();
            lore.replaceAll(text -> BukkitComponentSerializer.legacy().serialize(deserialize(text, player)));
            meta.setLore(lore);
        }
    }

    public static Generation getBookGeneration(String generation) {
        if (generation == null)
            return null;
        generation = generation.toUpperCase();
        switch (generation) {
            case "ORIGINAL":
                return Generation.ORIGINAL;
            case "COPY_OF_ORIGINAL":
                return Generation.COPY_OF_ORIGINAL;
            case "COPY_OF_COPY":
                return Generation.COPY_OF_COPY;
            case "TATTERED":
                return Generation.TATTERED;
            default:
                return null;
        }
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

    private BooksUtils() {

    }
}
