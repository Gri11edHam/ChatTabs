package net.grilledham.chattabs.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.Strictness;
import com.google.gson.annotations.Expose;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.MultiElementListEntry;
import me.shedaniel.clothconfig2.gui.entries.NestedListListEntry;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.grilledham.chattabs.ChatTabs;
import net.grilledham.chattabs.tabs.ChatLineFilter;
import net.grilledham.chattabs.tabs.ChatTab;
import net.minecraft.text.Text;

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
	public List<ChatTab> chatTabs = new ArrayList<>();
	
	private static ChatTabsConfig INSTANCE;
	
	public ConfigBuilder generateConfig() {
		ConfigBuilder builder = ConfigBuilder.create()
				.setTitle(Text.translatable("chattabsconfig.title"))
				.setSavingRunnable(ChatTabsConfig::save)
				.transparentBackground();
		builder.setGlobalizedExpanded(true);
		builder.setGlobalized(true);
		ConfigEntryBuilder entryBuilder = builder.entryBuilder();
		SubCategoryBuilder chatSubCategory = entryBuilder
				.startSubCategory(Text.translatable("chattabsconfig.category.general.chat"))
				.setExpanded(true);
		chatSubCategory.add(entryBuilder
				.startIntField(Text.translatable("chattabsconfig.maxlines"), maxLines)
				.setTooltip(Text.translatable("chattabsconfig.maxlines.tooltip"))
				.setDefaultValue(100)
				.setMin(100)
				.setSaveConsumer(i -> maxLines = i)
				.build());
		chatSubCategory.add(entryBuilder
				.startBooleanToggle(Text.translatable("chattabsconfig.clearhistory"), clearHistory)
				.setTooltip(Text.translatable("chattabsconfig.clearhistory.tooltip"))
				.setDefaultValue(true)
				.setSaveConsumer(i -> clearHistory = i)
				.build());
		NestedListListEntry<ChatTab, MultiElementListEntry<ChatTab>> chatTabsList = new NestedListListEntry<>(
				Text.translatable("chattabsconfig.chattabs"),
				chatTabs,
				true,
				Optional::empty,
				l -> {
					chatTabs = l;
					selectedTab = Math.min(chatTabs.size(), selectedTab);
				},
				List::of,
				Text.translatable("chattabsconfig.reset"),
				true,
				false,
				(value, entry) -> {
					ChatTab tab = value != null ? value : new ChatTab();
					return new MultiElementListEntry<>(Text.literal(tab.getName()), tab, List.of(
							entryBuilder.startStrField(Text.translatable("chattabsconfig.chattab.name"), tab.getName())
									.setDefaultValue("New Tab")
									.setSaveConsumer(tab::setName)
									.build(),
							entryBuilder.startBooleanToggle(Text.translatable("chattabsconfig.chattab.save"), tab.shouldSave())
									.setDefaultValue(true)
									.setSaveConsumer(tab::setSave)
									.build(),
							new MultiElementListEntry<>(Text.translatable("chattabsconfig.chattab.filter"), tab.getFilter(), List.of(
									entryBuilder.startBooleanToggle(Text.translatable("chattabsconfig.chattab.filter.messages"), tab.getFilter().filtersMessages())
											.setDefaultValue(false)
											.setSaveConsumer(tab.getFilter()::filterMessages)
											.build(),
									entryBuilder.startStrField(Text.translatable("chattabsconfig.chattab.filter.regex"), tab.getFilter().getRegex())
											.setTooltip(Text.translatable("chattabsconfig.chattab.filter.regex.tooltip"))
											.setDefaultValue(".*")
											.setSaveConsumer(tab.getFilter()::setRegex)
											.build()
							), true),
							new MultiElementListEntry<>(Text.translatable("chattabsconfig.chattab.sendmodifier"), tab.getFilter(), List.of(
									entryBuilder.startStrField(Text.translatable("chattabsconfig.chattab.sendmodifier.prefix"), tab.getSendModifier().getPrefix())
											.setDefaultValue("")
											.setSaveConsumer(tab.getSendModifier()::setPrefix)
											.build(),
									entryBuilder.startStrField(Text.translatable("chattabsconfig.chattab.sendmodifier.suffix"), tab.getSendModifier().getSuffix())
											.setDefaultValue("")
											.setSaveConsumer(tab.getSendModifier()::setSuffix)
											.build()
							), true)
					), true);
				}
		);
		builder.getOrCreateCategory(Text.translatable("chattabsconfig.category.general"))
				.addEntry(entryBuilder
						.startBooleanToggle(Text.translatable("chattabsconfig.enabled"), enabled)
						.setTooltip(Text.translatable("chattabsconfig.enabled.tooltip"))
						.setDefaultValue(true)
						.setSaveConsumer(b -> enabled = b)
						.build())
				.addEntry(chatSubCategory.build());
		builder.getOrCreateCategory(Text.translatable("chattabsconfig.category.tabs"))
				.addEntry(entryBuilder
						.startBooleanToggle(Text.translatable("chattabsconfig.autogeneratemsgtabs"), autoGenerateMsgTabs)
						.setTooltip(Text.translatable("chattabsconfig.autogeneratemsgtabs.tooltip"))
						.setDefaultValue(true)
						.setSaveConsumer(b -> autoGenerateMsgTabs = b)
						.build())
				.addEntry(entryBuilder
						.startBooleanToggle(Text.translatable("chattabsconfig.savegenerated"), saveGenerated)
						.setDefaultValue(false)
						.setSaveConsumer(b -> saveGenerated = b)
						.build())
				.addEntry(chatTabsList);
		builder.getOrCreateCategory(Text.translatable("chattabsconfig.category.appearance"))
				.addEntry(entryBuilder
						.startBooleanToggle(Text.translatable("chattabsconfig.textshadow"), textShadow)
						.setTooltip(Text.translatable("chattabsconfig.textshadow.tooltip"))
						.setDefaultValue(true)
						.setSaveConsumer(b -> textShadow = b)
						.build())
				.addEntry(entryBuilder
						.startAlphaColorField(Text.translatable("chattabsconfig.bgcolor"), bgColor.getRGB())
						.setTooltip(Text.translatable("chattabsconfig.bgcolor.tooltip"))
						.setDefaultValue(0x80000000)
						.setSaveConsumer(i -> bgColor = new Color(i, true))
						.build())
				.addEntry(entryBuilder
						.startAlphaColorField(Text.translatable("chattabsconfig.bgcolorhovered"), bgColorHovered.getRGB())
						.setTooltip(Text.translatable("chattabsconfig.bgcolorhovered.tooltip"))
						.setDefaultValue(0x80FFFFFF)
						.setSaveConsumer(i -> bgColorHovered = new Color(i, true))
						.build())
				.addEntry(entryBuilder
						.startAlphaColorField(Text.translatable("chattabsconfig.selectedtabcolor"), selectedTabColor.getRGB())
						.setTooltip(Text.translatable("chattabsconfig.selectedtabcolor.tooltip"))
						.setDefaultValue(0xFFFFFFFF)
						.setSaveConsumer(i -> selectedTabColor = new Color(i, true))
						.build())
				.addEntry(entryBuilder
						.startAlphaColorField(Text.translatable("chattabsconfig.unreadcolor"), unreadColor.getRGB())
						.setTooltip(Text.translatable("chattabsconfig.unreadcolor.tooltip"))
						.setDefaultValue(0xFF00AAAA)
						.setSaveConsumer(i -> unreadColor = new Color(i, true))
						.build())
				.addEntry(entryBuilder
						.startIntField(Text.translatable("chattabsconfig.chatwidth"), chatWidth)
						.setDefaultValue(320)
						.setMin(40)
						.setSaveConsumer(i -> chatWidth = i)
						.build())
				.addEntry(entryBuilder
						.startIntField(Text.translatable("chattabsconfig.chatheightfocused"), chatHeightFocused)
						.setDefaultValue(180)
						.setMin(20)
						.setSaveConsumer(i -> chatHeightFocused = i)
						.build())
				.addEntry(entryBuilder
						.startIntField(Text.translatable("chattabsconfig.chatheightunfocused"), chatHeightUnfocused)
						.setDefaultValue(90)
						.setMin(20)
						.setSaveConsumer(i -> chatHeightUnfocused = i)
						.build())
		;
		return builder;
//		return YetAnotherConfigLib.createBuilder()
//				.title(Text.translatable("chattabsconfig.title"))
//				.category(ConfigCategory.createBuilder()
//						.name(Text.translatable("chattabsconfig.category.general"))
//						.tooltip(Text.translatable("chattabsconfig.category.general.tooltip"))
//						.option(Option.<Boolean>createBuilder()
//								.name(Text.translatable("chattabsconfig.enabled"))
//								.description(OptionDescription.of(Text.translatable("chattabsconfig.enabled.description")))
//								.binding(true, () -> ChatTabsConfig.enabled, newVal -> ChatTabsConfig.enabled = newVal)
//								.controller(TickBoxControllerBuilder::create)
//								.build())
//						.group(OptionGroup.createBuilder()
//								.name(Text.translatable("chattabsconfig.category.general.chat"))
//								.description(OptionDescription.of(Text.translatable("chattabsconfig.category.general.chat.description")))
//								.option(Option.<Integer>createBuilder()
//										.name(Text.translatable("chattabsconfig.maxlines"))
//										.description(OptionDescription.of(Text.translatable("chattabsconfig.maxlines.description")))
//										.binding(100, () -> ChatTabsConfig.maxLines, newVal -> ChatTabsConfig.maxLines = newVal)
//										.controller(opt -> new IntegerFieldControllerBuilderImpl(opt).min(100))
//										.build())
//								.build())
//						.build())
//				.category(ConfigCategory.createBuilder()
//						.name(Text.translatable("chattabsconfig.category.tabs"))
//						.tooltip(Text.translatable("chattabsconfig.category.tabs.tooltip"))
//						.option(Option.<Boolean>createBuilder()
//								.name(Text.translatable("chattabsconfig.autogeneratemsgtabs"))
//								.description(OptionDescription.of(Text.translatable("chattabsconfig.autogeneratemsgtabs.description")))
//								.binding(true, () -> ChatTabsConfig.autoGenerateMsgTabs, newVal -> ChatTabsConfig.autoGenerateMsgTabs = newVal)
//								.controller(TickBoxControllerBuilder::create)
//								.build())
//						.option(ListOption.<ChatTab>createBuilder()
//								.name(Text.translatable("chattabsconfig.chattabs"))
//								.description(OptionDescription.of(Text.translatable("chattabsconfig.chattabs.description")))
//								.binding(new ArrayList<>(), () -> ChatTabsConfig.chatTabs, newVal -> {
//									ChatTabsConfig.chatTabs = new ArrayList<>(newVal);
//									ChatTabsConfig.selectedTab = Math.min(ChatTabsConfig.selectedTab, ChatTabsConfig.chatTabs.size());
//								})
//								.controller(ChatTabControllerBuilder::create)
//								.initial(new ChatTab("New Tab", false))
//								.insertEntriesAtEnd(true)
//								.build())
//						.build())
//				.category(ConfigCategory.createBuilder()
//						.name(Text.translatable("chattabsconfig.category.appearance"))
//						.tooltip(Text.translatable("chattabsconfig.category.appearance.tooltip"))
//						.option(Option.<Boolean>createBuilder()
//								.name(Text.translatable("chattabsconfig.textshadow"))
//								.description(OptionDescription.of(Text.translatable("chattabsconfig.textshadow.description")))
//								.binding(true, () -> ChatTabsConfig.textShadow, newVal -> ChatTabsConfig.textShadow = newVal)
//								.controller(TickBoxControllerBuilder::create)
//								.build())
//						.option(Option.<Color>createBuilder()
//								.name(Text.translatable("chattabsconfig.bgcolor"))
//								.description(OptionDescription.of(Text.translatable("chattabsconfig.bgcolor.description")))
//								.binding(new Color(0x80000000, true), () -> ChatTabsConfig.bgColor, newVal -> ChatTabsConfig.bgColor = newVal)
//								.controller(opt -> new ColorControllerBuilderImpl(opt).allowAlpha(true))
//								.build())
//						.option(Option.<Color>createBuilder()
//								.name(Text.translatable("chattabsconfig.bgcolorhovered"))
//								.description(OptionDescription.of(Text.translatable("chattabsconfig.bgcolorhovered.description")))
//								.binding(new Color(0x80FFFFFF, true), () -> ChatTabsConfig.bgColorHovered, newVal -> ChatTabsConfig.bgColorHovered = newVal)
//								.controller(opt -> new ColorControllerBuilderImpl(opt).allowAlpha(true))
//								.build())
//						.build())
//				.build();
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