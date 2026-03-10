package net.grilledham.chattabs.mixininterface;

import net.minecraft.client.font.DrawnTextConsumer;
import net.minecraft.text.Style;

public interface IChatHudDrawer {
	
	Style chatTabs$getStyle();
	DrawnTextConsumer chatTabs$getDrawer();
}
