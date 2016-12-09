/*
 * Copyright 2016 qyh.me
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.qyh.blog.util;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.annotations.Expose;

import me.qyh.blog.entity.DateDeserializer;
import me.qyh.blog.message.Message;
import me.qyh.blog.message.MessageSerializer;
import me.qyh.blog.ui.PageSerializer;
import me.qyh.blog.ui.page.Page;

/**
 * json处理工具类
 * 
 * @author Administrator
 *
 */
public class Jsons {
	private static final Gson gson = new GsonBuilder().setPrettyPrinting()
			.addSerializationExclusionStrategy(new ExclusionStrategy() {

				@Override
				public boolean shouldSkipField(FieldAttributes f) {
					Expose expose = f.getAnnotation(Expose.class);
					if (expose != null) {
						return !expose.serialize();
					}
					return false;
				}

				@Override
				public boolean shouldSkipClass(Class<?> clazz) {
					return false;
				}
			}).addDeserializationExclusionStrategy(new ExclusionStrategy() {

				@Override
				public boolean shouldSkipField(FieldAttributes f) {
					Expose expose = f.getAnnotation(Expose.class);
					if (expose != null) {
						return !expose.deserialize();
					}
					return false;
				}

				@Override
				public boolean shouldSkipClass(Class<?> clazz) {
					return false;
				}
			}).registerTypeAdapter(Message.class, new MessageSerializer())
			.registerTypeAdapter(Timestamp.class, new DateDeserializer())
			.registerTypeAdapter(Page.class, new PageSerializer()).create();

	private Jsons() {
		super();
	}

	/**
	 * 将json tree转化为指定对象
	 * 
	 * @param t
	 *            目标类型
	 * @param json
	 *            jsontree
	 * @return
	 */
	public static <T> T readValue(Class<T> t, JsonElement json) {
		return gson.fromJson(json, t);
	}

	/**
	 * 将json tree转化为指定对象
	 * 
	 * @param type
	 *            目标类型
	 * @param json
	 *            jsontree
	 * @return
	 */
	public static <T> T readValue(Type type, JsonElement json) {
		return gson.fromJson(json, type);
	}

	/**
	 * 将Reader转化为指定对象
	 * 
	 * @param type
	 *            目标类型
	 * @param reader
	 *            输入流
	 * @return
	 */
	public static <T> T readValue(Type type, Reader reader) {
		return gson.fromJson(reader, type);
	}

	/**
	 * 将json字符串转化为目标对象
	 * 
	 * @param t
	 *            目标类型
	 * @param json
	 *            json字符串
	 * @return
	 */
	public static <T> T readValue(Class<T> t, String json) {
		return gson.fromJson(json, t);
	}

	/**
	 * 将对象输出成jsontree
	 * 
	 * @param object
	 *            对象
	 * @return
	 */
	public static JsonElement readTree(Object object) {
		return gson.toJsonTree(object);
	}

	/**
	 * 将json字符串转化为ArrayList集合
	 * 
	 * @param clazz
	 *            目标Array类型
	 * @param json
	 *            json字符串
	 * @return
	 */
	public static <T> List<T> readList(Class<T[]> clazz, String json) {
		final T[] jsonToObject = gson.fromJson(json, clazz);
		return Arrays.asList(jsonToObject);
	}

	/**
	 * 将对象输出为json文本
	 * 
	 * @param toWrite
	 *            对象
	 * @return
	 */
	public static String write(Object toWrite) {
		return gson.toJson(toWrite);
	}

	/**
	 * 将对象输出到writer
	 * 
	 * @param toWrite
	 *            对象
	 * @param writer
	 *            输出流
	 */
	public static void write(Object toWrite, Writer writer) {
		gson.toJson(toWrite, writer);
	}
}
