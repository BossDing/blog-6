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
package me.qyh.blog.lock;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * 锁管理器
 * 
 * @author Administrator
 *
 */
public class LockManager implements InitializingBean {
	@Autowired
	private SysLockProvider sysLockProvider;
	private ExpandedLockProvider expandedLockProvider;

	private List<String> allTypes = Lists.newArrayList();

	/**
	 * 根据id获取锁
	 * 
	 * @param id
	 *            锁id
	 * @return 如果不存在返回null
	 */
	public Lock findLock(String id) {
		Lock lock = expandedLockProvider.findLock(id);
		return lock == null ? sysLockProvider.findLock(id) : lock;
	}

	/**
	 * 获取所有的锁
	 * 
	 * @return 所有的锁
	 */
	public List<Lock> allLock() {
		Map<String, Lock> idsMap = Maps.newLinkedHashMap();
		for (Lock lock : expandedLockProvider.allLock())
			idsMap.put(lock.getId(), lock);
		for (Lock lock : sysLockProvider.allLock())
			if (!idsMap.containsKey(lock.getId()))
				idsMap.put(lock.getId(), lock);
		return Collections.unmodifiableList(Lists.newArrayList(idsMap.values()));
	}

	/**
	 * 获取所有的锁类型
	 * 
	 * @return
	 */
	public List<String> allTypes() {
		return allTypes;
	}

	/**
	 * 检查锁类型是否存在
	 * 
	 * @param lockType
	 *            锁类型
	 * @return 存在true，不存在false
	 */
	public boolean checkLockTypeExists(String lockType) {
		return expandedLockProvider.checkLockTypeExists(lockType) || sysLockProvider.checkLockTypeExists(lockType);
	}

	/**
	 * 根据锁类型获取默认模板
	 * 
	 * @param lockType
	 *            锁类型
	 * @return 模板资源
	 */
	public Resource getDefaultTemplateResource(String lockType) {
		Resource resource = expandedLockProvider.getDefaultTemplateResource(lockType);
		return resource == null ? sysLockProvider.getDefaultTemplateResource(lockType) : resource;
	}

	public void setExpandedLockProvider(ExpandedLockProvider expandedLockProvider) {
		this.expandedLockProvider = expandedLockProvider;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (expandedLockProvider == null)
			expandedLockProvider = new ExpandedLockProvider();
		Set<String> types = Sets.newLinkedHashSet();
		for (String type : expandedLockProvider.getLockTypes())
			types.add(type);
		for (String type : sysLockProvider.getLockTypes())
			types.add(type);
		allTypes.addAll(types);
	}
}
