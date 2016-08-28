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
