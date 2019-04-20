package net.leomixer17.interactivebooks.nms;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.PlaceholderAPIPlugin;
import net.leomixer17.interactivebooks.InteractiveBooks;
import net.md_5.bungee.api.chat.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.BookMeta.Generation;

import java.util.ArrayList;
import java.util.List;

public interface IBooksUtils {

    String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];

    static IBooksUtils setupIBooksUtils()
    {
        try
        {
            return (IBooksUtils) Class.forName(InteractiveBooks.getPlugin().getClass().getPackage().getName() + ".nms.IBooksUtils_" + version).newInstance();
        }
        catch (InstantiationException | IllegalAccessException | ClassNotFoundException e)
        {
            return null;
        }
    }

    void openBook(final ItemStack book, final Player player);

    BookMeta getBookMeta(final BookMeta meta, final List<String> rawPages, final Player player);

    static boolean hasPlaceholderAPISupport()
    {
        return Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null && Bukkit.getPluginManager().getPlugin("PlaceholderAPI") instanceof PlaceholderAPIPlugin;
    }

    static void replacePlaceholders(final BookMeta meta, final Player player)
    {
        meta.setDisplayName(PlaceholderAPI.setPlaceholders((OfflinePlayer) player, meta.getDisplayName()));
        meta.setTitle(PlaceholderAPI.setPlaceholders((OfflinePlayer) player, meta.getTitle()));
        meta.setAuthor(PlaceholderAPI.setPlaceholders((OfflinePlayer) player, meta.getAuthor()));
        meta.setLore(PlaceholderAPI.setPlaceholders((OfflinePlayer) player, meta.getLore()));
    }

    static void replaceColorCodes(final BookMeta meta)
    {
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', meta.getDisplayName()));
        meta.setTitle(ChatColor.translateAlternateColorCodes('&', meta.getTitle()));
        meta.setAuthor(ChatColor.translateAlternateColorCodes('&', meta.getAuthor()));
        for (int i = 0; i < meta.getLore().size(); i++)
            meta.getLore().set(i, ChatColor.translateAlternateColorCodes('&', meta.getLore().get(i)));
    }

    static boolean hasBookGenerationSupport()
    {
        return !version.equals("v1_8_R2") && !version.equals("v1_8_R3") && !version.equals("v1_9_R1") && !version.equals("v1_9_R2");
    }

    static Generation getBookGeneration(final String generation)
    {
        return generation == null ? Generation.ORIGINAL : Generation.valueOf(generation.toUpperCase());
    }

    @SuppressWarnings("deprecation")
    static ItemStack getItemInMainHand(final Player player)
    {
        if (version.equals("v1_8_R2") || version.equals("v1_8_R3"))
            return player.getInventory().getItemInHand();
        return player.getInventory().getItemInMainHand();
    }

    @SuppressWarnings("deprecation")
    static void setItemInMainHand(final Player player, final ItemStack item)
    {
        if (version.equals("v1_8_R2") || version.equals("v1_8_R3"))
        {
            player.getInventory().setItemInHand(item);
            return;
        }
        player.getInventory().setItemInMainHand(item);
    }

    static List<String> getPages(final BookMeta meta)
    {
        final List<String> plainPages = new ArrayList<>();
        final List<BaseComponent[]> components = meta.spigot().getPages();
        components.forEach(component -> plainPages.add(getPage(component)));
        return plainPages;
    }

    static String getPage(final BaseComponent[] components)
    {
        final StringBuilder sb = new StringBuilder();
        for (final BaseComponent component : components)
            sb.append(BaseComponentSerializer.toString(component));
        return sb.toString();
    }

    List<?> getPages(final BookMeta meta, final List<String> rawPages, final Player player);

    static TextComponent[] getPage(final String page, final Player player)
    {
        final String[] plainRows = page.split("\n");
        final TextComponentBuilder compBuilder = new TextComponentBuilder();
        for (int i = 0; i < plainRows.length; i++)
            compBuilder.add(parseRow(plainRows[i] + (i < plainRows.length - 1 ? "\n" : ""), player));
        return convertListToArray(compBuilder.getComponents());
    }

    static TextComponentBuilder parseRow(String plainRow, final Player player)
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

    static Character getEscapedChar(final String escapeCode)
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

    static String setPlaceholders(final Player player, final String text, final boolean papi)
    {
        if (papi)
            return PlaceholderAPI.setPlaceholders(player, text);
        else
            return ChatColor.translateAlternateColorCodes('&', text);
    }

    static ClickEvent getClickEvent(final String type, final String value)
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

    static HoverEvent getHoverEvent(final String type, final String value)
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

    static boolean isValidTag(final String tag)
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

    static boolean isClickEvent(final String type)
    {
        switch (type.toLowerCase())
        {
            case "tooltip":
            case "show text":
                return false;
        }
        return true;
    }

    static TextComponent[] convertListToArray(final List<TextComponent> list)
    {
        final TextComponent[] objects = new TextComponent[list.size()];
        for (int i = 0; i < list.size(); i++)
            objects[i] = list.get(i);
        return objects;
    }

}
