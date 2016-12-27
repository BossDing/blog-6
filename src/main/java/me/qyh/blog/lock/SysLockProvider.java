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

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import me.qyh.blog.dao.ArticleDao;
import me.qyh.blog.dao.SpaceDao;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.lock.support.PasswordLock;
import me.qyh.blog.lock.support.SysLock;
import me.qyh.blog.lock.support.SysLock.SysLockType;
import me.qyh.blog.lock.support.SysLockDao;
import me.qyh.blog.security.BCrypts;
import me.qyh.blog.util.UUIDs;

/**
 * 系统锁管理
 * 
 * @author Administrator
 *
 */
public class SysLockProvider {

	@Autowired
	private SysLockDao sysLockDao;
	@Autowired
	private SpaceDao spaceDao;
	@Autowired
	private ArticleDao articleDao;

	private static final String[] LOCK_TYPES = { SysLockType.PASSWORD.name(), SysLockType.QA.name() };

	/**
	 * 删除锁
	 * 
	 * @param id
	 *            锁id
	 */
	@Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED)
	@CacheEvict(value = "lockCache", key = "'lock-'+#id")
	public void removeLock(String id) {
		sysLockDao.delete(id);
		articleDao.deleteLock(id);
		spaceDao.deleteLock(id);
	}

	/**
	 * 根据id查找锁
	 * 
	 * @param id
	 *            锁id
	 * @return 如果不存在返回null
	 */
	@Transactional(readOnly = true)
	@Cacheable(value = "lockCache", key = "'lock-'+#id", unless = "#result == null")
	public SysLock findLock(String id) {
		return sysLockDao.selectById(id);
	}

	/**
	 * 获取所有的系统锁
	 * 
	 * @return 所有的锁
	 */
	@Transactional(readOnly = true)
	public List<SysLock> allLock() {
		return sysLockDao.selectAll();
	}

	/**
	 * 新增系统锁
	 * 
	 * @param lock
	 *            待新增的系统锁
	 */
	@Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED)
	public void addLock(SysLock lock) {
		lock.setId(UUIDs.uuid());
		lock.setCreateDate(Timestamp.valueOf(LocalDateTime.now()));
		encryptPasswordLock(lock);
		sysLockDao.insert(lock);
	}

	/**
	 * 更新系统锁
	 * 
	 * @param lock
	 *            待更新的锁
	 * @throws LogicException
	 *             逻辑异常：锁不存在|锁类型不匹配
	 */
	@Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED)
	@CacheEvict(value = "lockCache", key = "'lock-'+#lock.id")
	public void updateLock(SysLock lock) throws LogicException {
		SysLock db = sysLockDao.selectById(lock.getId());
		if (db == null) {
			throw new LogicException("lock.notexists", "锁不存在，可能已经被删除");
		}
		if (!db.getType().equals(lock.getType())) {
			throw new LogicException("lock.type.unmatch", "锁类型不匹配");
		}
		encryptPasswordLock(lock);
		sysLockDao.update(lock);
	}

	/**
	 * 获取所有的系统锁类型
	 * 
	 * @return 所有的锁类型
	 */
	public String[] getLockTypes() {
		return LOCK_TYPES;
	}

	/**
	 * 检查目标锁类型是否存在
	 * 
	 * @param lockType
	 *            锁类型
	 * @return 存在：true，不存在：false
	 */
	public boolean checkLockTypeExists(String lockType) {
		for (String _lockType : LOCK_TYPES) {
			if (_lockType.equals(lockType)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 根据锁类型获取默认的模板资源
	 * 
	 * @param lockType
	 *            锁类型
	 * @return 模板资源
	 */
	public Resource getDefaultTemplateResource(String lockType) {
		return new ClassPathResource("resources/page/LOCK_" + lockType + ".html");
	}

	private void encryptPasswordLock(SysLock lock) {
		if (SysLockType.PASSWORD.equals(lock.getType())) {
			PasswordLock plock = (PasswordLock) lock;
			plock.setPassword(BCrypts.encode(plock.getPassword()));
		}
	}
}
