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
package me.qyh.blog.comment;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedSet;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.web.util.HtmlUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import me.qyh.blog.comment.Comment.CommentStatus;
import me.qyh.blog.comment.CommentConfig.CommentMode;
import me.qyh.blog.comment.CommentDao.ArticleComments;
import me.qyh.blog.config.Constants;
import me.qyh.blog.config.Limit;
import me.qyh.blog.config.UserConfig;
import me.qyh.blog.entity.Article;
import me.qyh.blog.entity.Space;
import me.qyh.blog.entity.User;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.exception.SystemException;
import me.qyh.blog.pageparam.PageQueryParam;
import me.qyh.blog.pageparam.PageResult;
import me.qyh.blog.security.AuthencationException;
import me.qyh.blog.security.UserContext;
import me.qyh.blog.security.input.HtmlClean;
import me.qyh.blog.service.CommentServer;
import me.qyh.blog.service.impl.ArticleCache;
import me.qyh.blog.util.Validators;
import me.qyh.blog.web.interceptor.SpaceContext;

@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
public class DftCommentService implements CommentServer, InitializingBean, ApplicationEventPublisherAware {

	@Autowired
	private ArticleCache articleCache;
	@Autowired
	private CommentDao commentDao;

	private CommentContentChecker commentContentChecker;
	private CommentUserChecker commentEmailChecker;
	private ApplicationEventPublisher applicationEventPublisher;

	private final CommentComparator ascCommentComparator = new CommentComparator();
	private final Comparator<Comment> descCommentComparator = new Comparator<Comment>() {

		@Override
		public int compare(Comment o1, Comment o2) {
			return -ascCommentComparator.compare(o1, o2);
		}
	};

	/**
	 * 为了保证一个树结构，这里采用 path来纪录层次结构
	 * {@link http://stackoverflow.com/questions/4057947/multi-tiered-comment-replies-display-and-storage}.
	 * 同时为了走索引，只能限制它为255个字符，由于id为数字的原因，实际上一般情况下很难达到255的长度(即便id很大)，所以这里完全够用
	 */
	private static final int PATH_MAX_LENGTH = 255;
	public static final int MAX_COMMENT_LENGTH = CommentValidator.MAX_COMMENT_LENGTH;

	/*
	 * 评论配置项
	 */
	private static final String COMMENT_MODE = "commentConfig.commentMode";
	private static final String COMMENT_ASC = "commentConfig.commentAsc";
	private static final String COMMENT_ALLOW_HTML = "commentConfig.commentAllowHtml";
	private static final String COMMENT_LIMIT_SEC = "commentConfig.commentLimitSec";
	private static final String COMMENT_LIMIT_COUNT = "commentConfig.commentLimitCount";
	private static final String COMMENT_CHECK = "commentConfig.commentCheck";
	private static final String COMMENT_PAGESIZE = "commentConfig.commentPageSize";

	/**
	 * 评论配置文件位置
	 */
	private static final Resource configResource = new ClassPathResource("resources/commentConfig.properties");
	private final Properties pros = new Properties();

	private CommentConfig config;

	/**
	 * 用来过滤Html标签
	 */
	private HtmlClean htmlClean;

	/**
	 * 分页查询评论
	 * 
	 * @param param
	 * @return
	 */
	@Transactional(readOnly = true)
	public CommentPageResult queryComment(CommentQueryParam param) {
		param.setPageSize(config.getPageSize());
		if (param.getArticle() == null)
			return new CommentPageResult(param, 0, Collections.emptyList(), new CommentConfig(config));
		Article article = articleCache.getArticleWithLockCheck(param.getArticle().getId());
		if (article == null || !article.isPublished()) {
			return new CommentPageResult(param, 0, Collections.emptyList(), new CommentConfig(config));
		}
		if (article.isPrivate() && UserContext.get() == null) {
			throw new AuthencationException();
		}
		if (!article.getSpace().equals(SpaceContext.get())) {
			return new CommentPageResult(param, 0, Collections.emptyList(), new CommentConfig(config));
		}
		int count = 0;
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
			return new CommentPageResult(param, 0, Collections.emptyList(), new CommentConfig(config));
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
			datas = commentDao.selectPageWithTree(param);
			for (Comment comment : datas)
				completeComment(comment);
			datas = handlerTree(datas, config);
			break;
		default:
			datas = commentDao.selectPageWithList(param);
			for (Comment comment : datas)
				completeComment(comment);
			break;
		}
		return new CommentPageResult(param, count, datas, new CommentConfig(config));
	}

	/**
	 * 插入评论
	 * 
	 * @param comment
	 * @return
	 * @throws LogicException
	 */
	public Comment insertComment(Comment comment) throws LogicException {
		long now = System.currentTimeMillis();
		String ip = comment.getIp();
		Article article = articleCache.getArticleWithLockCheck(comment.getArticle().getId());
		// 博客不存在
		if (article == null || !article.getSpace().equals(SpaceContext.get()) || !article.isPublished())
			throw new LogicException("article.notExists", "文章不存在");
		// 如果私人文章并且没有登录
		if (article.isPrivate() && UserContext.get() == null)
			throw new AuthencationException();

		if (!article.getAllowComment() && UserContext.get() == null)
			throw new LogicException("article.notAllowComment", "文章不允许被评论");
		if (UserContext.get() == null) {
			// 检查频率
			Limit limit = config.getLimit();
			long start = now - limit.getUnit().toMillis(limit.getTime());
			int count = commentDao.selectCountByIpAndDatePeriod(new Timestamp(start), new Timestamp(now), ip) + 1;
			if (count > limit.getCount()) {
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
		if (parentPath.length() > PATH_MAX_LENGTH)
			throw new LogicException("comment.path.toolong", "该评论不能再被回复了");

		Comment last = commentDao.selectLast(comment);
		if (last != null && last.getContent().equals(comment.getContent()))
			throw new LogicException("comment.content.same", "已经回复过相同的评论了");

		if (UserContext.get() == null) {
			String email = comment.getEmail();
			if (email != null) {
				commentEmailChecker.doCheck(comment.getNickname(), email);
				// set gravatar md5
				comment.setGravatar(DigestUtils.md5DigestAsHex(email.getBytes(Constants.CHARSET)));
			}
			comment.setAdmin(false);

		} else {
			// 管理员回复无需设置评论用户信息
			comment.setEmail(null);
			comment.setNickname(null);
			comment.setAdmin(true);
		}

		comment.setParentPath(parentPath);
		comment.setCommentDate(new Timestamp(now));

		boolean check = config.getCheck() && (UserContext.get() == null);
		comment.setStatus(check ? CommentStatus.CHECK : CommentStatus.NORMAL);

		commentDao.insert(comment);

		comment.setParent(parent);
		comment.setArticle(article);// 用来获取文章链接

		completeComment(comment);

		applicationEventPublisher.publishEvent(new CommentEvent(this, comment));
		return comment;
	}

	/**
	 * 审核评论
	 * 
	 * @param id
	 *            评论id
	 * @throws LogicException
	 */
	public void checkComment(Integer id) throws LogicException {
		Comment comment = commentDao.selectById(id);// 查询父评论
		if (comment == null) {
			throw new LogicException("comment.notExists", "评论不存在");
		}
		if (!comment.isChecking()) {
			throw new LogicException("comment.checked", "评论审核过了");
		}
		commentDao.updateStatusToNormal(comment);
	}

	/**
	 * 删除评论以及该评论下的所有子评论
	 * 
	 * @param id
	 *            评论id
	 * @throws LogicException
	 */
	public void deleteComment(Integer id) throws LogicException {
		Comment comment = commentDao.selectById(id);
		if (comment == null) {
			throw new LogicException("comment.notExists", "评论不存在");
		}
		commentDao.deleteByPath(comment.getParentPath() + comment.getId());
		commentDao.deleteById(id);
	}

	/**
	 * 删除评论
	 * 
	 * @param ip
	 *            用户ip
	 * @param articleId
	 *            文章id
	 * @throws LogicException
	 */
	public void deleteComment(String ip, Integer articleId) throws LogicException {
		Article article = articleCache.getArticle(articleId);
		if (article == null) {
			throw new LogicException("article.notExists", "文章不存在");
		}
		commentDao.deleteByIpAndArticle(ip, article);
	}

	/**
	 * 查询某空间下最近的评论
	 * 
	 * @param space
	 *            空间
	 * @param limit
	 *            数目限制
	 * @return
	 */
	@Transactional(readOnly = true)
	public List<Comment> queryLastComments(Space space, int limit) {
		List<Comment> comments = commentDao.selectLastComments(space, limit, UserContext.get() != null, space != null);
		for (Comment comment : comments)
			completeComment(comment);
		return comments;
	}

	/**
	 * 查询会话
	 * 
	 * @param articleId
	 *            文章id
	 * @param id
	 *            当前评论id
	 * @return
	 * @throws LogicException
	 */
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
		List<Comment> comments = Lists.newArrayList();
		for (Integer pid : comment.getParents()) {
			Comment p = commentDao.selectById(pid);
			completeComment(p);
			comments.add(p);
		}
		completeComment(comment);
		comments.add(comment);
		return comments;
	}

	/**
	 * 获取评论配置
	 * 
	 * @return
	 */
	public CommentConfig getCommentConfig() {
		return new CommentConfig(config);
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	@Override
	public int queryArticlesTotalCommentCount(Space space, boolean queryPrivate, boolean queryHidden) {
		return commentDao.selectArticlesTotalCommentCount(space, queryPrivate, queryHidden);
	}

	@Override
	public Map<Integer, Integer> queryArticlesCommentCount(List<Integer> ids) {
		List<ArticleComments> results = commentDao.selectArticlesCommentCount(ids);
		Map<Integer, Integer> map = Maps.newHashMap();
		for (ArticleComments result : results)
			map.put(result.getId(), result.getComments());
		return map;
	}

	@Override
	public int queryArticleCommentCount(Integer id) {
		return commentDao.selectArticleCommentCount(id);
	}

	/**
	 * 更新评论配置
	 * 
	 * @param config
	 *            配置
	 */
	public synchronized void updateCommentConfig(CommentConfig config) {
		pros.setProperty(COMMENT_ALLOW_HTML, config.getAllowHtml().toString());
		pros.setProperty(COMMENT_ASC, config.getAsc().toString());
		pros.setProperty(COMMENT_CHECK, config.getCheck().toString());
		pros.setProperty(COMMENT_LIMIT_COUNT, config.getLimitCount().toString());
		pros.setProperty(COMMENT_LIMIT_SEC, config.getLimitSec().toString());
		pros.setProperty(COMMENT_MODE, config.getCommentMode().name());
		pros.setProperty(COMMENT_PAGESIZE, config.getPageSize() + "");
		try (OutputStream os = new FileOutputStream(configResource.getFile())) {
			pros.store(os, "");
		} catch (IOException e) {
			throw new SystemException(e.getMessage(), e);
		}
		loadConfig();
	}

	protected List<Comment> handlerTree(List<Comment> comments, CommentConfig config) {
		if (comments.isEmpty()) {
			return comments;
		}
		Map<Comment, List<Comment>> treeMap = Maps.newHashMap();
		for (Comment comment : comments) {
			if (comment.isRoot()) {
				if (treeMap.containsKey(comment)) {
					treeMap.put(comment, treeMap.get(comment));
				} else {
					treeMap.put(comment, Lists.newArrayList());
				}
			} else {
				Comment parent = new Comment();
				parent.setId(comment.getParents().get(0));
				if (treeMap.containsKey(parent)) {
					treeMap.get(parent).add(comment);
				} else {
					List<Comment> _comments = Lists.newArrayList();
					_comments.add(comment);
					treeMap.put(parent, _comments);
				}
			}
		}
		SortedSet<Comment> keys = Sets.newTreeSet(config.getAsc() ? ascCommentComparator : descCommentComparator);
		keys.addAll(treeMap.keySet());
		List<Comment> sorted = Lists.newArrayList();
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
		List<Comment> children = Lists.newArrayList();
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

	@Override
	public void afterPropertiesSet() throws Exception {
		if (htmlClean == null) {
			throw new SystemException("必须要提供一个html内容的清理器");
		}

		// 读取配置文件内容
		try (InputStream is = configResource.getInputStream()) {
			pros.load(is);
			loadConfig();
		}

		if (commentContentChecker == null)
			commentContentChecker = new DefaultCommentContentChecker();
		if (commentEmailChecker == null)
			commentEmailChecker = new DefaultCommentUserChecker();
	}

	private void loadConfig() {
		config = new CommentConfig();
		config.setAllowHtml(Boolean.parseBoolean(pros.getProperty(COMMENT_ALLOW_HTML, "false")));
		config.setAsc(Boolean.parseBoolean(pros.getProperty(COMMENT_ASC, "true")));
		config.setCheck(Boolean.parseBoolean(pros.getProperty(COMMENT_CHECK, "false")));
		String commentMode = pros.getProperty(COMMENT_MODE);
		if (commentMode == null)
			config.setCommentMode(CommentMode.LIST);
		else
			config.setCommentMode(CommentMode.valueOf(commentMode));
		config.setLimitCount(Integer.parseInt(pros.getProperty(COMMENT_LIMIT_COUNT, "10")));
		config.setLimitSec(Integer.parseInt(pros.getProperty(COMMENT_LIMIT_SEC, "60")));
		config.setPageSize(Integer.parseInt(pros.getProperty(COMMENT_PAGESIZE, "10")));
	}

	private void completeComment(Comment comment) {
		Comment p = comment.getParent();
		if (p != null)
			completeComment(p);
		if (comment.getAdmin() == null || !comment.getAdmin())
			return;
		User user = UserConfig.get();
		comment.setNickname(user.getName());
		String email = user.getEmail();
		comment.setEmail(email);
		comment.setGravatar(user.getGravatar());
	}

	protected String buildLastCommentsCacheKey(Space space) {
		return "last-comments" + (space == null ? "" : "-space-" + space.getId());
	}

	public void setHtmlClean(HtmlClean htmlClean) {
		this.htmlClean = htmlClean;
	}

	public void setCommentEmailChecker(CommentUserChecker commentEmailChecker) {
		this.commentEmailChecker = commentEmailChecker;
	}

	private final class DefaultCommentContentChecker implements CommentContentChecker {

		@Override
		public void doCheck(String content, boolean html) throws LogicException {

		}

	}

	private final class DefaultCommentUserChecker extends CommentUserChecker {

		@Override
		protected void checkMore(final String name, final String email) throws LogicException {
		}

	}

	public static final class CommentPageResult extends PageResult<Comment> {
		private final CommentConfig commentConfig;

		public CommentPageResult(PageQueryParam param, int totalRow, List<Comment> datas, CommentConfig commentConfig) {
			super(param, totalRow, datas);
			this.commentConfig = commentConfig;
		}

		public CommentConfig getCommentConfig() {
			return commentConfig;
		}
	}
}
