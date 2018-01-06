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
package me.qyh.blog.web.lock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.util.CollectionUtils;
import org.springframework.web.util.WebUtils;

import me.qyh.blog.core.entity.LockKey;
import me.qyh.blog.core.entity.LockResource;
import me.qyh.blog.core.vo.LockBean;

/**
 * 锁辅助类
 * 
 * @author Administrator
 *
 */
public final class LockHelper {

	private static final String LOCKKEY_SESSION_KEY = LockHelper.class.getName() + ".lockKeys";
	public static final String LOCK_SESSION_KEY = LockHelper.class.getName() + ".lockResources";

	private LockHelper() {

	}

	/**
	 * 获取指定的锁
	 * 
	 * @param request
	 *            请求
	 */
	public static Optional<LockBean> getLockBean(HttpServletRequest request, String beanId) {
		if (beanId == null) {
			return Optional.empty();
		}
		List<LockBean> lockBeans = getLockBeans(request.getSession(false));
		if (CollectionUtils.isEmpty(lockBeans)) {
			return Optional.empty();
		}
		return lockBeans.stream().filter(bean -> bean.getId().equals(beanId)).findAny();
	}

	/**
	 * 从请求中获取中获取资源id和钥匙集合
	 * 
	 * @param request
	 *            当前请求
	 * @return 资源id和钥匙的集合，可能为null
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, List<LockKey>> getKeysMap(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session == null) {
			return null;
		}
		return (Map<String, List<LockKey>>) session.getAttribute(LOCKKEY_SESSION_KEY);
	}

	/**
	 * 在session中为对应锁增加钥匙
	 * 
	 * @param request
	 *            当前请求
	 * @param key
	 *            用户提供的钥匙
	 * @param resourceId
	 *            资源Id
	 */
	public static void addKey(HttpServletRequest request, LockKey key, LockBean lockBean) {
		HttpSession session = request.getSession();
		synchronized (WebUtils.getSessionMutex(session)) {
			Map<String, List<LockKey>> keysMap = (Map<String, List<LockKey>>) getKeysMap(request);
			if (keysMap == null) {
				keysMap = new HashMap<>();
			}
			LockResource lockResource = lockBean.getLockResource();
			List<LockKey> keys = keysMap.get(lockResource.getResource());
			if (CollectionUtils.isEmpty(keys)) {
				keys = new ArrayList<>();
				keys.add(key);
				keysMap.put(lockResource.getResource(), keys);
			} else {
				keys.removeIf(_key -> _key.lockId().equals(key.lockId()));
				keys.add(key);
			}
			session.setAttribute(LOCKKEY_SESSION_KEY, keysMap);

			List<LockBean> beans = getLockBeans(session);

			if (beans != null) {
				beans.removeIf(bean -> {
					return bean.getId().equals(lockBean.getId());
				});

				if (beans.isEmpty()) {
					session.removeAttribute(LOCK_SESSION_KEY);
				}
			}
		}
	}

	/**
	 * 在session中存储解锁失败后的锁对象
	 * 
	 * @param request
	 *            当前请求
	 * @param lockBean
	 *            解锁失败后的所对象
	 */
	public static void storeLockBean(HttpServletRequest request, final LockBean lockBean) {
		HttpSession session = request.getSession();
		synchronized (WebUtils.getSessionMutex(session)) {

			List<LockBean> beans = getLockBeans(session);
			if (beans == null) {
				beans = new ArrayList<>();
			}
			if (!beans.isEmpty()) {

				beans.removeIf(bean -> {
					if (bean.getLockResource().getResource().equals(lockBean.getLockResource().getResource())) {
						lockBean.setId(bean.getId());
						return true;
					}
					return false;
				});
			}
			beans.add(lockBean);
			session.setAttribute(LOCK_SESSION_KEY, beans);
		}
	}

	@SuppressWarnings("unchecked")
	private static List<LockBean> getLockBeans(HttpSession session) {
		if (session == null) {
			return null;
		}
		return (List<LockBean>) session.getAttribute(LOCK_SESSION_KEY);
	}
}
