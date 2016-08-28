package me.qyh.blog.web.interceptor;

import me.qyh.blog.entity.Space;

public class SpaceContext {

	private static final ThreadLocal<Space> spaceLocal = new ThreadLocal<Space>();

	public static void set(Space space) {
		spaceLocal.set(space);
	}

	public static Space get() {
		return spaceLocal.get();
	}

	public static void remove() {
		spaceLocal.remove();
	}

}
