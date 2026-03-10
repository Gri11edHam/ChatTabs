package net.grilledham.chattabs.mixin;

import net.grilledham.chattabs.config.ChatTabsConfig;
import net.grilledham.chattabs.mixininterface.IChatHud;
import net.minecraft.client.font.DrawnTextConsumer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
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
	ChatInputSuggestor chatInputSuggestor;
	
	@Shadow
	protected abstract boolean shouldInsert();
	
	protected MixinChatScreen(Text title) {
		super(title);
	}
	
	@Inject(method = "render", at = @At("TAIL"))
	private void renderChatContextMenu(DrawContext context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
		ChatHud chatHud = this.client.inGameHud.getChatHud();
		((IChatHud)chatHud).chatTabs$renderContextMenu(context, width, height, mouseX, mouseY, deltaTicks);
	}
	
	@Redirect(method = "sendMessage", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;sendChatMessage(Ljava/lang/String;)V"))
	private void modifyChatMessage(ClientPlayNetworkHandler instance, String content) {
		if(ChatTabsConfig.getInstance().enabled && ChatTabsConfig.getInstance().selectedTab > 0) {
			content = ChatTabsConfig.getInstance().chatTabs.get(ChatTabsConfig.getInstance().selectedTab - 1).modifySend(content);
			if (content.startsWith("/")) {
				instance.sendChatCommand(content.substring(1));
			} else {
				instance.sendChatMessage(content);
			}
		} else {
			instance.sendChatMessage(content);
		}
	}
	
	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	public void mouseClicked(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
		ChatHud chatHud = this.client.inGameHud.getChatHud();
		if(((IChatHud)chatHud).chatTabs$mouseClicked(click, doubled)) {
			cir.setReturnValue(true);
		}
	}
	
	@Override
	public boolean mouseReleased(Click click) {
		ChatHud chatHud = this.client.inGameHud.getChatHud();
		if(((IChatHud)chatHud).chatTabs$mouseReleased(click)) {
			return true;
		}
		return super.mouseReleased(click);
	}
	
	@Override
	public boolean mouseDragged(Click click, double offsetX, double offsetY) {
		ChatHud chatHud = this.client.inGameHud.getChatHud();
		if(((IChatHud)chatHud).chatTabs$mouseDragged(click, offsetX, offsetY)) {
			return true;
		}
		return super.mouseDragged(click, offsetX, offsetY);
	}
}
