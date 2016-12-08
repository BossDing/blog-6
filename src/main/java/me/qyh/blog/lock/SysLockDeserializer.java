package me.qyh.blog.lock;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import me.qyh.blog.lock.support.PasswordLock;
import me.qyh.blog.lock.support.QALock;
import me.qyh.blog.lock.support.SysLock;
import me.qyh.blog.lock.support.SysLock.SysLockType;
import me.qyh.blog.util.Jsons;

public class SysLockDeserializer implements JsonDeserializer<SysLock> {

	@Override
	public SysLock deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
			throws JsonParseException {
		if (json.isJsonObject()) {
			JsonObject obj = json.getAsJsonObject();
			JsonElement typeEle = obj.get("type");
			SysLockType type = SysLockType.valueOf(typeEle.getAsString());
			switch (type) {
			case PASSWORD:
				return Jsons.readValue(PasswordLock.class, json);
			case QA:
				return Jsons.readValue(QALock.class, json);
			default:
				return null;
			}
		}
		return null;
	}

}
