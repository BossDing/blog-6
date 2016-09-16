package me.qyh.blog.service.impl;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import me.qyh.blog.dao.ArticleDao;
import me.qyh.blog.dao.ArticleTagDao;
import me.qyh.blog.dao.CommentDao;
import me.qyh.blog.dao.SpaceDao;
import me.qyh.blog.dao.TagDao;
import me.qyh.blog.entity.Article;
import me.qyh.blog.entity.Article.ArticleStatus;
import me.qyh.blog.entity.ArticleTag;
import me.qyh.blog.entity.Space;
import me.qyh.blog.entity.Tag;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.lock.Lock;
import me.qyh.blog.lock.LockManager;
import me.qyh.blog.pageparam.ArticleQueryParam;
import me.qyh.blog.pageparam.PageResult;
import me.qyh.blog.security.AuthencationException;
import me.qyh.blog.security.UserContext;
import me.qyh.blog.service.ArticleService;
import me.qyh.blog.ui.widget.ArticleDateFiles;
import me.qyh.blog.ui.widget.ArticleDateFiles.ArticleDateFileMode;
import me.qyh.blog.ui.widget.ArticleSpaceFile;
import me.qyh.blog.web.interceptor.SpaceContext;

@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
public class ArticleServiceImpl implements ArticleService, InitializingBean {

	@Autowired
	private ArticleDao articleDao;
	@Autowired
	private CommentDao commentDao;
	@Autowired
	private SpaceDao spaceDao;
	@Autowired
	private ArticleTagDao articleTagDao;
	@Autowired
	private TagDao tagDao;
	@Autowired
	private ArticleIndexer articleIndexer;
	@Autowired
	private LockManager<?> lockManager;
	@Autowired
	private ArticleQuery articleQuery;

	private boolean rebuildIndex = true;

	private static final Logger logger = LoggerFactory.getLogger(ArticleServiceImpl.class);

	@Override
	@Transactional(readOnly = true)
	public Article getArticleForView(Integer id) {
		Article article = articleQuery.getArticleWithLockCheck(id);
		if (article != null) {
			if (article.isPublished()) {
				if (article.isPrivate() && UserContext.get() == null) {
					throw new AuthencationException();
				}
				return article;
			}
		}
		return null;
	}

	@Override
	@Transactional(readOnly = true)
	public Article getArticleForEdit(Integer id) throws LogicException {
		Article article = articleQuery.getArticle(id);
		if (article == null || article.isDeleted()) {
			throw new LogicException("article.notExists", "文章不存在");
		}
		return article;
	}

	@Override
	public Article hit(Integer id) {
		Article article = articleQuery.getArticle(id);
		if (article != null) {
			boolean hit = (article.isPublished() && article.getSpace().equals(SpaceContext.get()))
					? article.isPrivate() ? UserContext.get() != null : true : false;
			if (hit) {
				articleDao.updateHits(id, 1);
				article.addHits();
				articleIndexer.addOrUpdateDocument(article);
				return article;
			}
		}
		return null;
	}

	@Override
	@Caching(evict = { @CacheEvict(value = "articleFilesCache", allEntries = true) })
	public Article writeArticle(Article article) throws LogicException {
		Space space = spaceDao.selectById(article.getSpace().getId());
		if (space == null) {
			throw new LogicException("space.notExists", "空间不存在");
		}
		article.setSpace(space);
		checkLock(article.getLockId());
		Timestamp now = Timestamp.valueOf(LocalDateTime.now());
		if (article.hasId()) {
			Article articleDb = articleDao.selectById(article.getId());
			if (articleDb == null) {
				throw new LogicException("article.notExists", "文章不存在");
			}
			if (articleDb.isDeleted()) {
				throw new LogicException("article.deleted", "文章已经被删除");
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
			Article updated = articleDao.selectById(article.getId());
			if (article.isPublished()) {
				articleIndexer.addOrUpdateDocument(updated);
			}
			articleQuery.addOrUpdate(updated);
		} else {
			if (!article.isSchedule()) {
				if (article.isDraft()) {
					article.setPubDate(null);
				} else {
					article.setPubDate(now);
				}
			}
			articleDao.insert(article);
			insertTags(article);
			Article updated = articleDao.selectById(article.getId());
			if (article.isPublished()) {
				articleIndexer.addOrUpdateDocument(updated);
			}
			articleQuery.addOrUpdate(updated);
		}
		return article;
	}

	@Override
	@Caching(evict = { @CacheEvict(value = "articleFilesCache", allEntries = true) })
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
		if (article.isPublished())
			articleIndexer.addOrUpdateDocument(article);
		articleQuery.addOrUpdate(article);
	}

	private void insertTags(Article article) {
		Set<Tag> tags = article.getTags();
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
				} else {
					articleTag.setTag(tagDb);
				}
				articleTagDao.insert(articleTag);
			}
		}
	}

	@Override
	@Transactional(readOnly = true)
	@Cacheable(value = "articleFilesCache")
	public ArticleDateFiles queryArticleDateFiles(Space space, ArticleDateFileMode mode, boolean queryPrivate) {
		return articleQuery.queryArticleDateFiles(space, mode, queryPrivate);
	}

	@Override
	@Transactional(readOnly = true)
	@Cacheable(value = "articleFilesCache")
	public List<ArticleSpaceFile> queryArticleSpaceFiles(boolean queryPrivate) {
		return articleQuery.queryArticleSpaceFiles(queryPrivate);
	}

	@Override
	@Transactional(readOnly = true)
	public PageResult<Article> queryArticle(ArticleQueryParam param) {
		return articleQuery.query(param);
	}

	@Override
	@Caching(evict = { @CacheEvict(value = "articleFilesCache", allEntries = true) })
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
		articleQuery.addOrUpdate(article);
	}

	@Override
	@Caching(evict = { @CacheEvict(value = "articleFilesCache", allEntries = true) })
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
		if (article.isPublished())
			articleIndexer.addOrUpdateDocument(article);
		articleQuery.addOrUpdate(article);
	}

	@Override
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
		commentDao.deleteByArticle(article);
		articleDao.deleteById(id);
		articleQuery.remove(id);
	}

	@Override
	public List<String> getTags(String content, int max) {
		return articleIndexer.getTags(content, max);
	}

	@Override
	public String getSummary(String content, int max) {
		return articleIndexer.getSummary(content, max);
	}

	@Override
	@CacheEvict(value = "articleFilesCache", allEntries = true, condition = "#result > 0")
	public int pushScheduled() {
		// 查询将要发表的文章
		List<Article> articles = articleQuery.queryScheduled(Timestamp.valueOf(LocalDateTime.now()));
		for (Article article : articles) {
			article.setStatus(ArticleStatus.PUBLISHED);
			articleDao.update(article);
			if (article.isPublished())
				articleIndexer.addOrUpdateDocument(article);
			articleQuery.addOrUpdate(article);
		}
		return articles.size();
	}

	@Transactional(readOnly = true)
	public synchronized void rebuildIndex() {
		logger.debug("开始重新建立博客索引" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
		long begin = System.currentTimeMillis();
		articleIndexer.deleteAll();
		List<Article> articles = articleQuery.selectPublished(null);
		for (Article article : articles) {
			articleIndexer.addOrUpdateDocument(article);
		}
		long end = System.currentTimeMillis();
		logger.debug("重建博客索引成功，共耗时" + (end - begin));
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (rebuildIndex) {
			rebuildIndex();
		}
	}

	private void checkLock(String lockId) throws LogicException {
		if (lockId != null) {
			Lock lock = lockManager.findLock(lockId);
			if (lock == null) {
				throw new LogicException("lock.notexists", "锁不存在");
			}
		}
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
}
