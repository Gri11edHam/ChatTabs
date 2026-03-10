package net.grilledham.chattabs.client;

import net.fabricmc.api.ClientModInitializer;
import net.grilledham.chattabs.config.ChatTabsConfig;

public class ChatTabsClient implements ClientModInitializer {
	
	@Override
	public void onInitializeClient() {
		ChatTabsConfig.load();
		Runtime.getRuntime().addShutdownHook(new Thread(ChatTabsConfig::save));
	}
}
