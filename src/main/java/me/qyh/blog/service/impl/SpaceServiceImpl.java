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
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import me.qyh.blog.dao.SpaceDao;
import me.qyh.blog.entity.Space;
import me.qyh.blog.evt.LockDeleteEvent;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.lock.LockManager;
import me.qyh.blog.message.Message;
import me.qyh.blog.pageparam.SpaceQueryParam;
import me.qyh.blog.service.SpaceService;
import me.qyh.blog.service.impl.SpaceCache.SpacesCacheKey;
import me.qyh.blog.util.Validators;

@Service
public class SpaceServiceImpl implements SpaceService, ApplicationListener<LockDeleteEvent> {

	@Autowired
	private SpaceDao spaceDao;
	@Autowired
	private LockManager lockManager;
	@Autowired
	private SpaceCache spaceCache;
	@Autowired
	private ArticleIndexer articleIndexer;
	@Autowired
	private ThreadPoolTaskExecutor threadPoolTaskExecutor;

	@Override
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void addSpace(Space space) throws LogicException {
		lockManager.ensureLockvailable(space.getLockId());

		if (spaceDao.selectByAlias(space.getAlias()) != null) {
			throw new LogicException(
					new Message("space.alias.exists", "别名为" + space.getAlias() + "的空间已经存在了", space.getAlias()));
		}
		if (spaceDao.selectByName(space.getName()) != null) {
			throw new LogicException(
					new Message("space.name.exists", "名称为" + space.getName() + "的空间已经存在了", space.getName()));
		}
		space.setCreateDate(Timestamp.valueOf(LocalDateTime.now()));
		if (space.getIsDefault()) {
			spaceDao.resetDefault();
		}
		spaceDao.insert(space);
		spaceCache.evit(space);
	}

	@Override
	@ArticleIndexRebuild
	@Caching(evict = { @CacheEvict(value = "articleCache", allEntries = true),
			@CacheEvict(value = "articleFilesCache", allEntries = true) })
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
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
		if (space.getIsPrivate()) {
			space.setLockId(null);
		} else {
			lockManager.ensureLockvailable(space.getLockId());
		}

		if (space.getIsDefault()) {
			spaceDao.resetDefault();
		}

		spaceDao.update(space);
		spaceCache.evit(db);

		threadPoolTaskExecutor.execute(() -> {
			articleIndexer.rebuildIndex();
		});
	}

	@Override
	public Optional<Space> selectSpaceByAlias(String alias, boolean lockCheck) {
		Optional<Space> spaceOptional = spaceCache.getSpace(alias);
		if (lockCheck) {
			spaceOptional.ifPresent(space -> lockManager.openLock(space));
		}
		return spaceOptional;
	}

	@Override
	public Optional<Space> getSpace(Integer id) {
		return spaceCache.getSpace(id);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Space> querySpace(SpaceQueryParam param) {
		if (Validators.isEmptyOrNull(param.getAlias(), true) && Validators.isEmptyOrNull(param.getName(), true)) {
			return spaceCache.getSpaces(new SpacesCacheKey(param.getQueryPrivate()));
		}
		return spaceDao.selectByParam(param);
	}

	@Override
	public void onApplicationEvent(LockDeleteEvent event) {
		// synchronized
		// do not worry about transaction
		spaceDao.deleteLock(event.getLockId());
	}
}
