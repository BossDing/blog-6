package me.qyh.blog.comment.module;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collectors;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import me.qyh.blog.comment.dao.ArticleCommentDao;
import me.qyh.blog.comment.dao.CommentDao;
import me.qyh.blog.comment.entity.Comment;
import me.qyh.blog.comment.entity.CommentModule;
import me.qyh.blog.comment.service.CommentService;
import me.qyh.blog.comment.vo.LastArticleComment;
import me.qyh.blog.comment.vo.ModuleCommentCount;
import me.qyh.blog.core.config.UrlHelper;
import me.qyh.blog.core.context.Environment;
import me.qyh.blog.core.dao.ArticleDao;
import me.qyh.blog.core.entity.Article;
import me.qyh.blog.core.entity.Editor;
import me.qyh.blog.core.entity.Space;
import me.qyh.blog.core.entity.User;
import me.qyh.blog.core.event.ArticleEvent;
import me.qyh.blog.core.event.EventType;
import me.qyh.blog.core.exception.LogicException;
import me.qyh.blog.core.message.Message;
import me.qyh.blog.core.message.Messages;
import me.qyh.blog.core.service.LockManager;
import me.qyh.blog.core.service.impl.ArticleCache;

@Component
public class ArticleCommentModuleHandler extends CommentModuleHandler implements InitializingBean{

	@Autowired
	private ArticleCache articleCache;
	@Autowired
	private LockManager lockManager;
	@Autowired
	private CommentDao commentDao;
	@Autowired
	private ArticleCommentDao articleCommentDao;
	@Autowired
	private ArticleDao articleDao;
	
	@Autowired
	private CommentService commentService;
	
	@Autowired
	private UrlHelper urlHelper;
	
	@Autowired
	private Messages messages;
	
	private static final String MODULE_NAME = "article";
	
	private static final Message PROTECTED_COMMENT_MD = new Message("comment.protected","\\*\\*\\*\\*\\*\\*");
	private static final Message PROTECTED_COMMENT_HTML = new Message("comment.protected","******");

	public ArticleCommentModuleHandler() {
		super(MODULE_NAME);
	}

	@Override
	public void doValidateBeforeInsert(Integer id) throws LogicException {
		Article article = articleCache.getArticle(id, false);
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

	@Override
	public boolean doValidateBeforeQuery(Integer id) {
		Article article = articleCache.getArticle(id, false);
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
		lockManager.openLock(article.getSpace());
		return true;
	}

	@Override
	public Map<Integer, Integer> queryCommentNums(Collection<Integer> ids) {

		List<CommentModule> modules = ids.stream().map(id -> new CommentModule(MODULE_NAME, id))
				.collect(Collectors.toList());
		return commentDao.selectCommentCounts(modules).stream()
				.collect(Collectors.toMap(ModuleCommentCount::getModuleId, ModuleCommentCount::getComments));
	}

	@Override
	public OptionalInt queryCommentNum(Integer id) {
		ModuleCommentCount count = commentDao.selectCommentCount(new CommentModule(MODULE_NAME, id));
		return count == null ? OptionalInt.empty() : OptionalInt.of(count.getComments());
	}

	@Override
	public int queryCommentNum(Space space, boolean queryPrivate) {
		return articleCommentDao.selectTotalCommentCount(space, queryPrivate);
	}

	@Override
	public Map<Integer, Object> getReferences(Collection<Integer> ids) {
		List<Article> articles = articleDao.selectSimpleByIds(ids);
		return articles.stream().collect(Collectors.toMap(Article::getId, art->art));
	}

	@EventListener
	public void handleArticleEvent(ArticleEvent articleEvent) {
		if (articleEvent.getEventType().equals(EventType.DELETE)) {
			List<Article> articles = articleEvent.getArticles();
			for (Article article : articles) {
				CommentModule module = new CommentModule(MODULE_NAME, article.getId());
				commentDao.deleteByModule(module);
			}
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		commentService.addCommentModuleHandler(this);	
	}

	@Override
	public List<Comment> queryLastComments(Space space, int limit, boolean queryPrivate, boolean queryAdmin) {
		
		User user = Environment.getUser();
		List<Comment> comments = articleCommentDao.selectLastComments(space, limit, queryPrivate, queryAdmin);
		for(Comment comment : comments){
			LastArticleComment lac = (LastArticleComment)comment;
			if(user == null && lac.getArticle() != null && lac.getArticle().hasLock()){
				comment.setContent(messages.getMessage(Editor.MD.equals(comment.getEditor()) ? PROTECTED_COMMENT_MD : PROTECTED_COMMENT_HTML));
			}
		}
		return comments;
	}

	@Override
	public Optional<String> getUrl(Integer id) {
		Article article = articleCache.getArticle(id, false);
		return article == null ? Optional.empty() : Optional.of(urlHelper.getUrls().getUrl(article));
	}

}
