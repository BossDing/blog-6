package me.qyh.util;

import java.util.UUID;

public class UUIDs {

	private UUIDs() {

	}

	public static String uuid() {
		return UUID.randomUUID().toString().replace("-", "");
	}

}
