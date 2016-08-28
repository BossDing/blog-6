package me.qyh.blog.lock;

import java.util.Map;

public class LockKeyContext {

	private static final ThreadLocal<Map<String, LockKey>> keysLocal = new ThreadLocal<Map<String, LockKey>>();

	public static LockKey getKey(String id) {
		Map<String, LockKey> keyMap = keysLocal.get();
		return keyMap == null ? null : keyMap.get(id);
	}

	public static void remove() {
		keysLocal.remove();
	}

	public static void set(Map<String, LockKey> keysMap) {
		keysLocal.set(keysMap);
	}

}
