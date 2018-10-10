package net.leomixer17.interactivebooks.nms;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;

import java.util.List;

final class BaseComponentSerializer {
    
    static String toString(final BaseComponent... components)
    {
        final StringBuilder sb = new StringBuilder();
        for (final BaseComponent component : components)
            sb.append(toString(component));
        return sb.toString();
    }
    
    static String toString(final List<BaseComponent> components)
    {
        final StringBuilder sb = new StringBuilder();
        components.forEach(component -> sb.append(toString(component)));
        return sb.toString();
    }
    
    static String toString(final BaseComponent component)
    {
        final StringBuilder sb = new StringBuilder();
        sb.append(getClickEvent(component));
        sb.append(getHoverEvent(component));
        sb.append(getColorAndFormatting(component) + component.toPlainText());
        sb.append("<reset>");
        return sb.toString();
    }
    
    private static String getColorAndFormatting(final BaseComponent component)
    {
        final StringBuilder sb = new StringBuilder();
        
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
    
    private static String getClickEvent(final BaseComponent component)
    {
        final ClickEvent event = component.getClickEvent();
        if (event == null)
            return "";
        final String action = event.getAction().toString().replace("_", " ").toLowerCase();
        final String value = event.getValue();
        return "<" + action + ":" + value + ">";
    }
    
    private static String getHoverEvent(final BaseComponent component)
    {
        final HoverEvent event = component.getHoverEvent();
        if (event == null)
            return "";
        final String action = event.getAction().toString().replace("_", " ").toLowerCase();
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
