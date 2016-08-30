package me.qyh.blog.service.impl;

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
		if (article == null) {
			throw new LogicException(new Message("article.notExists", "文章不存在"));
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

}
