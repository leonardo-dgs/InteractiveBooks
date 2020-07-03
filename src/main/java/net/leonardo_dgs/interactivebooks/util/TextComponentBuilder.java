package net.leonardo_dgs.interactivebooks.util;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.ArrayList;
import java.util.List;

final class TextComponentBuilder {

    private final List<TextComponent> components = new ArrayList<>();
    private ClickEvent nextClickEvent;
    private HoverEvent nextHoverEvent;

    TextComponentBuilder() {

    }

    List<TextComponent> getComponents() {
        return this.components;
    }

    void add(TextComponent component) {
        if (getNextClickEvent() != null)
            component.setClickEvent(this.getNextClickEvent());
        if (getNextHoverEvent() != null)
            component.setHoverEvent(this.getNextHoverEvent());
        this.components.add(component);
    }

    void add(TextComponentBuilder builder) {
        builder.getComponents().forEach(this::add);
    }

    void add(List<TextComponent> components) {
        components.forEach(this::add);
    }

    private ClickEvent getNextClickEvent() {
        return this.nextClickEvent;
    }

    void setNextClickEvent(ClickEvent nextClickEvent) {
        this.nextClickEvent = nextClickEvent;
    }

    private HoverEvent getNextHoverEvent() {
        return this.nextHoverEvent;
    }

    void setNextHoverEvent(HoverEvent nextHoverEvent) {
        this.nextHoverEvent = nextHoverEvent;
    }

}
