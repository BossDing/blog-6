package me.qyh.blog.comment.base;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.util.DigestUtils;
import org.springframework.web.util.HtmlUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import me.qyh.blog.comment.base.BaseComment.CommentStatus;
import me.qyh.blog.config.Constants;
import me.qyh.blog.config.Limit;
import me.qyh.blog.config.UrlHelper;
import me.qyh.blog.config.UserConfig;
import me.qyh.blog.entity.User;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.exception.SystemException;
import me.qyh.blog.pageparam.PageQueryParam;
import me.qyh.blog.pageparam.PageResult;
import me.qyh.blog.security.UserContext;
import me.qyh.blog.security.input.HtmlClean;
import me.qyh.blog.util.Validators;

public class CommentSupport<T extends BaseComment<T>, E extends BaseCommentDao<T>> implements InitializingBean {

	private CommentContentChecker commentContentChecker;
	private CommentUserChecker commentUserChecker;
	private HtmlClean htmlClean;
	protected E commentDao;

	@Autowired
	private TaskExecutor taskExecutor;
	@Autowired
	protected UrlHelper urlHelper;

	/**
	 * 评论配置项
	 */
	protected static final String COMMENT_MODE = "commentConfig.commentMode";
	protected static final String COMMENT_ASC = "commentConfig.commentAsc";
	protected static final String COMMENT_ALLOW_HTML = "commentConfig.commentAllowHtml";
	protected static final String COMMENT_LIMIT_SEC = "commentConfig.commentLimitSec";
	protected static final String COMMENT_LIMIT_COUNT = "commentConfig.commentLimitCount";
	protected static final String COMMENT_CHECK = "commentConfig.commentCheck";
	protected static final String COMMENT_PAGESIZE = "commentConfig.commentPageSize";

	private final CommentComparator ascCommentComparator = new CommentComparator();
	protected final Comparator<T> descCommentComparator = new Comparator<T>() {

		@Override
		public int compare(T o1, T o2) {
			return -ascCommentComparator.compare(o1, o2);
		}
	};

	/**
	 * 为了保证一个树结构，这里采用 path来纪录层次结构
	 * {@link http://stackoverflow.com/questions/4057947/multi-tiered-comment-replies-display-and-storage}.
	 * 同时为了走索引，只能限制它为255个字符，由于id为数字的原因，实际上一般情况下很难达到255的长度(即便id很大)，所以这里完全够用
	 */
	private static final int PATH_MAX_LENGTH = 255;
	private static final int MAX_COMMENT_LENGTH = BaseCommentValidator.MAX_COMMENT_LENGTH;

	private CommentEmailNotifySupport<T> emailSupport;

	private final class CommentComparator implements Comparator<T> {
		@Override
		public int compare(T o1, T o2) {
			int compare = o1.getCommentDate().compareTo(o2.getCommentDate());
			if (compare == 0) {
				return o1.getId().compareTo(o2.getId());
			}
			return compare;
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
		if (commentUserChecker == null) {
			commentUserChecker = new DefaultCommentUserChecker();
		}
	}

	protected void sendEmail(T t) {
		if (emailSupport != null) {
			taskExecutor.execute(() -> {
				emailSupport.handle(t);
			});
		}
	}

	protected void checkComment(Integer id) throws LogicException {
		T comment = commentDao.selectById(id);// 查询父评论
		if (comment == null) {
			throw new LogicException("comment.notExists", "评论不存在");
		}
		if (!comment.isChecking()) {
			throw new LogicException("comment.checked", "评论审核过了");
		}
		commentDao.updateStatusToNormal(comment);
	}

	protected void deleteComment(Integer id) throws LogicException {
		T comment = commentDao.selectById(id);
		if (comment == null) {
			throw new LogicException("comment.notExists", "评论不存在");
		}
		commentDao.deleteByPath(comment.getParentPath() + comment.getId());
		commentDao.deleteById(id);
	}

	protected CommentPageResult<T> queryComment(BaseCommentQueryParam param, CommentConfig config) {
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
			return new CommentPageResult<>(param, 0, Collections.emptyList(), new CommentConfig(config));
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
		List<T> datas = null;
		switch (config.getCommentMode()) {
		case TREE:
			datas = commentDao.selectPageWithTree(param);
			for (T comment : datas) {
				completeComment(comment);
			}
			datas = handlerTree(datas, config);
			break;
		default:
			datas = commentDao.selectPageWithList(param);
			for (T comment : datas) {
				completeComment(comment);
			}
			break;
		}
		return new CommentPageResult<>(param, count, datas, new CommentConfig(config));
	}

	protected void insertComment(T comment, CommentConfig config) throws LogicException {
		long now = System.currentTimeMillis();
		String ip = comment.getIp();
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

		this.commentContentChecker.doCheck(comment.getContent(), config.getAllowHtml());

		String parentPath = "/";
		// 判断是否存在父评论
		T parent = comment.getParent();
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

		T last = commentDao.selectLast(comment);
		if (last != null && last.getContent().equals(comment.getContent())) {
			throw new LogicException("comment.content.same", "已经回复过相同的评论了");
		}

		if (UserContext.get() == null) {
			String email = comment.getEmail();

			commentUserChecker.doCheck(comment.getNickname(), email, comment.getWebsite());
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

		boolean check = config.getCheck() && (UserContext.get() == null);
		comment.setStatus(check ? CommentStatus.CHECK : CommentStatus.NORMAL);

		comment.setParent(parent);
		completeComment(comment);

		commentDao.insert(comment);
	}

	private void setContent(T comment, CommentConfig commentConfig) throws LogicException {
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

	private List<T> handlerTree(List<T> comments, CommentConfig config) {
		if (comments.isEmpty()) {
			return comments;
		}
		Map<T, List<T>> treeMap = Maps.newHashMap();
		for (T comment : comments) {
			if (comment.isRoot()) {
				if (treeMap.containsKey(comment)) {
					treeMap.put(comment, treeMap.get(comment));
				} else {
					treeMap.put(comment, Lists.newArrayList());
				}
			} else {
				T parent = find(comment.getParents().get(0), comments);
				parent.setId(comment.getParents().get(0));
				if (treeMap.containsKey(parent)) {
					treeMap.get(parent).add(comment);
				} else {
					List<T> _comments = Lists.newArrayList();
					_comments.add(comment);
					treeMap.put(parent, _comments);
				}
			}
		}
		SortedSet<T> keys = Sets.newTreeSet(config.getAsc() ? ascCommentComparator : descCommentComparator);
		keys.addAll(treeMap.keySet());
		List<T> sorted = Lists.newArrayList();
		for (T root : keys) {
			build(root, treeMap.get(root));
			sorted.add(root);
		}
		treeMap.clear();
		keys.clear();
		return sorted;
	}

	private T find(Integer id, List<T> comments) {
		for (T comment : comments) {
			if (comment.getId().equals(id)) {
				return comment;
			}
		}
		return null;
	}

	private void build(T root, List<T> comments) {
		List<T> children = findChildren(root, comments);
		comments.removeAll(children);
		for (Iterator<T> it = children.iterator(); it.hasNext();) {
			T child = it.next();
			build(child, comments);
		}
		root.setChildren(children);
	}

	private List<T> findChildren(T root, List<T> comments) {
		List<T> children = Lists.newArrayList();
		for (T comment : comments) {
			if (root.equals(comment.getParent())) {
				comment.setParent(null);// 防止json死循环
				children.add(comment);
			}
		}
		Collections.sort(children, ascCommentComparator);
		return children;
	}

	private final class DefaultCommentContentChecker implements CommentContentChecker {

		@Override
		public void doCheck(String content, boolean html) throws LogicException {

		}

	}

	private final class DefaultCommentUserChecker extends CommentUserChecker {

		@Override
		protected void checkMore(final String name, final String email, final String website) throws LogicException {
		}

	}

	protected void completeComment(T comment) {
		T p = comment.getParent();
		if (p != null) {
			completeComment(p);
		}
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

	public void setCommentDao(E commentDao) {
		this.commentDao = commentDao;
	}

	public void setCommentContentChecker(CommentContentChecker commentContentChecker) {
		this.commentContentChecker = commentContentChecker;
	}

	public void setCommentUserChecker(CommentUserChecker commentUserChecker) {
		this.commentUserChecker = commentUserChecker;
	}

	public void setHtmlClean(HtmlClean htmlClean) {
		this.htmlClean = htmlClean;
	}

	public void setEmailSupport(CommentEmailNotifySupport<T> emailSupport) {
		this.emailSupport = emailSupport;
	}

	public static final class CommentPageResult<E extends BaseComment<E>> extends PageResult<E> {
		private final CommentConfig commentConfig;

		public CommentPageResult(PageQueryParam param, int totalRow, List<E> datas, CommentConfig commentConfig) {
			super(param, totalRow, datas);
			this.commentConfig = commentConfig;
		}

		public CommentConfig getCommentConfig() {
			return commentConfig;
		}
	}

}
