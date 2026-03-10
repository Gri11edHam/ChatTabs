package net.grilledham.chattabs.render;

import net.grilledham.chattabs.config.ChatTabsConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.cursor.StandardCursors;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class ChatContextMenu {
	
	private static final int ELEMENT_HEIGHT = 12;
	private static final int DIVIDER_HEIGHT = 1;
	
	private int x;
	private int y;
	
	private int width = 0;
	private int height = 0;
	
	private final List<Element> elements = new ArrayList<>();
	
	public ChatContextMenu(int x, int y, Element... elements) {
		this.x = x;
		this.y = y;
		this.elements.addAll(List.of(elements));
	}
	
	public void render(MinecraftClient client, DrawContext context, int windowWidth, int windowHeight, int mouseX, int mouseY) {
		if(x + width > windowWidth) {
			x = windowWidth - width;
		}
		if(y + height > windowHeight) {
			y = windowHeight - height;
		}
		
		if(this.width == 0 || this.height == 0) {
			for(Element element : elements) {
				this.width = Math.max(client.textRenderer.getWidth(element.text) + 4, this.width);
				height += element.isDivider() ? DIVIDER_HEIGHT : ELEMENT_HEIGHT;
			}
		}
		
		context.fill(x - 1, y - 1, x + this.width + 1, y + height + 1, -1);
		context.fill(x, y, x + this.width, y + height, 0xFF000000);
		int ey = y;
		for(Element element : elements) {
			if(element.isDivider()) {
				context.fill(x, ey, x + this.width, ey + DIVIDER_HEIGHT, -1);
				ey += DIVIDER_HEIGHT;
			} else {
				boolean hovered = mouseX >= x && mouseY >= ey && mouseX < x + this.width && mouseY < ey + ELEMENT_HEIGHT;
				if(hovered) context.setCursor(StandardCursors.POINTING_HAND);
				context.fill(x, ey, x + this.width, ey + ELEMENT_HEIGHT, hovered ? 0x80FFFFFF : 0x80000000);
				context.drawText(client.textRenderer, element.text(), x + 2, ey + 2, -1, ChatTabsConfig.getInstance().textShadow);
				ey += ELEMENT_HEIGHT;
			}
		}
	}
	
	public boolean click(Click click) {
		if(click.button() == 0) {
			int ey = y;
			for(Element element : elements) {
				if(element.isDivider()) {
					ey += DIVIDER_HEIGHT;
				} else {
					boolean hovered = click.x() >= x && click.y() >= ey && click.x() < x + width && click.y() < ey + ELEMENT_HEIGHT;
					if(hovered) {
						element.handleClick();
					}
					ey += ELEMENT_HEIGHT;
				}
			}
		}
		return true;
	}
	
	public static class Element {
		
		private final boolean divider;
		private final Text text;
		private final Runnable clickHandler;
		
		public Element(Text text, Runnable clickHandler) {
			this.text = text;
			this.clickHandler = clickHandler;
			divider = false;
		}
		
		public Element() {
			text = Text.empty();
			clickHandler = () -> {};
			divider = true;
		}
		
		public Text text() {
			return text;
		}
		
		public boolean isDivider() {
			return divider;
		}
		
		public Runnable clickHandler() {
			return clickHandler;
		}
		
		public void handleClick() {
			clickHandler.run();
		}
	}
}
