package net.grilledham.chattabs.mixininterface;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;

public interface IChatHud {
	
	boolean chatTabs$mouseClicked(Click click, boolean doubled);
	boolean chatTabs$mouseReleased(Click click);
	boolean chatTabs$mouseDragged(Click click, double deltaX, double deltaY);
	
	void chatTabs$renderContextMenu(DrawContext context, int windowWidth, int windowHeight, int mouseX, int mouseY, float deltaTicks);
	
	void chatTabs$renderDummy(DrawContext context, TextRenderer textRenderer, int ticks, int mouseX, int mouseY, boolean checked);
}
