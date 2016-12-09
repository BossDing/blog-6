package me.qyh.blog.service.impl;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;

import me.qyh.blog.dao.SpaceDao;
import me.qyh.blog.entity.Space;
import me.qyh.blog.lock.LockProtected;

@Component
public class SpaceCache {

	private final Map<String, Space> aliasCache = Maps.newConcurrentMap();
	private final Map<Integer, Space> idCache = Maps.newConcurrentMap();

	@Autowired
	private SpaceDao spaceDao;

	@LockProtected
	public Space getSpaceWithLockCheck(String alias) {
		if (alias == null) {
			return null;
		}
		Space space = aliasCache.get(alias);
		if (space == null) {
			space = spaceDao.selectByAlias(alias);
			if (space != null) {
				aliasCache.put(alias, space);
				idCache.put(space.getId(), space);
			}
		}
		return space;
	}

	public Space getSpaceWithoutLockCheck(String alias) {
		return getSpaceWithLockCheck(alias);
	}

	public Space getSpace(Integer id) {
		if (id == null) {
			return null;
		}
		Space space = idCache.get(id);
		if (space == null) {
			space = spaceDao.selectById(id);
			if (space != null) {
				aliasCache.put(space.getAlias(), space);
				idCache.put(id, space);
			}
		}
		return space;
	}

	public void evit(Space space) {
		aliasCache.remove(space.getAlias());
		idCache.remove(space.getId());
	}
}
