package me.qyh.blog.ui.widget;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;

import me.qyh.blog.config.UrlHelper;
import me.qyh.blog.entity.Article;
import me.qyh.blog.entity.Comment;
import me.qyh.blog.entity.Space;
import me.qyh.blog.oauth2.OauthUser;
import me.qyh.blog.service.CommentService;
import me.qyh.blog.ui.Params;

/**
 * 文章归档
 * 
 * @author Administrator
 *
 */
public class LastCommentsWidgetHandler extends SysWidgetHandler implements InitializingBean {

	@Autowired
	private UrlHelper urlHelper;

	public LastCommentsWidgetHandler(Integer id, String name, String dataName, Resource tplRes) {
		super(id, name, dataName, tplRes);
	}

	private static final Integer DEFAULT_LIMIT = 10;
	private int limit = DEFAULT_LIMIT;

	@Autowired
	private CommentService commentService;

	@Override
	public Object getWidgetData(Space space, Params params) {
		return commentService.queryLastComments(space, limit);
	}

	@Override
	public Object buildWidgetDataForTest() {
		List<Comment> comments = new ArrayList<Comment>();
		Comment comment = new Comment();
		comment.setCommentDate(Timestamp.valueOf(LocalDateTime.now()));
		comment.setContent("测试内容");
		OauthUser user = new OauthUser();
		user.setAdmin(false);
		user.setAvatar(urlHelper.getUrl() + "/static/img/guest.png");
		user.setServerId("qq");
		user.setServerName("QQ");
		user.setNickname("测试");
		Article article = new Article();
		article.setId(1);
		article.setTitle("测试文章标题");
		comment.setArticle(article);
		comment.setId(1);
		comment.setUser(user);
		comments.add(comment);
		return comments;
	}

	@Override
	public boolean canProcess(Space space, Params params) {
		return true;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (limit <= 0) {
			limit = DEFAULT_LIMIT;
		}
	}
}
