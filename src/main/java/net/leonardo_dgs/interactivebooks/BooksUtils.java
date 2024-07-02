package net.leonardo_dgs.interactivebooks;

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
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.BookMeta.Generation;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class BooksUtils {
    @Getter
    private static final boolean isPluginSupported;
    @Getter
    private static final boolean isBookGenerationSupported = classExists("org.bukkit.inventory.meta.BookMeta.Generation");
    @Getter
    private static final boolean isOffHandSupported = methodExists("org.bukkit.inventory.PlayerInventory", "getItemInOffHand");
    @Getter
    private static final boolean isPlayerOpenBookSupported = methodExists("org.bukkit.entity.Player", "openBook", ItemStack.class);
    private static final boolean isPlayerGetLocaleSupported = methodExists("org.bukkit.entity.Player", "getLocale");
    private static final boolean isBookMetaSpigotSupported = classExists("org.bukkit.inventory.meta.BookMeta$Spigot");

    private static final MiniMessage MINI_MESSAGE;
    private static final Plugin PAPI_PLUGIN = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");
    private static final Field PAGES_FIELD;

    static {
        Field pagesField = null;
        if (!isBookMetaSpigotSupported) {
            try {
                String nmsVersion = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
                pagesField = Class.forName("org.bukkit.craftbukkit." + nmsVersion + ".inventory.CraftMetaBook").getDeclaredField("pages");
            } catch (ClassNotFoundException | NoSuchFieldException ignored) {

            }
        }
        PAGES_FIELD = pagesField;
        isPluginSupported = isBookMetaSpigotSupported || PAGES_FIELD != null;

        HashMap<String, String> tokens = new HashMap<>();
        tokens.put("0", "<black>");
        tokens.put("1", "<dark_blue>");
        tokens.put("2", "<dark_green>");
        tokens.put("3", "<dark_aqua>");
        tokens.put("4", "<dark_red>");
        tokens.put("5", "<dark_purple>");
        tokens.put("6", "<gold>");
        tokens.put("7", "<gray>");
        tokens.put("8", "<dark_gray>");
        tokens.put("9", "<blue>");
        tokens.put("a", "<green>");
        tokens.put("A", "<green>");
        tokens.put("b", "<aqua>");
        tokens.put("B", "<aqua>");
        tokens.put("c", "<red>");
        tokens.put("C", "<red>");
        tokens.put("d", "<light_purple>");
        tokens.put("D", "<light_purple>");
        tokens.put("e", "<yellow>");
        tokens.put("E", "<yellow>");
        tokens.put("f", "<white>");
        tokens.put("F", "<white>");
        tokens.put("k", "<obf>");
        tokens.put("K", "<obf>");
        tokens.put("l", "<b>");
        tokens.put("L", "<b>");
        tokens.put("m", "<st>");
        tokens.put("M", "<st>");
        tokens.put("n", "<u>");
        tokens.put("N", "<u>");
        tokens.put("o", "<i>");
        tokens.put("O", "<i>");
        tokens.put("r", "<reset>");
        tokens.put("R", "<reset>");
        tokens.put("[xX](([&§][0-9a-fA-F]){6})", "");
        String patternString = "[&§](" + String.join("|", tokens.keySet()) + ")";
        Pattern pattern = Pattern.compile(patternString);

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
        ).preProcessor(s -> {
            Matcher matcher = pattern.matcher(s);
            StringBuffer sb = new StringBuffer();
            while (matcher.find()) {
                matcher.appendReplacement(sb, matcher.group(2) == null ? tokens.get(matcher.group(1)) : "<#" + matcher.group(2).replaceAll("[&§]", "") + ">");
            }
            matcher.appendTail(sb);
            return sb.toString();
        }).build();
    }

    private static boolean classExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static boolean methodExists(String className, String methodName, Class<?>... parameterTypes) {
        try {
            Class.forName(className).getMethod(methodName, parameterTypes);
            return true;
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            return false;
        }
    }

    @SuppressWarnings({"unchecked", "UnstableApiUsage"})
    static BookMeta getBookMeta(BookMeta meta, List<String> plainPages, Player player) {
        BookMeta bookMeta = meta.clone();
        setPlaceholders(bookMeta, player);
        if (isBookMetaSpigotSupported) {
            plainPages.forEach(page -> bookMeta.spigot().addPage(BungeeComponentSerializer.get().serialize(deserialize(page, player))));
        } else {
            try {
                List<Object> pages = (List<Object>) PAGES_FIELD.get(bookMeta);
                plainPages.forEach(page -> pages.add(MinecraftComponentSerializer.get().serialize(deserialize(page, player))));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return bookMeta;
    }

    @SuppressWarnings({"unchecked", "UnstableApiUsage"})
    static List<String> getPages(BookMeta meta) {
        List<String> plainPages = new ArrayList<>();
        if (isBookMetaSpigotSupported) {
            List<BaseComponent[]> pages = meta.spigot().getPages();
            pages.forEach(page -> plainPages.add(MiniMessage.miniMessage().serialize(BungeeComponentSerializer.get().deserialize(page))));
        } else {
            try {
                List<Object> pages = (List<Object>) PAGES_FIELD.get(meta);
                pages.forEach(page -> plainPages.add(MiniMessage.miniMessage().serialize(MinecraftComponentSerializer.get().deserialize(page))));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return plainPages;
    }

    static Component deserialize(String text, Player player, TagResolver... tagResolvers) {
        if (text == null)
            return Component.text("");
        return MINI_MESSAGE.deserialize(setPlaceholders(text, player), tagResolvers);
    }

    static String setPlaceholders(String text, CommandSender sender) {
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

    static Generation getBookGeneration(String generation) {
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
    static ItemStack getItemInHand(Player player, EquipmentSlot hand) {
        if (!isOffHandSupported)
            return player.getInventory().getItemInHand();
        else if (hand == EquipmentSlot.HAND)
            return player.getInventory().getItemInMainHand();
        else
            return player.getInventory().getItemInOffHand();
    }

    @SuppressWarnings("deprecation")
    static void setItemInHand(Player player, ItemStack item, EquipmentSlot hand) {
        if (!isOffHandSupported)
            player.getInventory().setItemInHand(item);
        else if (hand == EquipmentSlot.HAND)
            player.getInventory().setItemInMainHand(item);
        else
            player.getInventory().setItemInOffHand(item);
    }

    static String getLocale(Player player) {
        if (isPlayerGetLocaleSupported)
            return player.getLocale();
        else
            return null;
    }

    private BooksUtils() {

    }
}
