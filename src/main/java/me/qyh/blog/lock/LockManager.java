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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;

public class LockManager implements InitializingBean {
	@Autowired
	private SysLockProvider sysLockProvider;
	private ExpandedLockProvider expandedLockProvider;

	private List<String> allTypes = new ArrayList<>();

	public Lock findLock(String id) {
		Lock lock = expandedLockProvider.findLock(id);
		return lock == null ? sysLockProvider.findLock(id) : lock;
	}

	public List<Lock> allLock() {
		Map<String, Lock> idsMap = new LinkedHashMap<String, Lock>();
		for (Lock lock : expandedLockProvider.allLock())
			idsMap.put(lock.getId(), lock);
		for (Lock lock : sysLockProvider.allLock())
			if (!idsMap.containsKey(lock.getId()))
				idsMap.put(lock.getId(), lock);
		return Collections.unmodifiableList(new ArrayList<>(idsMap.values()));
	}

	public List<String> allTypes() {
		return allTypes;
	}

	public boolean checkLockTypeExists(String lockType) {
		return expandedLockProvider.checkLockTypeExists(lockType) || sysLockProvider.checkLockTypeExists(lockType);
	}

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
		Set<String> types = new LinkedHashSet<String>();
		for (String type : expandedLockProvider.getLockTypes())
			types.add(type);
		for (String type : sysLockProvider.getLockTypes())
			types.add(type);
		allTypes.addAll(types);
	}
}
