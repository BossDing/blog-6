package me.qyh.blog.security;

import me.qyh.blog.entity.User;

public class UserContext {

	private static final ThreadLocal<User> userLocal = new ThreadLocal<User>();

	public static void set(User user) {
		userLocal.set(user);
	}

	public static User get() {
		return userLocal.get();
	}

	public static void remove() {
		userLocal.remove();
	}
}
