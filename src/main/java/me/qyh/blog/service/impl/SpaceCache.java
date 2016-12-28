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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;

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

	private final LoadingCache<String, Space> aliasCache = CacheBuilder.newBuilder()
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

	private final LoadingCache<SpacesCacheKey, List<Space>> spacesCache = CacheBuilder.newBuilder()
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

	private final LoadingCache<Integer, Space> idCache = CacheBuilder.newBuilder()
			.build(new CacheLoader<Integer, Space>() {

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
		return spacesCache.getUnchecked(key);
	}

	public Space getSpace(String alias) {
		try {
			return aliasCache.getUnchecked(alias);
		} catch (UncheckedExecutionException e) {
			if (e.getCause() instanceof LogicException) {
				return null;
			}
			throw new SystemException(e.getMessage(), e);
		}
	}

	public Space getSpace(Integer id) {
		try {
			return idCache.getUnchecked(id);
		} catch (UncheckedExecutionException e) {
			if (e.getCause() instanceof LogicException) {
				return null;
			}
			throw new SystemException(e.getMessage(), e);
		}
	}

	public void evit(Space db) {
		idCache.invalidate(db.getId());
		aliasCache.invalidate(db.getAlias());
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
