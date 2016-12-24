package me.qyh.blog.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;

import me.qyh.blog.dao.SpaceDao;
import me.qyh.blog.entity.Space;
import me.qyh.blog.exception.RuntimeLogicException;
import me.qyh.blog.exception.SystemException;
import me.qyh.blog.message.Message;

@Component
public class SpaceCache {
	@Autowired
	private SpaceDao spaceDao;
	@Autowired
	private TransactionTemplate readOnlyTransactionTemplate;

	private final LoadingCache<String, Space> aliasCache = CacheBuilder.newBuilder()
			.build(new CacheLoader<String, Space>() {

				@Override
				public Space load(String key) throws Exception {
					return readOnlyTransactionTemplate.execute(new TransactionCallback<Space>() {

						@Override
						public Space doInTransaction(TransactionStatus status) {
							Space space = spaceDao.selectByAlias(key);
							if (space == null) {
								throw new RuntimeLogicException(new Message("space.notExists", "空间不存在"));
							}
							return space;
						}

					});
				}

			});

	private final LoadingCache<Integer, Space> idCache = CacheBuilder.newBuilder()
			.build(new CacheLoader<Integer, Space>() {

				@Override
				public Space load(Integer key) throws Exception {
					return readOnlyTransactionTemplate.execute(new TransactionCallback<Space>() {

						@Override
						public Space doInTransaction(TransactionStatus status) {
							Space space = spaceDao.selectById(key);
							if (space == null) {
								throw new RuntimeLogicException(new Message("space.notExists", "空间不存在"));
							}
							return space;
						}

					});
				}

			});

	public Space getSpace(String alias) {
		try {
			return aliasCache.getUnchecked(alias);
		} catch (UncheckedExecutionException e) {
			if (e.getCause() instanceof RuntimeLogicException) {
				return null;
			}
			throw new SystemException(e.getMessage(), e);
		}
	}

	public Space getSpace(Integer id) {
		try {
			return idCache.getUnchecked(id);
		} catch (UncheckedExecutionException e) {
			if (e.getCause() instanceof RuntimeLogicException) {
				return null;
			}
			throw new SystemException(e.getMessage(), e);
		}
	}

	public void evit(Space db) {
		idCache.invalidate(db.getId());
		aliasCache.invalidate(db.getAlias());
	}

}
