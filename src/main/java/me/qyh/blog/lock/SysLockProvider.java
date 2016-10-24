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

import me.qyh.blog.exception.LogicException;
import me.qyh.blog.lock.support.PasswordLock;
import me.qyh.blog.lock.support.SysLock;
import me.qyh.blog.lock.support.SysLock.SysLockType;
import me.qyh.blog.lock.support.SysLockDao;
import me.qyh.blog.security.BCrypts;
import me.qyh.util.UUIDs;

public class SysLockProvider {

	@Autowired
	private SysLockDao sysLockDao;

	private static final String[] LOCK_TYPES = { SysLockType.PASSWORD.name(), SysLockType.QA.name() };

	@Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
	@CacheEvict(value = "lockCache", key = "'lock-'+#id")
	public void removeLock(String id) throws LogicException {
		sysLockDao.delete(id);
	}

	@Transactional(readOnly = true)
	@Cacheable(value = "lockCache", key = "'lock-'+#id", unless = "#result == null")
	public SysLock findLock(String id) {
		return sysLockDao.selectById(id);
	}

	@Transactional(readOnly = true)
	public List<SysLock> allLock() {
		return sysLockDao.selectAll();
	}

	@Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
	public void addLock(SysLock lock) throws LogicException {
		lock.setId(UUIDs.uuid());
		lock.setCreateDate(Timestamp.valueOf(LocalDateTime.now()));
		encryptPasswordLock(lock);
		sysLockDao.insert(lock);
	}

	@Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
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
	 * @return
	 */
	public String[] getLockTypes() {
		return LOCK_TYPES;
	}

	/**
	 * 检查目标锁类型是否存在
	 * 
	 * @param lockType
	 * @return
	 */
	public boolean checkLockTypeExists(String lockType) {
		for (String _lockType : LOCK_TYPES)
			if (_lockType.equals(lockType))
				return true;
		return false;
	}

	public Resource getDefaultTemplateResource(String lockType) {
		return new ClassPathResource("me/qyh/blog/ui/page/LOCK_" + lockType + ".html");
	}

	private void encryptPasswordLock(SysLock lock) {
		if (SysLockType.PASSWORD.equals(lock.getType())) {
			PasswordLock plock = (PasswordLock) lock;
			plock.setPassword(BCrypts.encode(plock.getPassword()));
		}
	}
}
