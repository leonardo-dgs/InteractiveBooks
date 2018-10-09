package net.leomixer17.interactivebooks.nms;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.ArrayList;
import java.util.List;

final class TextComponentBuilder {
	
	private final List<TextComponent> components = new ArrayList<>();
	private ClickEvent nextClickEvent;
	private HoverEvent nextHoverEvent;
	
	TextComponentBuilder()
	{
	
	}
	
	List<TextComponent> getComponents()
	{
		return this.components;
	}
	
	void add(final TextComponent component)
	{
		if (this.getNextClickEvent() != null)
			component.setClickEvent(this.getNextClickEvent());
		if (this.getNextHoverEvent() != null)
			component.setHoverEvent(this.getNextHoverEvent());
		this.components.add(component);
	}
	
	void add(final TextComponentBuilder builder)
	{
		builder.getComponents().forEach(component -> this.add(component));
	}
	
	void add(final List<TextComponent> components)
	{
		components.forEach(component -> this.add(component));
	}
	
	public ClickEvent getNextClickEvent()
	{
		return this.nextClickEvent;
	}
	
	public void setNextClickEvent(final ClickEvent nextClickEvent)
	{
		this.nextClickEvent = nextClickEvent;
	}
	
	public HoverEvent getNextHoverEvent()
	{
		return this.nextHoverEvent;
	}
	
	public void setNextHoverEvent(final HoverEvent nextHoverEvent)
	{
		this.nextHoverEvent = nextHoverEvent;
	}
	
}
