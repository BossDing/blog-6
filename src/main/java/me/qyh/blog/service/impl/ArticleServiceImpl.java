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

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.util.CollectionUtils;

import me.qyh.blog.api.metaweblog.MetaweblogArticle;
import me.qyh.blog.bean.ArticleDateFile;
import me.qyh.blog.bean.ArticleDateFiles;
import me.qyh.blog.bean.ArticleDateFiles.ArticleDateFileMode;
import me.qyh.blog.bean.ArticleNav;
import me.qyh.blog.bean.ArticleSpaceFile;
import me.qyh.blog.bean.TagCount;
import me.qyh.blog.config.GlobalConfig;
import me.qyh.blog.dao.ArticleDao;
import me.qyh.blog.dao.ArticleTagDao;
import me.qyh.blog.dao.SpaceDao;
import me.qyh.blog.dao.TagDao;
import me.qyh.blog.entity.Article;
import me.qyh.blog.entity.Article.ArticleStatus;
import me.qyh.blog.entity.ArticleTag;
import me.qyh.blog.entity.Space;
import me.qyh.blog.entity.Tag;
import me.qyh.blog.evt.ArticleEvent;
import me.qyh.blog.evt.ArticleIndexRebuildEvent;
import me.qyh.blog.evt.EventType;
import me.qyh.blog.evt.LockDeleteEvent;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.lock.LockManager;
import me.qyh.blog.pageparam.ArticleQueryParam;
import me.qyh.blog.pageparam.PageResult;
import me.qyh.blog.security.Environment;
import me.qyh.blog.service.ArticleService;
import me.qyh.blog.service.CommentServer;
import me.qyh.blog.service.ConfigService;
import me.qyh.blog.util.Times;

public class ArticleServiceImpl implements ArticleService, InitializingBean, ApplicationEventPublisherAware {

	private static final Logger LOGGER = LoggerFactory.getLogger(ArticleServiceImpl.class);
	@Autowired
	private ArticleDao articleDao;
	@Autowired
	private SpaceDao spaceDao;
	@Autowired
	private SpaceCache spaceCache;
	@Autowired
	private ArticleTagDao articleTagDao;
	@Autowired
	private TagDao tagDao;
	@Autowired
	private LockManager lockManager;
	@Autowired
	private ArticleCache articleCache;
	@Autowired
	private ConfigService configService;
	@Autowired
	private CommentServer articleCommentStatisticsService;
	@Autowired
	private PlatformTransactionManager transactionManager;
	@Autowired
	private ArticleIndexer articleIndexer;

	private ApplicationEventPublisher applicationEventPublisher;

	private boolean rebuildIndex = true;

	@Autowired(required = false)
	private ArticleContentHandler articleContentHandler;
	private final ScheduleManager scheduleManager = new ScheduleManager();

	/**
	 * 点击策略
	 */
	private HitsStrategy hitsStrategy;

	private ArticleHitManager articleHitManager;

	@Override
	@Transactional(readOnly = true)
	public Optional<Article> getArticleForView(String idOrAlias) {
		Optional<Article> optionalArticle = getCheckedArticle(idOrAlias);
		if (optionalArticle.isPresent()) {

			Article clone = new Article(optionalArticle.get());
			clone.setComments(articleCommentStatisticsService.queryArticleCommentCount(clone.getId()).orElse(0));
			clone.setHits(articleHitManager.getCurrentHits(clone));

			if (articleContentHandler != null) {
				articleContentHandler.handle(clone);
			}

			return Optional.of(clone);
		}
		return Optional.empty();
	}

	@Override
	@Transactional(readOnly = true)
	public Article getArticleForEdit(Integer id) throws LogicException {
		Article article = articleDao.selectById(id);
		if (article == null || article.isDeleted()) {
			throw new LogicException("article.notExists", "文章不存在");
		}
		return article;
	}

	@Override
	public void hit(Integer id) {
		articleHitManager.hit(id);
	}

	@Override
	@ArticleIndexRebuild
	@Caching(evict = { @CacheEvict(value = "articleFilesCache", allEntries = true),
			@CacheEvict(value = "hotTags", allEntries = true) })
	@Sync
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public Article writeArticle(MetaweblogArticle mba) throws LogicException {
		Space space = mba.getSpace() == null ? spaceDao.selectDefault() : spaceDao.selectByName(mba.getSpace());
		if (space == null) {
			throw new LogicException("space.notExists", "空间不存在");
		}
		Article article;
		if (mba.hasId()) {
			Article articleDb = articleDao.selectById(mba.getId());
			if (articleDb != null) {
				article = new Article(articleDb);
				mba.mergeArticle(article);
			} else {
				article = mba.toArticle();
				article.setId(mba.getId());
			}
		} else {
			article = mba.toArticle();
		}
		article.setSpace(space);
		return writeArticle(article);
	}

	@Override
	@ArticleIndexRebuild
	@Caching(evict = { @CacheEvict(value = "articleFilesCache", allEntries = true),
			@CacheEvict(value = "hotTags", allEntries = true) })
	@Sync
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public Article writeArticle(Article article) throws LogicException {
		Space space = spaceCache.checkSpace(article.getSpace().getId());
		article.setSpace(space);
		// 如果文章是私有的，无法设置锁
		if (article.isPrivate()) {
			article.setLockId(null);
		} else {
			lockManager.ensureLockvailable(article.getLockId());
		}
		Timestamp now = Timestamp.valueOf(LocalDateTime.now());
		boolean update = article.hasId();
		if (update) {
			Article articleDb = articleDao.selectById(article.getId());
			if (articleDb == null) {
				throw new LogicException("article.notExists", "文章不存在");
			}
			if (articleDb.isDeleted()) {
				throw new LogicException("article.deleted", "文章已经被删除");
			}
			if (article.getAlias() != null) {
				Article aliasDb = articleDao.selectByAlias(article.getAlias());
				if (aliasDb != null && !aliasDb.equals(article)) {
					throw new LogicException("article.alias.exists", "别名" + article.getAlias() + "已经存在",
							article.getAlias());
				}
			}

			if (nochange(article, articleDb)) {
				return articleDb;
			}

			Timestamp pubDate = null;
			switch (article.getStatus()) {
			case DRAFT:
				pubDate = articleDb.isSchedule() ? null
						: articleDb.getPubDate() != null ? articleDb.getPubDate() : null;
				break;
			case PUBLISHED:
				pubDate = articleDb.isSchedule() ? now : articleDb.getPubDate() != null ? articleDb.getPubDate() : now;
				break;
			case SCHEDULED:
				pubDate = article.getPubDate();
				break;
			default:
				break;
			}

			article.setPubDate(pubDate);

			if (articleDb.getPubDate() != null && article.isPublished()) {
				article.setLastModifyDate(now);
			}

			articleTagDao.deleteByArticle(articleDb);

			articleDao.update(article);
			Transactions.afterCommit(() -> articleCache.evit(article.getId()));
		} else {
			if (article.getAlias() != null) {
				Article aliasDb = articleDao.selectByAlias(article.getAlias());
				if (aliasDb != null) {
					throw new LogicException("article.alias.exists", "别名" + article.getAlias() + "已经存在",
							article.getAlias());
				}
			}

			Timestamp pubDate = null;
			switch (article.getStatus()) {
			case DRAFT:
				// 如果是草稿
				pubDate = null;
				break;
			case PUBLISHED:
				pubDate = now;
				break;
			case SCHEDULED:
				pubDate = article.getPubDate();
				break;
			default:
				break;
			}
			article.setPubDate(pubDate);

			articleDao.insert(article);
		}

		boolean rebuildIndexWhenTagChange = insertTags(article);
		if (article.isSchedule()) {
			scheduleManager.update();
		}

		Transactions.afterCommit(() -> {
			if (rebuildIndexWhenTagChange) {
				applicationEventPublisher.publishEvent(new ArticleIndexRebuildEvent(this));
			} else {
				if (update) {
					articleIndexer.deleteDocument(article.getId());
				}
				if (article.isPublished()) {
					articleIndexer.addOrUpdateDocument(article.getId());
				}
			}
		});
		applicationEventPublisher
				.publishEvent(new ArticleEvent(this, article, update ? EventType.UPDATE : EventType.INSERT));
		return article;
	}

	@Override
	@ArticleIndexRebuild
	@Caching(evict = { @CacheEvict(value = "articleFilesCache", allEntries = true),
			@CacheEvict(value = "hotTags", allEntries = true) })
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void publishDraft(Integer id) throws LogicException {
		Article article = articleDao.selectById(id);
		if (article == null) {
			throw new LogicException("article.notExists", "文章不存在");
		}
		if (!article.isDraft()) {
			throw new LogicException("article.notDraft", "文章已经被删除");
		}
		Timestamp now = Timestamp.valueOf(LocalDateTime.now());
		article.setPubDate(article.isSchedule() ? now : article.getPubDate() != null ? article.getPubDate() : now);
		article.setStatus(ArticleStatus.PUBLISHED);
		articleDao.update(article);
		Transactions.afterCommit(() -> articleIndexer.addOrUpdateDocument(id));
		applicationEventPublisher.publishEvent(new ArticleEvent(this, article, EventType.UPDATE));
	}

	private boolean insertTags(Article article) {
		Set<Tag> tags = article.getTags();
		boolean rebuildIndexWhenTagChange = false;
		if (!CollectionUtils.isEmpty(tags)) {
			for (Tag tag : tags) {
				Tag tagDb = tagDao.selectByName(cleanTag(tag.getName()));
				ArticleTag articleTag = new ArticleTag();
				articleTag.setArticle(article);
				if (tagDb == null) {
					// 插入标签
					tag.setCreate(Timestamp.valueOf(LocalDateTime.now()));
					tag.setName(tag.getName().trim());
					tagDao.insert(tag);
					articleTag.setTag(tag);
					articleIndexer.addTags(tag.getName());
					rebuildIndexWhenTagChange = true;
				} else {
					articleTag.setTag(tagDb);
				}
				articleTagDao.insert(articleTag);
			}
		}
		return rebuildIndexWhenTagChange;
	}

	@Override
	@Transactional(readOnly = true)
	@Cacheable(value = "articleFilesCache", key = "'dateFiles-'+'space-'+(T(me.qyh.blog.security.Environment).getSpace().orElse(null))+'-mode-'+#mode.name()+'-private-'+(T(me.qyh.blog.security.Environment).getUser().isPresent())")
	public ArticleDateFiles queryArticleDateFiles(ArticleDateFileMode mode) throws LogicException {
		List<ArticleDateFile> files = articleDao.selectDateFiles(Environment.getSpace().orElse(null), mode,
				Environment.isLogin());
		ArticleDateFiles _files = new ArticleDateFiles(files, mode);
		_files.calDate();
		return _files;
	}

	@Override
	@Transactional(readOnly = true)
	@Cacheable(value = "articleFilesCache", key = "'spaceFiles-private-'+(T(me.qyh.blog.security.Environment).getUser().isPresent())")
	public List<ArticleSpaceFile> queryArticleSpaceFiles() {
		if (Environment.hasSpace()) {
			return Collections.emptyList();
		}
		return articleDao.selectSpaceFiles(Environment.isLogin());
	}

	@Override
	@Transactional(readOnly = true)
	public PageResult<Article> queryArticle(ArticleQueryParam param) {
		GlobalConfig globalConfig = configService.getGlobalConfig();
		if (param.getSpace() == null) {
			param.setPageSize(globalConfig.getArticlePageSize());
		} else {
			Optional<Space> space = spaceCache.getSpace(param.getSpace().getId());
			if (!space.isPresent()) {
				param.setPageSize(globalConfig.getArticlePageSize());
				return new PageResult<>(param, 0, Collections.emptyList());
			} else {
				param.setPageSize(space.get().getArticlePageSize());
			}
		}
		if (param.isQueryPrivate() && !Environment.isLogin()) {
			param.setQueryPrivate(false);
		}
		PageResult<Article> page;
		if (param.hasQuery()) {
			page = articleIndexer.query(param);
		} else {
			int count = articleDao.selectCount(param);
			List<Article> datas = articleDao.selectPage(param);
			page = new PageResult<>(param, count, datas);
		}
		// query comments
		List<Article> datas = page.getDatas();
		if (!CollectionUtils.isEmpty(datas)) {
			List<Integer> ids = new ArrayList<>(datas.size());
			for (Article article : datas) {
				ids.add(article.getId());
			}
			Map<Integer, Integer> countsMap = articleCommentStatisticsService.queryArticlesCommentCount(ids);
			for (Article article : datas) {
				Integer comments = countsMap.get(article.getId());
				article.setComments(comments == null ? 0 : comments);
			}
		}
		return page;
	}

	@Override
	@ArticleIndexRebuild
	@Caching(evict = { @CacheEvict(value = "articleFilesCache", allEntries = true),
			@CacheEvict(value = "hotTags", allEntries = true) })
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void logicDeleteArticle(Integer id) throws LogicException {
		Article article = articleDao.selectById(id);
		if (article == null) {
			throw new LogicException("article.notExists", "文章不存在");
		}
		if (article.isDeleted()) {
			throw new LogicException("article.deleted", "文章已经被删除");
		}
		article.setStatus(ArticleStatus.DELETED);
		articleDao.update(article);
		articleIndexer.deleteDocument(id);
		Transactions.afterCommit(() -> articleCache.evit(id));

		applicationEventPublisher.publishEvent(new ArticleEvent(this, article, EventType.UPDATE));
	}

	@Override
	@ArticleIndexRebuild
	@Caching(evict = { @CacheEvict(value = "articleFilesCache", allEntries = true),
			@CacheEvict(value = "hotTags", allEntries = true) })
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void recoverArticle(Integer id) throws LogicException {
		Article article = articleDao.selectById(id);
		if (article == null) {
			throw new LogicException("article.notExists", "文章不存在");
		}
		if (!article.isDeleted()) {
			throw new LogicException("article.undeleted", "文章未删除");
		}
		ArticleStatus status = ArticleStatus.PUBLISHED;
		if (article.getPubDate().after(Timestamp.valueOf(LocalDateTime.now()))) {
			status = ArticleStatus.SCHEDULED;
		}
		article.setStatus(status);
		articleDao.update(article);

		Transactions.afterCommit(() -> articleIndexer.addOrUpdateDocument(id));

		applicationEventPublisher.publishEvent(new ArticleEvent(this, article, EventType.UPDATE));
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void deleteArticle(Integer id) throws LogicException {
		Article article = articleDao.selectById(id);
		if (article == null) {
			throw new LogicException("article.notExists", "文章不存在");
		}
		if (!article.isDraft() && !article.isDeleted()) {
			throw new LogicException("article.undeleted", "文章未删除");
		}
		// 删除博客的引用
		articleTagDao.deleteByArticle(article);
		articleDao.deleteById(id);

		applicationEventPublisher.publishEvent(new ArticleEvent(this, article, EventType.DELETE));
	}

	@Override
	@Caching(evict = { @CacheEvict(value = "hotTags", allEntries = true, condition = "#result > 0"),
			@CacheEvict(value = "articleFilesCache", allEntries = true, condition = "#result > 0") })
	public int publishScheduled() {
		return scheduleManager.publish();
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<ArticleNav> getArticleNav(String idOrAlias) {
		Optional<Article> optionalArticle = getCheckedArticle(idOrAlias);
		if (optionalArticle.isPresent()) {
			Article article = optionalArticle.get();
			if (!Environment.match(article.getSpace())) {
				return Optional.empty();
			}
			boolean queryPrivate = Environment.isLogin();
			Article previous = articleDao.getPreviousArticle(article, queryPrivate);
			Article next = articleDao.getNextArticle(article, queryPrivate);
			return (previous != null || next != null) ? Optional.of(new ArticleNav(previous, next)) : Optional.empty();
		}
		return Optional.empty();
	}

	@Override
	@Transactional(readOnly = true)
	@Cacheable(value = "hotTags", key = "'hotTags-'+'space-'+(T(me.qyh.blog.security.Environment).getSpace().orElse(null))+'-private-'+(T(me.qyh.blog.security.Environment).getUser().isPresent())")
	public List<TagCount> queryTags() throws LogicException {
		return new ArrayList<>(articleTagDao.selectTags(Environment.getSpace().orElse(null), Environment.isLogin()));
	}

	@Override
	@Transactional(readOnly = true)
	public List<Article> queryRecentArticles(Integer limit) {
		return articleDao.selectRecentArticles(limit);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Article> findSimilar(String idOrAlias, int limit) throws LogicException {
		Optional<Article> optionalArticle = getCheckedArticle(idOrAlias);

		if (optionalArticle.isPresent()) {
			Article article = optionalArticle.get();
			if (!Environment.match(article.getSpace())) {
				return Collections.emptyList();
			}
			return articleIndexer.querySimilar(article, Environment.isLogin(), limit).stream()
					.collect(Collectors.toList());
		}

		return Collections.emptyList();
	}

	@Override
	public void preparePreview(Article article) {
		if (articleContentHandler != null) {
			articleContentHandler.handlePreview(article);
		}
	}

	@EventListener
	public void handleLockDeleteEvent(LockDeleteEvent event) {
		// synchronized
		// do not worry about transaction
		articleDao.deleteLock(event.getLockId());
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (rebuildIndex) {
			this.articleIndexer.rebuildIndex();
		}

		if (hitsStrategy == null) {
			hitsStrategy = new DefaultHitsStrategy();
		}

		this.articleHitManager = new ArticleHitManager(hitsStrategy);

		scheduleManager.update();
	}

	private Optional<Article> getCheckedArticle(String idOrAlias) {
		Article article = null;
		try {
			int id = Integer.parseInt(idOrAlias);
			article = articleCache.getArticle(id);
		} catch (NumberFormatException e) {
			article = articleCache.getArticle(idOrAlias);
		}
		if (article != null && article.isPublished() && Environment.match(article.getSpace())) {
			if (article.isPrivate()) {
				Environment.doAuthencation();
			}

			lockManager.openLock(article);
			return Optional.of(article);
		}

		return Optional.empty();
	}

	/**
	 * 查询标签是否存在的时候清除两边空格并且忽略大小写
	 * 
	 * @param tag
	 * @return
	 */
	protected String cleanTag(String tag) {
		return tag.trim().toLowerCase();
	}

	public void setRebuildIndex(boolean rebuildIndex) {
		this.rebuildIndex = rebuildIndex;
	}

	private final class ScheduleManager {
		private Timestamp start;

		public int publish() {
			if (start == null) {
				LOGGER.debug("没有待发布的文章");
				return 0;
			}
			long now = System.currentTimeMillis();
			if (now < start.getTime()) {
				LOGGER.debug("没有到发布日期：" + Times.format(start.toLocalDateTime(), "yyyy-MM-dd HH:mm:ss"));
				return 0;
			} else {
				LOGGER.debug("开始查询发布文章");
				List<Article> articles;
				Timestamp startCopy = new Timestamp(start.getTime());
				TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
				try {
					articles = articleDao.selectScheduled(new Timestamp(now));
					if (!articles.isEmpty()) {
						for (Article article : articles) {
							article.setStatus(ArticleStatus.PUBLISHED);
							articleDao.update(article);
						}
						LOGGER.debug("发布了" + articles.size() + "篇文章");
						applicationEventPublisher.publishEvent(new ArticleEvent(this, articles, EventType.UPDATE));
					}
					start = articleDao.selectMinimumScheduleDate();
				} catch (RuntimeException | Error e) {
					start = startCopy;
					status.setRollbackOnly();
					throw e;
				} finally {
					transactionManager.commit(status);
				}
				articleIndexer.addOrUpdateDocument(articles.stream().map(Article::getId).toArray(i -> new Integer[i]));
				return articles.size();
			}
		}

		public void update() {
			start = articleDao.selectMinimumScheduleDate();
			LOGGER.debug(start == null ? "没有发现待发布文章"
					: "发现待发布文章最小日期:" + Times.format(start.toLocalDateTime(), "yyyy-MM-dd HH:mm:ss"));
		}
	}

	private final class ArticleHitManager {

		private final HitsStrategy hitsStrategy;

		public ArticleHitManager(HitsStrategy hitsStrategy) {
			super();
			this.hitsStrategy = hitsStrategy;
		}

		void hit(Integer id) {
			TransactionStatus ts = transactionManager.getTransaction(new DefaultTransactionDefinition());
			try {
				Article article = articleCache.getArticle(id);
				if (article != null && validHit(article)) {
					hitsStrategy.hit(article);
				}
			} catch (RuntimeException | Error e) {
				ts.setRollbackOnly();
				throw e;
			} finally {
				transactionManager.commit(ts);
			}
		}

		int getCurrentHits(Article article) {
			return hitsStrategy.getCurrentHits(article);
		}

		private boolean validHit(Article article) {
			boolean hit = !Environment.isLogin() && article.isPublished() && Environment.match(article.getSpace())
					&& !article.getIsPrivate();

			if (hit) {
				lockManager.openLock(article);
			}
			return hit;
		}

	}

	public interface HitsStrategy {
		/**
		 * 点击文章
		 * <p>
		 * <b>这个方法的执行处于事务中</b>
		 * <p>
		 * 
		 * @param article
		 * @see DefaultHitsStrategy
		 */
		void hit(Article article);

		/**
		 * 获取文章当前点击数
		 * 
		 * @param article
		 * @return
		 */
		int getCurrentHits(Article article);
	}

	private final class DefaultHitsStrategy implements HitsStrategy {

		@Override
		public void hit(Article article) {
			synchronized (this) {
				Integer id = article.getId();
				articleDao.addHits(id, 1);
				Transactions.afterCommit(() -> articleIndexer.addOrUpdateDocument(id));
			}
		}

		@Override
		public int getCurrentHits(Article article) {
			return articleDao.selectHits(article.getId());
		}
	}

	/**
	 * 判断文章是否需要更新
	 * 
	 * @param newArticle
	 *            当前文章
	 * @param old
	 *            已经存在的文章
	 * @return
	 */
	protected boolean nochange(Article newArticle, Article old) {
		Objects.requireNonNull(newArticle);
		Objects.requireNonNull(old);
		return Objects.equals(newArticle.getAlias(), old.getAlias())
				&& Objects.equals(newArticle.getAllowComment(), old.getAllowComment())
				&& Objects.equals(newArticle.getContent(), old.getContent())
				&& Objects.equals(newArticle.getFrom(), old.getFrom())
				&& Objects.equals(newArticle.getIsPrivate(), old.getIsPrivate())
				&& Objects.equals(newArticle.getLevel(), old.getLevel())
				&& Objects.equals(newArticle.getLockId(), old.getLockId())
				&& Objects.equals(newArticle.getSpace(), old.getSpace())
				&& Objects.equals(newArticle.getSummary(), old.getSummary())
				&& Objects.equals(newArticle.getTagStr(), old.getTagStr())
				&& Objects.equals(newArticle.getTitle(), old.getTitle())
				&& Objects.equals(newArticle.getStatus(), old.getStatus());
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	public void setHitsStrategy(HitsStrategy hitsStrategy) {
		this.hitsStrategy = hitsStrategy;
	}
}
