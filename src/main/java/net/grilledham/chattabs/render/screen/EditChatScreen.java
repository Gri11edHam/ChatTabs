package net.grilledham.chattabs.render.screen;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.grilledham.chattabs.config.ChatTabsConfig;
import net.grilledham.chattabs.mixininterface.IChatHud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class EditChatScreen extends Screen {
	
	public static final List<GuiMessage.Line> DUMMY_CHAT;
	
	static {
		DUMMY_CHAT = new ArrayList<>(100);
		for(int i = 0; i < 100; i++) {
			GuiMessage message = new GuiMessage(0, Component.literal("Line " + (i + 1)), null, GuiMessageSource.SYSTEM_CLIENT, GuiMessageTag.systemSinglePlayer());
			DUMMY_CHAT.add(new GuiMessage.Line(message, message.content().getVisualOrderText(), true));
		}
	}
	
	private final Screen parent;
	
	private final Checkbox editFocusedWidget;
	
	private float topEdgeTicks = 0;
	private float rightEdgeTicks = 0;
	
	private double dragStartX;
	private double dragStartY;
	
	private int dragStartWidth;
	private int dragStartHeight;
	
	// -1 - not dragging, 0 - drag height, 1 - drag width
	private int dragging = -1;
	
	public EditChatScreen(Screen parent) {
		super(Component.translatable("chattabs.editchatscreen"));
		this.parent = parent;
		editFocusedWidget = Checkbox.builder(Component.translatable("chattabs.editchatscreen.editfocused"), font)
				.pos(4, height - 30)
				.selected(true)
				.build();
		addRenderableWidget(editFocusedWidget);
	}
	
	@Override
	protected void init() {
		editFocusedWidget.setPosition(4, height - 30);
		addRenderableWidget(editFocusedWidget);
		
		int lineHeight = (int)(9 * (minecraft.options.chatLineSpacing().get() + 1));
		ChatTabsConfig.getInstance().chatHeightFocused = (ChatTabsConfig.getInstance().chatHeightFocused / lineHeight) * lineHeight;
		ChatTabsConfig.getInstance().chatHeightUnfocused = (ChatTabsConfig.getInstance().chatHeightUnfocused / lineHeight) * lineHeight;
	}
	
	@Override
	public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks) {
		((IChatHud)minecraft.gui.getChat()).chatTabs$renderDummy(context, this.font, this.minecraft.gui.getGuiTicks(), mouseX, mouseY, editFocusedWidget.selected());
		context.requestCursor(CursorTypes.ARROW);
		super.extractRenderState(context, mouseX, mouseY, deltaTicks);
		
		context.pose().pushMatrix();
		
		int chatWidth = ChatTabsConfig.getInstance().chatWidth + (int)(12 * minecraft.options.chatScale().get());
		int chatY = height - 41;
		int chatHeight = (editFocusedWidget.selected() ? ChatTabsConfig.getInstance().chatHeightFocused : ChatTabsConfig.getInstance().chatHeightUnfocused);
		int chatVisualHeight = (int)(chatHeight * minecraft.options.chatScale().get());
		// top edge
		context.horizontalLine(0, chatWidth, chatY - chatVisualHeight, fade(-1, topEdgeTicks));
		if((mouseX >= 0 && mouseX < chatWidth && mouseY >= chatY - chatVisualHeight - 3 && mouseY < chatY - chatVisualHeight + 3) || dragging == 0) {
			context.requestCursor(CursorTypes.RESIZE_NS);
			topEdgeTicks += deltaTicks / 2f;
			if(topEdgeTicks > 1) {
				topEdgeTicks = 1;
			}
		} else {
			topEdgeTicks -= deltaTicks / 2f;
			if(topEdgeTicks < 0) {
				topEdgeTicks = 0;
			}
		}
		// right edge
		context.verticalLine(chatWidth, chatY - chatVisualHeight, chatY, fade(-1, rightEdgeTicks));
		if((mouseX >= chatWidth - 3 && mouseX < chatWidth + 3 && mouseY >= chatY - chatVisualHeight && mouseY < chatY) || dragging == 1) {
			context.requestCursor(CursorTypes.RESIZE_EW);
			rightEdgeTicks += deltaTicks / 2f;
			if(rightEdgeTicks > 1) {
				rightEdgeTicks = 1;
			}
		} else {
			rightEdgeTicks -= deltaTicks / 2f;
			if(rightEdgeTicks < 0) {
				rightEdgeTicks = 0;
			}
		}
		
		context.pose().popMatrix();
	}
	
	@Override
	public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
		if(minecraft.options.chatScale().get() == 0) return super.mouseClicked(click, doubled);
		int chatWidth = ChatTabsConfig.getInstance().chatWidth + 12;
		int chatY = height - 41;
		int chatHeight = (editFocusedWidget.selected() ? ChatTabsConfig.getInstance().chatHeightFocused : ChatTabsConfig.getInstance().chatHeightUnfocused);
		int chatVisualHeight = (int)(chatHeight * minecraft.options.chatScale().get());
		if(click.x() >= 0 && click.x() < chatWidth && click.y() >= chatY - chatVisualHeight - 3 && click.y() < chatY - chatVisualHeight + 3) {
			dragging = 0;
			dragStartX = click.x();
			dragStartY = click.y() / minecraft.options.chatScale().get();
			dragStartHeight = chatHeight;
			return true;
		} else if(click.x() >= chatWidth - 3 && click.x() < chatWidth + 3 && click.y() >= chatY - chatVisualHeight && click.y() < chatY) {
			dragging = 1;
			dragStartX = click.x();
			dragStartY = click.y() / minecraft.options.chatScale().get();
			dragStartWidth = chatWidth - 12;
			return true;
		} else {
			return super.mouseClicked(click, doubled);
		}
	}
	
	@Override
	public boolean mouseDragged(MouseButtonEvent click, double offsetX, double offsetY) {
		if(minecraft.options.chatScale().get() == 0) return super.mouseDragged(click, offsetX, offsetY);
		if(dragging == 0) {
			int lineHeight = (int)(9 * (minecraft.options.chatLineSpacing().get() + 1));
			if(editFocusedWidget.selected()) {
				ChatTabsConfig.getInstance().chatHeightFocused = (Math.clamp(dragStartHeight + (int)(dragStartY - (click.y() / minecraft.options.chatScale().get())), 20, 900) / lineHeight) * lineHeight;
			} else {
				ChatTabsConfig.getInstance().chatHeightUnfocused = (Math.clamp(dragStartHeight + (int)(dragStartY - (click.y() / minecraft.options.chatScale().get())), 20, 900) / lineHeight) * lineHeight;
			}
			return true;
		} else if(dragging == 1) {
			ChatTabsConfig.getInstance().chatWidth = Math.max(dragStartWidth - (int)(dragStartX - click.x()), 40);
			return true;
		} else {
			return super.mouseDragged(click, offsetX, offsetY);
		}
	}
	
	@Override
	public boolean mouseReleased(MouseButtonEvent click) {
		if(dragging != -1) {
			dragging = -1;
			return true;
		} else {
			return super.mouseReleased(click);
		}
	}
	
	@Override
	public void onClose() {
		minecraft.setScreen(parent);
	}
	
	private int fade(int color, float amount) {
		int alpha = (color >> 24) & 0xFF;
		alpha = (int)((float)alpha * amount);
		return (color & 0x00FFFFFF) + (alpha << 24);
	}
}
