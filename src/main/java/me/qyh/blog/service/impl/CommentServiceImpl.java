package me.qyh.blog.service.impl;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import me.qyh.blog.dao.ArticleDao;
import me.qyh.blog.dao.CommentDao;
import me.qyh.blog.dao.OauthUserDao;
import me.qyh.blog.entity.Article;
import me.qyh.blog.entity.Comment;
import me.qyh.blog.entity.OauthUser;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.message.Message;
import me.qyh.blog.security.AuthencationException;
import me.qyh.blog.security.UserContext;
import me.qyh.blog.service.CommentService;

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

	/**
	 * 为了保证一个树结构，这里采用 path来纪录层次结构
	 * {@link http://stackoverflow.com/questions/4057947/multi-tiered-comment-replies-display-and-storage}.
	 * 同时为了走索引，只能限制它为255个字符，由于id为数字的原因，实际上一般情况下很难达到255的长度(即便id很大)，所以这里完全够用
	 */
	private static final int PATH_MAX_LENGTH = 255;

	@Override
	public void insertComment(Comment comment) throws LogicException {
		OauthUser user = oauthUserDao.selectById(comment.getUser().getId());
		if (user == null) {
			throw new LogicException(new Message("comment.user.notExists", "账户不存在"));
		}
		if (user.isDisabled()) {
			throw new LogicException(new Message("comment.user.ban", "该账户被禁止评论"));
		}
		Article article = articleCache.getArticle(comment.getArticle().getId());
		// 博客不存在
		if (article == null || !article.isPublished()) {
			throw new LogicException(new Message("article.notExists", "文章不存在"));
		}
		if (!article.getAllowComment()) {
			throw new LogicException(new Message("article.notAllowComment", "文章不允许被评论"));
		}
		// 如果私人文章并且没有登录
		if (article.getIsPrivate() && UserContext.get() == null) {
			throw new AuthencationException();
		}
		String parentPath = "/";
		// 判断是否存在父评论
		Comment parent = comment.getParent();
		if (parent != null) {
			parent = commentDao.selectById(parent.getId());// 查询父评论
			if (parent == null) {
				throw new LogicException(new Message("comment.parent.notExists", "父评论不存在"));
			}
			parentPath += parent.getParentPath() + parent.getId() + "/";
		}
		if (parentPath.length() > PATH_MAX_LENGTH) {
			throw new LogicException(new Message("comment.path.toolong", "该回复不能再被回复了"));
		}
		comment.setParentPath(parentPath);
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
			throw new LogicException(new Message("comment.notExists", "父评论不存在"));
		}
		// 查询自评论数目
		int count = commentDao.selectPathCount(comment.getSubPath());
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

	private void sort(List<Comment> comments, boolean asc) {
		Map<Integer, Comment> map = new HashMap<Integer, Comment>();
		for (Comment comment : comments) {
			map.put(comment.getId(), comment);
		}
		Collections.sort(comments, new CommentCompare() {

			@Override
			protected Comment getDetail(Integer id) {
				return map.get(id);
			}

			@Override
			protected boolean asc() {
				return asc;
			}
		});
	}

	public void test() {
		Comment comment1 = new Comment();
		comment1.setId(1);
		comment1.setParentPath("/");
		comment1.setCommentDate(new Timestamp(1));

		Comment comment2 = new Comment();
		comment2.setId(2);
		comment2.setParent(comment1);
		comment2.setParentPath("/1/");
		comment2.setCommentDate(new Timestamp(2));

		Comment comment3 = new Comment();
		comment3.setId(3);
		comment3.setParent(comment2);
		comment3.setParentPath("/1/2/");
		comment3.setCommentDate(new Timestamp(3));

		Comment comment4 = new Comment();
		comment4.setId(4);
		comment4.setParentPath("/");
		comment4.setCommentDate(new Timestamp(4));

		Comment comment5 = new Comment();
		comment5.setId(5);
		comment5.setParent(comment4);
		comment5.setParentPath("/4/");
		comment5.setCommentDate(new Timestamp(5));

		Comment comment6 = new Comment();
		comment6.setId(6);
		comment6.setParent(comment5);
		comment6.setParentPath("/4/5/");
		comment6.setCommentDate(new Timestamp(6));

		Comment comment7 = new Comment();
		comment7.setId(7);
		comment7.setParentPath("/");
		comment7.setCommentDate(new Timestamp(7));

		Comment comment8 = new Comment();
		comment8.setId(8);
		comment8.setParent(comment7);
		comment8.setParentPath("/7/");
		comment8.setCommentDate(new Timestamp(8));

		Comment comment9 = new Comment();
		comment9.setId(9);
		comment9.setParent(comment8);
		comment9.setParentPath("/7/8/");
		comment9.setCommentDate(new Timestamp(9));

		Comment comment10 = new Comment();
		comment10.setId(10);
		comment10.setParent(comment9);
		comment10.setParentPath("/7/8/9/");
		comment10.setCommentDate(new Timestamp(10));

		Comment comment11 = new Comment();
		comment11.setId(11);
		comment11.setParent(comment2);
		comment11.setParentPath("/1/2/");
		comment11.setCommentDate(new Timestamp(11));

		Comment comment12 = new Comment();
		comment12.setId(12);
		comment12.setParent(comment1);
		comment12.setParentPath("/1/");
		comment12.setCommentDate(new Timestamp(12));

		Comment comment13 = new Comment();
		comment13.setId(13);
		comment13.setParent(comment2);
		comment13.setParentPath("/1/2/");
		comment13.setCommentDate(new Timestamp(13));

		List<Comment> comments = Arrays.asList(new Comment[] { comment12, comment13, comment4, comment7, comment10,
				comment5, comment8, comment9, comment1, comment6, comment3, comment2, comment11 });
		sort(comments, true);
		for (Comment comment : comments) {
			System.out.println(comment.getId() + ".." + comment.getParentPath());
		}
	}

	public static void main(String[] ags) {
		new CommentServiceImpl().test();
	}

	private abstract class CommentCompare implements Comparator<Comment> {

		@Override
		public int compare(Comment o1, Comment o2) {
			Integer o1Id = o1.getId();
			Integer o2Id = o2.getId();
			if (o1.isRoot() && !o2.isRoot()) {
				Integer o2RootId = o2.getParents().get(0);
				if (o2RootId.intValue() == o1Id.intValue()) {
					return -1;
				} else {
					int compare = _compare(getDetail(o2RootId), o1);
					return asc() ? -compare : compare;
				}
			}
			if (o1.isRoot() && o2.isRoot()) {
				int compare = _compare(o1, o2);
				return asc() ? compare : -compare;
			}
			if (!o1.isRoot() && o2.isRoot()) {
				Integer o1RootId = o1.getParents().get(0);
				if (o1RootId.intValue() == o2Id.intValue()) {
					return 1;
				} else {
					int compare = _compare(getDetail(o1RootId), o2);
					return asc() ? compare : -compare;
				}
			}
			if (!o1.isRoot() && !o2.isRoot()) {
				List<Integer> p1 = o1.getParents();
				List<Integer> p2 = o2.getParents();
				if (p1.get(0).equals(p2.get(0))) {
					if (o1.getParentPath().equals(o2.getParentPath())) {
						return _compare(o1, o2);
					}
					if (is(o1, o2)) {
						return p1.size() > p2.size() ? 1 : (p1.size() < p2.size() ? -1 : 0);
					} else {
						Integer r1 = p1.size() > 1 ? p1.get(1) : o1Id;
						Integer r2 = p2.size() > 1 ? p2.get(1) : o2Id;
						return _compare(getDetail(r1), getDetail(r2));
					}
				} else {
					// 不在同一棵树下
					int compare = _compare(getDetail(p1.get(0)), getDetail(p2.get(0)));
					return asc() ? compare : -compare;
				}
			}
			return 0;
		}

		private int _compare(Comment o1, Comment o2) {
			int compare = o1.getCommentDate().compareTo(o2.getCommentDate());
			if (compare == 0) {
				return o1.getId().compareTo(o2.getId());
			}
			return compare;
		}

		private boolean is(Comment o1, Comment o2) {
			return o1.getParents().contains(o2.getId()) || o2.getParents().contains(o1.getId());
		}

		protected abstract Comment getDetail(Integer id);

		protected abstract boolean asc();
	}
}
