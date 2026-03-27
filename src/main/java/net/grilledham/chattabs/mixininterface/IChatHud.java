package net.grilledham.chattabs.mixininterface;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;

public interface IChatHud {
	
	boolean chatTabs$mouseClicked(MouseButtonEvent click, boolean doubled);
	boolean chatTabs$mouseReleased(MouseButtonEvent click);
	boolean chatTabs$mouseDragged(MouseButtonEvent click, double deltaX, double deltaY);
	
	void chatTabs$renderContextMenu(GuiGraphicsExtractor context, int windowWidth, int windowHeight, int mouseX, int mouseY, float deltaTicks);
	
	void chatTabs$renderDummy(GuiGraphicsExtractor context, Font textRenderer, int ticks, int mouseX, int mouseY, boolean checked);
}
