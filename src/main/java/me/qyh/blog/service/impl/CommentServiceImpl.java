package me.qyh.blog.service.impl;

import java.io.InputStream;
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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.mail.internet.MimeMessage;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import me.qyh.blog.config.Constants;
import me.qyh.blog.config.Limit;
import me.qyh.blog.config.UrlHelper;
import me.qyh.blog.dao.ArticleDao;
import me.qyh.blog.dao.CommentDao;
import me.qyh.blog.dao.OauthUserDao;
import me.qyh.blog.dao.UserDao;
import me.qyh.blog.entity.Article;
import me.qyh.blog.entity.Article.CommentConfig;
import me.qyh.blog.entity.Comment;
import me.qyh.blog.entity.Comment.CommentStatus;
import me.qyh.blog.entity.Space;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.exception.SystemException;
import me.qyh.blog.input.HtmlClean;
import me.qyh.blog.message.Messages;
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
public class CommentServiceImpl implements CommentService, InitializingBean {

	private static final Logger logger = LoggerFactory.getLogger(CommentServiceImpl.class);

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

	private MessageProcessor messageProcessor;

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
		CommentConfig config = article.getCommentConfig();
		switch (config.getCommentMode()) {
		case TREE:
			count = commentDao.selectCountWithTree(param);
			break;
		default:
			count = commentDao.selectCountWithList(param);
			break;
		}
		if (count == 0) {
			return new PageResult<>(param, 0, Collections.emptyList());
		}
		boolean asc = config.getAsc();
		if (param.getCurrentPage() <= 0) {
			if (asc) {
				// 查询最后一页
				int pageSize = configService.getPageSizeConfig().getCommentPageSize();
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
		if (!article.getCommentConfig().getAllowComment() && (UserContext.get() == null || user.getAdmin())) {
			throw new LogicException("article.notAllowComment", "文章不允许被评论");
		}
		// 如果私人文章并且没有登录
		if (article.isPrivate() && UserContext.get() == null) {
			throw new AuthencationException();
		}
		CommentConfig config = article.getCommentConfig();
		if (UserContext.get() == null || !Boolean.TRUE.equals(user.getAdmin())) {
			// 检查频率
			Limit limit = config.getLimit();
			long start = now - limit.getUnit().toMillis(limit.getTime());
			int count = commentDao.selectCountByUserAndDatePeriod(new Timestamp(start), new Timestamp(now), user) + 1;
			if (count > limit.getLimit()) {
				InvalidCount invalidCount = invalidCountMap.increase(user, now);
				logger.debug("用户" + user + "非法点击:" + invalidCount.count.get() + "..."
						+ DateFormatUtils.format(now, "yyyy-MM-dd HH:mm:ss"));
				if (invalidCount.reach(now)) {
					logger.debug("用户" + user + "被放置到非法用户中" + DateFormatUtils.format(now, "yyyy-MM-dd HH:mm:ss"));
					invalidUserMap.put(user, now);
					invalidCountMap.remove(user);
				} else if (invalidCount.overtime(now)) {
					logger.debug("用户" + user + "非法点击数过期");
					invalidCountMap.put(user, new InvalidCount(now, 1));
				}
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

		if ((!user.getAdmin() && parent == null) || (!user.getAdmin() && (parent.getUser().getAdmin()))
				&& messageProcessor != null && UserContext.get() == null) {
			comment.setArticle(article);// 用来获取文章链接
			messageProcessor.add(comment);
		}

		return comment;
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

		public InvalidCount(long start, int count) {
			this.start = start;
			this.count = new AtomicInteger(count);
		}

		public boolean reach(long now) {
			return (now - start) / 1000 <= invalidLimitSecond && count.get() >= invalidLimitCount;
		}

		public boolean overtime(long now) {
			return (now - start) / 1000 > invalidLimitSecond;
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
		Executors.newScheduledThreadPool(1).scheduleAtFixedRate(new Runnable() {

			@Override
			public void run() {
				invalidUserMap.removeOvertimes();
				invalidCountMap.removeOvertimes();
			}
		}, invalidClearSecond, invalidClearSecond, TimeUnit.SECONDS);

	}

	private boolean isInvalidUser(OauthUser user) {
		if (UserContext.get() != null) {
			return false;
		}
		Long start = invalidUserMap.get(user);
		if (start != null && (System.currentTimeMillis() - start) / 1000 <= invalidSecond) {
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
			while (true) {
				Long old = map.get(user);
				if (old == null) {
					old = map.putIfAbsent(user, time);
					if (old == null) {
						return;
					}
				}
				if (map.replace(user, old, time)) {
					return;
				}
			}
		}

		public void removeOvertimes() {
			Iterator<Map.Entry<OauthUser, Long>> entryIterator = map.entrySet().iterator();
			while (entryIterator.hasNext()) {
				Map.Entry<OauthUser, Long> entry = entryIterator.next();
				Long time = entry.getValue();
				if (time != null && ((System.currentTimeMillis() - time) / 1000) > invalidSecond) {
					entryIterator.remove();
				}
			}
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
			while (true) {
				InvalidCount oldCount = map.get(user);
				if (oldCount == null) {
					oldCount = map.putIfAbsent(user, count);
					if (oldCount == null) {
						return;
					}
				}
				if (map.replace(user, oldCount, count)) {
					return;
				}
			}
		}

		public void remove(OauthUser user) {
			map.remove(user);
		}

		public InvalidCount increase(OauthUser user, long now) {
			outer: while (true) {
				InvalidCount oldCount = map.get(user);
				if (oldCount == null) {
					InvalidCount toPut = new InvalidCount(now);
					// 如果用户没有开始计数，那么设置为0
					oldCount = map.putIfAbsent(user, toPut);
					if (oldCount == null) {
						return toPut;
					}
				}

				while (true) {
					int oldValue = oldCount.count.get();
					if (oldValue == 0L) {
						InvalidCount toPut = new InvalidCount(now, 1);
						if (map.replace(user, oldCount, toPut)) {
							return toPut;
						}
						continue outer;
					}

					int newValue = oldValue + 1;
					if (oldCount.count.compareAndSet(oldValue, newValue)) {
						return oldCount;
					}
				}
			}
		}

		public void removeOvertimes() {
			long now = System.currentTimeMillis();
			Iterator<Map.Entry<OauthUser, InvalidCount>> entryIterator = map.entrySet().iterator();
			while (entryIterator.hasNext()) {
				Map.Entry<OauthUser, InvalidCount> entry = entryIterator.next();
				InvalidCount count = entry.getValue();
				if (count != null && count.overtime(now)) {
					entryIterator.remove();
				}
			}
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

	public void setMessageProcessor(MessageProcessor messageProcessor) {
		this.messageProcessor = messageProcessor;
	}

	/**
	 * 用来向管理员发送评论|回复通知邮件
	 * <p>
	 * <strong>删除评论不会对邮件的发送造成影响，即如果发送队列中或者待发送列表中的一条评论已经被删除，那么它将仍然被发送</strong>
	 * </p>
	 * 
	 * @author Administrator
	 *
	 */
	public static class MessageProcessor implements InitializingBean {
		private ConcurrentLinkedQueue<Comment> toProcesses = new ConcurrentLinkedQueue<>();
		private List<Comment> toSend = Collections.synchronizedList(new ArrayList<Comment>());
		private MailTemplateEngine mailTemplateEngine = new MailTemplateEngine();
		private Resource mailTemplateResource = new ClassPathResource(
				"me/qyh/blog/service/impl/defaultMailTemplate.html");
		private String mailTemplate;
		private String mailSubject;

		/**
		 * 每隔5秒从评论队列中获取评论放入待发送列表
		 */
		private static final Integer MESSAGE_PROCESS_SEC = 5;

		/**
		 * 如果待发送列表中有10或以上的评论，立即发送邮件
		 */
		private static final Integer MESSAGE_TIP_COUNT = 10;

		/**
		 * 如果发送列表中存在待发送评论，但是数量始终没有达到10条，那么每隔300秒会发送邮件同时清空发送列表
		 */
		private static final Integer MESSAGE_PROCESS_PERIOD_SEC = 300;

		private int messageProcessSec = MESSAGE_PROCESS_SEC;
		private int messageProcessPeriodSec = MESSAGE_PROCESS_PERIOD_SEC;
		private int messageTipCount = MESSAGE_TIP_COUNT;

		/**
		 * 邮件发送，用来提示管理员有新的回复或者评论
		 */
		@Autowired
		private JavaMailSender javaMailSender;
		@Autowired
		private UrlHelper urlHelper;
		@Autowired
		private UserDao userDao;
		@Autowired
		private Messages messages;

		public void shutdown() {
			synchronized (toSend) {
				if (!toSend.isEmpty()) {
					sendMail();
					toSend.clear();
				}
			}
		}

		void add(Comment comment) {
			toProcesses.add(comment);
		}

		void sendMail() {
			Context context = new Context();
			context.setVariable("urls", urlHelper.getUrls());
			context.setVariable("comments", toSend);
			context.setVariable("messages", messages);
			final String mailContent = mailTemplateEngine.process(mailTemplate, context);
			MimeMessagePreparator preparator = new MimeMessagePreparator() {

				@Override
				public void prepare(MimeMessage mimeMessage) throws Exception {
					MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, Constants.CHARSET.name());
					helper.setText(mailContent, true);
					helper.setTo(userDao.select().getEmail());
					helper.setSubject(mailSubject);
					mimeMessage.setFrom();
				}
			};
			javaMailSender.send(preparator);
		}

		private final class MailTemplateEngine extends TemplateEngine {
			public MailTemplateEngine() {
				setTemplateResolver(new StringTemplateResolver());
			}
		}

		public MessageProcessor() {
		}

		@Override
		public void afterPropertiesSet() throws Exception {
			if (mailSubject == null) {
				throw new SystemException("邮件标题不能为空");
			}
			InputStream is = null;
			try {
				is = mailTemplateResource.getInputStream();
				mailTemplate = IOUtils.toString(is, Constants.CHARSET);
			} finally {
				IOUtils.closeQuietly(is);
			}
			if (messageProcessPeriodSec <= 0)
				messageProcessPeriodSec = MESSAGE_PROCESS_PERIOD_SEC;
			if (messageProcessSec <= 0)
				messageProcessSec = MESSAGE_PROCESS_SEC;
			if (messageTipCount <= 0)
				messageTipCount = MESSAGE_TIP_COUNT;
			Executors.newScheduledThreadPool(1).scheduleAtFixedRate(new Runnable() {

				@Override
				public void run() {
					synchronized (toSend) {
						int size = toSend.size();
						for (Iterator<Comment> iterator = toProcesses.iterator(); iterator.hasNext();) {
							Comment toProcess = iterator.next();
							toSend.add(toProcess);
							size++;
							iterator.remove();
							if (size >= messageTipCount) {
								logger.debug("发送列表尺寸达到" + messageTipCount + "立即发送邮件通知");
								sendMail();
								toSend.clear();
								break;
							}
						}
					}
				}
			}, messageProcessSec, messageProcessSec, TimeUnit.SECONDS);

			Executors.newScheduledThreadPool(1).scheduleAtFixedRate(new Runnable() {

				@Override
				public void run() {
					synchronized (toSend) {
						if (!toSend.isEmpty()) {
							logger.debug("待发送列表不为空，将会发送邮件，无论发送列表是否达到" + messageTipCount);
							sendMail();
							toSend.clear();
						}
					}
				}
			}, messageProcessPeriodSec, messageProcessPeriodSec, TimeUnit.SECONDS);
		}

		public void setMailTemplateResource(Resource mailTemplateResource) {
			this.mailTemplateResource = mailTemplateResource;
		}

		public void setMailSubject(String mailSubject) {
			this.mailSubject = mailSubject;
		}

		public void setMessageProcessSec(int messageProcessSec) {
			this.messageProcessSec = messageProcessSec;
		}

		public void setMessageProcessPeriodSec(int messageProcessPeriodSec) {
			this.messageProcessPeriodSec = messageProcessPeriodSec;
		}

		public void setMessageTipCount(int messageTipCount) {
			this.messageTipCount = messageTipCount;
		}
	}

}
