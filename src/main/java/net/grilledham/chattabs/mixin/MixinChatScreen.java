package net.grilledham.chattabs.mixin;

import net.grilledham.chattabs.config.ChatTabsConfig;
import net.grilledham.chattabs.mixininterface.IChatHud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public abstract class MixinChatScreen extends Screen {
	
	@Shadow
	private CommandSuggestions commandSuggestions;
	
	@Shadow
	protected abstract boolean insertionClickMode();
	
	protected MixinChatScreen(Component title) {
		super(title);
	}
	
	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void renderChatContextMenu(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
		ChatComponent chatHud = this.minecraft.gui.getChat();
		((IChatHud)chatHud).chatTabs$renderContextMenu(context, width, height, mouseX, mouseY, deltaTicks);
	}
	
	@Redirect(method = "handleChatInput", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;sendChat(Ljava/lang/String;)V"))
	private void modifyChatMessage(ClientPacketListener instance, String content) {
		if(ChatTabsConfig.getInstance().enabled && ChatTabsConfig.getInstance().selectedTab > 0) {
			content = ChatTabsConfig.getInstance().chatTabs.get(ChatTabsConfig.getInstance().selectedTab - 1).modifySend(content);
			if (content.startsWith("/")) {
				instance.sendCommand(content.substring(1));
			} else {
				instance.sendChat(content);
			}
		} else {
			instance.sendChat(content);
		}
	}
	
	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	public void mouseClicked(MouseButtonEvent click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
		ChatComponent chatHud = this.minecraft.gui.getChat();
		if(((IChatHud)chatHud).chatTabs$mouseClicked(click, doubled)) {
			cir.setReturnValue(true);
		}
	}
	
	@Override
	public boolean mouseReleased(MouseButtonEvent click) {
		ChatComponent chatHud = this.minecraft.gui.getChat();
		if(((IChatHud)chatHud).chatTabs$mouseReleased(click)) {
			return true;
		}
		return super.mouseReleased(click);
	}
	
	@Override
	public boolean mouseDragged(MouseButtonEvent click, double offsetX, double offsetY) {
		ChatComponent chatHud = this.minecraft.gui.getChat();
		if(((IChatHud)chatHud).chatTabs$mouseDragged(click, offsetX, offsetY)) {
			return true;
		}
		return super.mouseDragged(click, offsetX, offsetY);
	}
}
