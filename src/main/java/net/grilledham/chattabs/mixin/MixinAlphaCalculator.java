package net.grilledham.chattabs.mixin;

import net.grilledham.chattabs.config.ChatTabsConfig;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(ChatComponent.AlphaCalculator.class)
public interface MixinAlphaCalculator {
	
	@ModifyConstant(method = "lambda$timeBased$0", constant = @Constant(doubleValue = 200.0))
	private static double modifyMaxTimeTicks(double constant) {
		if(ChatTabsConfig.getInstance().enabled) {
			return ChatTabsConfig.getInstance().previewTime * 20;
		}
		return constant;
	}
	
	@ModifyConstant(method = "lambda$timeBased$0", constant = @Constant(doubleValue = 10.0))
	private static double modifyMaxTimeSeconds(double constant) {
		if(ChatTabsConfig.getInstance().enabled) {
			return ChatTabsConfig.getInstance().previewTime;
		}
		return constant;
	}
}
