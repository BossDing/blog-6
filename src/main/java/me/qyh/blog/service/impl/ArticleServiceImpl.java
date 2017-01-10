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
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
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
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.util.CollectionUtils;

import com.google.common.collect.Lists;

import me.qyh.blog.api.metaweblog.MetaweblogArticle;
import me.qyh.blog.bean.ArticleDateFile;
import me.qyh.blog.bean.ArticleDateFiles;
import me.qyh.blog.bean.ArticleDateFiles.ArticleDateFileMode;
import me.qyh.blog.bean.ArticleNav;
import me.qyh.blog.bean.ArticleSpaceFile;
import me.qyh.blog.bean.ArticleStatistics;
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
import me.qyh.blog.evt.ArticleEvent.EventType;
import me.qyh.blog.evt.LockDeleteEvent;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.lock.LockManager;
import me.qyh.blog.pageparam.ArticleQueryParam;
import me.qyh.blog.pageparam.PageResult;
import me.qyh.blog.security.Environment;
import me.qyh.blog.service.ArticleService;
import me.qyh.blog.service.CommentServer;
import me.qyh.blog.service.ConfigService;
import me.qyh.blog.service.impl.SpaceCache.SpacesCacheKey;

public class ArticleServiceImpl implements ArticleService, InitializingBean, ApplicationEventPublisherAware,
		ApplicationListener<LockDeleteEvent> {

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
	private ThreadPoolTaskExecutor threadPoolTaskExecutor;
	@Autowired
	private CommentServer commentServer;
	@Autowired
	private ArticleIndexer articleIndexer;
	@Autowired
	private PlatformTransactionManager transactionManager;

	private ApplicationEventPublisher applicationEventPublisher;

	private boolean rebuildIndex = true;

	private List<ArticleContentHandler> articleContentHandlers = Lists.newArrayList();
	private final ScheduleManager scheduleManager = new ScheduleManager();

	@Override
	public Optional<Article> getArticleForView(String idOrAlias) {
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

			Article clone = new Article(article);
			clone.setComments(commentServer.queryArticleCommentCount(article.getId()).orElse(0));

			if (!CollectionUtils.isEmpty(articleContentHandlers)) {
				for (ArticleContentHandler handler : articleContentHandlers) {
					handler.handle(clone);
				}
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
	@ArticleIndexRebuild
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public OptionalInt hit(Integer id) {
		Article article = articleCache.getArticle(id);
		if (article != null) {
			boolean hit = !Environment.isLogin() && article.isPublished() && Environment.match(article.getSpace())
					&& !article.getIsPrivate();
			lockManager.openLock(article);
			if (hit) {
				articleDao.updateHits(id, 1);
				articleIndexer.getIndexer().addOrUpdateDocument(article);
				article.addHits();
				applicationEventPublisher.publishEvent(new ArticleEvent(this, new Article(article), EventType.HITS));
			}
			return OptionalInt.of(article.getHits());
		}
		return OptionalInt.empty();
	}

	@Override
	@ArticleIndexRebuild
	@Caching(evict = { @CacheEvict(value = "articleFilesCache", allEntries = true),
			@CacheEvict(value = "hotTags", allEntries = true) })
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
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public Article writeArticle(Article article) throws LogicException {
		Space space = spaceDao.selectById(article.getSpace().getId());
		if (space == null) {
			throw new LogicException("space.notExists", "空间不存在");
		}
		article.setSpace(space);
		// 如果文章是私有的，无法设置锁
		if (article.isPrivate()) {
			article.setLockId(null);
		} else {
			lockManager.ensureLockvailable(article.getLockId());
		}
		Timestamp now = Timestamp.valueOf(LocalDateTime.now());
		if (article.hasId()) {
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

			article.setLastModifyDate(now);
			articleTagDao.deleteByArticle(articleDb);

			articleDao.update(article);

			insertTags(article);
			articleIndexer.getIndexer().deleteDocument(article.getId());
			// 由于alias的存在，硬编码删除cache
			articleCache.evit(articleDb);
			Article updated = articleDao.selectById(article.getId());
			if (article.isPublished()) {
				articleIndexer.getIndexer().addOrUpdateDocument(updated);
			}
			applicationEventPublisher.publishEvent(new ArticleEvent(this, updated, EventType.UPDATE));
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
			insertTags(article);
			Article updated = articleDao.selectById(article.getId());
			if (article.isPublished()) {
				articleIndexer.getIndexer().addOrUpdateDocument(updated);
			}
			applicationEventPublisher.publishEvent(new ArticleEvent(this, updated, EventType.INSERT));
		}
		scheduleManager.update();
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
		if (article.isPublished()) {
			articleIndexer.getIndexer().addOrUpdateDocument(article);
		}
		applicationEventPublisher.publishEvent(new ArticleEvent(this, article, EventType.UPDATE));
	}

	private void insertTags(Article article) {
		Set<Tag> tags = article.getTags();
		if (!CollectionUtils.isEmpty(tags)) {
			boolean rebuildIndex = false;
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
					articleIndexer.getIndexer().addTags(tag.getName());
					rebuildIndex = true;
				} else {
					articleTag.setTag(tagDb);
				}
				articleTagDao.insert(articleTag);
			}
			if (rebuildIndex) {
				rebuildIndexBackground();
			}
		}
	}

	@Override
	@Transactional(readOnly = true)
	@Cacheable(value = "articleFilesCache", key = "'dateFiles-'+'space-'+#space+'-mode-'+#mode.name()+'-private-'+(T(me.qyh.blog.security.Environment).getUser().isPresent())")
	public ArticleDateFiles queryArticleDateFiles(Space space, ArticleDateFileMode mode) {
		List<ArticleDateFile> files = articleDao.selectDateFiles(space, mode, Environment.isLogin());
		ArticleDateFiles _files = new ArticleDateFiles(files, mode);
		_files.calDate();
		return _files;
	}

	@Override
	@Transactional(readOnly = true)
	@Cacheable(value = "articleFilesCache", key = "'spaceFiles-private-'+(T(me.qyh.blog.security.Environment).getUser().isPresent())")
	public List<ArticleSpaceFile> queryArticleSpaceFiles() {
		return articleDao.selectSpaceFiles(Environment.isLogin());
	}

	@Override
	@Transactional(readOnly = true)
	@ArticleIndexRebuild
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
		PageResult<Article> page;
		if (param.hasQuery()) {
			page = articleIndexer.getIndexer().query(param, (List<Integer> articleIds) -> {
				return CollectionUtils.isEmpty(articleIds) ? Lists.newArrayList() : selectByIds(articleIds);
			});
		} else {
			int count = articleDao.selectCount(param);
			List<Article> datas = articleDao.selectPage(param);
			page = new PageResult<>(param, count, datas);
		}
		// query comments
		List<Article> datas = page.getDatas();
		if (!CollectionUtils.isEmpty(datas)) {
			List<Integer> ids = Lists.newArrayList(datas.size());
			for (Article article : datas) {
				ids.add(article.getId());
			}
			Map<Integer, Integer> countsMap = commentServer.queryArticlesCommentCount(ids);
			for (Article article : datas) {
				Integer comments = countsMap.get(article.getId());
				article.setComments(comments == null ? 0 : comments);
			}
		}
		return page;
	}

	private List<Article> selectByIds(List<Integer> ids) {
		// mysql can use order by field
		// but h2 can not
		List<Article> articles = articleDao.selectByIds(ids);
		if (articles.isEmpty()) {
			return Collections.emptyList();
		}
		Map<Integer, Article> map = articles.stream().collect(Collectors.toMap(Article::getId, article -> article));
		return ids.stream().map(id -> map.get(id)).filter(Objects::nonNull).collect(Collectors.toList());
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
		articleCache.evit(article);
		articleIndexer.getIndexer().deleteDocument(id);
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
		if (article.isPublished()) {
			articleIndexer.getIndexer().addOrUpdateDocument(article);
		}

		applicationEventPublisher.publishEvent(new ArticleEvent(this, article, EventType.UPDATE));
	}

	@Override
	@ArticleIndexRebuild
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
		// 删除博客所有的评论
		commentServer.deleteComments(article);
		articleDao.deleteById(id);

		applicationEventPublisher.publishEvent(new ArticleEvent(this, article, EventType.DELETE));
	}

	@Override
	@Caching(evict = { @CacheEvict(value = "hotTags", allEntries = true, condition = "#result > 0"),
			@CacheEvict(value = "articleFilesCache", allEntries = true, condition = "#result > 0") })
	@ArticleIndexRebuild
	public int pushScheduled() {
		return scheduleManager.push();
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<ArticleNav> getArticleNav(Article article) {
		Objects.requireNonNull(article);
		if (!Environment.match(article.getSpace())) {
			return Optional.empty();
		}
		boolean queryPrivate = Environment.isLogin();
		Article previous = articleDao.getPreviousArticle(article, queryPrivate);
		Article next = articleDao.getNextArticle(article, queryPrivate);
		return (previous != null || next != null) ? Optional.of(new ArticleNav(previous, next)) : Optional.empty();
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<ArticleNav> getArticleNav(String idOrAlias) {
		Optional<Article> article = getArticleForView(idOrAlias);
		return article.isPresent() ? getArticleNav(article.get()) : Optional.empty();
	}

	@Override
	@Transactional(readOnly = true)
	public ArticleStatistics queryArticleStatistics(Space space) {
		boolean queryPrivate = Environment.isLogin();
		ArticleStatistics statistics = articleDao.selectStatistics(space, queryPrivate);
		statistics.setTotalSpaces(spaceCache.getSpaces(new SpacesCacheKey(queryPrivate)).size());
		statistics.setTotalTags(articleTagDao.selectTagsCount(space, queryPrivate, queryPrivate));
		statistics.setTotalComments(commentServer.queryArticlesTotalCommentCount(space, queryPrivate));
		return statistics;
	}

	@Override
	@Transactional(readOnly = true)
	@Cacheable(value = "hotTags")
	public List<TagCount> queryTags(Space space, boolean hasLock, boolean queryPrivate) {
		return articleTagDao.selectTags(space, hasLock, queryPrivate);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Article> queryRecentArticles(Integer limit) {
		return articleDao.selectRecentArticles(limit);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Article> findSimilar(String idOrAlias, int limit) throws LogicException {
		Optional<Article> article = getArticleForView(idOrAlias);
		return article.isPresent() ? findSimilar(article.get(), limit) : Collections.emptyList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<Article> findSimilar(Article article, int limit) throws LogicException {
		Objects.requireNonNull(article);
		if (!Environment.match(article.getSpace())) {
			return Collections.emptyList();
		}
		return articleIndexer.getIndexer().querySimilar(article, articleIds -> articleDao.selectByIds(articleIds),
				limit);
	}

	@Override
	public void preparePreview(Article article) {

		if (!CollectionUtils.isEmpty(articleContentHandlers)) {
			for (ArticleContentHandler handler : articleContentHandlers) {
				handler.handlePreview(article);
			}
		}
	}

	@Override
	public void onApplicationEvent(LockDeleteEvent event) {
		// synchronized
		// do not worry about transaction
		articleDao.deleteLock(event.getLockId());
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (rebuildIndex) {
			articleIndexer.rebuildIndex();
		}
		scheduleManager.update();
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

	public interface ArticleContentHandler {
		/**
		 * 用来处理文章
		 * 
		 * @param article
		 */
		void handle(Article article);

		/**
		 * 用来处理预览文章
		 * 
		 * @param article
		 */
		void handlePreview(Article article);
	}

	public void setArticleContentHandlers(List<ArticleContentHandler> articleContentHandlers) {
		this.articleContentHandlers = articleContentHandlers;
	}

	private final class ScheduleManager {
		private Timestamp start;

		public int push() {
			if (start == null) {
				LOGGER.debug("没有待发布的文章");
				return 0;
			}
			long now = System.currentTimeMillis();
			if (now < start.getTime()) {
				LOGGER.debug("没有到发布日期：" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(start));
				return 0;
			} else {
				LOGGER.debug("开始查询发布文章");
				Timestamp startCopy = new Timestamp(start.getTime());
				TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
				try {
					List<Article> articles = articleDao.selectScheduled(new Timestamp(now));
					if (!articles.isEmpty()) {
						for (Article article : articles) {
							article.setStatus(ArticleStatus.PUBLISHED);
							articleDao.update(article);
							articleIndexer.getIndexer().addOrUpdateDocument(article);
						}
						LOGGER.debug("发布了" + articles.size() + "篇文章");
						applicationEventPublisher.publishEvent(new ArticleEvent(this, articles, EventType.UPDATE));
					}
					start = articleDao.selectMinimumScheduleDate();
					return articles.size();
				} catch (Throwable e) {
					start = startCopy;
					status.setRollbackOnly();

					rebuildIndexBackground();
					throw e;
				} finally {
					transactionManager.commit(status);
				}
			}
		}

		public void update() {
			start = articleDao.selectMinimumScheduleDate();
			LOGGER.debug(start == null ? "没有发现待发布文章"
					: "发现待发布文章最小日期:" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(start));
		}
	}

	private void rebuildIndexBackground() {
		threadPoolTaskExecutor.execute(() -> articleIndexer.rebuildIndex());
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}
}
