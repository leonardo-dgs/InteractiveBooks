package net.leomixer17.interactivebooks.util;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;

import java.util.List;

final class BaseComponentSerializer {

    private static String toString(BaseComponent... components)
    {
        StringBuilder sb = new StringBuilder();
        for (BaseComponent component : components)
            sb.append(toString(component));
        return sb.toString();
    }

    static String toString(List<BaseComponent> components)
    {
        StringBuilder sb = new StringBuilder();
        components.forEach(component -> sb.append(toString(component)));
        return sb.toString();
    }

    static String toString(BaseComponent component)
    {
        return getClickEvent(component) +
                getHoverEvent(component) +
                getColorAndFormatting(component) + component.toPlainText() +
                "<reset>";
    }

    private static String getColorAndFormatting(BaseComponent component)
    {
        StringBuilder sb = new StringBuilder();

        sb.append(component.getColor());

        if (component.isBold())
            sb.append(ChatColor.BOLD);
        if (component.isItalic())
            sb.append(ChatColor.ITALIC);
        if (component.isUnderlined())
            sb.append(ChatColor.UNDERLINE);
        if (component.isStrikethrough())
            sb.append(ChatColor.STRIKETHROUGH);
        if (component.isObfuscated())
            sb.append(ChatColor.MAGIC);

        return sb.toString();
    }

    private static String getClickEvent(BaseComponent component)
    {
        ClickEvent event = component.getClickEvent();
        if (event == null)
            return "";
        String action = event.getAction().toString().replace("_", " ").toLowerCase();
        String value = event.getValue();
        return "<" + action + ":" + value + ">";
    }

    private static String getHoverEvent(BaseComponent component)
    {
        HoverEvent event = component.getHoverEvent();
        if (event == null)
            return "";
        String action = event.getAction().toString().replace("_", " ").toLowerCase();
        String value = null;
        switch (event.getAction())
        {
            case SHOW_ACHIEVEMENT:
                return "";
            case SHOW_ENTITY:
                return "";
            case SHOW_ITEM:
                return "";
            case SHOW_TEXT:
                value = toString(event.getValue());
        }
        return "<" + action + ":" + value + ">";
    }

}
