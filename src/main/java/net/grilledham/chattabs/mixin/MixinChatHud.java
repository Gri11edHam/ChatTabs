package net.grilledham.chattabs.mixin;

import net.grilledham.chattabs.ChatTabs;
import net.grilledham.chattabs.config.ChatTabsConfig;
import net.grilledham.chattabs.mixininterface.IChatHud;
import net.grilledham.chattabs.mixininterface.IChatHudDrawer;
import net.grilledham.chattabs.render.ChatContextMenu;
import net.grilledham.chattabs.render.ChatHudOverlays;
import net.grilledham.chattabs.render.screen.EditChatScreen;
import net.grilledham.chattabs.tabs.ChatLineFilter;
import net.grilledham.chattabs.tabs.ChatTab;
import net.grilledham.chattabs.tabs.SendModifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;

@Mixin(ChatComponent.class)
public abstract class MixinChatHud implements IChatHud {
	
	@Shadow
	protected abstract int getHeight();
	
	@Shadow
	public abstract void scrollChat(int scroll);
	
	@Shadow
	@Final
	private List<GuiMessage.Line> trimmedMessages;
	@Shadow
	private int chatScrollbarPos;
	@Shadow @Final
	private Minecraft minecraft;
	
	@Shadow
	protected abstract int getWidth();
	
	@Shadow
	protected abstract int forEachLine(ChatComponent.AlphaCalculator opacityRule, ChatComponent.LineConsumer lineConsumer);
	
	@Shadow
	protected abstract double getScale();
	
	@Shadow
	public abstract void extractRenderState(GuiGraphicsExtractor context, Font textRenderer, int currentTick, int mouseX, int mouseY, ChatComponent.DisplayMode interactable, boolean bl);
	
	@Shadow
	public abstract boolean isChatFocused();
	
	@Unique
	private static double opacityMultiplier;
	
	@Unique
	private int mouseX;
	@Unique
	private int mouseY;
	@Unique
	private int scrollbarOpacity;
	@Unique
	private int scrollbarColorLeft;
	@Unique
	private int scrollbarX;
	@Unique
	private int scrollbarY1;
	@Unique
	private int scrollbarY2;
	
	@Unique
	private boolean mouseDown = false;
	@Unique
	private int scrollStart;
	@Unique
	private double scrollMouseStart;
	
	@Unique
	private int hoveredTab;
	
	@Unique
	private GuiMessage chatMessage;
	
	@Unique
	private int tabScroll = 0;
	
	@Unique
	private boolean firstMessageUnread = true;
	@Unique
	private GuiMessage.Line lastSeenLine;
	
	@Unique
	private ChatContextMenu contextMenu;
	
	@Unique
	private float windowHeight;
	
	@Unique
	private GuiGraphicsExtractor drawContext;
	
	@Unique
	private boolean dummyChat = false;
	@Unique
	private boolean dummyChatFocused;
	
	@ModifyConstant(method = "addMessageToDisplayQueue", constant = @Constant(intValue = 100))
	private int modifyVisibleChatLength(int constant) {
		if(ChatTabsConfig.getInstance().enabled) {
			return ChatTabsConfig.getInstance().maxLines;
		}
		return constant;
	}
	
	@ModifyConstant(method = "addMessageToQueue", constant = @Constant(intValue = 100))
	private int modifyChatLength(int constant) {
		if(ChatTabsConfig.getInstance().enabled) {
			return ChatTabsConfig.getInstance().maxLines;
		}
		return constant;
	}
	
	@ModifyVariable(method = "extractRenderState(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;)V", at = @At("STORE"), name = "textOpacity")
	private float captureOpacityMultiplier(float value) {
		opacityMultiplier = value;
		scrollbarOpacity = (int)(value * 255);
		return value;
	}
	
	@ModifyVariable(method = "extractRenderState(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;)V", at = @At("STORE"), name = "backgroundOpacity")
	private float modifyChatBGTransparency(float value) {
		if(ChatTabsConfig.getInstance().enabled) {
			return (float)((ChatTabsConfig.getInstance().bgColor.getAlpha() / 255F) * opacityMultiplier);
		}
		return value;
	}
	
	@ModifyArg(method = "lambda$extractRenderState$1", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;fill(IIIII)V", ordinal = 0), index = 4)
	private static int modifyChatBGColor(int color) {
		if(ChatTabsConfig.getInstance().enabled) {
			return (ChatTabsConfig.getInstance().bgColor.getRGB() & 0x00FFFFFF) + (color & 0xFF000000);
		}
		return color;
	}
	
	@Redirect(method = "extractRenderState(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;handleMessage(IFLnet/minecraft/util/FormattedCharSequence;)Z"))
	private boolean redirectDrawText(ChatComponent.ChatGraphicsAccess instance, int y, float opacity, FormattedCharSequence text) {
		if(ChatTabsConfig.getInstance().enabled && instance instanceof ChatComponent.DrawingFocusedGraphicsAccess interactable) {
			Style style = ((IChatHudDrawer)interactable).chatTabs$getStyle();
			if(style != null) {
				if(ChatTabsConfig.getInstance().textShadow) {
					if(style.getColor() == null) {
						style = style.withShadowColor(ARGB.scaleRGB(-1, 0.25F));
					} else {
						style = style.withShadowColor(ARGB.scaleRGB(style.getColor().getValue(), 0.25F));
					}
				} else {
					style = style.withoutShadow();
				}
				interactable.accept(style);
			}
			return instance.handleMessage(y, opacity, text);
		}
		return instance.handleMessage(y, opacity, text);
	}
	
	@Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V", at = @At(value = "HEAD"))
	private void captureScrollbarVars(GuiGraphicsExtractor graphics, Font font, int ticks, int mouseX, int mouseY, ChatComponent.DisplayMode displayMode, boolean changeCursorOnInsertions, CallbackInfo ci) {
		this.drawContext = graphics;
		this.mouseX = (int)(mouseX / getScale());
		this.mouseY = (int)(mouseY / getScale());
	}
	
	@ModifyArg(method = "extractRenderState(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;fill(IIIII)V", ordinal = 2), index = 4)
	private int modifyScrollbarColorLeft(int x1, int y1, int x2, int y2, int color) {
		scrollbarX = x1 + 3;
		scrollbarY1 = y2;
		scrollbarY2 = y1;
		if(ChatTabsConfig.getInstance().enabled && (mouseX >= scrollbarX && mouseX < scrollbarX + 4 && mouseY >= scrollbarY1 && mouseY < scrollbarY2) || mouseDown) {
			scrollbarColorLeft = color;
			return 13421772 + (scrollbarOpacity << 24);
		}
		return color;
	}
	
	@ModifyArg(method = "extractRenderState(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;fill(IIIII)V", ordinal = 3), index = 4)
	private int modifyScrollbarColorRight(int x1, int y1, int x2, int y2, int color) {
		if(ChatTabsConfig.getInstance().enabled && (mouseX >= scrollbarX && mouseX < scrollbarX + 4 && mouseY >= scrollbarY1 && mouseY < scrollbarY2) || mouseDown) {
			return scrollbarColorLeft;
		}
		return color;
	}
	
	@Inject(method = "clearMessages", at = @At("HEAD"), cancellable = true)
	private void clear(boolean clearHistory, CallbackInfo ci) {
		if(ChatTabsConfig.getInstance().enabled && !ChatTabsConfig.getInstance().clearHistory && clearHistory) {
			ci.cancel();
		}
	}
	
	@Override
	public boolean chatTabs$mouseClicked(MouseButtonEvent click, boolean doubled) {
		if(getScale() == 0) return false;
		int mouseX = (int)(click.x() / getScale());
		int mouseY = (int)(click.y() / getScale());
		if(contextMenu != null) {
			if(contextMenu.click(click)) {
				contextMenu = null;
			}
			return true;
		}
		boolean clicked = false;
		if(ChatTabsConfig.getInstance().enabled && click.button() == 0 && mouseX >= scrollbarX && mouseX < scrollbarX + 4 && mouseY >= scrollbarY1 && mouseY < scrollbarY2) {
			clicked = true;
			scrollStart = chatScrollbarPos;
			scrollMouseStart = mouseY;
			mouseDown = true;
		}
		
		if(ChatTabsConfig.getInstance().enabled) {
			if(hoveredTab >= 0) {
				clicked = true;
				if(click.button() == 0) {
					if(ChatTabsConfig.getInstance().selectedTab > 0) {
						ChatTabsConfig.getInstance().getSelectedChatTab().setFocused(false);
					} else {
						if(!trimmedMessages.isEmpty()) {
							lastSeenLine = trimmedMessages.getFirst();
							firstMessageUnread = false;
						} else {
							firstMessageUnread = true;
						}
					}
					ChatTabsConfig.getInstance().selectedTab = hoveredTab;
					if(hoveredTab > 0) {
						chatScrollbarPos = ChatTabsConfig.getInstance().getVisibleChatTabs().get(hoveredTab - 1).getLastSeenMessage() - (getHeight() / 9);
						scrollChat(0);
						ChatTabsConfig.getInstance().getVisibleChatTabs().get(hoveredTab - 1).setFocused(true);
					} else {
						if(firstMessageUnread) {
							chatScrollbarPos = trimmedMessages.size() - (getHeight() / 9);
							scrollChat(0);
						} else if(lastSeenLine == null) {
							chatScrollbarPos = 0;
							scrollChat(0);
						} else {
							chatScrollbarPos = trimmedMessages.indexOf(lastSeenLine) - (getHeight() / 9);
							scrollChat(0);
						}
						firstMessageUnread = false;
						lastSeenLine = null;
					}
				} else if(click.button() == 1) {
					if(hoveredTab > 0) {
						final int contextMenuTab = hoveredTab - 1;
						contextMenu = new ChatContextMenu((int)click.x(), (int)click.y(),
								new ChatContextMenu.Element(Component.translatable("chattabsconfig.contextmenu.chat.configure"),
										() -> minecraft.setScreen(ChatTabsConfig.getInstance().generateConfig().setParentScreen(minecraft.screen).build())),
								new ChatContextMenu.Element(Component.translatable("chattabsconfig.contextmenu.chat.edit"),
										() -> minecraft.setScreen(new EditChatScreen(minecraft.screen))),
								new ChatContextMenu.Element(),
								new ChatContextMenu.Element(Component.translatable("chattabsconfig.contextmenu.tab.addleft"), () -> {
									if(ChatTabsConfig.getInstance().selectedTab >= contextMenuTab + 1) {
										ChatTabsConfig.getInstance().selectedTab++;
									}
									ChatTabsConfig.getInstance().addChatTabLeft(contextMenuTab, new ChatTab());
								}),
								new ChatContextMenu.Element(Component.translatable("chattabsconfig.contextmenu.tab.addright"), () -> {
									if(ChatTabsConfig.getInstance().selectedTab > contextMenuTab + 1) {
										ChatTabsConfig.getInstance().selectedTab++;
									}
									ChatTabsConfig.getInstance().addChatTabRight(contextMenuTab, new ChatTab());
								}),
								new ChatContextMenu.Element(Component.translatable("chattabsconfig.contextmenu.tab.moveleft"), () -> {
									ChatTabsConfig.getInstance().moveChatTabLeft(contextMenuTab);
									if(contextMenuTab - 1 >= 0) {
										if(ChatTabsConfig.getInstance().selectedTab == contextMenuTab + 1) {
											ChatTabsConfig.getInstance().selectedTab--;
										} else if(ChatTabsConfig.getInstance().selectedTab == contextMenuTab) {
											ChatTabsConfig.getInstance().selectedTab++;
										}
									}
								}),
								new ChatContextMenu.Element(Component.translatable("chattabsconfig.contextmenu.tab.moveright"), () -> {
									ChatTabsConfig.getInstance().moveChatTabRight(contextMenuTab);
									if(contextMenuTab + 1 < ChatTabsConfig.getInstance().getVisibleChatTabs().size()) {
										if(ChatTabsConfig.getInstance().selectedTab == contextMenuTab + 1) {
											ChatTabsConfig.getInstance().selectedTab++;
										} else if(ChatTabsConfig.getInstance().selectedTab == contextMenuTab + 2) {
											ChatTabsConfig.getInstance().selectedTab--;
										}
									}
								}),
								new ChatContextMenu.Element(Component.translatable("chattabsconfig.contextmenu.tab.delete"), () -> {
									if(ChatTabsConfig.getInstance().selectedTab >= contextMenuTab + 1) {
										ChatTabsConfig.getInstance().selectedTab--;
									}
									ChatTabsConfig.getInstance().removeVisibleChatTab(contextMenuTab);
								}));
					} else {
						contextMenu = new ChatContextMenu((int)click.x(), (int)click.y(),
								new ChatContextMenu.Element(Component.translatable("chattabsconfig.contextmenu.chat.configure"),
										() -> minecraft.setScreen(ChatTabsConfig.getInstance().generateConfig().setParentScreen(minecraft.screen).build())),
								new ChatContextMenu.Element(Component.translatable("chattabsconfig.contextmenu.chat.edit"),
										() -> minecraft.setScreen(new EditChatScreen(minecraft.screen))),
								new ChatContextMenu.Element(),
								new ChatContextMenu.Element(Component.translatable("chattabsconfig.contextmenu.tab.addright"), () -> {
									if(ChatTabsConfig.getInstance().selectedTab > -1) {
										ChatTabsConfig.getInstance().selectedTab++;
									}
									ChatTabsConfig.getInstance().addChatTabFirst(new ChatTab());
								}));
					}
				}
			} else if(hoveredTab == -2) {
				clicked = true;
				if(click.button() == 0) {
					if(tabScroll > 0) {
						tabScroll--;
					}
				}
			} else if(hoveredTab == -3) {
				clicked = true;
				if(click.button() == 0) {
					tabScroll++;
				}
			}
		}
		if(clicked) {
			return true;
		}
		if(click.button() == 1) {
			float f = (float)this.getScale();
			final int k = Mth.floor((windowHeight - 40) / f);
			final int l = 9;
			double d = this.minecraft.options.chatLineSpacing().get();
			final int n = (int)(l * (d + 1.0));
			final int o = (int)Math.round(8.0 * (d + 1.0) - 4.0 * d);
			this.forEachLine(ChatComponent.AlphaCalculator.FULLY_VISIBLE, (visible, ix, fx) -> {
				int jx = k - ix * n;
				int lx = jx - o;
				
				if(click.x() >= 0 && mouseY >= lx && click.x() <= getWidth() && mouseY <= lx + o) {
					String copyText = visible.parent().content().getString();
					String copyTextJson = ComponentSerialization.CODEC.encodeStart(NbtOps.INSTANCE, visible.parent().content())
							.resultOrPartial(ChatTabs.LOGGER::error)
							.map(NbtUtils::toPrettyComponent)
							.orElse(Component.empty())
							.getString();
					contextMenu = new ChatContextMenu((int)click.x(), (int)click.y(),
							new ChatContextMenu.Element(Component.translatable("chattabsconfig.contextmenu.chat.configure"),
									() -> minecraft.setScreen(ChatTabsConfig.getInstance().generateConfig().setParentScreen(minecraft.screen).build())),
							new ChatContextMenu.Element(Component.translatable("chattabsconfig.contextmenu.chat.edit"),
									() -> minecraft.setScreen(new EditChatScreen(minecraft.screen))),
							new ChatContextMenu.Element(),
							new ChatContextMenu.Element(Component.translatable("chattabsconfig.contextmenu.chat.copy"),
									() -> minecraft.keyboardHandler.setClipboard(copyText)),
							new ChatContextMenu.Element(Component.translatable("chattabsconfig.contextmenu.chat.copyjson"),
									() -> minecraft.keyboardHandler.setClipboard(copyTextJson)));
				}
			});
			if(contextMenu == null) {
				contextMenu = new ChatContextMenu((int)click.x(), (int)click.y(),
						new ChatContextMenu.Element(Component.translatable("chattabsconfig.contextmenu.chat.configure"),
								() -> minecraft.setScreen(ChatTabsConfig.getInstance().generateConfig().setParentScreen(minecraft.screen).build())),
						new ChatContextMenu.Element(Component.translatable("chattabsconfig.contextmenu.chat.edit"),
								() -> minecraft.setScreen(new EditChatScreen(minecraft.screen))),
						new ChatContextMenu.Element(),
						new ChatContextMenu.Element(Component.translatable("chattabsconfig.contextmenu.tab.newtab"), () -> {
							ChatTabsConfig.getInstance().addChatTabLast(new ChatTab());
						}));
			}
		}
		return contextMenu != null;
	}
	
	@Override
	public boolean chatTabs$mouseReleased(MouseButtonEvent click) {
		if(click.button() != 0) return false;
		mouseDown = false;
		return true;
	}
	
	@Override
	public boolean chatTabs$mouseDragged(MouseButtonEvent click, double deltaX, double deltaY) {
		if(click.button() != 0 || getScale() == 0) return false;
		if(mouseDown) {
			List<GuiMessage.Line> messages = ChatTabsConfig.getInstance().enabled && ChatTabsConfig.getInstance().selectedTab > 0 ? ChatTabsConfig.getInstance().getSelectedChatTab().getVisibleChatLines() : trimmedMessages;
			chatScrollbarPos = scrollStart - (int)((((click.y() / getScale()) - scrollMouseStart) / getHeight()) * messages.size());
			scrollChat(0);
			return true;
		}
		return false;
	}
	
	@Inject(method = "isChatFocused", at = @At("HEAD"), cancellable = true)
	private void modifyFocused(CallbackInfoReturnable<Boolean> cir) {
		if(ChatTabsConfig.getInstance().enabled && dummyChat) {
			cir.setReturnValue(dummyChatFocused);
		}
	}
	
	@Inject(method = "getWidth()I", at = @At("HEAD"), cancellable = true)
	private void modifyWidth(CallbackInfoReturnable<Integer> cir) {
		if(ChatTabsConfig.getInstance().enabled) {
			cir.setReturnValue(ChatTabsConfig.getInstance().chatWidth);
		}
	}
	
	@Inject(method = "getHeight()I", at = @At("HEAD"), cancellable = true)
	private void modifyHeight(CallbackInfoReturnable<Integer> cir) {
		if(ChatTabsConfig.getInstance().enabled) {
			cir.setReturnValue(isChatFocused() ? ChatTabsConfig.getInstance().chatHeightFocused : ChatTabsConfig.getInstance().chatHeightUnfocused);
		}
	}
	
	/*
	 * Chat Tabs
	 */
	
	@Inject(method = "extractRenderState(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;)V", at = @At("HEAD"), cancellable = true)
	private void renderChatTabs(ChatComponent.ChatGraphicsAccess drawer, int windowHeight, int ticks, ChatComponent.DisplayMode displayMode, CallbackInfo ci) {
		this.windowHeight = windowHeight;
		if(!displayMode.foreground) contextMenu = null;
		if(minecraft.screen instanceof EditChatScreen && !dummyChat) {
			ci.cancel();
		} else if(ChatTabsConfig.getInstance().enabled && !dummyChat) {
			int i = trimmedMessages.size();
			if(ChatTabsConfig.getInstance().selectedTab > 0) {
				i = ChatTabsConfig.getInstance().getSelectedChatTab().getVisibleChatLines().size();
			}
			if(i == 0) {
				drawer.updatePose(pose -> pose.scale((float)getScale(), (float)getScale()));
				boolean unreads = (lastSeenLine != null && trimmedMessages.indexOf(lastSeenLine) > 0) || (!trimmedMessages.isEmpty() && firstMessageUnread);
				int[] ret = ChatHudOverlays.renderChatTabs(minecraft, tabScroll, drawContext, windowHeight, (float)getScale(), displayMode.foreground, getWidth(), mouseX, mouseY, 0, unreads);
				hoveredTab = ret[0];
				tabScroll = ret[1];
				drawer.updatePose(pose -> pose.scale((float)(1 / getScale()), (float)(1 / getScale())));
			}
		}
	}
	
	@Inject(method = "extractRenderState(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;)V", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/components/ChatComponent;chatScrollbarPos:I", opcode = Opcodes.GETFIELD), locals = LocalCapture.CAPTURE_FAILSOFT)
	private void renderChatTabs(ChatComponent.ChatGraphicsAccess drawer, int windowHeight, int currentTick, ChatComponent.DisplayMode displayMode, CallbackInfo ci, boolean isForeground, boolean isRestricted, int i, ProfilerFiller profiler, float f, int j, int k, float g, float h, int l, int m, double d, int n, int o, long p, ChatComponent.AlphaCalculator opacityRule, int q) {
		if(ChatTabsConfig.getInstance().enabled) {
			drawer.updatePose(pose -> pose.translate(-4, 0));
			boolean unreads = (lastSeenLine != null && trimmedMessages.indexOf(lastSeenLine) > 0) || (!trimmedMessages.isEmpty() && firstMessageUnread);
			int[] ret = ChatHudOverlays.renderChatTabs(minecraft, tabScroll, drawContext, windowHeight, (float)getScale(), displayMode.foreground, getWidth(), mouseX, mouseY, q, unreads);
			hoveredTab = ret[0];
			tabScroll = ret[1];
			drawer.updatePose(pose -> pose.translate(4, 0));
		}
	}
	
	@Override
	public void chatTabs$renderContextMenu(GuiGraphicsExtractor context, int windowWidth, int windowHeight, int mouseX, int mouseY, float deltaTicks) {
		if(contextMenu != null) {
			contextMenu.render(minecraft, drawContext, windowWidth, windowHeight, mouseX, mouseY);
		}
	}
	
	@Override
	public void chatTabs$renderDummy(GuiGraphicsExtractor context, Font textRenderer, int ticks, int mouseX, int mouseY, boolean focused) {
		dummyChat = true;
		dummyChatFocused = focused;
		extractRenderState(context, textRenderer, ticks, mouseX, mouseY, ChatComponent.DisplayMode.FOREGROUND, false);
		dummyChat = false;
	}
	
	@Redirect(method = {"extractRenderState(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;)V", "scrollChat", "forEachLine"}, at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/components/ChatComponent;trimmedMessages:Ljava/util/List;", opcode = Opcodes.GETFIELD))
	private List<GuiMessage.Line> modifyMessages(ChatComponent instance) {
		if(dummyChat) {
			return EditChatScreen.DUMMY_CHAT;
		}
		if(ChatTabsConfig.getInstance().enabled && ChatTabsConfig.getInstance().selectedTab > 0) {
			return ChatTabsConfig.getInstance().getSelectedChatTab().getVisibleChatLines();
		}
		return trimmedMessages;
	}
	
	@Inject(method = "refreshTrimmedMessages", at = @At("HEAD"))
	private void refreshTabs(CallbackInfo ci) {
		ChatTabsConfig.getInstance().getChatTabs().forEach(tab -> tab.clear(false));
	}
	
	@Inject(method = "refreshTrimmedMessages", at = @At("TAIL"))
	private void updateLastSeen(CallbackInfo ci) {
		int i = 0;
		boolean wasFocused;
		for(ChatTab tab : ChatTabsConfig.getInstance().getChatTabs()) {
			wasFocused = i == ChatTabsConfig.getInstance().selectedTab - 1;
			tab.setFocused(false);
			tab.setFocused(wasFocused);
			i++;
		}
		if(!trimmedMessages.isEmpty()) {
			lastSeenLine = trimmedMessages.getFirst();
			firstMessageUnread = false;
		} else {
			firstMessageUnread = true;
		}
	}
	
	@Inject(method = "clearMessages", at = @At("HEAD"))
	private void clearTabs(CallbackInfo ci) {
		firstMessageUnread = true;
		ChatTabsConfig.getInstance().getChatTabs().forEach(tab -> tab.clear(true));
	}
	
	@Inject(method = "addMessageToDisplayQueue", at = @At("HEAD"))
	private void captureChatHudLine(GuiMessage message, CallbackInfo ci) {
		this.chatMessage = message;
		if(message.content().getContents() instanceof TranslatableContents msg) {
			if(msg.getKey().startsWith("commands.message.display")) {
				boolean hasDM = false;
				for(ChatTab tab : ChatTabsConfig.getInstance().getChatTabs()) {
					if(tab.getFilter().filtersMessages() && tab.getFilter().test(message)) {
						hasDM = true;
						break;
					}
				}
				if(!hasDM && ChatTabsConfig.getInstance().autoGenerateMsgTabs) {
					final String name = msg.getArgument(0).getString();
					ChatTabsConfig.getInstance().addChatTabLast(new ChatTab(name, false, new ChatLineFilter(name, true), new SendModifier("/msg " + name + " ")));
				}
			}
		}
	}
	
	@Redirect(method = "addMessageToDisplayQueue", at = @At(value = "INVOKE", target = "Ljava/util/List;addFirst(Ljava/lang/Object;)V"))
	private void addToFilteredMessages(List<GuiMessage.Line> instance, Object e) {
		instance.addFirst((GuiMessage.Line)e);
		if(ChatTabsConfig.getInstance().enabled) {
			for(ChatTab tab : ChatTabsConfig.getInstance().getChatTabs()) {
				if(tab.getFilter().test(this.chatMessage)) {
					tab.addChatLine((GuiMessage.Line)e);
				}
			}
		}
	}
}
