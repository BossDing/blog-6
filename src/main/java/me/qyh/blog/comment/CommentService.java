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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Properties;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.html.HtmlEscapers;

import me.qyh.blog.comment.Comment.CommentStatus;
import me.qyh.blog.comment.CommentConfig.CommentMode;
import me.qyh.blog.comment.CommentDao.ModuleCommentCount;
import me.qyh.blog.comment.CommentModule.ModuleType;
import me.qyh.blog.config.Constants;
import me.qyh.blog.config.Limit;
import me.qyh.blog.config.UrlHelper;
import me.qyh.blog.config.UserConfig;
import me.qyh.blog.dao.UserPageDao;
import me.qyh.blog.entity.Article;
import me.qyh.blog.entity.Editor;
import me.qyh.blog.entity.Space;
import me.qyh.blog.entity.User;
import me.qyh.blog.evt.ArticleEvent;
import me.qyh.blog.evt.EventType;
import me.qyh.blog.evt.UserPageEvent;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.exception.SystemException;
import me.qyh.blog.lock.LockManager;
import me.qyh.blog.security.Environment;
import me.qyh.blog.security.input.HtmlClean;
import me.qyh.blog.security.input.Markdown2Html;
import me.qyh.blog.service.CommentServer;
import me.qyh.blog.service.impl.ArticleCache;
import me.qyh.blog.ui.page.UserPage;
import me.qyh.blog.util.Validators;

public class CommentService implements InitializingBean, CommentServer {

	private CommentChecker commentChecker;
	private HtmlClean htmlClean;

	@Autowired
	protected CommentDao commentDao;
	@Autowired
	protected UrlHelper urlHelper;
	@Autowired
	private ArticleCache articleCache;
	@Autowired
	private LockManager lockManager;
	@Autowired
	private UserPageDao userPageDao;
	@Autowired
	private Markdown2Html markdown2Html;
	@Autowired
	private ThreadPoolTaskExecutor threadPoolTaskExecutor;

	private CommentEmailNotifySupport commentEmailNotifySupport;

	/**
	 * 评论配置项
	 */
	protected static final String COMMENT_MODE = "commentConfig.commentMode";
	protected static final String COMMENT_ASC = "commentConfig.commentAsc";
	protected static final String COMMENT_EDITOR = "commentConfig.editor";
	protected static final String COMMENT_LIMIT_SEC = "commentConfig.commentLimitSec";
	protected static final String COMMENT_LIMIT_COUNT = "commentConfig.commentLimitCount";
	protected static final String COMMENT_CHECK = "commentConfig.commentCheck";
	protected static final String COMMENT_PAGESIZE = "commentConfig.commentPageSize";

	private final Comparator<Comment> ascCommentComparator = Comparator.comparing(Comment::getCommentDate)
			.thenComparing(Comment::getId);
	private final Comparator<Comment> descCommentComparator = (t1, t2) -> -ascCommentComparator.compare(t1, t2);

	/**
	 * 为了保证一个树结构，这里采用 path来纪录层次结构
	 * {@link http://stackoverflow.com/questions/4057947/multi-tiered-comment-replies-display-and-storage}.
	 * 同时为了走索引，只能限制它为255个字符，由于id为数字的原因，实际上一般情况下很难达到255的长度(即便id很大)，所以这里完全够用
	 */
	private static final int PATH_MAX_LENGTH = 255;
	private static final int MAX_COMMENT_LENGTH = CommentValidator.MAX_COMMENT_LENGTH;

	/**
	 * 评论配置文件位置
	 */
	private static final Resource configResource = new ClassPathResource("resources/commentConfig.properties");
	private final Properties pros = new Properties();

	private CommentConfig config;

	@Override
	public void afterPropertiesSet() throws Exception {

		if (htmlClean == null) {
			throw new SystemException("必须要提供一个html内容的清理器");
		}

		if (commentChecker == null) {
			commentChecker = (comment, config) -> {
			};
		}

		// 读取配置文件内容
		try (InputStream is = configResource.getInputStream()) {
			pros.load(is);
			loadConfig();
		}
	}

	/**
	 * 审核评论
	 * 
	 * @param id
	 *            评论id
	 * @throws LogicException
	 */

	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
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
	 * 删除评论
	 * 
	 * @param id
	 *            评论id
	 * @throws LogicException
	 */
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void deleteComment(Integer id) throws LogicException {
		Comment comment = commentDao.selectById(id);
		if (comment == null) {
			throw new LogicException("comment.notExists", "评论不存在");
		}
		commentDao.deleteByPath(comment.getParentPath() + comment.getId());
		commentDao.deleteById(id);
	}

	/**
	 * 更新评论配置
	 * 
	 * @param config
	 *            配置
	 */
	public synchronized void updateCommentConfig(CommentConfig config) {
		pros.setProperty(COMMENT_EDITOR, config.getEditor().name());
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

	/**
	 * 获取评论配置
	 * 
	 * @return
	 */
	public CommentConfig getCommentConfig() {
		return new CommentConfig(config);
	}

	/**
	 * 新增一条评论
	 * 
	 * @param comment
	 * @return
	 * @throws LogicException
	 */
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public Comment insertComment(Comment comment) throws LogicException {
		CommentModule module = comment.getCommentModule();
		switch (module.getType()) {
		case ARTICLE:
			doArticleCommentValid(module.getId());
			break;
		case USERPAGE:
			doUserPageCommmentValid(module.getId());
			break;
		default:
			throw new SystemException("无效的ModuleType:" + comment.getCommentModule().getType());
		}
		long now = System.currentTimeMillis();
		String ip = comment.getIp();
		if (!Environment.isLogin()) {
			// 检查频率
			Limit limit = config.getLimit();
			long start = now - limit.getUnit().toMillis(limit.getTime());
			int count = commentDao.selectCountByIpAndDatePeriod(comment.getCommentModule(), new Timestamp(start),
					new Timestamp(now), ip) + 1;
			if (count > limit.getCount()) {
				throw new LogicException("comment.overlimit", "评论太过频繁，请稍作休息");
			}
		}

		setContent(comment, config);
		commentChecker.checkComment(comment, config);

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

			if (!comment.matchParent(parent)) {
				throw new LogicException("comment.parent.unmatch", "评论匹配失败");
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

		if (!Environment.isLogin()) {
			String email = comment.getEmail();
			if (email != null) {
				// set gravatar md5
				comment.setGravatar(DigestUtils.md5DigestAsHex(email.getBytes(Constants.CHARSET)));
			}
			comment.setAdmin(false);

		} else {
			// 管理员回复无需设置评论用户信息
			comment.setEmail(null);
			comment.setNickname(null);
			comment.setAdmin(true);
			comment.setWebsite(null);
		}

		comment.setParentPath(parentPath);
		comment.setCommentDate(new Timestamp(now));
		comment.setEditor(config.getEditor());

		boolean check = config.getCheck() && !Environment.isLogin();
		comment.setStatus(check ? CommentStatus.CHECK : CommentStatus.NORMAL);

		comment.setParent(parent);
		completeComment(comment);

		commentDao.insert(comment);

		if (commentEmailNotifySupport != null) {
			threadPoolTaskExecutor.execute(() -> commentEmailNotifySupport.handle(comment));
		}
		return comment;
	}

	@Transactional(readOnly = true)
	public CommentPageResult queryComment(CommentQueryParam param) {
		param.setPageSize(config.getPageSize());
		if (!param.complete()) {
			return new CommentPageResult(param, 0, Collections.emptyList(), new CommentConfig(config));
		}
		CommentModule module = param.getModule();

		if (!doValidateBeforeQuery(module)) {
			return new CommentPageResult(param, 0, Collections.emptyList(), new CommentConfig(config));
		}

		int count;
		switch (config.getCommentMode()) {
		case TREE:
			count = commentDao.selectCountWithTree(param);
			break;
		default:
			count = commentDao.selectCountWithList(param);
			break;
		}
		int pageSize = config.getPageSize();
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
		List<Comment> datas;
		switch (config.getCommentMode()) {
		case TREE:
			datas = commentDao.selectPageWithTree(param);
			for (Comment comment : datas) {
				completeComment(comment);
			}
			datas = handlerTree(datas, config);
			break;
		default:
			datas = commentDao.selectPageWithList(param);
			for (Comment comment : datas) {
				completeComment(comment);
			}
			break;
		}
		return new CommentPageResult(param, count, datas, new CommentConfig(config));
	}

	/**
	 * 删除评论
	 * 
	 * @param ip
	 * @param referenceId
	 * @throws LogicException
	 */
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void deleteComment(CommentModule module) {
		commentDao.deleteByModule(module);
	}

	/**
	 * 查询某空间下 某个模块类型的最近的评论
	 * 
	 * @param type
	 *            模块类型
	 * @param space
	 *            空间
	 * @param limit
	 *            数目限制
	 * @param queryAdmin
	 *            是否包含管理员
	 * @return
	 */
	@Transactional(readOnly = true)
	public List<Comment> queryLastComments(ModuleType type, Space space, int limit, boolean queryAdmin) {
		return commentDao.selectLastComments(type, space, limit, Environment.isLogin(), queryAdmin);
	}

	@EventListener
	public void handleArticleEvent(ArticleEvent articleEvent) {
		if (articleEvent.getEventType().equals(EventType.DELETE)) {
			List<Article> articles = articleEvent.getArticles();
			for (Article article : articles) {
				CommentModule module = new CommentModule(ModuleType.ARTICLE, article.getId());
				deleteComment(module);
			}
		}
	}

	@EventListener
	public void handleUserPageEvent(UserPageEvent event) {
		if (event.getType().equals(EventType.DELETE)) {
			deleteComment(new CommentModule(ModuleType.USERPAGE, event.getDeleted().getId()));
		}
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
	public List<Comment> queryConversations(CommentModule module, Integer id) throws LogicException {
		if (!doValidateBeforeQuery(module)) {
			return Collections.emptyList();
		}
		Comment comment = commentDao.selectById(id);
		if (comment == null) {
			throw new LogicException("comment.notExists", "评论不存在");
		}
		if (!comment.getCommentModule().equals(module)) {
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
	 * 获取最终的跳转地址
	 * 
	 * @param module
	 *            评论模块
	 * @return
	 */
	@Transactional(readOnly = true)
	public Optional<String> getLink(CommentModule module) {
		String link = null;
		switch (module.getType()) {
		case ARTICLE:
			Article article = articleCache.getArticle(module.getId());
			if (article != null) {
				link = urlHelper.getUrls().getUrl(article);
			}
			break;
		case USERPAGE:
			UserPage userPage = userPageDao.selectById(module.getId());
			if (userPage != null) {
				link = urlHelper.getUrls().getUrl(userPage);
			}
			break;
		default:
			break;
		}
		return Optional.ofNullable(link);
	}

	@Override
	@Transactional(readOnly = true)
	public Map<Integer, Integer> queryArticlesCommentCount(List<Integer> ids) {
		List<CommentModule> modules = ids.stream().map(id -> new CommentModule(ModuleType.ARTICLE, id))
				.collect(Collectors.toList());
		List<ModuleCommentCount> counts = commentDao.selectCommentCounts(modules);
		// TODO Collectors.toMap ???
		Map<Integer, Integer> map = Maps.newHashMap();
		for (ModuleCommentCount count : counts) {
			map.put(count.getModule().getId(), count.getComments());
		}
		return map;
	}

	@Override
	@Transactional(readOnly = true)
	public OptionalInt queryArticleCommentCount(Integer id) {
		ModuleCommentCount count = commentDao.selectCommentCount(new CommentModule(ModuleType.ARTICLE, id));
		return count == null ? OptionalInt.empty() : OptionalInt.of(count.getComments());
	}

	@Override
	@Transactional(readOnly = true)
	public int queryArticlesTotalCommentCount(Space space, boolean queryPrivate) {
		return commentDao.selectTotalCommentCount(ModuleType.ARTICLE, space, queryPrivate);
	}

	/**
	 * 获取评论链接
	 * 
	 * @param comment
	 * @return
	 */
	protected String getCommentLink(Comment comment) {
		CommentModule module = comment.getCommentModule();
		return new StringBuilder(urlHelper.getUrl()).append("/comment/link/")
				.append(module.getType().name().toLowerCase()).append("/").append(module.getId()).toString();
	}

	private boolean doValidateBeforeQuery(CommentModule module) {
		boolean valid = false;
		switch (module.getType()) {
		case ARTICLE:
			valid = doValidaBeforeQueryArticleComment(module.getId());
			break;
		case USERPAGE:
			valid = doValidaBeforeQueryUserPageComment(module.getId());
			break;
		default:
			throw new SystemException("无效的ModuleType:" + module.getType());
		}
		return valid;
	}

	private boolean doValidaBeforeQueryArticleComment(Integer moduleId) {
		Article article = articleCache.getArticle(moduleId);
		if (article == null || !article.isPublished()) {
			return false;
		}
		if (article.isPrivate()) {
			Environment.doAuthencation();
		}
		if (!Environment.match(article.getSpace())) {
			return false;
		}
		lockManager.openLock(article);
		return true;
	}

	private boolean doValidaBeforeQueryUserPageComment(Integer moduleId) {
		UserPage page = userPageDao.selectById(moduleId);
		if (page == null || !Environment.match(page.getSpace())) {
			return false;
		}
		return true;
	}

	private void doArticleCommentValid(Integer moduleId) throws LogicException {
		Article article = articleCache.getArticle(moduleId);
		// 博客不存在
		if (article == null || !Environment.match(article.getSpace()) || !article.isPublished()) {
			throw new LogicException("article.notExists", "文章不存在");
		}
		// 如果私人文章并且没有登录
		if (article.isPrivate()) {
			Environment.doAuthencation();
		}
		if (!article.getAllowComment() && !Environment.isLogin()) {
			throw new LogicException("article.notAllowComment", "文章不允许被评论");
		}
		lockManager.openLock(article);
	}

	private void doUserPageCommmentValid(Integer moduleId) throws LogicException {
		UserPage page = userPageDao.selectById(moduleId);
		if (page == null) {
			throw new LogicException("page.user.notExists", "页面不存在");
		}
		if (!page.getAllowComment() && !Environment.isLogin()) {
			throw new LogicException("page.notAllowComment", "页面不允许评论");
		}
	}

	private void setContent(Comment comment, CommentConfig commentConfig) throws LogicException {
		String content = comment.getContent();
		if (Editor.HTML.equals(comment.getEditor())) {
			content = htmlClean.clean(content);
		} else {
			content = HtmlEscapers.htmlEscaper().escape(content);

			String parsed = htmlClean.clean(markdown2Html.toHtml(content));
			validContentLength(parsed);
		}
		validContentLength(content);
		comment.setContent(content);
	}

	private void validContentLength(String content) throws LogicException {
		if (Validators.isEmptyOrNull(content, true)) {
			throw new LogicException("comment.content.blank");
		}
		// 再次检查content的长度
		if (content.length() > MAX_COMMENT_LENGTH) {
			throw new LogicException("comment.content.toolong", "回复的内容不能超过" + MAX_COMMENT_LENGTH + "个字符",
					MAX_COMMENT_LENGTH);
		}
	}

	private List<Comment> buildTree(List<Comment> comments) {
		CollectFilteredFilter filter = new CollectFilteredFilter(null);
		List<Comment> roots = Lists.newArrayList();
		comments.stream().filter(filter).collect(Collectors.toList())
				.forEach(comment -> roots.add(pickByParent(comment, filter.rests)));
		return roots;
	}

	private Comment pickByParent(Comment parent, List<Comment> comments) {
		Objects.requireNonNull(parent);
		CollectFilteredFilter filter = new CollectFilteredFilter(parent);
		List<Comment> children = comments.stream().filter(filter).collect(Collectors.toList());
		children.forEach(child -> pickByParent(child, filter.rests));
		parent.setChildren(children);
		return parent;
	}

	private List<Comment> handlerTree(List<Comment> comments, CommentConfig config) {
		if (comments.isEmpty()) {
			return comments;
		}
		List<Comment> tree = buildTree(comments);
		tree.sort(config.getAsc() ? ascCommentComparator : descCommentComparator);
		return tree;
	}

	private void completeComment(Comment comment) {
		if (comment.getEditor().equals(Editor.MD)) {
			String html = markdown2Html.toHtml(comment.getContent());
			comment.setContent(htmlClean.clean(html));
		}
		comment.setUrl(getCommentLink(comment));
		Comment p = comment.getParent();
		if (p != null) {
			fillComment(p);
		}
		fillComment(comment);
	}

	private void fillComment(Comment comment) {
		if (comment.getAdmin() == null || !comment.getAdmin()) {
			return;
		}
		User user = UserConfig.get();
		comment.setNickname(user.getName());
		String email = user.getEmail();
		comment.setEmail(email);
		comment.setWebsite(urlHelper.getUrl());
		comment.setGravatar(user.getGravatar());
	}

	private void loadConfig() {
		config = new CommentConfig();
		config.setEditor(Editor.valueOf(pros.getProperty(COMMENT_EDITOR, "MD")));
		config.setAsc(Boolean.parseBoolean(pros.getProperty(COMMENT_ASC, "true")));
		config.setCheck(Boolean.parseBoolean(pros.getProperty(COMMENT_CHECK, "false")));
		String commentMode = pros.getProperty(COMMENT_MODE);
		if (commentMode == null) {
			config.setCommentMode(CommentMode.LIST);
		} else {
			config.setCommentMode(CommentMode.valueOf(commentMode));
		}
		config.setLimitCount(Integer.parseInt(pros.getProperty(COMMENT_LIMIT_COUNT, "10")));
		config.setLimitSec(Integer.parseInt(pros.getProperty(COMMENT_LIMIT_SEC, "60")));
		config.setPageSize(Integer.parseInt(pros.getProperty(COMMENT_PAGESIZE, "10")));
	}

	public void setHtmlClean(HtmlClean htmlClean) {
		this.htmlClean = htmlClean;
	}

	public void setCommentChecker(CommentChecker commentChecker) {
		this.commentChecker = commentChecker;
	}

	public void setCommentEmailNotifySupport(CommentEmailNotifySupport commentEmailNotifySupport) {
		this.commentEmailNotifySupport = commentEmailNotifySupport;
	}

	private final class CollectFilteredFilter implements Predicate<Comment> {
		private final Comment parent;
		private List<Comment> rests = Lists.newArrayList();

		public CollectFilteredFilter(Comment parent) {
			this.parent = parent;
		}

		@Override
		public boolean test(Comment t) {
			if (Objects.equals(parent, t.getParent())) {
				return true;
			}
			rests.add(t);
			return false;
		}
	}

}
