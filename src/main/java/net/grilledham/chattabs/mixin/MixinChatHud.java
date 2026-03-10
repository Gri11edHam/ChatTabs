package net.grilledham.chattabs.mixin;

import net.grilledham.chattabs.config.ChatTabsConfig;
import net.grilledham.chattabs.mixininterface.IChatHud;
import net.grilledham.chattabs.mixininterface.IChatHudDrawer;
import net.grilledham.chattabs.render.ChatContextMenu;
import net.grilledham.chattabs.render.ChatHudOverlays;
import net.grilledham.chattabs.render.screen.EditChatScreen;
import net.grilledham.chattabs.tabs.ChatLineFilter;
import net.grilledham.chattabs.tabs.ChatTab;
import net.grilledham.chattabs.tabs.SendModifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.profiler.Profiler;
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

@Mixin(ChatHud.class)
public abstract class MixinChatHud implements IChatHud {
	
	@Shadow
	protected abstract int getHeight();
	
	@Shadow
	public abstract void scroll(int scroll);
	
	@Shadow
	@Final
	private List<ChatHudLine.Visible> visibleMessages;
	@Shadow
	private int scrolledLines;
	@Shadow @Final
	MinecraftClient client;
	
	@Shadow
	protected abstract int getWidth();
	
	@Shadow
	protected abstract int forEachVisibleLine(ChatHud.OpacityRule opacityRule, ChatHud.LineConsumer lineConsumer);
	
	@Shadow
	protected abstract double getChatScale();
	
	@Shadow
	public abstract void render(DrawContext context, TextRenderer textRenderer, int currentTick, int mouseX, int mouseY, boolean interactable, boolean bl);
	
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
	private ChatHudLine chatMessage;
	
	@Unique
	private int tabScroll = 0;
	
	@Unique
	private boolean firstMessageUnread = true;
	@Unique
	private ChatHudLine.Visible lastSeenLine;
	
	@Unique
	private ChatContextMenu contextMenu;
	
	@Unique
	private float windowHeight;
	
	@Unique
	private DrawContext drawContext;
	
	@Unique
	private boolean dummyChat = false;
	@Unique
	private boolean dummyChatFocused;
	
	@ModifyConstant(method = "addVisibleMessage", constant = @Constant(intValue = 100))
	private int modifyVisibleChatLength(int constant) {
		if(ChatTabsConfig.getInstance().enabled) {
			return ChatTabsConfig.getInstance().maxLines;
		}
		return constant;
	}
	
	@ModifyConstant(method = "addMessage(Lnet/minecraft/client/gui/hud/ChatHudLine;)V", constant = @Constant(intValue = 100))
	private int modifyChatLength(int constant) {
		if(ChatTabsConfig.getInstance().enabled) {
			return ChatTabsConfig.getInstance().maxLines;
		}
		return constant;
	}
	
	@ModifyVariable(method = "render(Lnet/minecraft/client/gui/hud/ChatHud$Backend;IIZ)V", at = @At("STORE"), index = 10)
	private float captureOpacityMultiplier(float value) {
		opacityMultiplier = value;
		scrollbarOpacity = (int)(value * 255);
		return value;
	}
	
	@ModifyVariable(method = "render(Lnet/minecraft/client/gui/hud/ChatHud$Backend;IIZ)V", at = @At("STORE"), index = 11)
	private float modifyChatBGTransparency(float value) {
		if(ChatTabsConfig.getInstance().enabled) {
			return (float)((ChatTabsConfig.getInstance().bgColor.getAlpha() / 255F) * opacityMultiplier);
		}
		return value;
	}
	
	@ModifyArg(method = "method_75802", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/ChatHud$Backend;fill(IIIII)V", ordinal = 0), index = 4)
	private static int modifyChatBGColor(int color) {
		if(ChatTabsConfig.getInstance().enabled) {
			return (ChatTabsConfig.getInstance().bgColor.getRGB() & 0x00FFFFFF) + (color & 0xFF000000);
		}
		return color;
	}
	
	@Redirect(method = "render(Lnet/minecraft/client/gui/hud/ChatHud$Backend;IIZ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/ChatHud$Backend;text(IFLnet/minecraft/text/OrderedText;)Z"))
	private boolean redirectDrawText(ChatHud.Backend instance, int y, float opacity, OrderedText text) {
		if(ChatTabsConfig.getInstance().enabled && instance instanceof ChatHud.Interactable interactable) {
			Style style = ((IChatHudDrawer)interactable).chatTabs$getStyle();
			if(ChatTabsConfig.getInstance().textShadow) {
				if(style.getColor() == null) {
					style = style.withShadowColor(ColorHelper.scaleRgb(-1, 0.25F));
				} else {
					style = style.withShadowColor(ColorHelper.scaleRgb(style.getColor().getRgb(), 0.25F));
				}
			} else {
				style = style.withoutShadow();
			}
			interactable.accept(style);
			return instance.text(y, opacity, text);
		}
		return instance.text(y, opacity, text);
	}
	
	@Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/font/TextRenderer;IIIZZ)V", at = @At(value = "HEAD"))
	private void captureScrollbarVars(DrawContext context, TextRenderer textRenderer, int currentTick, int mouseX, int mouseY, boolean interactable, boolean bl, CallbackInfo ci) {
		this.drawContext = context;
		this.mouseX = (int)(mouseX / getChatScale());
		this.mouseY = (int)(mouseY / getChatScale());
	}
	
	@ModifyArg(method = "render(Lnet/minecraft/client/gui/hud/ChatHud$Backend;IIZ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/ChatHud$Backend;fill(IIIII)V", ordinal = 1), index = 4)
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
	
	@ModifyArg(method = "render(Lnet/minecraft/client/gui/hud/ChatHud$Backend;IIZ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/ChatHud$Backend;fill(IIIII)V", ordinal = 2), index = 4)
	private int modifyScrollbarColorRight(int x1, int y1, int x2, int y2, int color) {
		if(ChatTabsConfig.getInstance().enabled && (mouseX >= scrollbarX && mouseX < scrollbarX + 4 && mouseY >= scrollbarY1 && mouseY < scrollbarY2) || mouseDown) {
			return scrollbarColorLeft;
		}
		return color;
	}
	
	@Inject(method = "clear", at = @At("HEAD"), cancellable = true)
	private void clear(boolean clearHistory, CallbackInfo ci) {
		if(ChatTabsConfig.getInstance().enabled && !ChatTabsConfig.getInstance().clearHistory && clearHistory) {
			ci.cancel();
		}
	}
	
	@Override
	public boolean chatTabs$mouseClicked(Click click, boolean doubled) {
		if(getChatScale() == 0) return false;
		int mouseX = (int)(click.x() / getChatScale());
		int mouseY = (int)(click.y() / getChatScale());
		if(contextMenu != null) {
			if(contextMenu.click(click)) {
				contextMenu = null;
			}
			return true;
		}
		boolean clicked = false;
		if(ChatTabsConfig.getInstance().enabled && click.button() == 0 && mouseX >= scrollbarX && mouseX < scrollbarX + 4 && mouseY >= scrollbarY1 && mouseY < scrollbarY2) {
			clicked = true;
			scrollStart = scrolledLines;
			scrollMouseStart = mouseY;
			mouseDown = true;
		}
		
		if(ChatTabsConfig.getInstance().enabled) {
			if(hoveredTab >= 0) {
				clicked = true;
				if(click.button() == 0) {
					if(ChatTabsConfig.getInstance().selectedTab > 0) {
						ChatTabsConfig.getInstance().chatTabs.get(ChatTabsConfig.getInstance().selectedTab - 1).setFocused(false);
					} else {
						if(!visibleMessages.isEmpty()) {
							lastSeenLine = visibleMessages.getFirst();
							firstMessageUnread = false;
						} else {
							firstMessageUnread = true;
						}
					}
					ChatTabsConfig.getInstance().selectedTab = hoveredTab;
					if(hoveredTab > 0) {
						scrolledLines = ChatTabsConfig.getInstance().chatTabs.get(hoveredTab - 1).getLastSeenMessage() - (getHeight() / 9);
						scroll(0);
						ChatTabsConfig.getInstance().chatTabs.get(hoveredTab - 1).setFocused(true);
					} else {
						if(firstMessageUnread) {
							scrolledLines = visibleMessages.size() - (getHeight() / 9);
							scroll(0);
						} else if(lastSeenLine == null) {
							scrolledLines = 0;
							scroll(0);
						} else {
							scrolledLines = visibleMessages.indexOf(lastSeenLine) - (getHeight() / 9);
							scroll(0);
						}
						firstMessageUnread = false;
						lastSeenLine = null;
					}
				} else if(click.button() == 1) {
					if(hoveredTab > 0) {
						final int contextMenuTab = hoveredTab - 1;
						contextMenu = new ChatContextMenu((int)click.x(), (int)click.y(),
								new ChatContextMenu.Element(Text.translatable("chattabsconfig.contextmenu.chat.configure"),
										() -> client.setScreen(ChatTabsConfig.getInstance().generateConfig().setParentScreen(client.currentScreen).build())),
								new ChatContextMenu.Element(Text.translatable("chattabsconfig.contextmenu.chat.edit"),
										() -> client.setScreen(new EditChatScreen(client.currentScreen))),
								new ChatContextMenu.Element(),
								new ChatContextMenu.Element(Text.translatable("chattabsconfig.contextmenu.tab.addleft"), () -> {
									if(ChatTabsConfig.getInstance().selectedTab >= contextMenuTab + 1) {
										ChatTabsConfig.getInstance().selectedTab++;
									}
									ChatTabsConfig.getInstance().chatTabs.add(contextMenuTab, new ChatTab());
								}),
								new ChatContextMenu.Element(Text.translatable("chattabsconfig.contextmenu.tab.addright"), () -> {
									if(ChatTabsConfig.getInstance().selectedTab > contextMenuTab + 1) {
										ChatTabsConfig.getInstance().selectedTab++;
									}
									ChatTabsConfig.getInstance().chatTabs.add(contextMenuTab + 1, new ChatTab());
								}),
								new ChatContextMenu.Element(Text.translatable("chattabsconfig.contextmenu.tab.moveleft"), () -> {
									ChatTab tab = ChatTabsConfig.getInstance().chatTabs.get(contextMenuTab);
									if(contextMenuTab - 1 >= 0) {
										if(ChatTabsConfig.getInstance().selectedTab == contextMenuTab + 1) {
											ChatTabsConfig.getInstance().selectedTab--;
										} else if(ChatTabsConfig.getInstance().selectedTab == contextMenuTab) {
											ChatTabsConfig.getInstance().selectedTab++;
										}
										ChatTabsConfig.getInstance().chatTabs.remove(tab);
										ChatTabsConfig.getInstance().chatTabs.add(contextMenuTab - 1, tab);
									}
								}),
								new ChatContextMenu.Element(Text.translatable("chattabsconfig.contextmenu.tab.moveright"), () -> {
									ChatTab tab = ChatTabsConfig.getInstance().chatTabs.get(contextMenuTab);
									if(contextMenuTab + 1 < ChatTabsConfig.getInstance().chatTabs.size()) {
										if(ChatTabsConfig.getInstance().selectedTab == contextMenuTab + 1) {
											ChatTabsConfig.getInstance().selectedTab++;
										} else if(ChatTabsConfig.getInstance().selectedTab == contextMenuTab + 2) {
											ChatTabsConfig.getInstance().selectedTab--;
										}
										ChatTabsConfig.getInstance().chatTabs.remove(tab);
										ChatTabsConfig.getInstance().chatTabs.add(contextMenuTab + 1, tab);
									}
								}),
								new ChatContextMenu.Element(Text.translatable("chattabsconfig.contextmenu.tab.delete"), () -> {
									if(ChatTabsConfig.getInstance().selectedTab >= contextMenuTab + 1) {
										ChatTabsConfig.getInstance().selectedTab--;
									}
									ChatTabsConfig.getInstance().chatTabs.remove(contextMenuTab);
								}));
					} else {
						contextMenu = new ChatContextMenu((int)click.x(), (int)click.y(),
								new ChatContextMenu.Element(Text.translatable("chattabsconfig.contextmenu.chat.configure"),
										() -> client.setScreen(ChatTabsConfig.getInstance().generateConfig().setParentScreen(client.currentScreen).build())),
								new ChatContextMenu.Element(Text.translatable("chattabsconfig.contextmenu.chat.edit"),
										() -> client.setScreen(new EditChatScreen(client.currentScreen))),
								new ChatContextMenu.Element(),
								new ChatContextMenu.Element(Text.translatable("chattabsconfig.contextmenu.tab.addright"), () -> {
									if(ChatTabsConfig.getInstance().selectedTab > -1) {
										ChatTabsConfig.getInstance().selectedTab++;
									}
									ChatTabsConfig.getInstance().chatTabs.addFirst(new ChatTab());
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
			float f = (float)this.getChatScale();
			final int k = MathHelper.floor((windowHeight - 40) / f);
			final int l = 9;
			double d = this.client.options.getChatLineSpacing().getValue();
			final int n = (int)(l * (d + 1.0));
			final int o = (int)Math.round(8.0 * (d + 1.0) - 4.0 * d);
			this.forEachVisibleLine(ChatHud.OpacityRule.CONSTANT, new ChatHud.LineConsumer() {
				final StringBuilder sb = new StringBuilder();
				boolean lineClicked = false;
				
				@Override
				public void accept(ChatHudLine.Visible visible, int ix, float fx) {
					int jx = k - ix * n;
					int lx = jx - o;
					
					visible.content().accept((index, style, codePoint) -> {
						sb.append((char)codePoint);
						return true;
					});
					if(click.x() >= 0 && mouseY >= lx && click.x() <= getWidth() && mouseY <= lx + o) {
						lineClicked = true;
					}
					if(visible.endOfEntry()) {
						if(lineClicked) {
							lineClicked = false;
							String copyText = sb.toString();
							contextMenu = new ChatContextMenu((int)click.x(), (int)click.y(),
									new ChatContextMenu.Element(Text.translatable("chattabsconfig.contextmenu.chat.configure"),
											() -> client.setScreen(ChatTabsConfig.getInstance().generateConfig().setParentScreen(client.currentScreen).build())),
									new ChatContextMenu.Element(Text.translatable("chattabsconfig.contextmenu.chat.edit"),
											() -> client.setScreen(new EditChatScreen(client.currentScreen))),
									new ChatContextMenu.Element(),
									new ChatContextMenu.Element(Text.translatable("chattabsconfig.contextmenu.chat.copy"),
											() -> client.keyboard.setClipboard(copyText)));
						}
						sb.setLength(0);
					}
				}
			});
			if(contextMenu == null) {
				contextMenu = new ChatContextMenu((int)click.x(), (int)click.y(),
						new ChatContextMenu.Element(Text.translatable("chattabsconfig.contextmenu.chat.configure"),
								() -> client.setScreen(ChatTabsConfig.getInstance().generateConfig().setParentScreen(client.currentScreen).build())),
						new ChatContextMenu.Element(Text.translatable("chattabsconfig.contextmenu.chat.edit"),
								() -> client.setScreen(new EditChatScreen(client.currentScreen))),
						new ChatContextMenu.Element(),
						new ChatContextMenu.Element(Text.translatable("chattabsconfig.contextmenu.tab.newtab"), () -> {
							ChatTabsConfig.getInstance().chatTabs.add(new ChatTab());
						}));
			}
		}
		return contextMenu != null;
	}
	
	@Override
	public boolean chatTabs$mouseReleased(Click click) {
		if(click.button() != 0) return false;
		mouseDown = false;
		return true;
	}
	
	@Override
	public boolean chatTabs$mouseDragged(Click click, double deltaX, double deltaY) {
		if(click.button() != 0 || getChatScale() == 0) return false;
		if(mouseDown) {
			List<ChatHudLine.Visible> messages = ChatTabsConfig.getInstance().enabled && ChatTabsConfig.getInstance().selectedTab > 0 ? ChatTabsConfig.getInstance().chatTabs.get(ChatTabsConfig.getInstance().selectedTab - 1).getVisibleChatLines() : visibleMessages;
			scrolledLines = scrollStart - (int)((((click.y() / getChatScale()) - scrollMouseStart) / getHeight()) * messages.size());
			scroll(0);
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
	
	@Inject(method = "render(Lnet/minecraft/client/gui/hud/ChatHud$Backend;IIZ)V", at = @At("HEAD"), cancellable = true)
	private void renderChatTabs(ChatHud.Backend drawer, int windowHeight, int currentTick, boolean expanded, CallbackInfo ci) {
		this.windowHeight = windowHeight;
		if(!expanded) contextMenu = null;
		if(client.currentScreen instanceof EditChatScreen && !dummyChat) {
			ci.cancel();
		} else if(ChatTabsConfig.getInstance().enabled && !dummyChat) {
			int i = visibleMessages.size();
			if(ChatTabsConfig.getInstance().selectedTab > 0) {
				i = ChatTabsConfig.getInstance().chatTabs.get(ChatTabsConfig.getInstance().selectedTab - 1).getVisibleChatLines().size();
			}
			if(i == 0) {
				drawer.updatePose(pose -> pose.scale((float)getChatScale(), (float)getChatScale()));
				boolean unreads = (lastSeenLine != null && visibleMessages.indexOf(lastSeenLine) > 0) || (!visibleMessages.isEmpty() && firstMessageUnread);
				int[] ret = ChatHudOverlays.renderChatTabs(client, tabScroll, drawContext, windowHeight, (float)getChatScale(), expanded, getWidth(), mouseX, mouseY, 0, unreads);
				hoveredTab = ret[0];
				tabScroll = ret[1];
				drawer.updatePose(pose -> pose.scale((float)(1 / getChatScale()), (float)(1 / getChatScale())));
			}
		}
	}
	
	@Inject(method = "render(Lnet/minecraft/client/gui/hud/ChatHud$Backend;IIZ)V", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/hud/ChatHud;scrolledLines:I", opcode = Opcodes.GETFIELD), locals = LocalCapture.CAPTURE_FAILSOFT)
	private void renderChatTabs(ChatHud.Backend drawer, int windowHeight, int currentTick, boolean expanded, CallbackInfo ci, int i, Profiler profiler, float f, int j, int k, float g, float h, int l, int m, double d, int n, int o, long p, ChatHud.OpacityRule opacityRule, int q) {
		if(ChatTabsConfig.getInstance().enabled) {
			drawer.updatePose(pose -> pose.translate(-4, 0));
			boolean unreads = (lastSeenLine != null && visibleMessages.indexOf(lastSeenLine) > 0) || (!visibleMessages.isEmpty() && firstMessageUnread);
			int[] ret = ChatHudOverlays.renderChatTabs(client, tabScroll, drawContext, windowHeight, (float)getChatScale(), expanded, getWidth(), mouseX, mouseY, q, unreads);
			hoveredTab = ret[0];
			tabScroll = ret[1];
			drawer.updatePose(pose -> pose.translate(4, 0));
		}
	}
	
	@Override
	public void chatTabs$renderContextMenu(DrawContext context, int windowWidth, int windowHeight, int mouseX, int mouseY, float deltaTicks) {
		if(contextMenu != null) {
			contextMenu.render(client, drawContext, windowWidth, windowHeight, mouseX, mouseY);
		}
	}
	
	@Override
	public void chatTabs$renderDummy(DrawContext context, TextRenderer textRenderer, int ticks, int mouseX, int mouseY, boolean focused) {
		dummyChat = true;
		dummyChatFocused = focused;
		render(context, textRenderer, ticks, mouseX, mouseY, true, false);
		dummyChat = false;
	}
	
	@Redirect(method = {"render(Lnet/minecraft/client/gui/hud/ChatHud$Backend;IIZ)V", "scroll", "forEachVisibleLine"}, at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/hud/ChatHud;visibleMessages:Ljava/util/List;", opcode = Opcodes.GETFIELD))
	private List<ChatHudLine.Visible> modifyMessages(ChatHud instance) {
		if(dummyChat) {
			return EditChatScreen.DUMMY_CHAT;
		}
		if(ChatTabsConfig.getInstance().enabled && ChatTabsConfig.getInstance().selectedTab > 0) {
			return ChatTabsConfig.getInstance().chatTabs.get(ChatTabsConfig.getInstance().selectedTab - 1).getVisibleChatLines();
		}
		return visibleMessages;
	}
	
	@Inject(method = "refresh", at = @At("HEAD"))
	private void refreshTabs(CallbackInfo ci) {
		ChatTabsConfig.getInstance().chatTabs.forEach(tab -> tab.clear(false));
	}
	
	@Inject(method = "clear", at = @At("HEAD"))
	private void clearTabs(CallbackInfo ci) {
		firstMessageUnread = true;
		ChatTabsConfig.getInstance().chatTabs.forEach(tab -> tab.clear(true));
	}
	
	@Inject(method = "addVisibleMessage", at = @At("HEAD"))
	private void captureChatHudLine(ChatHudLine message, CallbackInfo ci) {
		this.chatMessage = message;
		if(message.content().getContent() instanceof TranslatableTextContent msg) {
			if(msg.getKey().startsWith("commands.message.display")) {
				boolean hasDM = false;
				for(ChatTab tab : ChatTabsConfig.getInstance().chatTabs) {
					if(tab.getFilter().filtersMessages() && tab.getFilter().test(message)) {
						hasDM = true;
						break;
					}
				}
				if(!hasDM && ChatTabsConfig.getInstance().autoGenerateMsgTabs) {
					final String name = msg.getArg(0).getString();
					ChatTabsConfig.getInstance().chatTabs.add(new ChatTab(name, false, new ChatLineFilter(name, true), new SendModifier("/msg " + name + " ")));
				}
			}
		}
	}
	
	@Redirect(method = "addVisibleMessage", at = @At(value = "INVOKE", target = "Ljava/util/List;addFirst(Ljava/lang/Object;)V"))
	private void addToFilteredMessages(List<ChatHudLine.Visible> instance, Object e) {
		instance.addFirst((ChatHudLine.Visible)e);
		if(ChatTabsConfig.getInstance().enabled) {
			for(ChatTab tab : ChatTabsConfig.getInstance().chatTabs) {
				if(tab.getFilter().test(this.chatMessage)) {
					tab.addChatLine((ChatHudLine.Visible)e);
				}
			}
		}
	}
}
