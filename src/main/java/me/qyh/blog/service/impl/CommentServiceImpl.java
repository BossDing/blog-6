package me.qyh.blog.service.impl;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import me.qyh.blog.config.CommentConfig;
import me.qyh.blog.config.Limit;
import me.qyh.blog.config.Limit.TimePeriod;
import me.qyh.blog.dao.ArticleDao;
import me.qyh.blog.dao.CommentDao;
import me.qyh.blog.dao.OauthUserDao;
import me.qyh.blog.entity.Article;
import me.qyh.blog.entity.Comment;
import me.qyh.blog.entity.OauthUser;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.pageparam.CommentQueryParam;
import me.qyh.blog.pageparam.PageResult;
import me.qyh.blog.security.AuthencationException;
import me.qyh.blog.security.UserContext;
import me.qyh.blog.service.CommentService;
import me.qyh.blog.service.ConfigService;
import me.qyh.blog.web.interceptor.SpaceContext;

@Service
@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
public class CommentServiceImpl implements CommentService {

	@Autowired
	private ArticleCache articleCache;
	@Autowired
	private OauthUserDao oauthUserDao;
	@Autowired
	private CommentDao commentDao;
	@Autowired
	private ArticleDao articleDao;

	@Autowired
	private ConfigService configService;

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

	@Override
	@Transactional(readOnly = true)
	public PageResult<Comment> queryComment(CommentQueryParam param) {
		Article article = articleCache.getArticle(param.getArticle().getId());
		if (article == null || !article.isPublished()) {
			return new PageResult<>(param, 0, Collections.emptyList());
		}
		if (article.getIsPrivate() && UserContext.get() == null) {
			throw new AuthencationException();
		}
		if (!article.getSpace().equals(SpaceContext.get())) {
			return new PageResult<>(param, 0, Collections.emptyList());
		}
		int count = commentDao.selectCount(param);
		List<Comment> datas = commentDao.selectPage(param);
		datas = handle(datas);
		return new PageResult<Comment>(param, count, datas);
	}

	@Override
	public void insertComment(Comment comment) throws LogicException {
		OauthUser user = oauthUserDao.selectById(comment.getUser().getId());
		if (user == null) {
			throw new LogicException("comment.user.notExists", "账户不存在");
		}
		if (user.isDisabled()) {
			throw new LogicException("comment.user.ban", "该账户被禁止评论");
		}
		Article article = articleCache.getArticle(comment.getArticle().getId());
		// 博客不存在
		if (article == null || !article.getSpace().equals(SpaceContext.get()) || !article.isPublished()) {
			throw new LogicException("article.notExists", "文章不存在");
		}
		if (!article.getAllowComment()) {
			throw new LogicException("article.notAllowComment", "文章不允许被评论");
		}
		// 如果私人文章并且没有登录
		if (article.getIsPrivate() && UserContext.get() == null) {
			throw new AuthencationException();
		}
		if (UserContext.get() == null) {
			// 检查频率
			Limit limit = configService.getCommentConfig().getLimit();
			TimePeriod period = limit.getPeriod();
			int count = commentDao.selectCountByUserAndDatePeriod(new Timestamp(period.getStart()),
					new Timestamp(period.getEnd()), user);
			if (count > limit.getLimit()) {
				throw new LogicException("comment.overlimit", "评论太过频繁，请稍作休息");
			}
		}
		String parentPath = "/";
		// 判断是否存在父评论
		Comment parent = comment.getParent();
		if (parent != null) {
			parent = commentDao.selectById(parent.getId());// 查询父评论
			if (parent == null) {
				throw new LogicException("comment.parent.notExists", "父评论不存在");
			}
			parentPath = parent.getParentPath() + parent.getId() + "/";
		}
		if (parentPath.length() > PATH_MAX_LENGTH) {
			throw new LogicException("comment.path.toolong", "该回复不能再被回复了");
		}
		comment.setParentPath(parentPath);
		comment.setCommentDate(Timestamp.valueOf(LocalDateTime.now()));
		commentDao.insert(comment);
		// 是不是每个回复都算评论数？
		if (article.isCacheable()) {
			article.addComments();
		}
		articleDao.updateComments(article.getId(), 1);
	}

	@Override
	public void deleteComment(Integer id) throws LogicException {
		Comment comment = commentDao.selectById(id);// 查询父评论
		if (comment == null) {
			throw new LogicException("comment.notExists", "父评论不存在");
		}
		// 查询自评论数目
		int count = commentDao.selectCountByPath(comment.getSubPath());
		int totalCount = count + 1;
		if (count > 0) {
			commentDao.deleteByPath(comment.getSubPath());
		}
		commentDao.deleteById(id);
		// 更新文章评论数量
		Article article = articleCache.getArticle(comment.getArticle().getId());
		if (article.isCacheable()) {
			article.decrementComment(totalCount);
		}
		articleDao.updateComments(article.getId(), -totalCount);
	}

	protected List<Comment> handle(List<Comment> comments) {
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
		CommentConfig commentConfig = configService.getCommentConfig();
		SortedSet<Comment> keys = new TreeSet<Comment>(
				commentConfig.isAsc() ? ascCommentComparator : descCommentComparator);
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
}
