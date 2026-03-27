package net.grilledham.chattabs.tabs;

import com.google.gson.annotations.Expose;
import net.grilledham.chattabs.config.ChatTabsConfig;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import org.apache.commons.compress.utils.Lists;

import java.util.List;

public class ChatTab {
	
	@Expose
	private String name;
	@Expose
	private boolean save;
	@Expose
	private ChatLineFilter filter;
	@Expose
	private SendModifier sendModifier;
	
	private final List<GuiMessage.Line> visibleLines = Lists.newArrayList();
	
	private boolean firstMessageUnread = true;
	private GuiMessage.Line lastSeenLine;
	
	public ChatTab(String name, boolean save, ChatLineFilter filter, SendModifier sendModifier) {
		this.name = name;
		this.save = save;
		this.filter = filter;
		this.sendModifier = sendModifier;
	}
	
	public ChatTab(String name, boolean save) {
		this(name, save, new ChatLineFilter(), new SendModifier());
	}
	
	public ChatTab() {
		this("New Tab", true);
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void setSave(boolean save) {
		this.save = save;
	}
	
	public String getName() {
		return name;
	}
	
	public boolean shouldSave() {
		return save;
	}
	
	public ChatLineFilter getFilter() {
		return filter;
	}
	
	public SendModifier getSendModifier() {
		return sendModifier;
	}
	
	public List<GuiMessage> filterChat(List<GuiMessage> chatLines) {
		return filter.filterChat(chatLines);
	}
	
	public void addChatLine(GuiMessage.Line line) {
		visibleLines.addFirst(line);
		while(visibleLines.size() > ChatTabsConfig.getInstance().maxLines) {
			visibleLines.removeLast();
		}
	}
	
	public void setFocused(boolean focused) {
		if(focused) {
			lastSeenLine = null;
			firstMessageUnread = false;
		} else if(!visibleLines.isEmpty()) {
			lastSeenLine = visibleLines.getFirst();
			firstMessageUnread = false;
		} else {
			firstMessageUnread = true;
		}
	}
	
	public List<GuiMessage.Line> getVisibleChatLines() {
		return visibleLines;
	}
	
	public String modifySend(String msg) {
		return sendModifier.apply(msg);
	}
	
	public void clear(boolean totalClear) {
		visibleLines.clear();
		firstMessageUnread = true;
	}
	
	public int getLastSeenMessage() {
		if(firstMessageUnread) return visibleLines.size() - 1;
		if(lastSeenLine == null) return 0;
		return visibleLines.indexOf(lastSeenLine);
	}
	
	public boolean hasUnreads() {
		return (lastSeenLine != null && visibleLines.indexOf(lastSeenLine) > 0) || (!visibleLines.isEmpty() && firstMessageUnread);
	}
}
