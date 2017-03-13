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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.locks.StampedLock;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.event.TransactionalEventListener;

import me.qyh.blog.entity.Article;
import me.qyh.blog.evt.ArticleEvent;
import me.qyh.blog.exception.SystemException;
import me.qyh.blog.service.impl.ArticleServiceImpl.ArticleViewedLogger;

/**
 * 将最近访问的文章纪录在内存中
 * <p>
 * <b>可能文章状态可能会变更，所以实际返回的数量可能小于<i>max</i></b>
 * </p>
 * <p>
 * <b>重启应用会导致数据丢失</b>
 * </p>
 * 
 * @author Administrator
 *
 */
public class SyncArticleViewdLogger implements ArticleViewedLogger {

	private final int max;
	private final Map<Integer, Boolean> articles;
	private StampedLock lock = new StampedLock();

	@Autowired
	private ArticleCache articleCache;

	private long timestamp = Long.MAX_VALUE;

	public SyncArticleViewdLogger(int max) {
		if (max < 0) {
			throw new SystemException("max必须大于0");
		}
		this.max = max;

		articles = new LinkedHashMap<Integer, Boolean>() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			protected boolean removeEldestEntry(Entry<Integer, Boolean> eldest) {
				return size() > max;
			}

		};
	}

	public SyncArticleViewdLogger() {
		this(10);
	}

	@Override
	public List<Article> getViewdArticles(int num) {
		long stamp = lock.tryOptimisticRead();
		List<Article> result = getCurrentViewed(num);
		if (!lock.validate(stamp)) {
			stamp = lock.readLock();
			try {
				result = getCurrentViewed(num);
			} finally {
				lock.unlockRead(stamp);
			}
		}
		return result;
	}

	private List<Article> getCurrentViewed(int num) {
		return articles.keySet().stream().sorted(Collections.reverseOrder()).limit(Math.min(num, max))
				.map(articleCache::getArticle).filter(Objects::nonNull).collect(Collectors.toList());
	}

	@Override
	public void logViewd(Article article) {
		long stamp = lock.writeLock();
		try {
			if (timestamp <= System.currentTimeMillis()) {
				return;
			}
			articles.remove(article.getId());
			articles.put(article.getId(), Boolean.TRUE);
		} finally {
			lock.unlockWrite(stamp);
		}
	}

	@TransactionalEventListener
	public void handleArticleEvent(ArticleEvent evt) {
		long stamp = lock.writeLock();
		// 设置当前时间
		timestamp = System.currentTimeMillis();
		try {
			switch (evt.getEventType()) {
			case DELETE:
				evt.getArticles().forEach(art -> articles.remove(art.getId()));
				break;
			case UPDATE:
				for (Article art : evt.getArticles()) {
					boolean valid = art.isPublished() && !art.isPrivate();
					if (!valid) {
						articles.remove(art.getId());
					}
				}
				break;
			default:
				break;
			}
		} finally {
			timestamp = Long.MAX_VALUE;
			lock.unlockWrite(stamp);
		}
	}
}
