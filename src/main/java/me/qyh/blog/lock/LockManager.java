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
