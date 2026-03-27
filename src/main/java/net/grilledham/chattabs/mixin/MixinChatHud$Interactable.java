package net.grilledham.chattabs.mixin;

import net.grilledham.chattabs.mixininterface.IChatHudDrawer;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Style;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChatComponent.DrawingFocusedGraphicsAccess.class)
public class MixinChatHud$Interactable implements IChatHudDrawer {
	
	@Shadow
	private @Nullable Style hoveredStyle;
	
	@Shadow
	@Final
	private ActiveTextCollector textRenderer;
	
	@Override
	public Style chatTabs$getStyle() {
		return hoveredStyle;
	}
	
	@Override
	public ActiveTextCollector chatTabs$getDrawer() {
		return textRenderer;
	}
}
