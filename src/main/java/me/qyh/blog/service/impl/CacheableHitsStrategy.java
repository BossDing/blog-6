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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import me.qyh.blog.dao.ArticleDao;
import me.qyh.blog.entity.Article;
import me.qyh.blog.evt.ArticleEvent;
import me.qyh.blog.evt.ArticleIndexRebuildEvent;
import me.qyh.blog.evt.EventType;
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
		implements HitsStrategy, ApplicationEventPublisherAware, InitializingBean, ApplicationListener<ArticleEvent> {

	@Autowired
	private ThreadPoolTaskScheduler taskScheduler;
	@Autowired
	private ArticleDao articleDao;
	@Autowired
	private ArticleCache articleCache;
	@Autowired
	private PlatformTransactionManager transactionManager;
	@Autowired
	private NRTArticleIndexer articleIndexer;
	private ApplicationEventPublisher applicationEventPublisher;

	private static final Logger LOGGER = LoggerFactory.getLogger(CacheableHitsStrategy.class);

	private final Map<Integer, HitsHandler> hitsMap = new ConcurrentHashMap<>();

	private final int flushSeconds;

	/**
	 * 如果该项为true，那么在flush之前，相同的ip点击只算一次点击量
	 * <p>
	 * 例如我点击一次增加了一次点击量，一分钟后flush，那么我在这一分钟内(ip的不变的情况下)，无论我点击了多少次，都只算一次
	 * </p>
	 */
	private boolean validIp = true;

	/**
	 * 每篇文章允许最多允许保存的ip数，如果ip超过这个数量，文章点击量将被立即更新
	 * 
	 * @see IPBasedHitsHandler
	 */
	private int maxIps = 100;

	/**
	 * 最多保存的文章数，如果超过该数目，将会立即更新
	 * <p>
	 * <b>因为没有批量更新的功能，所以这个值应该设置的较小</b>
	 * </p>
	 */
	private int maxArticles = 10;

	public CacheableHitsStrategy(int flushSeconds) {
		if (flushSeconds < 1) {
			throw new SystemException("刷新时间不能小于1秒");
		}

		this.flushSeconds = flushSeconds;
	}

	public CacheableHitsStrategy() {
		this(600);
	}

	@Override
	public void hit(Article article) {
		// increase
		hitsMap.computeIfAbsent(article.getId(), k -> {
			return validIp ? new IPBasedHitsHandler(article.getHits(), maxIps)
					: new DefaultHitsHandler(article.getHits());
		}).hit(article);

		/**
		 * 并不会在hitsMap.size()刚刚超过maxArticles的执行，也有可能会在hitsMap.size()远远大于maxArticles的时候执行
		 * 
		 * 1.8 ConcurrentHashMap的size方法性能跟HashMap差不多
		 * https://stackoverflow.com/questions/10754675/concurrent-hashmap-size-method-complexity/22996395#22996395
		 */
		if (hitsMap.size() > maxArticles) {
			flush();
		}
	}

	private void flush(Integer id) {
		List<HitsWrapper> wrappers = new ArrayList<>();
		hitsMap.compute(id, (ck, cv) -> {
			if (cv != null) {
				wrappers.add(new HitsWrapper(id, cv.getHits()));
			}
			return null;
		});
		doFlush(wrappers);
	}

	private void flush() {
		if (!hitsMap.isEmpty()) {
			List<HitsWrapper> wrappers = new ArrayList<>();
			// not atomic ???
			for (Iterator<Entry<Integer, HitsHandler>> iter = hitsMap.entrySet().iterator(); iter.hasNext();) {
				Entry<Integer, HitsHandler> entry = iter.next();
				Integer key = entry.getKey();
				hitsMap.compute(key, (ck, cv) -> {
					if (cv != null) {
						wrappers.add(new HitsWrapper(key, cv.getHits()));
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
					Article art = articleCache.getArticle(wrapper.id);
					if (art != null) {
						art.setHits(wrapper.hits);
						articleIndexer.addOrUpdateDocument(art);
					}
				}
			} catch (RuntimeException | Error e) {
				ts.setRollbackOnly();
				LOGGER.error(e.getMessage(), e);
				applicationEventPublisher.publishEvent(new ArticleIndexRebuildEvent(this));
			} finally {
				transactionManager.commit(ts);
			}
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

			if (ips.size() > maxIps) {
				flush(article.getId());
			}
		}

		@Override
		public int getHits() {
			return adder.intValue();
		}
	}

	/**
	 * destroy method
	 */
	public void close() {
		flush();
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.taskScheduler.scheduleAtFixedRate(this::flush, flushSeconds * 1000);
	}

	// inner bean EventListener not work ???
	@Override
	public void onApplicationEvent(ArticleEvent event) {
		if (EventType.DELETE.equals(event.getEventType())) {
			event.getArticles().stream().map(Article::getId).forEach(hitsMap::remove);
		}
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
		if (maxArticles <= 0) {
			throw new SystemException("最多保存的文章数应该大于0");
		}
		this.maxArticles = maxArticles;
	}
}