package net.leonardo_dgs.interactivebooks.util;

import lombok.Getter;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import net.kyori.adventure.platform.bukkit.MinecraftComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.template.TemplateResolver;
import net.kyori.adventure.text.minimessage.transformation.Transformation;
import net.kyori.adventure.text.minimessage.transformation.TransformationFactory;
import net.kyori.adventure.text.minimessage.transformation.TransformationType;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.BookMeta.Generation;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class BooksUtils {

    @Getter
    private static final boolean isBookGenerationSupported = MinecraftVersion.getRunningVersion().isAfterOrEqual(MinecraftVersion.parse("1.10"));

    private static final MiniMessage MINI_MESSAGE;
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

        Predicate<String> names = TransformationType.acceptingNames("tooltip", "show text", "run command", "run_command", "command", "cmd", "open url", "url", "link", "change page");
        MINI_MESSAGE = MiniMessage.builder().transformations(builder -> builder.add(TransformationType.transformationType(names, (TransformationFactory<Transformation>) (ctx, name, args) -> new Transformation() {
            @Override
            public Component apply() {
                switch (name) {
                    case "tooltip":
                    case "show text":
                        return Component.text().hoverEvent(HoverEvent.showText(ctx.parse(args.get(0).value()))).build();
                    case "run command":
                    case "run_command":
                    case "command":
                    case "cmd":
                        return Component.text().clickEvent(ClickEvent.runCommand(args.get(0).value())).build();
                    case "open url":
                    case "url":
                    case "link":
                        return Component.text().clickEvent(ClickEvent.openUrl(args.get(0).value())).build();
                    case "change page":
                        return Component.text().clickEvent(ClickEvent.changePage(args.get(0).value())).build();
                }
                return null;
            }

            @Override
            public boolean equals(Object o) {
                return this == o;
            }

            @Override
            public int hashCode() {
                return 0;
            }
        }))).build();
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
        return MINI_MESSAGE.deserialize(PAPIUtil.setPlaceholders(player, page), TemplateResolver.resolving("br", "\n"));
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
