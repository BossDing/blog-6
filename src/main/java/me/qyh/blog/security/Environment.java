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
package me.qyh.blog.security;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import me.qyh.blog.entity.Space;
import me.qyh.blog.entity.User;

public final class Environment {

	private static final ThreadLocal<User> userLocal = new ThreadLocal<>();
	private static final ThreadLocal<Space> spaceLocal = new ThreadLocal<>();
	private static final ThreadLocal<String> ipLocal = new ThreadLocal<>();

	public static Optional<User> getUser() {
		return Optional.ofNullable(userLocal.get());
	}

	public static Optional<Space> getSpace() {
		return Optional.ofNullable(spaceLocal.get());
	}

	/**
	 * 验证当前用户是否已经登录
	 * 
	 * @param authencation
	 *            如果没有登录，抛出一个指定的AuthencationException
	 */
	public static void doAuthencation(Supplier<? extends AuthencationException> authencation) {
		getUser().orElseThrow(authencation);
	}

	/**
	 * 验证当前用户是否已经登录
	 * 
	 * @param authencation
	 *            如果没有登录，抛出一个AuthencationException
	 */
	public static void doAuthencation() {
		doAuthencation(AuthencationException::new);
	}

	/**
	 * 目标空间是否匹配当前空间<br>
	 * <b>如果两个空间id相同或者拥有相同的别名，则认为匹配</b>
	 * 
	 * @param space
	 *            目标空间 ，可以为null
	 * @return
	 */
	public static boolean match(Space space) {
		return Objects.equals(getSpace().orElse(null), space)
				|| (space != null && Objects.equals(getSpaceAlias().orElse(null), space.getAlias()));
	}

	/**
	 * 设置用户上下文
	 * 
	 * @param user
	 *            用户
	 */
	public static void setUser(User user) {
		userLocal.set(user);
	}

	/**
	 * 移除用户上下文
	 */
	public static void removeUser() {
		userLocal.remove();
	}

	/**
	 * 判断用户是否已经登录
	 * 
	 * @return
	 */
	public static boolean isLogin() {
		return getUser().isPresent();
	}

	/**
	 * 设置空间上下文
	 * 
	 * @param space
	 */
	public static void setSpace(Space space) {
		spaceLocal.set(space);
	}

	/**
	 * 移除空间上下文
	 */
	public static void removeSpace() {
		spaceLocal.remove();
	}

	/**
	 * 是否处于空间中
	 * 
	 * @return
	 */
	public static boolean hasSpace() {
		return getSpace().isPresent();
	}

	/**
	 * 获取当前空间别名
	 * 
	 * @return
	 */
	public static Optional<String> getSpaceAlias() {
		return getSpace().map(Space::getAlias);
	}

	/**
	 * 获取当前访问的IP
	 * 
	 * @return
	 */
	public static Optional<String> getIP() {
		return Optional.ofNullable(ipLocal.get());
	}

	/**
	 * 设置当前访问IP
	 * 
	 * @param ip
	 */
	public static void setIP(String ip) {
		ipLocal.set(ip);
	}

	/**
	 * 清空所有的上下文
	 */
	public static void remove() {
		userLocal.remove();
		spaceLocal.remove();
		ipLocal.remove();
	}
}
