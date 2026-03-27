package net.grilledham.chattabs.mixininterface;

import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.network.chat.Style;

public interface IChatHudDrawer {
	
	Style chatTabs$getStyle();
	ActiveTextCollector chatTabs$getDrawer();
}
