package me.qyh.blog.service.impl;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import me.qyh.blog.dao.ArticleDao;
import me.qyh.blog.entity.Article;
import me.qyh.blog.evt.ArticleEvent;
import me.qyh.blog.evt.ArticleIndexRebuildEvent;
import me.qyh.blog.evt.EventType;
import me.qyh.blog.exception.SystemException;
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

	private final Map<Integer, LongAdder> hitsMap = Maps.newConcurrentMap();

	private final int flushSeconds;

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
			LongAdder adder = new LongAdder();
			adder.add(article.getHits());
			return adder;
		}).increment();
	}

	// not atomic ???
	private void flush() {
		if (!hitsMap.isEmpty()) {
			List<HitsWrapper> wrappers = Lists.newArrayList();
			for (Iterator<Entry<Integer, LongAdder>> iter = hitsMap.entrySet().iterator(); iter.hasNext();) {
				Entry<Integer, LongAdder> entry = iter.next();
				Integer key = entry.getKey();
				hitsMap.compute(key, (ck, cv) -> {
					if (cv != null) {
						wrappers.add(new HitsWrapper(key, cv.intValue()));
					}
					return null;
				});
			}
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
		this.taskScheduler.scheduleAtFixedRate(() -> flush(), flushSeconds * 1000);
	}

	// inner bean EventListener not work ???
	@Override
	public void onApplicationEvent(ArticleEvent event) {
		if (EventType.DELETE.equals(event.getEventType())) {
			event.getArticles().stream().map(Article::getId).forEach(hitsMap::remove);
		}
	}
}