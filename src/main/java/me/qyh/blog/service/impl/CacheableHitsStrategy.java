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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import me.qyh.blog.dao.ArticleDao;
import me.qyh.blog.entity.Article;
import me.qyh.blog.evt.listener.ArticleEventHandlerRegister;
import me.qyh.blog.evt.listener.EventHandlerAdapter;
import me.qyh.blog.exception.SystemException;
import me.qyh.blog.security.Environment;
import me.qyh.blog.service.impl.ArticleServiceImpl.HitsStrategy;

/**
 * 将点击数缓存起来，每隔一定的时间刷入数据库
 * <p>
 * <b>由于缓存原因，根据点击量查询无法实时的反应当前结果</b>
 * </p>
 * 
 * @author Administrator
 *
 */
public final class CacheableHitsStrategy
		implements HitsStrategy, InitializingBean, ApplicationListener<ContextClosedEvent> {

	@Autowired
	private ArticleDao articleDao;
	@Autowired
	private ArticleCache articleCache;
	@Autowired
	private PlatformTransactionManager transactionManager;
	@Autowired
	private ArticleIndexer articleIndexer;
	@Autowired
	private ArticleEventHandlerRegister articleEventHandlerRegister;
	@Autowired
	private ThreadPoolTaskExecutor threadPoolTaskExecutor;

	/**
	 * 存储所有文章的点击数
	 */
	private final Map<Integer, HitsHandler> hitsMap = new ConcurrentHashMap<>();

	/**
	 * 储存待刷新点击数的文章
	 */
	private final Map<Integer, Boolean> flushMap = new ConcurrentHashMap<>();

	/**
	 * 如果该项为true，那么在flush之前，相同的ip点击只算一次点击量
	 * <p>
	 * 例如我点击一次增加了一次点击量，一分钟后flush，那么我在这一分钟内(ip的不变的情况下)，无论我点击了多少次，都只算一次
	 * </p>
	 */
	private boolean validIp = false;

	/**
	 * 最多保存的文章数，如果达到或超过该数目，将会立即更新
	 */
	private int maxIps = 100;

	/**
	 * 做多保存文章数，如果达到或超过该数目，将会立即刷新
	 * <p>
	 * <b>因为没有批量更新的功能，所以这个值应该设置的较小</b>
	 * </p>
	 */
	private int maxArticles = 10;

	private final Object lock = new Object();

	@Override
	public void hit(Article article) {
		// increase
		hitsMap.computeIfAbsent(article.getId(), k -> {
			return (validIp && maxArticles > 1) ? new IPBasedHitsHandler(article.getHits(), maxIps)
					: new DefaultHitsHandler(article.getHits());
		}).hit(article);

		flushMap.putIfAbsent(article.getId(), Boolean.TRUE);

		/**
		 * 并不会在flushMap.size()刚刚超过maxArticles的执行，也有可能会在flushMap.size()远远大于maxArticles的时候执行
		 * 
		 * 1.8 ConcurrentHashMap的size方法性能跟HashMap差不多
		 * https://stackoverflow.com/questions/10754675/concurrent-hashmap-size-method-complexity/22996395#22996395
		 */
		if (flushMap.size() >= maxArticles) {
			threadPoolTaskExecutor.submit(() -> {
				synchronized (lock) {
					if (flushMap.size() >= maxArticles) {
						flush();
					}
				}
			});
		}
	}

	private synchronized void flush(Integer id) {
		List<HitsWrapper> wrappers = new ArrayList<>();
		flushMap.compute(id, (ck, cv) -> {
			if (cv != null) {
				wrappers.add(new HitsWrapper(id, hitsMap.get(id).getHits()));
			}
			return null;
		});
		doFlush(wrappers);
	}

	public synchronized void flush() {
		if (!flushMap.isEmpty()) {
			List<HitsWrapper> wrappers = new ArrayList<>();
			for (Iterator<Entry<Integer, Boolean>> iter = flushMap.entrySet().iterator(); iter.hasNext();) {
				Entry<Integer, Boolean> entry = iter.next();
				Integer key = entry.getKey();
				flushMap.compute(key, (ck, cv) -> {
					if (cv != null) {
						wrappers.add(new HitsWrapper(key, hitsMap.get(key).getHits()));
					}
					return null;
				});
			}
			doFlush(wrappers);
		}
	}

	private void doFlush(List<HitsWrapper> wrappers) {
		if (!wrappers.isEmpty()) {
			TransactionStatus ts = transactionManager.getTransaction(new DefaultTransactionDefinition());
			try {
				for (HitsWrapper wrapper : wrappers) {
					articleDao.updateHits(wrapper.id, wrapper.hits);
				}
			} catch (RuntimeException | Error e) {
				ts.setRollbackOnly();
				throw e;
			} finally {
				transactionManager.commit(ts);
			}
			Integer[] ids = wrappers.stream().map(wrapper -> wrapper.id).toArray(i -> new Integer[i]);
			articleCache.evit(ids);
			articleIndexer.addOrUpdateDocument(ids);
		}
	}

	private final class HitsWrapper {
		private final Integer id;
		private final Integer hits;

		public HitsWrapper(Integer id, Integer hits) {
			super();
			this.id = id;
			this.hits = hits;
		}
	}

	private interface HitsHandler {
		void hit(Article article);

		int getHits();
	}

	private final class DefaultHitsHandler implements HitsHandler {

		private final LongAdder adder;

		private DefaultHitsHandler(int init) {
			adder = new LongAdder();
			adder.add(init);
		}

		@Override
		public void hit(Article article) {
			adder.increment();
		}

		@Override
		public int getHits() {
			return adder.intValue();
		}
	}

	private final class IPBasedHitsHandler implements HitsHandler {
		private final Map<String, Boolean> ips = new ConcurrentHashMap<String, Boolean>();
		private final LongAdder adder;
		private final int maxIps;

		private IPBasedHitsHandler(int init, int maxIps) {
			adder = new LongAdder();
			adder.add(init);
			this.maxIps = maxIps;
		}

		@Override
		public void hit(Article article) {
			Environment.getIP().ifPresent(ip -> {
				if (ips.putIfAbsent(ip, Boolean.TRUE) == null) {
					adder.increment();
				}
			});

			if (ips.size() >= maxIps) {
				threadPoolTaskExecutor.submit(() -> {
					synchronized (lock) {
						if (ips.size() >= maxIps) {
							flush(article.getId());
						}
					}
				});

			}
		}

		@Override
		public int getHits() {
			return adder.intValue();
		}
	}

	@Override
	public void onApplicationEvent(ContextClosedEvent event) {
		flush();
	}

	@Override
	public void afterPropertiesSet() throws Exception {

		articleEventHandlerRegister.registerTransactionalEventHandler(new EventHandlerAdapter<List<Article>>() {

			@Override
			public void handleDelete(List<Article> articles) {
				articles.stream().map(Article::getId).forEach(id -> {
					flushMap.remove(id);
					hitsMap.remove(id);
				});
			}

		});
	}

	@Override
	public int getCurrentHits(Article article) {
		return article.getHits();
	}

	public void setValidIp(boolean validIp) {
		this.validIp = validIp;
	}

	public void setMaxIps(int maxIps) {
		if (maxIps <= 0) {
			throw new SystemException("每篇文章允许最多允许保存的ip数应该大于0");
		}
		this.maxIps = maxIps;
	}

	public void setMaxArticles(int maxArticles) {
		this.maxArticles = maxArticles;
	}
}