package net.grilledham.chattabs.config;

import com.google.gson.*;
import net.grilledham.chattabs.tabs.ChatLineFilter;

import java.lang.reflect.Type;

public class ChatLineFilterTypeAdapter implements JsonSerializer<ChatLineFilter>, JsonDeserializer<ChatLineFilter> {
	
	@Override
	public ChatLineFilter deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		boolean filterMessages = json.getAsJsonObject().has("filterMessages") && json.getAsJsonObject().get("filterMessages").getAsBoolean();
		String regex = json.getAsJsonObject().has("regex") ? json.getAsJsonObject().get("regex").getAsString() : ".*";
		return new ChatLineFilter(regex, filterMessages);
	}
	
	@Override
	public JsonElement serialize(ChatLineFilter src, Type typeOfSrc, JsonSerializationContext context) {
		JsonObject obj = new JsonObject();
		obj.addProperty("filterMessages", src.filtersMessages());
		obj.addProperty("regex", src.getRegex());
		return obj;
	}
}
