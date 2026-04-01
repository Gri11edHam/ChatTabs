package net.grilledham.chattabs.config;

import com.google.gson.*;
import net.grilledham.chattabs.tabs.ChatLineFilter;
import net.grilledham.chattabs.tabs.ChatTab;
import net.grilledham.chattabs.tabs.SendModifier;

import java.lang.reflect.Type;

public class ChatTabTypeAdapter implements JsonSerializer<ChatTab>, JsonDeserializer<ChatTab> {
	
	@Override
	public ChatTab deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		String name = json.getAsJsonObject().has("name") ? json.getAsJsonObject().get("name").getAsString() : "New Tab";
		String id = json.getAsJsonObject().has("id") ? json.getAsJsonObject().get("id").getAsString() : name;
		boolean save = !json.getAsJsonObject().has("save") || json.getAsJsonObject().get("save").getAsBoolean();
		boolean visiblyByDefault = !json.getAsJsonObject().has("visiblyByDefault") || json.getAsJsonObject().get("visibleByDefault").getAsBoolean();
		ChatLineFilter filter = json.getAsJsonObject().has("filter") ? context.deserialize(json.getAsJsonObject().get("filter"), ChatLineFilter.class) : new ChatLineFilter();
		SendModifier sendModifier = json.getAsJsonObject().has("sendModifier") ? context.deserialize(json.getAsJsonObject().get("sendModifier"), SendModifier.class) : new SendModifier();
		return new ChatTab(id, name, save, visiblyByDefault, filter, sendModifier);
	}
	
	@Override
	public JsonElement serialize(ChatTab src, Type typeOfSrc, JsonSerializationContext context) {
		JsonObject obj = new JsonObject();
		obj.addProperty("id", src.getId());
		obj.addProperty("name", src.getName());
		obj.addProperty("save", src.shouldSave());
		obj.addProperty("visibleByDefault", src.isVisibleByDefault());
		obj.add("filter", context.serialize(src.getFilter(), ChatLineFilter.class));
		obj.add("sendModifier", context.serialize(src.getSendModifier(), SendModifier.class));
		return obj;
	}
}
