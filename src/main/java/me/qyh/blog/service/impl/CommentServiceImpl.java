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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import me.qyh.blog.config.Limit;
import me.qyh.blog.dao.ArticleDao;
import me.qyh.blog.dao.CommentDao;
import me.qyh.blog.dao.OauthUserDao;
import me.qyh.blog.entity.Article;
import me.qyh.blog.entity.Comment;
import me.qyh.blog.entity.Comment.CommentStatus;
import me.qyh.blog.entity.CommentConfig;
import me.qyh.blog.entity.Space;
import me.qyh.blog.entity.SpaceConfig;
import me.qyh.blog.evt.CommentEvent;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.exception.SystemException;
import me.qyh.blog.input.HtmlClean;
import me.qyh.blog.oauth2.OauthUser;
import me.qyh.blog.pageparam.CommentQueryParam;
import me.qyh.blog.pageparam.PageResult;
import me.qyh.blog.security.AuthencationException;
import me.qyh.blog.security.UserContext;
import me.qyh.blog.service.CommentService;
import me.qyh.blog.service.ConfigService;
import me.qyh.blog.web.controller.form.CommentValidator;
import me.qyh.blog.web.interceptor.SpaceContext;
import me.qyh.util.Validators;

@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
public class CommentServiceImpl implements CommentService, InitializingBean, ApplicationEventPublisherAware {

	@Autowired
	private ArticleCache articleCache;
	@Autowired
	private OauthUserDao oauthUserDao;
	@Autowired
	private CommentDao commentDao;
	@Autowired
	private ArticleDao articleDao;
	@Autowired
	private ArticleIndexer articleIndexer;
	@Autowired
	private SpaceCache spaceCache;
	@Autowired
	private ThreadPoolTaskScheduler threadPoolTaskScheduler;
	@Autowired
	private ConfigService configService;

	private CommentContentChecker commentContentChecker;

	private final CommentComparator ascCommentComparator = new CommentComparator();
	private final Comparator<Comment> descCommentComparator = new Comparator<Comment>() {

		@Override
		public int compare(Comment o1, Comment o2) {
			return -ascCommentComparator.compare(o1, o2);
		}
	};

	private InvalidCountMap invalidCountMap = new InvalidCountMap();
	private InvalidUserMap invalidUserMap = new InvalidUserMap();

	/**
	 * 非法点击：当用户达到单位时间内的限制评论数之后的点击<br>
	 * 如果在invalidLimitSecond中再次非法点击invalidLimitCount次，那么将会被放入invalidUserMap中,有效期为invalidSecond
	 */
	private static final int INVALID_LIMIT_COUNT = 3;
	private static final int INVALID_LIMIT_SECOND = 10;
	private static final int INVALID_SECOND = 300;

	/**
	 * 每隔invaliClearSecond就清除map中过期的对象
	 */
	private static final int INVALID_CLEAR_SECOND = 10 * 60;

	private int invalidLimitCount = INVALID_LIMIT_COUNT;
	private int invalidLimitSecond = INVALID_LIMIT_SECOND;
	private int invalidSecond = INVALID_SECOND;
	private int invalidClearSecond = INVALID_CLEAR_SECOND;

	/**
	 * 为了保证一个树结构，这里采用 path来纪录层次结构
	 * {@link http://stackoverflow.com/questions/4057947/multi-tiered-comment-replies-display-and-storage}.
	 * 同时为了走索引，只能限制它为255个字符，由于id为数字的原因，实际上一般情况下很难达到255的长度(即便id很大)，所以这里完全够用
	 */
	private static final int PATH_MAX_LENGTH = 255;
	public static final int MAX_COMMENT_LENGTH = CommentValidator.MAX_COMMENT_LENGTH;

	/**
	 * 用来过滤Html标签
	 */
	private HtmlClean htmlClean;

	@Override
	@Transactional(readOnly = true)
	public PageResult<Comment> queryComment(CommentQueryParam param) {
		Article article = articleCache.getArticleWithLockCheck(param.getArticle().getId());
		if (article == null || !article.isPublished()) {
			return new PageResult<>(param, 0, Collections.emptyList());
		}
		if (article.isPrivate() && UserContext.get() == null) {
			throw new AuthencationException();
		}
		if (!article.getSpace().equals(SpaceContext.get())) {
			return new PageResult<>(param, 0, Collections.emptyList());
		}
		int count = 0;
		CommentConfig config = getConfig(article);
		switch (config.getCommentMode()) {
		case TREE:
			count = commentDao.selectCountWithTree(param);
			break;
		default:
			count = commentDao.selectCountWithList(param);
			break;
		}
		int pageSize = config.getPageSize();
		param.setPageSize(pageSize);
		if (count == 0) {
			return new PageResult<>(param, 0, Collections.emptyList());
		}
		boolean asc = config.getAsc();
		if (param.getCurrentPage() <= 0) {
			if (asc) {
				param.setCurrentPage(count % pageSize == 0 ? count / pageSize : count / pageSize + 1);
			} else {
				param.setCurrentPage(1);
			}
		}
		param.setAsc(asc);
		List<Comment> datas = null;
		switch (config.getCommentMode()) {
		case TREE:
			datas = handlerTree(commentDao.selectPageWithTree(param), config);
			break;
		default:
			datas = commentDao.selectPageWithList(param);
			break;
		}

		// 为了在评论中获取配置信息
		Article _article = new Article(article.getId());
		_article.setCommentConfig(config);
		param.setArticle(_article);

		return new PageResult<Comment>(param, count, datas);
	}

	@Override
	@ArticleIndexRebuild
	public Comment insertComment(Comment comment) throws LogicException {
		long now = System.currentTimeMillis();
		if (isInvalidUser(comment.getUser())) {
			throw new LogicException("comment.user.invalid", "该账户暂时被禁止评论");
		}
		OauthUser user = oauthUserDao.selectById(comment.getUser().getId());
		if (user == null) {
			throw new LogicException("comment.user.notExists", "账户不存在");
		}
		if (user.isDisabled()) {
			throw new LogicException("comment.user.ban", "该账户被禁止评论");
		}
		Article article = articleCache.getArticleWithLockCheck(comment.getArticle().getId());
		// 博客不存在
		if (article == null || !article.getSpace().equals(SpaceContext.get()) || !article.isPublished()) {
			throw new LogicException("article.notExists", "文章不存在");
		}
		CommentConfig config = getConfig(article);
		if (!config.getAllowComment() && (UserContext.get() == null || user.getAdmin())) {
			throw new LogicException("article.notAllowComment", "文章不允许被评论");
		}
		// 如果私人文章并且没有登录
		if (article.isPrivate() && UserContext.get() == null) {
			throw new AuthencationException();
		}
		if (UserContext.get() == null || !Boolean.TRUE.equals(user.getAdmin())) {
			// 检查频率
			Limit limit = config.getLimit();
			long start = now - limit.getUnit().toMillis(limit.getTime());
			int count = commentDao.selectCountByUserAndDatePeriod(new Timestamp(start), new Timestamp(now), user) + 1;
			if (count > limit.getLimit()) {
				invalidCountMap.increase(user, now);
				throw new LogicException("comment.overlimit", "评论太过频繁，请稍作休息");
			}
		}

		setContent(comment, config);
		commentContentChecker.doCheck(comment.getContent(), config.getAllowHtml());

		String parentPath = "/";
		// 判断是否存在父评论
		Comment parent = comment.getParent();
		if (parent != null) {
			parent = commentDao.selectById(parent.getId());// 查询父评论
			if (parent == null) {
				throw new LogicException("comment.parent.notExists", "父评论不存在");
			}

			// 如果父评论正在审核
			if (parent.isChecking()) {
				throw new LogicException("comment.parent.checking", "父评论正在审核");
			}
			parentPath = parent.getParentPath() + parent.getId() + "/";
		}
		if (parentPath.length() > PATH_MAX_LENGTH) {
			throw new LogicException("comment.path.toolong", "该评论不能再被回复了");
		}
		Comment last = commentDao.selectLast(comment);
		if (last != null && last.getContent().equals(comment.getContent())) {
			throw new LogicException("comment.content.same", "已经回复过相同的评论了");
		}
		comment.setParentPath(parentPath);
		comment.setCommentDate(new Timestamp(now));
		boolean check = config.getCheck() && !user.getAdmin() && (UserContext.get() == null);
		comment.setStatus(check ? CommentStatus.CHECK : CommentStatus.NORMAL);
		commentDao.insert(comment);

		if (!check) {
			// 如果不检查，增加评论数
			// 是不是每个回复都算评论数？
			articleDao.updateComments(article.getId(), 1);
			articleIndexer.addOrUpdateDocument(article);
			article.addComments();

		}
		comment.setParent(parent);
		comment.setUser(user);
		comment.setArticle(article);// 用来获取文章链接

		applicationEventPublisher.publishEvent(new CommentEvent(this, comment));
		return comment;
	}

	private CommentConfig getConfig(Article article) {
		CommentConfig config = article.getCommentConfig();
		if (config == null) {
			SpaceConfig spaceConfig = spaceCache.getSpace(article.getSpace().getId()).getConfig();
			if (spaceConfig != null)
				config = spaceConfig.getCommentConfig();
		}
		if (config == null)
			config = configService.getGlobalConfig().getCommentConfig();
		return config;
	}

	@Override
	@ArticleIndexRebuild
	public void checkComment(Integer id) throws LogicException {
		Comment comment = commentDao.selectById(id);// 查询父评论
		if (comment == null) {
			throw new LogicException("comment.notExists", "评论不存在");
		}
		if (!comment.isChecking()) {
			throw new LogicException("comment.checked", "评论审核过了");
		}
		commentDao.updateStatusToNormal(comment);
		Article article = articleCache.getArticle(comment.getArticle().getId());
		articleDao.updateComments(article.getId(), 1);
		article.addComments();
	}

	@Override
	@ArticleIndexRebuild
	public void deleteComment(Integer id) throws LogicException {
		Comment comment = commentDao.selectById(id);
		if (comment == null) {
			throw new LogicException("comment.notExists", "评论不存在");
		}
		int totalCount = commentDao.deleteByPath(comment.getSubPath(), CommentStatus.NORMAL)
				+ (comment.isChecking() ? 0 : 1);
		commentDao.deleteByPath(comment.getSubPath(), CommentStatus.CHECK);
		commentDao.deleteById(id);
		if (totalCount > 0) {
			// 更新文章评论数量
			Article article = articleCache.getArticle(comment.getArticle().getId());
			articleDao.updateComments(article.getId(), -totalCount);
			article.decrementComment(totalCount);
		}
	}

	@Override
	@ArticleIndexRebuild
	public void deleteComment(Integer userId, Integer articleId) throws LogicException {
		OauthUser user = oauthUserDao.selectById(userId);
		if (user == null) {
			throw new LogicException("comment.user.notExists", "账户不存在");
		}
		Article article = articleCache.getArticle(articleId);
		if (article == null) {
			throw new LogicException("article.notExists", "文章不存在");
		}

		int count = commentDao.deleteByUserAndArticle(user, article, CommentStatus.NORMAL);
		commentDao.deleteByUserAndArticle(user, article, CommentStatus.CHECK);
		if (count > 0) {
			articleDao.updateComments(article.getId(), -count);
			article.decrementComment(count);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public List<Comment> queryLastComments(Space space, int limit) {
		return commentDao.selectLastComments(space, limit, UserContext.get() != null);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Comment> queryConversations(Integer articleId, Integer id) throws LogicException {
		Article article = articleCache.getArticleWithLockCheck(articleId);
		if (article == null)
			throw new LogicException("article.notExists", "文章不存在");
		if (!article.isPublished()) {
			return Collections.emptyList();
		}
		if (article.isPrivate() && UserContext.get() == null) {
			throw new AuthencationException();
		}
		if (!article.getSpace().equals(SpaceContext.get())) {
			return Collections.emptyList();
		}
		Comment comment = commentDao.selectById(id);
		if (comment == null) {
			throw new LogicException("comment.notExists", "评论不存在");
		}
		if (!article.equals(comment.getArticle())) {
			return Collections.emptyList();
		}
		if (comment.getParents().isEmpty()) {
			return Arrays.asList(comment);
		}
		List<Comment> comments = new ArrayList<>();
		for (Integer pid : comment.getParents()) {
			comments.add(commentDao.selectById(pid));
		}
		comments.add(comment);
		return comments;
	}

	protected List<Comment> handlerTree(List<Comment> comments, CommentConfig config) {
		if (comments.isEmpty()) {
			return comments;
		}
		Map<Comment, List<Comment>> treeMap = new HashMap<>();

		for (Comment comment : comments) {
			if (comment.isRoot()) {
				if (treeMap.containsKey(comment)) {
					treeMap.put(comment, treeMap.get(comment));
				} else {
					treeMap.put(comment, new ArrayList<Comment>());
				}
			} else {
				Comment parent = new Comment();
				parent.setId(comment.getParents().get(0));
				if (treeMap.containsKey(parent)) {
					treeMap.get(parent).add(comment);
				} else {
					List<Comment> _comments = new ArrayList<Comment>();
					_comments.add(comment);
					treeMap.put(parent, _comments);
				}
			}
		}
		SortedSet<Comment> keys = new TreeSet<Comment>(config.getAsc() ? ascCommentComparator : descCommentComparator);
		keys.addAll(treeMap.keySet());
		List<Comment> sorted = new ArrayList<Comment>();
		for (Comment root : keys) {
			build(root, treeMap.get(root));
			sorted.add(root);
		}
		treeMap.clear();
		keys.clear();
		return sorted;
	}

	private void setContent(Comment comment, CommentConfig commentConfig) throws LogicException {
		String content = comment.getContent();
		if (commentConfig.getAllowHtml()) {
			content = htmlClean.clean(content);
		} else {
			content = HtmlUtils.htmlEscape(content);
		}
		if (Validators.isEmptyOrNull(content, true)) {
			throw new LogicException("comment.content.blank");
		}
		// 再次检查content的长度
		if (content.length() > MAX_COMMENT_LENGTH) {
			throw new LogicException("comment.content.toolong", "回复的内容不能超过" + MAX_COMMENT_LENGTH + "个字符",
					MAX_COMMENT_LENGTH);
		}
		comment.setContent(content);
	}

	private List<Comment> findChildren(Comment root, List<Comment> comments) {
		List<Comment> children = new ArrayList<>();
		for (Comment comment : comments) {
			if (root.equals(comment.getParent())) {
				comment.setParent(null);// 防止json死循环
				children.add(comment);
			}
		}
		Collections.sort(children, ascCommentComparator);
		return children;
	}

	private final class CommentComparator implements Comparator<Comment> {
		@Override
		public int compare(Comment o1, Comment o2) {
			int compare = o1.getCommentDate().compareTo(o2.getCommentDate());
			if (compare == 0)
				return o1.getId().compareTo(o2.getId());
			return compare;
		}
	}

	private void build(Comment root, List<Comment> comments) {
		List<Comment> children = findChildren(root, comments);
		comments.removeAll(children);
		for (Iterator<Comment> it = children.iterator(); it.hasNext();) {
			Comment child = it.next();
			build(child, comments);
		}
		root.setChildren(children);
	}

	private final class DefaultCommentContentChecker implements CommentContentChecker {

		@Override
		public void doCheck(String content, boolean html) throws LogicException {

		}

	}

	private final class InvalidCount {
		private long start;
		private AtomicInteger count;

		public InvalidCount(long start) {
			this.start = start;
			this.count = new AtomicInteger(0);
		}

		public boolean overtime(long now) {
			return (now - start) > (invalidLimitSecond * 1000);
		}

		public int increase() {
			return count.incrementAndGet();
		}

		public InvalidCount(long start, int count) {
			this.start = start;
			this.count = new AtomicInteger(count);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + (int) (start ^ (start >>> 32));
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			InvalidCount other = (InvalidCount) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (start != other.start)
				return false;
			return true;
		}

		private CommentServiceImpl getOuterType() {
			return CommentServiceImpl.this;
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (htmlClean == null) {
			throw new SystemException("必须要提供一个html内容的清理器");
		}
		if (commentContentChecker == null) {
			commentContentChecker = new DefaultCommentContentChecker();
		}
		if (invalidLimitCount < 0 || invalidLimitSecond < 0 || invalidSecond < 0) {
			invalidLimitCount = INVALID_LIMIT_COUNT;
			invalidLimitSecond = INVALID_LIMIT_SECOND;
			invalidSecond = INVALID_SECOND;
		}
		if (invalidClearSecond < 0) {
			invalidClearSecond = INVALID_CLEAR_SECOND;
		}
		threadPoolTaskScheduler.scheduleAtFixedRate(new Runnable() {

			@Override
			public void run() {
				invalidUserMap.removeOvertimes();
				invalidCountMap.removeOvertimes();
			}
		}, invalidClearSecond * 1000);
	}

	private boolean isInvalidUser(OauthUser user) {
		if (UserContext.get() != null) {
			return false;
		}
		Long start = invalidUserMap.get(user);
		if (start != null && (System.currentTimeMillis() - start) <= (invalidSecond * 1000)) {
			return true;
		}
		return false;
	}

	private final class InvalidUserMap {
		private final ConcurrentHashMap<OauthUser, Long> map;

		public InvalidUserMap() {
			map = new ConcurrentHashMap<>();
		}

		public void put(OauthUser user, long time) {
			map.computeIfAbsent(user, k -> time);
		}

		public void removeOvertimes() {
			map.values().removeIf(x -> (x != null && ((System.currentTimeMillis() - x) > invalidSecond * 1000)));
		}

		public Long get(OauthUser user) {
			return map.get(user);
		}
	}

	private final class InvalidCountMap {
		private final ConcurrentHashMap<OauthUser, InvalidCount> map;

		public InvalidCountMap() {
			map = new ConcurrentHashMap<>();
		}

		public void put(OauthUser user, InvalidCount count) {
			map.computeIfAbsent(user, k -> count);
		}

		public void remove(OauthUser user) {
			map.remove(user);
		}

		public void increase(OauthUser user, long now) {
			InvalidCount oldCount = map.computeIfAbsent(user, k -> new InvalidCount(now));
			int count = oldCount.increase();
			if (!oldCount.overtime(now) && (count >= invalidLimitCount)) {
				invalidUserMap.put(user, now);
				invalidCountMap.remove(user);
			} else if (oldCount.overtime(now))
				invalidCountMap.put(user, new InvalidCount(now, 1));
		}

		public void removeOvertimes() {
			map.values().removeIf(x -> (x != null && x.overtime(System.currentTimeMillis())));
		}
	}

	protected String buildLastCommentsCacheKey(Space space) {
		return "last-comments" + (space == null ? "" : "-space-" + space.getId());
	}

	public void setInvalidLimitCount(int invalidLimitCount) {
		this.invalidLimitCount = invalidLimitCount;
	}

	public void setInvalidLimitSecond(int invalidLimitSecond) {
		this.invalidLimitSecond = invalidLimitSecond;
	}

	public void setInvalidSecond(int invalidSecond) {
		this.invalidSecond = invalidSecond;
	}

	public void setInvalidClearSecond(int invalidClearSecond) {
		this.invalidClearSecond = invalidClearSecond;
	}

	public void setHtmlClean(HtmlClean htmlClean) {
		this.htmlClean = htmlClean;
	}

	private ApplicationEventPublisher applicationEventPublisher;

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}
}
