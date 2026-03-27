package net.grilledham.chattabs.mixin;

import net.grilledham.chattabs.mixininterface.IChatHudDrawer;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChatComponent.DrawingBackgroundGraphicsAccess.class)
public class MixinChatHud$Hud implements IChatHudDrawer {
	
	@Shadow
	@Final
	private ActiveTextCollector textRenderer;
	
	@Override
	public Style chatTabs$getStyle() {
		return Style.EMPTY;
	}
	
	@Override
	public ActiveTextCollector chatTabs$getDrawer() {
		return textRenderer;
	}
}
