package net.grilledham.chattabs.tabs;

import com.google.gson.annotations.Expose;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.util.List;
import java.util.function.Predicate;
import java.util.regex.PatternSyntaxException;

public class ChatLineFilter {
	
	@Expose
	private boolean filterMessages;
	
	@Expose
	private String regex;
	
	@Expose
	private ColorFilter colorFilter;
	@Expose
	private int hexColor;
	
	private final Predicate<GuiMessage> filter;
	
	public ChatLineFilter() {
		this(".*");
	}
	
	public ChatLineFilter(String filter, ColorFilter colorFilter, int hexColor, boolean filterMessages) {
		this.regex = filter;
		this.colorFilter = colorFilter;
		this.hexColor = hexColor;
		this.filter = line -> {
			try {
				boolean colorMatches = matchesLineColor(line);
				if(filterMessages) {
					if(line.content().getContents() instanceof TranslatableContents m) {
						return m.getKey().startsWith("commands.message.display") && m.getArgument(0).getString().equals(regex) && colorMatches;
					}
					return false;
				}
				return line.content().getString().matches(regex) && colorMatches;
			} catch(PatternSyntaxException e) {
				return true;
			}
		};
		this.filterMessages = filterMessages;
	}
	
	private boolean matchesLineColor(GuiMessage line) {
		boolean colorMatches = true;
		TextColor color = line.content().getStyle().getColor();
		if(color != null) {
			if(this.colorFilter == ColorFilter.HEX) {
				colorMatches = color.getValue() == this.hexColor;
			} else if(this.colorFilter != ColorFilter.DISABLED) {
				colorMatches = color.getValue() == this.colorFilter.getColor();
			}
		} else if(this.colorFilter != ColorFilter.DISABLED) {
			if(this.colorFilter == ColorFilter.HEX) {
				colorMatches = 0xFFFFFF == this.hexColor;
			} else {
				colorMatches = this.colorFilter == ColorFilter.WHITE;
			}
		}
		return colorMatches;
	}
	
	public ChatLineFilter(String filter, boolean filterMessages) {
		this(filter, ColorFilter.DISABLED, 0xFFFFFF, filterMessages);
	}
	
	public ChatLineFilter(String filter) {
		this(filter, false);
	}
	
	public List<GuiMessage> filterChat(List<GuiMessage> chatLines) {
		return chatLines.stream().filter(filter).toList();
	}
	
	public boolean test(GuiMessage message) {
		return filter.test(message);
	}
	
	public String getRegex() {
		return regex;
	}
	
	public ColorFilter getColorFilter() {
		return colorFilter;
	}
	
	public int getHexColor() {
		return this.hexColor;
	}
	
	public void setRegex(String regex) {
		this.regex = regex;
	}
	
	public void setColorFilter(ColorFilter colorFilter) {
		this.colorFilter = colorFilter;
	}
	
	public void setHexColor(int hexColor) {
		this.hexColor = hexColor;
	}
	
	public boolean filtersMessages() {
		return filterMessages;
	}
	
	public void filterMessages(boolean filterMessages) {
		this.filterMessages = filterMessages;
	}
	
	public enum ColorFilter {
		DISABLED,
		HEX,
		BLACK(0x000000),
		DARK_BLUE(0x0000AA),
		DARK_GREEN(0x00AA00),
		DARK_AQUA(0x00AAAA),
		DARK_RED(0xAA0000),
		DARK_PURPLE(0xAA00AA),
		GOLD(0xFFAA00),
		GRAY(0xAAAAAA),
		DARK_GRAY(0x555555),
		BLUE(0x5555FF),
		GREEN(0x55FF55),
		AQUA(0x55FFFF),
		RED(0xFF5555),
		LIGHT_PURPLE(0xFF55FF),
		YELLOW(0xFFFF55),
		WHITE(0xFFFFFF);
		
		private final int color;
		
		ColorFilter(int color) {
			this.color = color;
		}
		
		ColorFilter() {
			this(-1);
		}
		
		public int getColor() {
			return color;
		}
	}
}
