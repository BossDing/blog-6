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
package me.qyh.blog.ui.data;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Maps;

import me.qyh.blog.entity.Space;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.ui.ContextVariables;
import me.qyh.blog.util.Validators;
import me.qyh.blog.web.interceptor.SpaceContext;

public abstract class DataTagProcessor<T> {

	/**
	 * 是否忽略逻辑异常
	 */
	private static final String IGNORE_LOGIC_EXCEPTION = "ignoreLogicException";
	private static final String DATA_NAME = "dataName";

	private String name;// 数据名，唯一
	private String dataName;// 默认数据绑定名，唯一

	protected static final Space previewSpace = new Space();

	static {
		previewSpace.setAlias("preview");
		previewSpace.setArticlePageSize(10);
		previewSpace.setCreateDate(Timestamp.valueOf(LocalDateTime.now()));
		previewSpace.setId(1);
		previewSpace.setIsDefault(false);
		previewSpace.setIsPrivate(false);
		previewSpace.setName("Preview");
		previewSpace.setLockId(null);
	}

	/**
	 * 构造器
	 * 
	 * @param name
	 *            数据处理器名称
	 * @param dataName
	 *            页面dataName
	 */
	public DataTagProcessor(String name, String dataName) {
		this.name = name;
		this.dataName = dataName;
	}

	public final DataBind<T> getData(Space space, ContextVariables variables, Map<String, String> attributes)
			throws LogicException {
		if (attributes == null) {
			attributes = Maps.newHashMap();
		}
		T result = null;
		Attributes atts = new Attributes(attributes);
		try {
			result = query(space, variables, atts);
		} catch (LogicException e) {
			if (!ignoreLogicException(attributes)) {
				throw e;
			}
		}
		DataBind<T> bind = new DataBind<>();
		bind.setData(result);
		String dataNameAttV = atts.get(DATA_NAME);
		if (validDataName(dataNameAttV)) {
			bind.setDataName(dataNameAttV);
		} else {
			bind.setDataName(dataName);
		}
		return bind;
	}

	private boolean ignoreLogicException(Map<String, String> attributes) {
		String v = attributes.get(IGNORE_LOGIC_EXCEPTION);
		if (v != null) {
			try {
				return Boolean.parseBoolean(v);
			} catch (Exception e) {
			}
		}
		return false;
	}

	/**
	 * 构造预览用数据
	 * 
	 * @param attributes
	 * @return
	 */
	public final DataBind<T> previewData(Space space, Map<String, String> attributes) {
		if (attributes == null) {
			attributes = Maps.newHashMap();
		}
		Attributes atts = new Attributes(attributes);
		T result = buildPreviewData(space, atts);
		DataBind<T> bind = new DataBind<>();
		bind.setData(result);
		String dataNameAttV = atts.get(DATA_NAME);
		if (validDataName(dataNameAttV)) {
			bind.setDataName(dataNameAttV);
		} else {
			bind.setDataName(dataName);
		}
		return bind;
	}

	/**
	 * 获取测试数据
	 * 
	 * @return
	 */
	protected abstract T buildPreviewData(Space space, Attributes attributes);

	protected abstract T query(Space space, ContextVariables variables, Attributes attributes) throws LogicException;

	public String getName() {
		return name;
	}

	public String getDataName() {
		return dataName;
	}

	protected Space getSpace() {
		Space current = SpaceContext.get();
		if (current == null) {
			return previewSpace;
		}
		return current;
	}

	protected final class Attributes {
		private final Map<String, String> attMap;

		public String get(String key) {
			return attMap.get(key);
		}

		public Attributes(Map<String, String> attMap) {
			ImmutableMap.Builder<String, String> builder = new Builder<>();
			for (Map.Entry<String, String> att : attMap.entrySet()) {
				builder.put(att.getKey(), att.getValue());
			}
			this.attMap = builder.build();
		}

		@Override
		public String toString() {
			return "Attributes [attMap=" + attMap + "]";
		}
	}

	/**
	 * 依次从attributes，pathVariable,param中获取属性
	 * 
	 * @param variables
	 * @param attributes
	 * @return
	 */
	protected String getVariables(String name, ContextVariables variables, Attributes attributes) {
		String result = attributes.get(name);
		if (result == null) {
			Object pathVariable = variables.getPathVariable(name);
			if (pathVariable != null) {
				result = pathVariable.toString();
			}
		}
		if (result == null) {
			result = variables.getParam(name);
		}
		return result;
	}

	private boolean validDataName(String dataName) {
		return !Validators.isEmptyOrNull(dataName, true) && dataName.matches("[a-zA-Z]+");
	}
}
