package me.qyh.blog.lock.support;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import me.qyh.blog.exception.LogicException;
import me.qyh.blog.lock.LockManager;
import me.qyh.blog.lock.support.DefaultLock.LockType;
import me.qyh.blog.security.BCrypts;
import me.qyh.util.UUIDs;

public class DefaultLockManager implements LockManager<DefaultLock> {

	@Autowired
	private DefaultLockDao defaultLockDao;

	@Override
	@Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
	@CacheEvict(value = "lockCache", key = "'lock-'+#id")
	public void removeLock(String id) throws LogicException {
		defaultLockDao.delete(id);
	}

	@Override
	@Transactional(readOnly = true)
	@Cacheable(value = "lockCache", key = "'lock-'+#id", unless = "#result == null")
	public DefaultLock findLock(String id) {
		return defaultLockDao.selectById(id);
	}

	@Override
	@Transactional(readOnly = true)
	public List<DefaultLock> allLock() {
		return defaultLockDao.selectAll();
	}

	@Override
	@Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
	public void addLock(DefaultLock lock) throws LogicException {
		lock.setId(UUIDs.uuid());
		lock.setCreateDate(Timestamp.valueOf(LocalDateTime.now()));
		encryptPasswordLock(lock);
		defaultLockDao.insert(lock);
	}

	@Override
	@Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
	@CacheEvict(value = "lockCache", key = "'lock-'+#lock.id")
	public void updateLock(DefaultLock lock) throws LogicException {
		DefaultLock db = defaultLockDao.selectById(lock.getId());
		if (db == null) {
			throw new LogicException("lock.notexists", "锁不存在，可能已经被删除");
		}
		if (!db.getType().equals(lock.getType())) {
			throw new LogicException("lock.type.unmatch", "锁类型不匹配");
		}
		encryptPasswordLock(lock);
		defaultLockDao.update(lock);
	}

	@Override
	public String managePage() {
		return "mgr/lock/default";
	}

	private void encryptPasswordLock(DefaultLock lock) {
		if (LockType.PASSWORD.equals(lock.getType())) {
			PasswordLock plock = (PasswordLock) lock;
			plock.setPassword(BCrypts.encode(plock.getPassword()));
		}
	}

}
