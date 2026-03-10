package net.grilledham.chattabs.render;

import net.grilledham.chattabs.config.ChatTabsConfig;
import net.grilledham.chattabs.render.screen.EditChatScreen;
import net.grilledham.chattabs.tabs.ChatTab;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.cursor.StandardCursors;
import net.minecraft.util.math.MathHelper;

public class ChatHudOverlays {
	
	public static int[] renderChatTabs(MinecraftClient client, int tabScroll, DrawContext context, int windowHeight, float chatScale, boolean expanded, int chatWidth, int mouseX, int mouseY, int messages, boolean mcTabUnreads) {
		int hoveredTab = -1;
		if(!ChatTabsConfig.getInstance().enabled || ChatTabsConfig.getInstance().chatTabs.isEmpty() || !expanded || chatScale == 0) return new int[]{hoveredTab, tabScroll};
		
		int height = 13;
		int x = 0;
		int y = MathHelper.floor((windowHeight - 40) / chatScale);
		y -= ((messages * (int)(9 * (client.options.getChatLineSpacing().getValue() + 1))) + height);
		int width = client.textRenderer.getWidth("MC") + 4;
		int scrollerWidth = client.textRenderer.getWidth("<") + 4;
		int tabNum = 0;
		boolean hovered = (mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height) && !(client.currentScreen instanceof EditChatScreen);
		context.fill(x, y, x + width, y + height, hovered ? ChatTabsConfig.getInstance().bgColorHovered.getRGB() : ChatTabsConfig.getInstance().bgColor.getRGB());
		context.drawText(client.textRenderer, "MC", x + 2, y + 2, -1, ChatTabsConfig.getInstance().textShadow);
		if(ChatTabsConfig.getInstance().selectedTab == tabNum) {
			context.fill(x, y - 1, x + width, y, ChatTabsConfig.getInstance().selectedTabColor.getRGB());
		} else if(mcTabUnreads) {
			context.fill(x, y - 1, x + width, y, ChatTabsConfig.getInstance().unreadColor.getRGB());
		}
		if(hovered) {
			hoveredTab = tabNum;
		}
		tabNum++;
		x += width;
		if(tabScroll > -1) {
			hovered = (mouseX >= x && mouseX < x + scrollerWidth && mouseY >= y && mouseY < y + height) && !(client.currentScreen instanceof EditChatScreen);
			context.fill(x, y, x + scrollerWidth, y + height, hovered ? ChatTabsConfig.getInstance().bgColorHovered.getRGB() : ChatTabsConfig.getInstance().bgColor.getRGB());
			context.drawText(client.textRenderer, "<", x + 2, y + 2, -1, ChatTabsConfig.getInstance().textShadow);
			if(hovered) {
				hoveredTab = -2;
			}
			x += scrollerWidth;
		}
		boolean shouldScrollTabs = false;
		for(ChatTab tab : ChatTabsConfig.getInstance().chatTabs) {
			if(tabNum < tabScroll + 1) {
				tabNum++;
				shouldScrollTabs = true;
				continue;
			}
			width = client.textRenderer.getWidth(tab.getName()) + 4;
			if(x + width > chatWidth - scrollerWidth) {
				shouldScrollTabs = true;
				hovered = (mouseX >= chatWidth - scrollerWidth && mouseX < chatWidth && mouseY >= y && mouseY < y + height) && !(client.currentScreen instanceof EditChatScreen);
				context.fill(chatWidth - scrollerWidth, y, chatWidth, y + height, hovered ? ChatTabsConfig.getInstance().bgColorHovered.getRGB() : ChatTabsConfig.getInstance().bgColor.getRGB());
				context.fill(x, y, chatWidth - scrollerWidth, y + height, ChatTabsConfig.getInstance().bgColor.getRGB());
				context.drawText(client.textRenderer, ">", chatWidth - scrollerWidth + 2, y + 2, -1, ChatTabsConfig.getInstance().textShadow);
				if(hovered) {
					hoveredTab = -3;
				}
				break;
			}
			hovered = (mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height) && !(client.currentScreen instanceof EditChatScreen);
			context.fill(x, y, x + width, y + height, hovered ? ChatTabsConfig.getInstance().bgColorHovered.getRGB() : ChatTabsConfig.getInstance().bgColor.getRGB());
			context.drawText(client.textRenderer, tab.getName(), x + 2, y + 2, -1, ChatTabsConfig.getInstance().textShadow);
			if(ChatTabsConfig.getInstance().selectedTab == tabNum) {
				context.fill(x, y - 1, x + width, y, ChatTabsConfig.getInstance().selectedTabColor.getRGB());
			} else if(tab.hasUnreads()) {
				context.fill(x, y - 1, x + width, y, ChatTabsConfig.getInstance().unreadColor.getRGB());
			}
			if(hovered) {
				hoveredTab = tabNum;
			}
			tabNum++;
			x += width;
		}
		
		if(shouldScrollTabs && tabScroll < 0) {
			tabScroll = 0;
		} else if(!shouldScrollTabs) {
			tabScroll = -1;
		}
		
		if(hoveredTab != -1) context.setCursor(StandardCursors.POINTING_HAND);
		
		return new int[]{hoveredTab, tabScroll};
	}
}
