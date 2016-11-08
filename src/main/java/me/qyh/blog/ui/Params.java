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
package me.qyh.blog.ui;

import java.util.HashMap;
import java.util.Map;

import me.qyh.blog.exception.SystemException;

public class Params {

	private Map<String, Object> datas = new HashMap<String, Object>();

	public Params add(String key, Object data) {
		datas.put(key, data);
		return this;
	}

	@SuppressWarnings("unchecked")
	public <T> T get(String key, Class<T> t) {
		Object v = datas.get(key);
		if (v != null && !t.isInstance(v)) {
			throw new SystemException("对象:" + v + "无法转化为:" + t.getName());
		}
		return (T) v;
	}

	public boolean has(String key) {
		return datas.containsKey(key);
	}
}
