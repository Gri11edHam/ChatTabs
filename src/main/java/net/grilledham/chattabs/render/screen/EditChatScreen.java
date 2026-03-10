package net.grilledham.chattabs.render.screen;

import net.grilledham.chattabs.config.ChatTabsConfig;
import net.grilledham.chattabs.mixininterface.IChatHud;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.cursor.StandardCursors;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class EditChatScreen extends Screen {
	
	public static final List<ChatHudLine.Visible> DUMMY_CHAT;
	
	static {
		DUMMY_CHAT = new ArrayList<>(100);
		for(int i = 0; i < 100; i++) {
			DUMMY_CHAT.add(new ChatHudLine.Visible(0, Text.of("Line " + (i + 1)).asOrderedText(), MessageIndicator.system(), true));
		}
	}
	
	private final Screen parent;
	
	private final CheckboxWidget editFocusedWidget;
	
	private float topEdgeTicks = 0;
	private float rightEdgeTicks = 0;
	
	private double dragStartX;
	private double dragStartY;
	
	private int dragStartWidth;
	private int dragStartHeight;
	
	// -1 - not dragging, 0 - drag height, 1 - drag width
	private int dragging = -1;
	
	public EditChatScreen(Screen parent) {
		super(Text.translatable("chattabs.editchatscreen"));
		this.parent = parent;
		editFocusedWidget = CheckboxWidget.builder(Text.translatable("chattabs.editchatscreen.editfocused"), textRenderer)
				.pos(4, height - 30)
				.checked(true)
				.build();
		addDrawableChild(editFocusedWidget);
	}
	
	@Override
	protected void init() {
		editFocusedWidget.setPosition(4, height - 30);
		addDrawableChild(editFocusedWidget);
		
		int lineHeight = (int)(9 * (client.options.getChatLineSpacing().getValue() + 1));
		ChatTabsConfig.getInstance().chatHeightFocused = (ChatTabsConfig.getInstance().chatHeightFocused / lineHeight) * lineHeight;
		ChatTabsConfig.getInstance().chatHeightUnfocused = (ChatTabsConfig.getInstance().chatHeightUnfocused / lineHeight) * lineHeight;
	}
	
	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		((IChatHud)client.inGameHud.getChatHud()).chatTabs$renderDummy(context, this.textRenderer, this.client.inGameHud.getTicks(), mouseX, mouseY, editFocusedWidget.isChecked());
		context.setCursor(StandardCursors.ARROW);
		super.render(context, mouseX, mouseY, deltaTicks);
		
		context.getMatrices().pushMatrix();
		
		int chatWidth = ChatTabsConfig.getInstance().chatWidth + (int)(12 * client.options.getChatScale().getValue());
		int chatY = height - 41;
		int chatHeight = (editFocusedWidget.isChecked() ? ChatTabsConfig.getInstance().chatHeightFocused : ChatTabsConfig.getInstance().chatHeightUnfocused);
		int chatVisualHeight = (int)(chatHeight * client.options.getChatScale().getValue());
		// top edge
		context.drawHorizontalLine(0, chatWidth, chatY - chatVisualHeight, fade(-1, topEdgeTicks));
		if((mouseX >= 0 && mouseX < chatWidth && mouseY >= chatY - chatVisualHeight - 3 && mouseY < chatY - chatVisualHeight + 3) || dragging == 0) {
			context.setCursor(StandardCursors.RESIZE_NS);
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
		context.drawVerticalLine(chatWidth, chatY - chatVisualHeight, chatY, fade(-1, rightEdgeTicks));
		if((mouseX >= chatWidth - 3 && mouseX < chatWidth + 3 && mouseY >= chatY - chatVisualHeight && mouseY < chatY) || dragging == 1) {
			context.setCursor(StandardCursors.RESIZE_EW);
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
		
		context.getMatrices().popMatrix();
	}
	
	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		if(client.options.getChatScale().getValue() == 0) return super.mouseClicked(click, doubled);
		int chatWidth = ChatTabsConfig.getInstance().chatWidth + 12;
		int chatY = height - 41;
		int chatHeight = (editFocusedWidget.isChecked() ? ChatTabsConfig.getInstance().chatHeightFocused : ChatTabsConfig.getInstance().chatHeightUnfocused);
		int chatVisualHeight = (int)(chatHeight * client.options.getChatScale().getValue());
		if(click.x() >= 0 && click.x() < chatWidth && click.y() >= chatY - chatVisualHeight - 3 && click.y() < chatY - chatVisualHeight + 3) {
			dragging = 0;
			dragStartX = click.x();
			dragStartY = click.y() / client.options.getChatScale().getValue();
			dragStartHeight = chatHeight;
			return true;
		} else if(click.x() >= chatWidth - 3 && click.x() < chatWidth + 3 && click.y() >= chatY - chatVisualHeight && click.y() < chatY) {
			dragging = 1;
			dragStartX = click.x();
			dragStartY = click.y() / client.options.getChatScale().getValue();
			dragStartWidth = chatWidth - 12;
			return true;
		} else {
			return super.mouseClicked(click, doubled);
		}
	}
	
	@Override
	public boolean mouseDragged(Click click, double offsetX, double offsetY) {
		if(client.options.getChatScale().getValue() == 0) return super.mouseDragged(click, offsetX, offsetY);
		if(dragging == 0) {
			int lineHeight = (int)(9 * (client.options.getChatLineSpacing().getValue() + 1));
			if(editFocusedWidget.isChecked()) {
				ChatTabsConfig.getInstance().chatHeightFocused = (Math.min(Math.max(dragStartHeight + (int)(dragStartY - (click.y() / client.options.getChatScale().getValue())), 20), 900) / lineHeight) * lineHeight;
			} else {
				ChatTabsConfig.getInstance().chatHeightUnfocused = (Math.min(Math.max(dragStartHeight + (int)(dragStartY - (click.y() / client.options.getChatScale().getValue())), 20), 900) / lineHeight) * lineHeight;
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
	public boolean mouseReleased(Click click) {
		if(dragging != -1) {
			dragging = -1;
			return true;
		} else {
			return super.mouseReleased(click);
		}
	}
	
	@Override
	public void close() {
		client.setScreen(parent);
	}
	
	private int fade(int color, float amount) {
		int alpha = (color >> 24) & 0xFF;
		alpha = (int)((float)alpha * amount);
		return (color & 0x00FFFFFF) + (alpha << 24);
	}
}
