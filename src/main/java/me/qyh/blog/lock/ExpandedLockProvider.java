package me.qyh.blog.lock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.util.CollectionUtils;

import me.qyh.blog.exception.SystemException;
import me.qyh.util.Validators;

public class ExpandedLockProvider implements InitializingBean {

	private static final int MAX_ID_LENGTH = 20;
	private static final int MAX_TYPE_LENGTH = 20;
	private static final int MAX_NAME_LENGTH = 20;

	private List<Lock> expandedLocks = new ArrayList<Lock>();

	private Map<String, List<Lock>> typesMap = new LinkedHashMap<>();
	private Map<String, Lock> idsMap = new LinkedHashMap<>();
	private Map<String, Resource> defaultTplResource = new LinkedHashMap<String, Resource>();

	public Lock findLock(String id) {
		return idsMap.get(id);
	}

	public void afterPropertiesSet() throws Exception {
		if (!CollectionUtils.isEmpty(expandedLocks)) {
			for (Lock lock : expandedLocks) {
				validLock(lock);
				String id = lock.getId();
				idsMap.put(id, lock);
				String type = lock.getLockType().trim();
				List<Lock> types = typesMap.get(type);
				if (types == null)
					types = new ArrayList<>();
				types.add(lock);
				typesMap.put(type, types);
			}
			for (String type : typesMap.keySet()) {
				if (defaultTplResource.get(type) == null)
					throw new SystemException("锁类型" + type + "对应的基本模板没有被设置");
			}
		}
	}

	private boolean valid(String str) {
		char[] chars = str.toCharArray();
		for (char ch : chars) {
			if (!isAllowLetter(ch))
				return false;
		}
		return true;
	}

	private void validLock(Lock lock) {
		if (Validators.isEmptyOrNull(lock.getId(), true))
			throw new SystemException("锁ID不能为空");
		String id = lock.getId().trim();
		if (id.length() > MAX_ID_LENGTH)
			throw new SystemException("ID" + lock.getId() + "不能超过" + MAX_ID_LENGTH + "个字符");
		if (!valid(id))
			throw new SystemException("ID只能包含英文字母和数字");
		lock.setId(id);
		if (Validators.isEmptyOrNull(lock.getLockType(), true))
			throw new SystemException("锁类型不能为空");
		String type = lock.getLockType().trim();
		if (type.length() > MAX_TYPE_LENGTH)
			throw new SystemException("锁类型" + lock.getLockType() + "不能超过" + MAX_TYPE_LENGTH + "个字符");
		if (!valid(type))
			throw new SystemException("锁类型只能包含英文字母和数字");
		if (Validators.isEmptyOrNull(lock.getName(), true))
			throw new SystemException("锁名称不能为空");
		String name = lock.getName().trim();
		if (name.length() > MAX_NAME_LENGTH)
			throw new SystemException("锁名称" + lock.getName() + "不能超过" + MAX_NAME_LENGTH + "个字符");
		lock.setName(name);
	}

	private boolean isAllowLetter(char ch) {
		return ('a' <= ch && ch <= 'z') || ('A' <= ch && ch <= 'Z') || ('0' <= ch && ch <= '9');
	}

	public List<? extends Lock> allLock() {
		return Collections.unmodifiableList(new ArrayList<>(idsMap.values()));
	}

	public String[] getLockTypes() {
		return typesMap.keySet().toArray(new String[typesMap.size()]);
	}

	public boolean checkLockTypeExists(String lockType) {
		return typesMap.containsKey(lockType);
	}

	public Resource getDefaultTemplateResource(String lockType) {
		return defaultTplResource.get(lockType);
	}

	public void setExpandedLocks(List<Lock> expandedLocks) {
		this.expandedLocks = expandedLocks;
	}

	public void setDefaultTplResource(Map<String, Resource> defaultTplResource) {
		this.defaultTplResource = defaultTplResource;
	}
}
