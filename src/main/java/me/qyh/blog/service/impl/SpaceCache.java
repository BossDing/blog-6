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

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import me.qyh.blog.dao.SpaceDao;
import me.qyh.blog.entity.Space;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.exception.SystemException;
import me.qyh.blog.message.Message;
import me.qyh.blog.pageparam.SpaceQueryParam;
import me.qyh.blog.util.Validators;

@Component
public class SpaceCache {
	@Autowired
	private SpaceDao spaceDao;
	@Autowired
	private PlatformTransactionManager transactionManager;

	private final LoadingCache<String, Space> aliasCache = Caffeine.newBuilder()
			.build(new CacheLoader<String, Space>() {

				@Override
				public Space load(String key) throws Exception {
					DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
					definition.setReadOnly(true);
					TransactionStatus status = transactionManager.getTransaction(definition);
					try {
						Space space = spaceDao.selectByAlias(key);
						if (space == null) {
							throw new LogicException(new Message("space.notExists", "空间不存在"));
						}
						return space;
					} catch (RuntimeException | Error e) {
						status.setRollbackOnly();
						throw e;
					} finally {
						transactionManager.commit(status);
					}
				}

			});

	private final LoadingCache<SpacesCacheKey, List<Space>> spacesCache = Caffeine.newBuilder()
			.build(new CacheLoader<SpacesCacheKey, List<Space>>() {

				@Override
				public List<Space> load(SpacesCacheKey key) throws Exception {
					DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
					definition.setReadOnly(true);
					TransactionStatus status = transactionManager.getTransaction(definition);
					try {
						SpaceQueryParam param = new SpaceQueryParam();
						param.setQueryPrivate(key.queryPrivate);
						return spaceDao.selectByParam(param);
					} catch (RuntimeException | Error e) {
						status.setRollbackOnly();
						throw e;
					} finally {
						transactionManager.commit(status);
					}
				}

			});

	private final LoadingCache<Integer, Space> idCache = Caffeine.newBuilder().build(new CacheLoader<Integer, Space>() {

		@Override
		public Space load(Integer key) throws Exception {
			DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
			definition.setReadOnly(true);
			TransactionStatus status = transactionManager.getTransaction(definition);
			try {
				Space space = spaceDao.selectById(key);
				if (space == null) {
					throw new LogicException(new Message("space.notExists", "空间不存在"));
				}
				return space;
			} catch (RuntimeException | Error e) {
				status.setRollbackOnly();
				throw e;
			} finally {
				transactionManager.commit(status);
			}
		}

	});

	public List<Space> getSpaces(SpacesCacheKey key) {
		return spacesCache.get(key);
	}

	public Optional<Space> getSpace(String alias) {
		try {
			return Optional.of(aliasCache.get(alias));
		} catch (CompletionException e) {
			if (e.getCause() instanceof LogicException) {
				return Optional.empty();
			}
			throw new SystemException(e.getMessage(), e);
		}
	}

	public Optional<Space> getSpace(Integer id) {
		try {
			return Optional.of(idCache.get(id));
		} catch (CompletionException e) {
			if (e.getCause() instanceof LogicException) {
				return Optional.empty();
			}
			throw new SystemException(e.getMessage(), e);
		}
	}

	/**
	 * 判断空间是否存在
	 * 
	 * @param spaceId
	 *            空间ID
	 * @return
	 * @throws LogicException
	 */
	public Space checkSpace(Integer spaceId) throws LogicException {
		if (spaceId == null) {
			return null;
		}
		return getSpace(spaceId).orElseThrow(() -> new LogicException("space.notExists", "空间不存在"));
	}

	public void evit(Space db) {
		if (db.hasId()) {
			idCache.invalidate(db.getId());
		}
		if (db.getAlias() != null) {
			aliasCache.invalidate(db.getAlias());
		}
		spacesCache.invalidateAll();
	}

	public static final class SpacesCacheKey {
		private boolean queryPrivate;

		public SpacesCacheKey() {
			super();
		}

		public SpacesCacheKey(boolean queryPrivate) {
			super();
			this.queryPrivate = queryPrivate;
		}

		public boolean isQueryPrivate() {
			return queryPrivate;
		}

		public void setQueryPrivate(boolean queryPrivate) {
			this.queryPrivate = queryPrivate;
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.queryPrivate);
		}

		@Override
		public boolean equals(Object obj) {
			if (Validators.baseEquals(this, obj)) {
				SpacesCacheKey rhs = (SpacesCacheKey) obj;
				return Objects.equals(this.queryPrivate, rhs.queryPrivate);
			}
			return false;
		}
	}

}
