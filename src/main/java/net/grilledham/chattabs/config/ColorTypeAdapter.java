package net.grilledham.chattabs.config;

import com.google.gson.*;

import java.awt.*;
import java.lang.reflect.Type;

public class ColorTypeAdapter implements JsonSerializer<Color>, JsonDeserializer<Color> {
	
	@Override
	public Color deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		return new Color(json.getAsInt(), true);
	}
	
	@Override
	public JsonElement serialize(Color src, Type typeOfSrc, JsonSerializationContext context) {
		return new JsonPrimitive(src.getRGB());
	}
}
