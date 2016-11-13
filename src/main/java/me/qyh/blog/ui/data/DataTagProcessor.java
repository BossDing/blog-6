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

import java.util.HashMap;
import java.util.Map;

import me.qyh.blog.entity.Space;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.ui.Params;
import me.qyh.util.Validators;

public abstract class DataTagProcessor<T> {

	/**
	 * 是否忽略逻辑异常
	 */
	private static final String IGNORE_LOGIC_EXCEPTION = "ignoreLogicException";
	private static final String DATA_NAME = "dataName";

	private String name;// 数据名，唯一
	private String dataName;// 默认数据绑定名，页面唯一

	public DataTagProcessor(String name, String dataName) {
		this.name = name;
		this.dataName = dataName;
	}

	public final DataBind<T> getData(Space space, Params params, Map<String, String> attributes) throws LogicException {
		if (attributes == null)
			attributes = new HashMap<>();
		T result = null;
		Attributes atts = new Attributes(attributes);
		try {
			result = query(space, params, atts);
		} catch (LogicException e) {
			if (!ignoreLogicException(attributes)) {
				throw e;
			}
		}
		DataBind<T> bind = new DataBind<>();
		bind.setData(result);
		String dataNameAttV = atts.get(DATA_NAME);
		if (!Validators.isEmptyOrNull(dataNameAttV, true)) {
			bind.setDataName(dataNameAttV);
		} else {
			bind.setDataName(dataName);
		}
		return bind;
	}

	private boolean ignoreLogicException(Map<String, String> attributes) {
		String v = attributes.get(IGNORE_LOGIC_EXCEPTION);
		if (v != null)
			try {
				return Boolean.parseBoolean(v);
			} catch (Exception e) {
			}
		return false;
	}

	/**
	 * 构造预览用数据
	 * 
	 * @param attributes
	 * @return
	 */
	public final DataBind<T> previewData(Map<String, String> attributes) {
		if (attributes == null)
			attributes = new HashMap<>();
		Attributes atts = new Attributes(attributes);
		T result = buildPreviewData(atts);
		DataBind<T> bind = new DataBind<>();
		bind.setData(result);
		String dataNameAttV = atts.get(DATA_NAME);
		if (!Validators.isEmptyOrNull(dataNameAttV, true)) {
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
	protected abstract T buildPreviewData(Attributes attributes);

	protected abstract T query(Space space, Params params, Attributes attributes) throws LogicException;

	public String getName() {
		return name;
	}

	public String getDataName() {
		return dataName;
	}

	protected final class Attributes {
		private Map<String, String> attMap = new HashMap<>();

		public String get(String key) {
			return attMap.get(key.toLowerCase());
		}

		public Attributes(Map<String, String> attMap) {
			this.attMap = attMap;
		}

		@Override
		public String toString() {
			return "Attributes [attMap=" + attMap + "]";
		}

	}

}
