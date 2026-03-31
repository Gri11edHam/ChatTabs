package net.grilledham.chattabs.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.Strictness;
import com.google.gson.annotations.Expose;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.MultiElementListEntry;
import me.shedaniel.clothconfig2.gui.entries.NestedListListEntry;
import me.shedaniel.clothconfig2.gui.entries.StringListEntry;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.grilledham.chattabs.ChatTabs;
import net.grilledham.chattabs.profiles.ServerProfile;
import net.grilledham.chattabs.tabs.ChatLineFilter;
import net.grilledham.chattabs.tabs.ChatTab;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ChatTabsConfig {
	
	private static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("chattabs.json");
	
	private static final Gson GSON = new GsonBuilder()
			.setPrettyPrinting()
			.registerTypeHierarchyAdapter(Color.class, new ColorTypeAdapter())
			.registerTypeHierarchyAdapter(ChatTab.class, new ChatTabTypeAdapter())
			.registerTypeHierarchyAdapter(ChatLineFilter.class, new ChatLineFilterTypeAdapter())
			.excludeFieldsWithoutExposeAnnotation()
			.setStrictness(Strictness.LENIENT)
			.create();
	
	@Expose
	public boolean enabled = true;
	
	@Expose
	public int maxLines = 100;
	
	@Expose
	public float previewTime = 10;
	
	@Expose
	public boolean clearHistory = true;
	
	@Expose
	public boolean textShadow = true;
	
	@Expose
	public Color bgColor = new Color(0x80000000, true);
	
	@Expose
	public Color bgColorHovered = new Color(0x80FFFFFF, true);
	
	@Expose
	public Color selectedTabColor = new Color(0xFFFFFFFF, true);
	
	@Expose
	public Color unreadColor = new Color(0xFF00AAAA, true);
	
	@Expose
	public int chatWidth = 320;
	
	@Expose
	public int chatHeightUnfocused = 90;
	
	@Expose
	public int chatHeightFocused = 180;
	
	@Expose
	public boolean autoGenerateMsgTabs = true;
	
	@Expose
	private boolean saveGenerated = false;
	
	public int selectedTab = 0;
	
	@Expose
	private List<ChatTab> chatTabs = new ArrayList<>();
	
	@Expose
	public List<ServerProfile> serverProfiles = new ArrayList<>();
	
	private static ChatTabsConfig INSTANCE;
	
	public ConfigBuilder generateConfig() {
		ConfigBuilder builder = ConfigBuilder.create()
				.setTitle(Component.translatable("chattabsconfig.title"))
				.setSavingRunnable(ChatTabsConfig::save)
				.transparentBackground();
		builder.setGlobalizedExpanded(true);
		builder.setGlobalized(true);
		ConfigEntryBuilder entryBuilder = builder.entryBuilder();
		SubCategoryBuilder chatSubCategory = entryBuilder
				.startSubCategory(Component.translatable("chattabsconfig.category.general.chat"))
				.setExpanded(true);
		chatSubCategory.add(entryBuilder
				.startIntField(Component.translatable("chattabsconfig.maxlines"), maxLines)
				.setTooltip(Component.translatable("chattabsconfig.maxlines.tooltip"))
				.setDefaultValue(100)
				.setMin(100)
				.setSaveConsumer(i -> maxLines = i)
				.build());
		chatSubCategory.add(entryBuilder
				.startFloatField(Component.translatable("chattabsconfig.previewtime"), previewTime)
				.setTooltip(Component.translatable("chattabsconfig.previewtime.tooltip"))
				.setDefaultValue(10.0F)
				.setMin(0.0F)
				.setSaveConsumer(f -> previewTime = f)
				.build());
		chatSubCategory.add(entryBuilder
				.startBooleanToggle(Component.translatable("chattabsconfig.clearhistory"), clearHistory)
				.setTooltip(Component.translatable("chattabsconfig.clearhistory.tooltip"))
				.setDefaultValue(true)
				.setSaveConsumer(i -> clearHistory = i)
				.build());
		NestedListListEntry<ChatTab, MultiElementListEntry<ChatTab>> chatTabsList = new NestedListListEntry<>(
				Component.translatable("chattabsconfig.chattabs"),
				chatTabs,
				true,
				Optional::empty,
				l -> {
					chatTabs = l;
					selectedTab = Math.min(chatTabs.size(), selectedTab);
				},
				List::of,
				Component.translatable("chattabsconfig.reset"),
				true,
				false,
				(value, entry) -> {
					ChatTab tab = value != null ? value : new ChatTab();
					return new MultiElementListEntry<>(Component.literal(tab.getName()), tab, List.of(
							entryBuilder.startStrField(Component.translatable("chattabsconfig.chattab.id"), tab.getId())
									.setDefaultValue("new_tab")
									.setSaveConsumer(tab::setId)
									.build(),
							entryBuilder.startStrField(Component.translatable("chattabsconfig.chattab.name"), tab.getName())
									.setDefaultValue("New Tab")
									.setSaveConsumer(tab::setName)
									.build(),
							entryBuilder.startBooleanToggle(Component.translatable("chattabsconfig.chattab.save"), tab.shouldSave())
									.setDefaultValue(true)
									.setSaveConsumer(tab::setSave)
									.build(),
							new MultiElementListEntry<>(Component.translatable("chattabsconfig.chattab.filter"), tab.getFilter(), List.of(
									entryBuilder.startBooleanToggle(Component.translatable("chattabsconfig.chattab.filter.messages"), tab.getFilter().filtersMessages())
											.setDefaultValue(false)
											.setSaveConsumer(tab.getFilter()::filterMessages)
											.build(),
									entryBuilder.startStrField(Component.translatable("chattabsconfig.chattab.filter.regex"), tab.getFilter().getRegex())
											.setTooltip(Component.translatable("chattabsconfig.chattab.filter.regex.tooltip"))
											.setDefaultValue(".*")
											.setSaveConsumer(tab.getFilter()::setRegex)
											.build(),
									entryBuilder.startEnumSelector(Component.translatable("chattabsconfig.chattab.filter.color"), ChatLineFilter.ColorFilter.class, tab.getFilter().getColorFilter())
											.setTooltip(Component.translatable("chattabsconfig.chattab.filter.color.tooltip"))
											.setDefaultValue(ChatLineFilter.ColorFilter.DISABLED)
											.setSaveConsumer(tab.getFilter()::setColorFilter)
											.build(),
									entryBuilder.startColorField(Component.translatable("chattabsconfig.chattab.filter.color.hex"), tab.getFilter().getHexColor())
											.setDefaultValue(0xFFFFFF)
											.setSaveConsumer(tab.getFilter()::setHexColor)
											.build()
							), true),
							new MultiElementListEntry<>(Component.translatable("chattabsconfig.chattab.sendmodifier"), tab.getFilter(), List.of(
									entryBuilder.startStrField(Component.translatable("chattabsconfig.chattab.sendmodifier.prefix"), tab.getSendModifier().getPrefix())
											.setDefaultValue("")
											.setSaveConsumer(tab.getSendModifier()::setPrefix)
											.build(),
									entryBuilder.startStrField(Component.translatable("chattabsconfig.chattab.sendmodifier.suffix"), tab.getSendModifier().getSuffix())
											.setDefaultValue("")
											.setSaveConsumer(tab.getSendModifier()::setSuffix)
											.build()
							), true)
					), true);
				}
		);
		NestedListListEntry<ServerProfile, MultiElementListEntry<ServerProfile>> serverProfilesList = new NestedListListEntry<>(
				Component.translatable("chattabsconfig.serverprofiles"),
				serverProfiles,
				true,
				Optional::empty,
				l -> serverProfiles = l,
				List::of,
				Component.translatable("chattabsconfig.reset"),
				true,
				false,
				(value, entry) -> {
					ServerProfile profile = value != null ? value : new ServerProfile();
					NestedListListEntry<String, StringListEntry> tabs = new NestedListListEntry<>(
							Component.translatable("chattabsconfig.serverprofile.tabs"),
							profile.getTabIds(),
							true,
							Optional::empty,
							profile::setTabIds,
							List::of,
							Component.translatable("chattabsconfig.reset"),
							true,
							false,
							(v, listEntry) -> {
								String tabId = v != null ? v : "";
								return entryBuilder.startStrField(Component.translatable("chattabsconfig.serverprofile.tabs.tabId"), tabId)
										.setDefaultValue("")
										.build();
							}
					);
					return new MultiElementListEntry<>(Component.literal(profile.getServerAddress()), profile, List.of(
							entryBuilder.startStrField(Component.translatable("chattabsconfig.serverprofile.serveraddress"), profile.getServerAddress())
									.setDefaultValue("")
									.setSaveConsumer(profile::setServerAddress)
									.build(),
							tabs
					), true);
				}
		);
		builder.getOrCreateCategory(Component.translatable("chattabsconfig.category.general"))
				.addEntry(entryBuilder
						.startBooleanToggle(Component.translatable("chattabsconfig.enabled"), enabled)
						.setTooltip(Component.translatable("chattabsconfig.enabled.tooltip"))
						.setDefaultValue(true)
						.setSaveConsumer(b -> enabled = b)
						.build())
				.addEntry(chatSubCategory.build());
		builder.getOrCreateCategory(Component.translatable("chattabsconfig.category.tabs"))
				.addEntry(entryBuilder
						.startBooleanToggle(Component.translatable("chattabsconfig.autogeneratemsgtabs"), autoGenerateMsgTabs)
						.setTooltip(Component.translatable("chattabsconfig.autogeneratemsgtabs.tooltip"))
						.setDefaultValue(true)
						.setSaveConsumer(b -> autoGenerateMsgTabs = b)
						.build())
				.addEntry(entryBuilder
						.startBooleanToggle(Component.translatable("chattabsconfig.savegenerated"), saveGenerated)
						.setDefaultValue(false)
						.setSaveConsumer(b -> saveGenerated = b)
						.build())
				.addEntry(chatTabsList)
				.addEntry(serverProfilesList);
		builder.getOrCreateCategory(Component.translatable("chattabsconfig.category.appearance"))
				.addEntry(entryBuilder
						.startBooleanToggle(Component.translatable("chattabsconfig.textshadow"), textShadow)
						.setTooltip(Component.translatable("chattabsconfig.textshadow.tooltip"))
						.setDefaultValue(true)
						.setSaveConsumer(b -> textShadow = b)
						.build())
				.addEntry(entryBuilder
						.startAlphaColorField(Component.translatable("chattabsconfig.bgcolor"), bgColor.getRGB())
						.setTooltip(Component.translatable("chattabsconfig.bgcolor.tooltip"))
						.setDefaultValue(0x80000000)
						.setSaveConsumer(i -> bgColor = new Color(i, true))
						.build())
				.addEntry(entryBuilder
						.startAlphaColorField(Component.translatable("chattabsconfig.bgcolorhovered"), bgColorHovered.getRGB())
						.setTooltip(Component.translatable("chattabsconfig.bgcolorhovered.tooltip"))
						.setDefaultValue(0x80FFFFFF)
						.setSaveConsumer(i -> bgColorHovered = new Color(i, true))
						.build())
				.addEntry(entryBuilder
						.startAlphaColorField(Component.translatable("chattabsconfig.selectedtabcolor"), selectedTabColor.getRGB())
						.setTooltip(Component.translatable("chattabsconfig.selectedtabcolor.tooltip"))
						.setDefaultValue(0xFFFFFFFF)
						.setSaveConsumer(i -> selectedTabColor = new Color(i, true))
						.build())
				.addEntry(entryBuilder
						.startAlphaColorField(Component.translatable("chattabsconfig.unreadcolor"), unreadColor.getRGB())
						.setTooltip(Component.translatable("chattabsconfig.unreadcolor.tooltip"))
						.setDefaultValue(0xFF00AAAA)
						.setSaveConsumer(i -> unreadColor = new Color(i, true))
						.build())
				.addEntry(entryBuilder
						.startIntField(Component.translatable("chattabsconfig.chatwidth"), chatWidth)
						.setDefaultValue(320)
						.setMin(40)
						.setSaveConsumer(i -> chatWidth = i)
						.build())
				.addEntry(entryBuilder
						.startIntField(Component.translatable("chattabsconfig.chatheightfocused"), chatHeightFocused)
						.setDefaultValue(180)
						.setMin(20)
						.setSaveConsumer(i -> chatHeightFocused = i)
						.build())
				.addEntry(entryBuilder
						.startIntField(Component.translatable("chattabsconfig.chatheightunfocused"), chatHeightUnfocused)
						.setDefaultValue(90)
						.setMin(20)
						.setSaveConsumer(i -> chatHeightUnfocused = i)
						.build())
		;
		return builder;
	}
	
	public ServerProfile getCurrentServerProfile() {
		ServerData serverData = Minecraft.getInstance().getCurrentServer();
		if(serverData == null) {
			return null;
		}
		List<ServerProfile> matchingProfiles = serverProfiles.stream().filter(profile -> serverData.ip.endsWith(profile.getServerAddress())).sorted((a, b) -> {
			int ac = serverData.ip.compareTo(a.getServerAddress());
			int bc = serverData.ip.compareTo(b.getServerAddress());
			return Integer.compare(ac, bc);
		}).toList();
		if(matchingProfiles.isEmpty()) {
			return null;
		} else if(matchingProfiles.size() == 1) {
			return matchingProfiles.getLast();
		}
		return null;
	}
	
	public List<ChatTab> getVisibleChatTabs() {
		ServerProfile profile = getCurrentServerProfile();
		if(profile == null) return chatTabs;
		return profile.getTabs();
	}
	
	public List<ChatTab> getChatTabs() {
		return chatTabs;
	}
	
	public ChatTab getSelectedChatTab() {
		return getVisibleChatTabs().get(selectedTab - 1);
	}
	
	public void addChatTabFirst(ChatTab newTab) {
		chatTabs.addFirst(newTab);
		ServerProfile profile = getCurrentServerProfile();
		if(profile != null) profile.addTabId(newTab.getId());
	}
	
	public void addChatTabLast(ChatTab newTab) {
		chatTabs.addLast(newTab);
		ServerProfile profile = getCurrentServerProfile();
		if(profile != null) profile.addTabId(newTab.getId());
	}
	
	public void addChatTabLeft(ChatTab tabToRight, ChatTab newTab) {
		chatTabs.add(chatTabs.indexOf(tabToRight), newTab);
		ServerProfile profile = getCurrentServerProfile();
		if(profile != null) profile.addTabId(newTab.getId());
	}
	
	public void addChatTabLeft(int tabToRight,  ChatTab newTab) {
		ChatTab ttr = getVisibleChatTabs().get(tabToRight);
		addChatTabLeft(ttr, newTab);
	}
	
	public void addChatTabRight(ChatTab tabToLeft, ChatTab newTab) {
		chatTabs.add(chatTabs.indexOf(tabToLeft) + 1, newTab);
		ServerProfile profile = getCurrentServerProfile();
		if(profile != null) profile.addTabId(newTab.getId());
	}
	
	public void addChatTabRight(int tabToLeft, ChatTab newTab) {
		ChatTab ttl = getVisibleChatTabs().get(tabToLeft);
		addChatTabRight(ttl, newTab);
	}
	
	public void removeChatTab(ChatTab tab) {
		chatTabs.remove(tab);
		ServerProfile profile = getCurrentServerProfile();
		if(profile != null && chatTabs.stream().filter(t -> t.getId().equals(tab.getId())).toList().isEmpty()) profile.removeTabId(tab.getId());
	}
	
	public void removeVisibleChatTab(int i) {
		removeChatTab(getVisibleChatTabs().get(i));
	}
	
	public void moveChatTabLeft(ChatTab tab) {
		int ttli = getVisibleChatTabs().indexOf(tab) - 1;
		if(ttli < 0) return;
		ChatTab ttl = getVisibleChatTabs().get(ttli);
		chatTabs.remove(tab);
		chatTabs.add(chatTabs.indexOf(ttl), tab);
	}
	
	public void moveChatTabLeft(int tab) {
		moveChatTabLeft(getVisibleChatTabs().get(tab));
	}
	
	public void moveChatTabRight(ChatTab tab) {
		int ttri = getVisibleChatTabs().indexOf(tab) + 1;
		if(ttri >= getVisibleChatTabs().size()) return;
		ChatTab ttr = getVisibleChatTabs().get(ttri);
		chatTabs.remove(tab);
		chatTabs.add(chatTabs.indexOf(ttr) + 1, tab);
	}
	
	public void moveChatTabRight(int tab) {
		moveChatTabRight(getVisibleChatTabs().get(tab));
	}
	
	public static ChatTabsConfig getInstance() {
		if(INSTANCE == null) INSTANCE = new ChatTabsConfig();
		return INSTANCE;
	}
	
	public static void load() {
		if(Files.notExists(CONFIG_FILE)) {
			save();
			return;
		}
		try {
			INSTANCE = GSON.fromJson(Files.newBufferedReader(CONFIG_FILE), ChatTabsConfig.class);
			if(!INSTANCE.saveGenerated) {
				INSTANCE.chatTabs.removeIf(tab -> !tab.shouldSave());
			}
		} catch(IOException e) {
			ChatTabs.LOGGER.error("Failed to load config file!", e);
		}
	}

	public static void save() {
		try {
			Files.writeString(CONFIG_FILE, GSON.toJson(getInstance()), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
		} catch(IOException e) {
			ChatTabs.LOGGER.error("Failed to save config file!", e);
		}
	}
}