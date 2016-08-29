package me.qyh.blog.service.impl;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import me.qyh.blog.dao.SpaceDao;
import me.qyh.blog.entity.Space;
import me.qyh.blog.entity.Space.SpaceStatus;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.lock.Lock;
import me.qyh.blog.lock.LockManager;
import me.qyh.blog.lock.LockProtected;
import me.qyh.blog.message.Message;
import me.qyh.blog.pageparam.SpaceQueryParam;
import me.qyh.blog.service.SpaceService;

@Service
@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
public class SpaceServiceImpl implements SpaceService {

	@Autowired
	private SpaceDao spaceDao;
	@Autowired
	private LockManager<?> lockManager;

	@Override
	public void addSpace(Space space) throws LogicException {
		checkLock(space.getLockId());

		if (spaceDao.selectByAlias(space.getAlias()) != null) {
			throw new LogicException(
					new Message("space.alias.exists", "别名为" + space.getAlias() + "的空间已经存在了", space.getAlias()));
		}
		if (space.getIsDefault()) {
			spaceDao.resetDefault();
		}
		space.setCreateDate(new Date());
		spaceDao.insert(space);
	}

	@Override
	@CacheEvict(value = "userCache", key = "'space-'+#space.alias")
	public void updateSpace(Space space) throws LogicException {
		Space db = spaceDao.selectById(space.getId());
		if (db == null) {
			throw new LogicException(new Message("space.notExists", "空间不存在"));
		}
		Space aliasDb = spaceDao.selectByAlias(space.getAlias());
		if (aliasDb != null && !aliasDb.equals(db)) {
			throw new LogicException(
					new Message("space.alias.exists", "别名为" + space.getAlias() + "的空间已经存在了", space.getAlias()));
		}

		checkLock(space.getLockId());

		if (space.getIsDefault()) {
			// 如果空间被禁用，不能设置为默认
			if (space.getStatus() == null && db.getStatus().equals(SpaceStatus.DISABLE)
					|| space.getStatus().equals(SpaceStatus.DISABLE)) {
				throw new LogicException(new Message("space.disabled.noDefault", "被禁用的空间不能被设置为默认空间"));
			}
			spaceDao.resetDefault();
		}
		spaceDao.update(space);
	}

	@Override
	@Cacheable(value = "userCache", key = "'space-'+#alias")
	@LockProtected
	@Transactional(readOnly = true)
	public Space selectSpaceByAlias(String alias) {
		Space space = spaceDao.selectByAlias(alias);
		if (space != null) {
			switch (space.getStatus()) {
			case DISABLE:
				return null;
			default:
				return space;
			}
		}
		return null;
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
				throw new LogicException(new Message("lock.notexists", "锁不存在"));
			}
		}
	}
}
