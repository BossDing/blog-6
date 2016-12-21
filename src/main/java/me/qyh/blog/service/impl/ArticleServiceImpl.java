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
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.google.common.collect.Lists;

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
import me.qyh.blog.evt.ArticlePublishedEvent;
import me.qyh.blog.evt.ArticlePublishedEvent.OP;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.lock.LockManager;
import me.qyh.blog.metaweblog.MetaweblogArticle;
import me.qyh.blog.pageparam.ArticleQueryParam;
import me.qyh.blog.pageparam.PageResult;
import me.qyh.blog.security.AuthencationException;
import me.qyh.blog.security.UserContext;
import me.qyh.blog.service.ArticleService;
import me.qyh.blog.service.CommentServer;
import me.qyh.blog.service.ConfigService;
import me.qyh.blog.web.interceptor.SpaceContext;

@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
public class ArticleServiceImpl implements ArticleService, InitializingBean, ApplicationEventPublisherAware {

	private static final Logger logger = LoggerFactory.getLogger(ArticleServiceImpl.class);
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
	private NRTArticleIndexer articleIndexer;
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

	private ApplicationEventPublisher applicationEventPublisher;

	private boolean rebuildIndex = true;

	private List<ArticleContentHandler> articleContentHandlers = Lists.newArrayList();
	private final ScheduleManager scheduleManager = new ScheduleManager();

	@Override
	@Transactional(readOnly = true)
	public Article getArticleForView(String idOrAlias) {
		Article article = null;
		try {
			int id = Integer.parseInt(idOrAlias);
			article = articleCache.getArticleWithLockCheck(id);
		} catch (NumberFormatException e) {
			article = articleCache.getArticleWithLockCheck(idOrAlias);
		}
		if (article != null) {
			if (article.isPublished()) {
				if (article.isPrivate() && UserContext.get() == null) {
					throw new AuthencationException();
				}
				// 如果文章不在目标空间下
				if (!Objects.equals(SpaceContext.get(), article.getSpace())) {
					return null;
				}

				Article clone = new Article(article);
				clone.setComments(commentServer.queryArticleCommentCount(article.getId()));

				if (!CollectionUtils.isEmpty(articleContentHandlers)) {
					for (ArticleContentHandler handler : articleContentHandlers) {
						handler.handle(clone);
					}
				}
				return clone;
			}
		}
		return null;
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
	public Article hit(Integer id) {
		Article article = articleCache.getArticleWithLockCheck(id);
		if (article != null) {
			boolean hit = (UserContext.get() == null && article.isPublished()
					&& article.getSpace().equals(SpaceContext.get()) && !article.getIsPrivate());
			if (hit) {
				articleDao.updateHits(id, 1);
				articleIndexer.addOrUpdateDocument(article);
				article.addHits();
				return article;
			}
		}
		return null;
	}

	@Override
	@ArticleIndexRebuild
	@Caching(evict = { @CacheEvict(value = "articleFilesCache", allEntries = true),
			@CacheEvict(value = "hotTags", allEntries = true) })
	public Article writeArticle(MetaweblogArticle mba) throws LogicException {
		Space space = mba.getSpace() == null ? spaceDao.selectDefault() : spaceDao.selectByName(mba.getSpace());
		if (space == null) {
			throw new LogicException("space.notExists", "空间不存在");
		}
		Article article = null;
		Timestamp now = Timestamp.valueOf(LocalDateTime.now());
		if (mba.hasId()) {
			Article articleDb = articleDao.selectById(mba.getId());
			if (articleDb == null) {
				throw new LogicException("article.notExists", "文章不存在");
			}
			if (articleDb.isDeleted()) {
				throw new LogicException("article.deleted", "文章已经被删除");
			}

			mba.mergeArticle(articleDb);
			articleDb.setSpace(space);
			article = articleDb;

			if (!article.isSchedule()) {
				if (article.isDraft()) {
					article.setPubDate(null);
				} else {
					article.setPubDate(articleDb.isPublished() ? articleDb.getPubDate() : now);
				}
			}
			article.setLastModifyDate(now);
			articleDao.update(article);
			articleIndexer.deleteDocument(article.getId());
			articleCache.evit(articleDb);
			if (article.isPublished()) {
				Article updated = articleDao.selectById(article.getId());
				articleIndexer.addOrUpdateDocument(updated);
				applicationEventPublisher.publishEvent(new ArticlePublishedEvent(this, updated, OP.UPDATE));
			}
		} else {
			article = mba.toArticle();
			article.setSpace(space);

			if (!article.isSchedule()) {
				if (article.isDraft()) {
					article.setPubDate(null);
				} else {
					article.setPubDate(now);
				}
			}
			articleDao.insert(article);
			if (article.isPublished()) {
				Article updated = articleDao.selectById(article.getId());
				articleIndexer.addOrUpdateDocument(updated);
				applicationEventPublisher.publishEvent(new ArticlePublishedEvent(this, updated, OP.INSERT));
			}
		}
		return article;
	}

	@Override
	@ArticleIndexRebuild
	@Caching(evict = { @CacheEvict(value = "articleFilesCache", allEntries = true),
			@CacheEvict(value = "hotTags", allEntries = true) })
	public Article writeArticle(Article article, boolean autoDraft) throws LogicException {
		Space space = spaceDao.selectById(article.getSpace().getId());
		if (space == null) {
			throw new LogicException("space.notExists", "空间不存在");
		}
		if (space.getArticleHidden()) {
			article.setHidden(null);
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

			if (!article.isSchedule()) {
				if (article.isDraft()) {
					article.setPubDate(null);
				} else {
					article.setPubDate(articleDb.isPublished() ? articleDb.getPubDate() : now);
				}
			}
			article.setLastModifyDate(now);
			articleTagDao.deleteByArticle(articleDb);

			articleDao.update(article);

			insertTags(article);
			articleIndexer.deleteDocument(article.getId());
			// 由于alias的存在，硬编码删除cache
			articleCache.evit(articleDb);
			if (article.isPublished()) {
				Article updated = articleDao.selectById(article.getId());
				articleIndexer.addOrUpdateDocument(updated);
				if (!autoDraft) {
					// 自动草稿不推事件
					applicationEventPublisher.publishEvent(new ArticlePublishedEvent(this, updated, OP.UPDATE));
				}
			}
		} else {
			if (article.getAlias() != null) {
				Article aliasDb = articleDao.selectByAlias(article.getAlias());
				if (aliasDb != null) {
					throw new LogicException("article.alias.exists", "别名" + article.getAlias() + "已经存在",
							article.getAlias());
				}
			}
			if (!article.isSchedule()) {
				if (article.isDraft()) {
					article.setPubDate(null);
				} else {
					article.setPubDate(now);
				}
			}
			articleDao.insert(article);
			insertTags(article);
			if (article.isPublished()) {
				Article updated = articleDao.selectById(article.getId());
				articleIndexer.addOrUpdateDocument(updated);
				if (!autoDraft) {
					applicationEventPublisher.publishEvent(new ArticlePublishedEvent(this, updated, OP.INSERT));
				}
			}
		}
		scheduleManager.update();
		return article;
	}

	@Override
	@ArticleIndexRebuild
	@Caching(evict = { @CacheEvict(value = "articleFilesCache", allEntries = true),
			@CacheEvict(value = "hotTags", allEntries = true) })
	public void publishDraft(Integer id) throws LogicException {
		Article article = articleDao.selectById(id);
		if (article == null) {
			throw new LogicException("article.notExists", "文章不存在");
		}
		if (!article.isDraft()) {
			throw new LogicException("article.notDraft", "文章已经被删除");
		}
		article.setPubDate(Timestamp.valueOf(LocalDateTime.now()));
		article.setStatus(ArticleStatus.PUBLISHED);
		articleDao.update(article);
		if (article.isPublished()) {
			articleIndexer.addOrUpdateDocument(article);
			applicationEventPublisher.publishEvent(new ArticlePublishedEvent(this, article, OP.UPDATE));
		}
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
					articleIndexer.addTags(tag.getName());
					rebuildIndex = true;
				} else {
					articleTag.setTag(tagDb);
				}
				articleTagDao.insert(articleTag);
			}
			if (rebuildIndex) {
				rebuildIndex(true);// 新增了标签，重新建立索引
			}
		}
	}

	@Override
	@Transactional(readOnly = true)
	@Cacheable(value = "articleFilesCache", key = "'dateFiles-'+'space-'+#space+'-mode-'+#mode.name()+'-private-'+(T(me.qyh.blog.security.UserContext).get() != null)")
	public ArticleDateFiles queryArticleDateFiles(Space space, ArticleDateFileMode mode) {
		List<ArticleDateFile> files = articleDao.selectDateFiles(space, mode, UserContext.get() != null);
		ArticleDateFiles _files = new ArticleDateFiles(files, mode);
		_files.calDate();
		return _files;
	}

	@Override
	@Transactional(readOnly = true)
	@Cacheable(value = "articleFilesCache", key = "'spaceFiles-private-'+(T(me.qyh.blog.security.UserContext).get() != null)")
	public List<ArticleSpaceFile> queryArticleSpaceFiles() {
		return articleDao.selectSpaceFiles(UserContext.get() != null);
	}

	@Override
	@Transactional(readOnly = true)
	@ArticleIndexRebuild(readOnly = true, conditionForWait = "#param.hasQuery()")
	public PageResult<Article> queryArticle(ArticleQueryParam param) {
		GlobalConfig globalConfig = configService.getGlobalConfig();
		if (param.getSpace() == null) {
			param.setPageSize(globalConfig.getArticlePageSize());
		} else {
			Space space = spaceCache.getSpace(param.getSpace().getId());
			if (space == null) {
				param.setPageSize(globalConfig.getArticlePageSize());
				return new PageResult<>(param, 0, Collections.emptyList());
			} else {
				param.setPageSize(space.getArticlePageSize());
			}
		}
		PageResult<Article> page = null;
		if (param.hasQuery()) {
			page = articleIndexer.query(param, (List<Integer> articleIds) -> {
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
		List<Article> ordered = Lists.newArrayList();
		Article art;
		for (Integer id : ids) {
			art = new Article(id);
			int index = articles.indexOf(art);
			if (index != -1) {
				ordered.add(articles.get(index));
			}
		}
		return ordered;
	}

	@Override
	@ArticleIndexRebuild
	@Caching(evict = { @CacheEvict(value = "articleFilesCache", allEntries = true),
			@CacheEvict(value = "hotTags", allEntries = true) })
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
		articleIndexer.deleteDocument(id);
	}

	@Override
	@ArticleIndexRebuild
	@Caching(evict = { @CacheEvict(value = "articleFilesCache", allEntries = true),
			@CacheEvict(value = "hotTags", allEntries = true) })
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
			articleIndexer.addOrUpdateDocument(article);
		}
	}

	@Override
	@ArticleIndexRebuild
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
	}

	@Override
	@ArticleIndexRebuild
	@Caching(evict = { @CacheEvict(value = "hotTags", allEntries = true, condition = "#result > 0"),
			@CacheEvict(value = "articleFilesCache", allEntries = true, condition = "#result > 0") })
	public int pushScheduled() {
		return scheduleManager.push();
	}

	@Transactional(readOnly = true)
	public synchronized void rebuildIndex(boolean async) {
		if (async) {
			threadPoolTaskExecutor.execute(() -> {
				rebuildIndex();
			});
		} else {
			rebuildIndex();
		}
	}

	private void rebuildIndex() {
		long begin = System.currentTimeMillis();
		articleIndexer.deleteAll();
		List<Article> articles = articleDao.selectPublished(null);
		for (Article article : articles) {
			articleIndexer.addOrUpdateDocument(article);
		}
		long end = System.currentTimeMillis();
		logger.debug("重建博客索引成功，共耗时" + (end - begin));
	}

	@Override
	@Transactional(readOnly = true)
	public ArticleNav getArticleNav(Article article) {
		boolean queryPrivate = UserContext.get() != null;
		Article previous = articleDao.getPreviousArticle(article, queryPrivate);
		Article next = articleDao.getNextArticle(article, queryPrivate);
		return (previous != null || next != null) ? new ArticleNav(previous, next) : null;
	}

	@Override
	@Transactional(readOnly = true)
	public ArticleNav getArticleNav(String idOrAlias) {
		Article article = getArticleForView(idOrAlias);
		if (article != null) {
			return getArticleNav(article);
		}
		return null;
	}

	@Override
	@Transactional(readOnly = true)
	public ArticleStatistics queryArticleStatistics(Space space, boolean queryHidden) {
		boolean queryPrivate = UserContext.get() != null;
		ArticleStatistics statistics = articleDao.selectStatistics(space, queryPrivate, queryHidden);
		statistics.setTotalComments(commentServer.queryArticlesTotalCommentCount(space, queryPrivate, queryHidden));
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
		Article article = getArticleForView(idOrAlias);
		return findSimilar(article, limit);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Article> findSimilar(Article article, int limit) throws LogicException {
		if (article == null) {
			return Collections.emptyList();
		}
		return articleIndexer.querySimilar(article, (List<Integer> articleIds) -> {
			return articleDao.selectByIds(articleIds);
		}, limit);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (rebuildIndex) {
			rebuildIndex();
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
		void handle(Article article);
	}

	public void setArticleContentHandlers(List<ArticleContentHandler> articleContentHandlers) {
		this.articleContentHandlers = articleContentHandlers;
	}

	private final class ScheduleManager {
		private Timestamp start;

		public int push() {
			if (start == null) {
				logger.debug("没有待发布的文章");
				return 0;
			}
			long now = System.currentTimeMillis();
			if (now < start.getTime()) {
				logger.debug("没有到发布日期：" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(start));
				return 0;
			} else {
				logger.debug("开始查询发布文章");
				List<Article> articles = articleDao.selectScheduled(new Timestamp(now));
				if (!articles.isEmpty()) {
					for (Article article : articles) {
						article.setStatus(ArticleStatus.PUBLISHED);
						articleDao.update(article);
						articleIndexer.addOrUpdateDocument(article);
					}
					logger.debug("发布了" + articles.size() + "篇文章");
					applicationEventPublisher.publishEvent(new ArticlePublishedEvent(this, articles, OP.UPDATE));
				}
				start = articleDao.selectMinimumScheduleDate();
				return articles.size();
			}
		}

		public void update() {
			start = articleDao.selectMinimumScheduleDate();
			logger.debug(start == null ? "没有发现待发布文章"
					: "发现待发布文章最小日期:" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(start));
		}
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}
}
