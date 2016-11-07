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

import me.qyh.blog.dao.CommentConfigDao;
import me.qyh.blog.dao.SpaceDao;
import me.qyh.blog.entity.CommentConfig;
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
	private LockManager lockManager;
	@Autowired
	private CommentConfigDao commentConfigDao;

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
		space.setCommentConfig(null);
		if (space.getIsDefault())
			spaceDao.resetDefault();
		spaceDao.insert(space);
	}

	@Override
	@ArticleIndexRebuild
	@Caching(evict = { @CacheEvict(value = "userCache", key = "'space-'+#result.alias") })
	public Space updateCommentConfig(Integer spaceId, CommentConfig newConfig) throws LogicException {
		Space db = spaceDao.selectById(spaceId);
		if (db == null)
			throw new LogicException("space.notExists", "空间不存在");
		CommentConfig oldConfig = db.getCommentConfig();
		if (oldConfig != null) {
			newConfig.setId(oldConfig.getId());
			commentConfigDao.update(newConfig);
		} else {
			commentConfigDao.insert(newConfig);
		}
		db.setCommentConfig(newConfig);
		spaceDao.update(db);
		return db;
	}

	@Override
	@ArticleIndexRebuild
	@Caching(evict = { @CacheEvict(value = "userCache", key = "'space-'+#result.alias") })
	public Space deleteCommentConfig(Integer spaceId) throws LogicException {
		Space db = spaceDao.selectById(spaceId);
		if (db == null)
			throw new LogicException("space.notExists", "空间不存在");
		CommentConfig oldConfig = db.getCommentConfig();
		if (oldConfig != null) {
			db.setCommentConfig(null);
			spaceDao.update(db);
			commentConfigDao.deleteById(oldConfig.getId());
		}
		return db;
	}

	@Override
	@ArticleIndexRebuild
	@Caching(evict = { @CacheEvict(value = "userCache", key = "'space-'+#space.alias"),
			@CacheEvict(value = "articleCache", key = "'space-'+#space.alias"),
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

		checkLock(space.getLockId());

		if (space.getIsDefault())
			spaceDao.resetDefault();

		space.setCommentConfig(db.getCommentConfig());
		spaceDao.update(space);
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
	public Space getSpace(Integer id) {
		return spaceDao.selectById(id);
	}

	@Override
	@Transactional(readOnly = true)
	public Space selectSpaceByName(String name) {
		Space space = spaceDao.selectByName(name);
		if (space != null) {
			if (space.getIsPrivate() && UserContext.get() == null) {
				throw new AuthencationException();
			}
		}
		return space;
	}

	@Override
	@Cacheable(value = "userCache", key = "'space-'+#alias", unless = "#result == null || #result.isPrivate")
	@Transactional(readOnly = true)
	public Space selectSpaceByAliasWithoutLockProtected(String alias) {
		return selectSpaceByAlias(alias);
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
