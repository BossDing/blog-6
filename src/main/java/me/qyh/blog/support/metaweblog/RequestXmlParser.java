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
package me.qyh.blog.support.metaweblog;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.springframework.util.CollectionUtils;

import me.qyh.blog.util.Times;
import me.qyh.blog.util.Validators;

/**
 * xmlrpc xml解析器
 * 
 * @see https://github.com/apache/oodt/blob/master/commons/src/main/java/org/apache/oodt/commons/util/XMLRPC.java
 * @author Administrator
 *
 */
public class RequestXmlParser {

	private static final String ISO_8601_PATTERN = "yyyyMMdd'T'HH:mm:ss";
	private static final String[] VALUES = new String[] { "i4", "int", "boolean", "string", "double",
			"dateTime.iso8601", "base64", "struct", "array" };

	private RequestXmlParser() {
		super();
	}

	/**
	 * 解析流获取方法和参数对象
	 * 
	 * @param is
	 *            流
	 * @return 方法详情
	 * @throws ParseException
	 *             解析失败
	 */
	public static MethodCaller parse(InputStream is) throws ParseException {
		SAXBuilder builder = new SAXBuilder();
		Document doc = null;
		try {
			doc = builder.build(is);
		} catch (Exception e) {
			throw new ParseException(e.getMessage(), e);
		}
		Element root = doc.getRootElement();
		if (!"methodCall".equals(root.getName())) {
			throw new ParseException("解析失败，无法匹配:methodCall根节点");
		}
		List<?> mnNodes = root.getChildren("methodName");
		if (mnNodes.size() != 1) {
			throw new ParseException("解析失败，存在:" + mnNodes.size() + "个methodName节点");
		}
		Element mnEle = (Element) mnNodes.get(0);
		if (Validators.isEmptyOrNull(mnEle.getTextTrim(), false)) {
			throw new ParseException("解析失败，节点:methodName没有对应的值");
		}
		List<?> paramsNodes = root.getChildren("params");
		if (paramsNodes.size() > 1) {
			throw new ParseException("解析失败，存在多个params节点");
		}
		List<Object> arguments = new ArrayList<>();
		if (!paramsNodes.isEmpty()) {
			List<?> params = ((Element) paramsNodes.get(0)).getChildren("param");
			if (!params.isEmpty()) {
				for (Object paramNode : params) {
					Element paramEle = (Element) paramNode;
					List<?> valueNodes = paramEle.getChildren("value");
					if (valueNodes.size() != 1) {
						throw new ParseException("解析失败，param节点下存在:" + valueNodes.size() + "个value节点");
					}
					Element valueEle = (Element) valueNodes.get(0);
					arguments.add(parseValueElement(valueEle));
				}
			}
		}
		return new MethodCaller(mnEle.getTextTrim(), arguments.toArray(new Object[arguments.size()]));
	}

	/**
	 * 创建响应报文
	 * 
	 * @param params
	 *            参数
	 * @return xml报文
	 */
	public static String createResponseXml(Object... params) {
		StringBuilder sb = new StringBuilder();
		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		sb.append("<methodResponse>");
		if (params != null && params.length > 0) {
			sb.append("<params>");
			for (Object p : params) {
				sb.append("<param>");
				sb.append(buildVXml(p));
				sb.append("</param>");
			}
			sb.append("</params>");
		}
		sb.append("</methodResponse>");
		return sb.toString();
	}

	/**
	 * 创建失败报文
	 * 
	 * @param code
	 *            错误码
	 * @param desc
	 *            描述
	 * @return xml报文
	 */
	public static String createFailXml(String code, String desc) {
		StringBuilder sb = new StringBuilder();
		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		sb.append("<methodResponse>");
		sb.append("<fault>");
		Map<String, String> map = new HashMap<>();
		map.put("faultCode", code);
		map.put("faultString", desc);
		sb.append(buildVXml(map));
		sb.append("</fault>");
		sb.append("</methodResponse>");
		return sb.toString();
	}

	private static String buildVXml(Object v) {
		StringBuilder sb = new StringBuilder("<value>");
		if (v instanceof Integer || v instanceof Short) {
			sb.append("<int>").append(v).append("</int>");
		} else if (v instanceof Boolean) {
			sb.append("<boolean>").append((Boolean) v ? "1" : "0").append("</boolean>");
		} else if (v instanceof String) {
			sb.append("<string>").append("<![CDATA[").append(v).append("]]>").append("</string>");
		} else if (v instanceof Double || v instanceof Float) {
			sb.append("<double>").append(v).append("</double>");
		} else if (v instanceof Date) {
			sb.append("<dateTime.iso8601>").append(Times.format(Times.toLocalDateTime((Date) v), ISO_8601_PATTERN))
					.append("</dateTime.iso8601>");
		} else if (v instanceof byte[]) {
			sb.append("<base64>").append("<![CDATA[").append(Base64.getEncoder().encodeToString((byte[]) v))
					.append("]]>").append("</base64>");
		} else if (v instanceof Collection<?>) {
			sb.append("<array>");
			sb.append("<data>");
			for (Object _v : (Collection<?>) v)
				sb.append(buildVXml(_v));
			sb.append("</data>");
			sb.append("</array>");
		} else if (v instanceof Map<?, ?>) {
			sb.append("<struct>");
			Map<?, ?> map = (Map<?, ?>) v;
			for (Object o : map.entrySet()) {
				sb.append("<member>");
				Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;
				if (!(entry.getKey() instanceof String)) {
					throw new IllegalArgumentException("Keys in maps for XML-RPC structs must be Strings");
				}
				sb.append("<name>");
				sb.append(entry.getKey().toString());
				sb.append("</name>");
				sb.append(buildVXml(entry.getValue()));
				sb.append("</member>");
			}
			sb.append("</struct>");
		}

		sb.append("</value>");
		return sb.toString();
	}

	private static Object parseValueElement(Element ve) throws ParseException {
		List<?> vNodes = null;
		for (String v : VALUES) {
			List<?> cvNodes = ve.getChildren(v);
			if (!cvNodes.isEmpty()) {
				if (vNodes != null && !vNodes.isEmpty()) {
					throw new ParseException("解析失败，value节点下存在多个可被解析的值");
				} else {
					vNodes = cvNodes;
				}
			}
		}
		if (CollectionUtils.isEmpty(vNodes)) {
			throw new ParseException("解析失败，value节点下不存在可被解析的值");
		}
		if (vNodes.size() != 1) {
			throw new ParseException("解析失败，value节点下存在多个可被解析的值");
		}
		Element ele = (Element) vNodes.get(0);
		return parseValueElement(ve, ele.getName(), ele.getTextTrim());
	}

	private static Object parseValueElement(Element ve, String type, String v) throws ParseException {
		switch (type) {
		case "i4":
		case "int":
			if (v.isEmpty()) {
				return null;
			}
			try {
				return Integer.parseInt(v);
			} catch (NumberFormatException e) {
				throw new ParseException("解析失败:" + e.getMessage(), e);
			}
		case "boolean":
			if (v.isEmpty()) {
				return null;
			}
			if ("0".equals(v)) {
				return false;
			}
			if ("1".equals(v)) {
				return true;
			}
			throw new ParseException("解析失败:如果节点为boolean值，那么值必须为0或1");
		case "string":
			return v;
		case "double":
			if (v.isEmpty()) {
				return null;
			}
			try {
				return Double.parseDouble(v);
			} catch (NumberFormatException e) {
				throw new ParseException("解析失败:" + e.getMessage(), e);
			}
		case "dateTime.iso8601":
			Optional<LocalDateTime> parsed = Times.parse(v, ISO_8601_PATTERN);
			if (parsed.isPresent()) {
				return Times.toDate(parsed.get());
			}
			throw new ParseException("无法将字符串:" + v + ",解析为日期");
		case "base64":
			try {
				return Base64.getDecoder().decode(v.getBytes());
			} catch (Exception e) {
				throw new ParseException("解析失败:" + e.getMessage(), e);
			}
		case "struct":
			List<?> memberNodes = ve.getChild("struct").getChildren("member");
			if (CollectionUtils.isEmpty(memberNodes)) {
				throw new ParseException("解析失败，struct节点不存在:member节点");
			}
			Map<String, Object> map = new HashMap<>();
			for (Object memberNode : memberNodes) {
				Element memberEle = (Element) memberNode;
				List<?> memberNameNodes = memberEle.getChildren("name");
				if (memberNameNodes.size() != 1) {
					throw new ParseException("解析失败，member节点下存在:" + memberNameNodes.size() + "个name节点");
				}
				List<?> memberValueNodes = memberEle.getChildren("value");
				if (memberValueNodes.size() != 1) {
					throw new ParseException("解析失败，member节点下存在:" + memberValueNodes.size() + "个value节点");
				}
				String name = ((Element) memberNameNodes.get(0)).getTextTrim();
				if (name.isEmpty()) {
					throw new ParseException("解析失败，name节点不能为空");
				}
				map.put(name, parseValueElement((Element) memberValueNodes.get(0)));
			}
			return new Struct(map);
		case "array":
			List<?> dataNodes = ve.getChild("array").getChildren("data");
			if (dataNodes.size() != 1) {
				throw new ParseException("解析失败，array节点下存在:" + dataNodes.size() + "个data节点");
			}
			List<Object> results = new ArrayList<>();
			for (Object vNode : ((Element) dataNodes.get(0)).getChildren("value")) {
				Element dataVe = (Element) vNode;
				results.add(parseValueElement(dataVe));
			}
			return results;
		default:
			throw new ParseException("解析失败，解析类型" + type + "无法被解析");
		}
	}

	static class ParseException extends Exception {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public ParseException(String message, Throwable cause) {
			super(message, cause);
		}

		public ParseException(String message) {
			super(message);
		}
	}

	/**
	 * 调用的方法
	 * 
	 * @author Administrator
	 *
	 */
	public static final class MethodCaller {
		private final String name;
		private final Object[] arguments;

		/**
		 * @param name
		 *            方法名
		 * @param arguments
		 *            参数
		 */
		public MethodCaller(String name, Object[] arguments) {
			this.name = name;
			this.arguments = arguments;
		}

		public String getName() {
			return name;
		}

		public Object[] getArguments() {
			return arguments;
		}
	}

	static final class Struct {
		private Map<String, Object> data = new HashMap<>();

		Struct(Map<String, Object> data) {
			this.data = data;
		}

		/**
		 * 获取字符串
		 * 
		 * @param key
		 *            键
		 * @return 如果不存在，返回null
		 * @throws ParseException
		 *             key对应的对象无法被转化为字符串
		 */
		public String getString(String key) throws ParseException {
			return get(key, String.class);
		}

		/**
		 * 获取Integer
		 * 
		 * @param key
		 *            键
		 * @return 如果不存在，返回null
		 * @throws ParseException
		 *             key对应的对象无法被转化为Integer
		 */
		public Integer getInt(String key) throws ParseException {
			return get(key, Integer.class);
		}

		/**
		 * 获取Boolean
		 * 
		 * @param key
		 *            键
		 * @return 如果不存在，返回null
		 * @throws ParseException
		 *             key对应的对象无法被转化为Boolean
		 */
		public Boolean getBoolean(String key) throws ParseException {
			return get(key, Boolean.class);
		}

		/**
		 * 获取Double
		 * 
		 * @param key
		 *            键
		 * @return 如果不存在，返回null
		 * @throws ParseException
		 *             key对应的对象无法被转化为Double
		 */
		public Double getDouble(String key) throws ParseException {
			return get(key, Double.class);
		}

		/**
		 * 获取Date
		 * 
		 * @param key
		 *            键
		 * @return 如果不存在，返回null
		 * @throws ParseException
		 *             key对应的对象无法被转化为Date
		 */
		public Date getDate(String key) throws ParseException {
			return get(key, Date.class);
		}

		/**
		 * 获取byte []
		 * 
		 * @param key
		 *            键
		 * @return 如果不存在，返回空集合
		 * @throws ParseException
		 *             key对应的对象无法被转化为byte[]
		 */
		public byte[] getBase64(String key) throws ParseException {
			return get(key, byte[].class);
		}

		/**
		 * 获取List
		 * 
		 * @param key
		 *            键
		 * @return 如果不存在，返回空集合
		 * @throws ParseException
		 *             key对应的对象无法被转化为List
		 */
		@SuppressWarnings("unchecked")
		public List<Object> getArray(String key) throws ParseException {
			return get(key, List.class);
		}

		/**
		 * 获取存放指定对象的List
		 * 
		 * @param key
		 *            键
		 * @param t
		 *            类型
		 * @return 如果不存在，返回null
		 * @throws ParseException
		 *             key对应的对象无法被转化为指定对象的List
		 * 
		 */
		@SuppressWarnings("unchecked")
		public <T> List<T> getArray(String key, Class<T> t) throws ParseException {
			List<Object> list = getArray(key);
			if (list == null) {
				return new ArrayList<>();
			}
			List<T> rest = new ArrayList<>();
			for (Object o : list) {
				if (o != null && !t.isInstance(o)) {
					throw new ParseException(o.getClass() + "不是" + t.getName() + "类型");
				}
				rest.add((T) o);
			}
			return rest;
		}

		/**
		 * 获取Struct
		 * 
		 * @param key
		 *            键
		 * @return 如果不存在，返回null
		 * @throws ParseException
		 *             key对应的对象无法被转化为Map
		 */
		@SuppressWarnings("unchecked")
		public Struct getStruct(String key) throws ParseException {
			Map<String, Object> map = get(key, Map.class);
			return map == null ? null : new Struct(map);
		}

		@SuppressWarnings("unchecked")
		private <T> T get(String key, Class<T> t) throws ParseException {
			Object v = data.get(key);
			if (v != null && !t.isInstance(v)) {
				throw new ParseException(v.getClass() + "不是" + t.getName() + "类型");
			}
			return (T) v;
		}
	}
}
