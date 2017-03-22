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

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.net.URL;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.UrlResource;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.Expose;

import me.qyh.blog.core.entity.DateDeserializer;
import me.qyh.blog.core.lock.SysLock;
import me.qyh.blog.core.lock.LockArgumentResolver.SysLockDeserializer;
import me.qyh.blog.core.message.Message;
import me.qyh.blog.core.message.MessageSerializer;
import me.qyh.blog.core.ui.PageSerializer;
import me.qyh.blog.core.ui.page.Page;

/**
 * json处理工具类
 * 
 * @author Administrator
 *
 */
@UIUtils
public class Jsons {

	private static final ExclusionStrategy SERIALIZATION_EXCLUSION_STRATEGY = new ExclusionStrategy() {

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
	};

	private static final ExclusionStrategy DESERIALIZATION_EXCLUSION_STRATEGY = new ExclusionStrategy() {

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
	};

	private static final Gson gson = new GsonBuilder().setPrettyPrinting()
			.addSerializationExclusionStrategy(SERIALIZATION_EXCLUSION_STRATEGY)
			.addDeserializationExclusionStrategy(DESERIALIZATION_EXCLUSION_STRATEGY).enableComplexMapKeySerialization()
			.registerTypeAdapter(Message.class, new MessageSerializer())
			.registerTypeAdapter(Timestamp.class, new DateDeserializer())
			.registerTypeAdapter(SysLock.class, new SysLockDeserializer())
			.registerTypeAdapter(Page.class, new PageSerializer()).create();

	private static final Logger LOGGER = LoggerFactory.getLogger(Jsons.class);
	private static final String SPLIT_STR = "->";
	private static final UrlReader DEFAULT_READER = new UrlResourceReader();

	private Jsons() {
		super();
	}

	/**
	 * for GsonHttpMessageConverter
	 * 
	 * @return
	 */
	public static Gson getGson() {
		return gson;
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

	/**
	 * @see Jsons#read(String, String, UrlReader)
	 * @param url
	 * @param expression
	 * @return
	 */
	public static ExpressionExecutor read(String url) {
		return read(url, DEFAULT_READER);
	}

	/**
	 * 读取连接中的内容(必须为json字符串)。通过表达式获取指定内容
	 * 
	 * @param url
	 *            url
	 * @param expression
	 *            表达式
	 * @param reader
	 *            表达式读取
	 * @return
	 */
	public static ExpressionExecutor read(String url, UrlReader reader) {
		try {
			return readJson(reader.read(url));
		} catch (IOException e) {
			return new ExpressionExecutor(JsonNull.INSTANCE);
		}
	}

	/**
	 * 读取连接中的内容(必须为json字符串)。通过表达式获取指定内容
	 * 
	 * @param url
	 *            url
	 * @param expression
	 *            表达式
	 * @param reader
	 *            表达式读取
	 * @return
	 */
	public static ExpressionExecutor readJson(String json) {
		JsonElement je = null;
		try {
			JsonParser jp = new JsonParser();
			je = jp.parse(json);
		} catch (Exception e) {
			LOGGER.debug(e.getMessage(), e);
			je = JsonNull.INSTANCE;
		}

		return new ExpressionExecutor(je);
	}

	/**
	 * 
	 * @author mhlx
	 *
	 */
	@FunctionalInterface
	public interface UrlReader {

		/**
		 * 读取Url中的内容
		 * 
		 * @param url
		 * @exception IOException
		 * @return
		 */
		String read(String url) throws IOException;

	}

	private static final class UrlResourceReader implements UrlReader {

		@Override
		public String read(String url) throws IOException {
			UrlResource urlResource = new UrlResource(new URL(url));
			return Resources.readResourceToString(urlResource);
		}

	}

	/**
	 * <p>
	 * 假如json内容为
	 * 
	 * <pre>
	{
	"success": true,
	"data": {
	"data": {
	  "files": [
	    {
	      "begin": "Jan 26, 2017 12:00:00 AM",
	      "end": "Jan 27, 2017 12:00:00 AM",
	      "count": 1
	    }
	  ],
	  "mode": "YMD"
	},
	"dataName": "articleDateFiles"
	}	
	}
	 * </pre>
	 * 
	 * 那么通过表达式 {@code data->data->files[0]->begin} 将会返回Jan 26, 2017 12:00:00
	 * AM<br>
	 * 通过表达式{@code data-data->files} 将会返回
	 * 
	 * <pre>
	 [{
	 "begin": "Jan 26, 2017 12:00:00 AM",
	 "end": "Jan 27, 2017 12:00:00 AM",
	 "count": 1
	 }]
	 * </pre>
	 * </p>
	 * 
	 * @author mhlx
	 *
	 */
	public static final class ExpressionExecutor {

		private static final Expression NULL_EXPRESSION = new NullExpression();

		private final JsonElement ele;

		private ExpressionExecutor(JsonElement ele) {
			super();
			this.ele = ele;
		}

		public ExpressionExecutor executeForExecutor(String expression) {
			return new ExpressionExecutor(doExecute(expression));
		}

		public String execute(String expression) {
			JsonElement executed = doExecute(expression);
			return executed == JsonNull.INSTANCE ? null
					: executed.isJsonPrimitive() ? executed.getAsString() : executed.toString();
		}

		private JsonElement doExecute(String expression) {
			if (isNull()) {
				return JsonNull.INSTANCE;
			}
			List<Expression> expressionList = parseExpressions(expression);
			JsonElement executed = null;
			for (Expression exp : expressionList) {
				if (exp == NULL_EXPRESSION) {
					return JsonNull.INSTANCE;
				}
				if (executed == null) {
					executed = exp.get(ele);
				} else {
					executed = exp.get(executed);
				}
			}
			return executed;
		}

		public boolean isNull() {
			return ele == JsonNull.INSTANCE;
		}

		private static List<Expression> parseExpressions(String expression) {
			expression = expression.replaceAll("\\s+", "");
			if (expression.isEmpty()) {
				return Arrays.asList(NULL_EXPRESSION);
			}
			if (expression.indexOf(SPLIT_STR) != -1) {
				// multi expressions
				List<Expression> expressionList = new ArrayList<>();
				for (String _expression : expression.split(SPLIT_STR)) {
					_expression = _expression.replaceAll("\\s+", "");
					if (_expression.isEmpty()) {
						return Arrays.asList(NULL_EXPRESSION);
					}
					Expression parsed = parseExpression(_expression);
					if (parsed == NULL_EXPRESSION) {
						return Arrays.asList(NULL_EXPRESSION);
					}
					expressionList.add(parsed);
				}
				return expressionList;
			}
			return Arrays.asList(parseExpression(expression));
		}

		private static Expression parseExpression(String expression) {
			String indexStr = StringUtils.substringBetween(expression, "[", "]");
			if (indexStr != null) {
				try {
					int index = Integer.parseInt(indexStr);
					String _expression = expression.substring(0, expression.indexOf('[')).trim();
					if (!_expression.isEmpty()) {
						return new ArrayExpression(_expression, index);
					}
				} catch (NumberFormatException e) {
					LOGGER.debug(e.getMessage(), e);
				}
			} else {
				return new Expression(expression);
			}
			return NULL_EXPRESSION;
		}

		private static class Expression {
			protected final String expression;

			public Expression(String expression) {
				super();
				this.expression = expression;
			}

			JsonElement get(JsonElement ele) {
				if (ele.isJsonObject()) {
					JsonObject jo = ele.getAsJsonObject();
					if (jo.has(expression)) {
						return jo.get(expression);
					}
				}
				return JsonNull.INSTANCE;
			}
		}

		private static class NullExpression extends Expression {
			public NullExpression() {
				super("");
			}

			JsonElement get(JsonElement ele) {
				return JsonNull.INSTANCE;
			}
		}

		private static class ArrayExpression extends Expression {
			private final int index;

			public ArrayExpression(String expression, int index) {
				super(expression);
				this.index = index;
			}

			@Override
			JsonElement get(JsonElement ele) {
				if (ele.isJsonObject()) {
					JsonObject jo = ele.getAsJsonObject();
					if (jo.has(expression)) {
						JsonElement expressionEle = jo.get(expression);
						if (expressionEle.isJsonArray()) {
							JsonArray array = expressionEle.getAsJsonArray();
							if (index >= 0 && index <= array.size() - 1) {
								return array.get(index);
							}
						}
					}
				}
				return JsonNull.INSTANCE;
			}
		}

		@Override
		public String toString() {
			return ele == JsonNull.INSTANCE ? null : ele.isJsonPrimitive() ? ele.getAsString() : ele.toString();
		}
	}
}
