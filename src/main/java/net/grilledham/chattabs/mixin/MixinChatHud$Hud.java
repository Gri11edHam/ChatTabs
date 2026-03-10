package net.grilledham.chattabs.mixin;

import net.grilledham.chattabs.mixininterface.IChatHudDrawer;
import net.minecraft.client.font.DrawnTextConsumer;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Style;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChatHud.Hud.class)
public class MixinChatHud$Hud implements IChatHudDrawer {
	
	@Shadow
	@Final
	private DrawnTextConsumer textConsumer;
	
	@Override
	public Style chatTabs$getStyle() {
		return Style.EMPTY;
	}
	
	@Override
	public DrawnTextConsumer chatTabs$getDrawer() {
		return textConsumer;
	}
}
