package net.grilledham.chattabs.tabs;

import com.google.gson.annotations.Expose;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.util.List;
import java.util.function.Predicate;
import java.util.regex.PatternSyntaxException;

public class ChatLineFilter {
	
	@Expose
	private boolean filterMessages = false;
	
	@Expose
	private String regex;
	
	private final Predicate<GuiMessage> filter;
	
	public ChatLineFilter() {
		this(".*");
	}
	
	public ChatLineFilter(String filter, boolean filterMessages) {
		this(filter);
		this.filterMessages = filterMessages;
	}
	
	public ChatLineFilter(String filter) {
		this.regex = filter;
		this.filter = line -> {
			try {
				if(filterMessages) {
					if(line.content().getContents() instanceof TranslatableContents m) {
						return m.getKey().startsWith("commands.message.display") && m.getArgument(0).getString().equals(regex);
					}
					return false;
				}
				return line.content().getString().matches(regex);
			} catch(PatternSyntaxException e) {
				return true;
			}
		};
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
	
	public void setRegex(String regex) {
		this.regex = regex;
	}
	
	public boolean filtersMessages() {
		return filterMessages;
	}
	
	public void filterMessages(boolean filterMessages) {
		this.filterMessages = filterMessages;
	}
}
