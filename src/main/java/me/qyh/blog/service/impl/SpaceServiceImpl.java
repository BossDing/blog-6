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
package me.qyh.blog.service.impl;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import me.qyh.blog.dao.SpaceDao;
import me.qyh.blog.entity.Space;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.lock.Lock;
import me.qyh.blog.lock.LockManager;
import me.qyh.blog.message.Message;
import me.qyh.blog.pageparam.SpaceQueryParam;
import me.qyh.blog.security.AuthencationException;
import me.qyh.blog.security.UserContext;
import me.qyh.blog.service.SpaceService;

@Service
@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
public class SpaceServiceImpl implements SpaceService {

	@Autowired
	private SpaceDao spaceDao;
	@Autowired
	private SpaceCache spaceCache;
	@Autowired
	private LockManager lockManager;

	@Override
	public void addSpace(Space space) throws LogicException {
		checkLock(space.getLockId());

		if (spaceDao.selectByAlias(space.getAlias()) != null) {
			throw new LogicException(
					new Message("space.alias.exists", "别名为" + space.getAlias() + "的空间已经存在了", space.getAlias()));
		}
		if (spaceDao.selectByName(space.getName()) != null) {
			throw new LogicException(
					new Message("space.name.exists", "名称为" + space.getName() + "的空间已经存在了", space.getName()));
		}
		space.setCreateDate(Timestamp.valueOf(LocalDateTime.now()));
		if (space.getIsDefault())
			spaceDao.resetDefault();
		spaceDao.insert(space);
	}

	@Override
	@ArticleIndexRebuild
	@Caching(evict = { @CacheEvict(value = "articleCache", allEntries = true),
			@CacheEvict(value = "articleFilesCache", allEntries = true) })
	public void updateSpace(Space space) throws LogicException {
		Space db = spaceDao.selectById(space.getId());
		if (db == null) {
			throw new LogicException("space.notExists", "空间不存在");
		}
		Space aliasDb = spaceDao.selectByAlias(space.getAlias());
		if (aliasDb != null && !aliasDb.equals(db)) {
			throw new LogicException(
					new Message("space.alias.exists", "别名为" + space.getAlias() + "的空间已经存在了", space.getAlias()));
		}

		Space nameDb = spaceDao.selectByName(space.getName());
		if (nameDb != null && !nameDb.equals(db)) {
			throw new LogicException(
					new Message("space.name.exists", "名称为" + space.getName() + "的空间已经存在了", space.getName()));
		}
		// 如果空间是私有的，那么无法加锁
		if (space.getIsPrivate())
			space.setLockId(null);
		else
			checkLock(space.getLockId());

		if (space.getIsDefault())
			spaceDao.resetDefault();

		spaceDao.update(space);
		spaceCache.evit(db);
	}

	@Override
	@Transactional(readOnly = true)
	public Space selectSpaceByAlias(String alias, boolean lockCheck) {
		Space space = lockCheck ? spaceCache.getSpaceWithLockCheck(alias) : spaceCache.getSpaceWithoutLockCheck(alias);
		if (space != null) {
			if (space.getIsPrivate() && UserContext.get() == null) {
				throw new AuthencationException();
			}
		}
		return space;
	}

	@Override
	@Transactional(readOnly = true)
	public Space getSpace(Integer id) {
		return spaceCache.getSpace(id);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Space> querySpace(SpaceQueryParam param) {
		return spaceDao.selectByParam(param);
	}

	private void checkLock(String id) throws LogicException {
		if (id != null) {
			Lock lock = lockManager.findLock(id);
			if (lock == null) {
				throw new LogicException("lock.notexists", "锁不存在");
			}
		}
	}

}
