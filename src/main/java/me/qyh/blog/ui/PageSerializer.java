package me.qyh.blog.ui;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import me.qyh.blog.ui.page.ErrorPage;
import me.qyh.blog.ui.page.ExpandedPage;
import me.qyh.blog.ui.page.LockPage;
import me.qyh.blog.ui.page.Page;
import me.qyh.blog.ui.page.Page.PageType;
import me.qyh.blog.ui.page.SysPage;
import me.qyh.blog.ui.page.UserPage;
import me.qyh.blog.util.Jsons;

public class PageSerializer implements JsonDeserializer<Page>, JsonSerializer<Page> {

	@Override
	public Page deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
			throws JsonParseException {
		if (json.isJsonObject()) {
			JsonObject obj = json.getAsJsonObject();
			PageType type;
			try {
				type = PageType.valueOf(obj.get("type").getAsString());
			} catch (Exception e) {
				return null;
			}
			switch (type) {
			case ERROR:
				return Jsons.readValue(ErrorPage.class, json);
			case EXPANDED:
				return Jsons.readValue(ExpandedPage.class, json);
			case LOCK:
				return Jsons.readValue(LockPage.class, json);
			case SYSTEM:
				return Jsons.readValue(SysPage.class, json);
			case USER:
				return Jsons.readValue(UserPage.class, json);
			default:
				return null;
			}
		}
		return null;
	}

	@Override
	public JsonElement serialize(Page src, Type typeOfSrc, JsonSerializationContext context) {
		if (src != null) {
			if (Page.class == src.getClass()) {
				JsonObject obj = new JsonObject();
				obj.add("space", context.serialize(src.getSpace()));
				obj.addProperty("tpl", src.getTpl());
				obj.addProperty("templateName", src.getTemplateName());
				obj.addProperty("id", src.getId());
				PageType type = src.getType();
				obj.add("type", type == null ? JsonNull.INSTANCE : new JsonPrimitive(type.name()));
				return obj;
			} else
				return context.serialize(src, src.getClass());
		}
		return null;
	}
}