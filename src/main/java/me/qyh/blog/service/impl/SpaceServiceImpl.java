package me.qyh.blog.service.impl;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import me.qyh.blog.dao.SpaceDao;
import me.qyh.blog.entity.Article;
import me.qyh.blog.entity.Space;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.lock.Lock;
import me.qyh.blog.lock.LockManager;
import me.qyh.blog.lock.LockProtected;
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
	private ArticleQuery articleQuery;
	@Autowired
	private ArticleIndexer articleIndexer;
	@Autowired
	private LockManager<?> lockManager;

	@Override
	public void addSpace(Space space) throws LogicException {
		checkLock(space.getLockId());

		if (spaceDao.selectByAlias(space.getAlias()) != null) {
			throw new LogicException(
					new Message("space.alias.exists", "别名为" + space.getAlias() + "的空间已经存在了", space.getAlias()));
		}
		space.setCreateDate(Timestamp.valueOf(LocalDateTime.now()));
		spaceDao.insert(space);
	}

	@Override
	@Caching(evict = { @CacheEvict(value = "userCache", key = "'space-'+#space.alias"),
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

		checkLock(space.getLockId());

		spaceDao.update(space);

		// 如果空间改变克私有性或者增加删除了锁，那么需要重建该空间下所有文章的索引
		if (!db.getIsPrivate().equals(space.getIsPrivate())
				|| ((db.hasLock() && !space.hasLock()) || (!db.hasLock() && space.hasLock()))) {
			for (Article article : articleQuery.selectPublished(db)) {
				articleIndexer.addOrUpdateDocument(article);
			}
		}
	}

	@Override
	@Cacheable(value = "userCache", key = "'space-'+#alias", unless = "#result == null || #result.isPrivate")
	@LockProtected
	@Transactional(readOnly = true)
	public Space selectSpaceByAlias(String alias) {
		Space space = spaceDao.selectByAlias(alias);
		if (space != null) {
			if (space.getIsPrivate() && UserContext.get() == null) {
				throw new AuthencationException();
			}
		}
		return space;
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
