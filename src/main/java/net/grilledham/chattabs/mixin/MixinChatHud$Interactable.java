package net.grilledham.chattabs.mixin;

import net.grilledham.chattabs.mixininterface.IChatHudDrawer;
import net.minecraft.client.font.DrawnTextConsumer;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Style;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChatHud.Interactable.class)
public class MixinChatHud$Interactable implements IChatHudDrawer {
	
	@Shadow
	private @Nullable Style style;
	
	@Shadow
	@Final
	private DrawnTextConsumer drawer;
	
	@Override
	public Style chatTabs$getStyle() {
		return style;
	}
	
	@Override
	public DrawnTextConsumer chatTabs$getDrawer() {
		return drawer;
	}
}
