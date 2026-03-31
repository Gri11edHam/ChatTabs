package net.grilledham.chattabs.config;

import com.google.gson.*;
import net.grilledham.chattabs.tabs.ChatLineFilter;

import java.lang.reflect.Type;

public class ChatLineFilterTypeAdapter implements JsonSerializer<ChatLineFilter>, JsonDeserializer<ChatLineFilter> {
	
	@Override
	public ChatLineFilter deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		boolean filterMessages = json.getAsJsonObject().has("filterMessages") && json.getAsJsonObject().get("filterMessages").getAsBoolean();
		String regex = json.getAsJsonObject().has("regex") ? json.getAsJsonObject().get("regex").getAsString() : ".*";
		ChatLineFilter.ColorFilter colorFilter = json.getAsJsonObject().has("colorFilter") ? ChatLineFilter.ColorFilter.valueOf(json.getAsJsonObject().get("colorFilter").getAsString()) : ChatLineFilter.ColorFilter.DISABLED;
		int hexColor = json.getAsJsonObject().has("hexColor") ? json.getAsJsonObject().get("hexColor").getAsInt() : 0xFFFFFF;
		return new ChatLineFilter(regex, colorFilter, hexColor, filterMessages);
	}
	
	@Override
	public JsonElement serialize(ChatLineFilter src, Type typeOfSrc, JsonSerializationContext context) {
		JsonObject obj = new JsonObject();
		obj.addProperty("filterMessages", src.filtersMessages());
		obj.addProperty("regex", src.getRegex());
		obj.addProperty("colorFilter", src.getColorFilter().name());
		obj.addProperty("hexColor", src.getHexColor());
		return obj;
	}
}
