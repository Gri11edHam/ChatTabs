package net.grilledham.chattabs.profiles;

import com.google.gson.annotations.Expose;
import net.grilledham.chattabs.config.ChatTabsConfig;
import net.grilledham.chattabs.tabs.ChatTab;

import java.util.ArrayList;
import java.util.List;

public class ServerProfile {
	
	@Expose
	private String serverAddress;
	
	@Expose
	private final List<String> tabs;
	
	public ServerProfile(String serverAddress, List<String> tabs) {
		this.serverAddress = serverAddress;
		this.tabs = tabs;
	}
	
	public ServerProfile(String serverAddress) {
		this(serverAddress, new ArrayList<>());
	}
	
	public ServerProfile() {
		this("");
	}
	
	public String getServerAddress() {
		return serverAddress;
	}
	
	public List<ChatTab> getTabs() {
		return ChatTabsConfig.getInstance().getChatTabs().stream().filter(tab -> tabs.contains(tab.getId())).toList();
	}
	
	public void setServerAddress(String serverAddress) {
		this.serverAddress = serverAddress;
	}
	
	public void addTabId(String id) {
		if(!tabs.contains(id)) tabs.add(id);
	}
	
	public void removeTabId(String id) {
		while(tabs.contains(id)) tabs.remove(id);
	}
	
	public List<String> getTabIds() {
		return tabs;
	}
	
	public void setTabIds(List<String> strings) {
		this.tabs.clear();
		this.tabs.addAll(strings);
	}
}
