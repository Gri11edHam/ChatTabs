package net.grilledham.chattabs.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class ModMenuIntegration implements ModMenuApi {
	
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return parentScreen -> ChatTabsConfig.getInstance().generateConfig().setParentScreen(parentScreen).build();
	}
}
