package me.qyh.blog.ui.widget;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;

import me.qyh.blog.entity.Article;
import me.qyh.blog.entity.Article.ArticleFrom;
import me.qyh.blog.entity.Article.ArticleStatus;
import me.qyh.blog.entity.Article.CommentConfig;
import me.qyh.blog.entity.Article.CommentConfig.CommentMode;
import me.qyh.blog.entity.Editor;
import me.qyh.blog.entity.Space;
import me.qyh.blog.entity.Tag;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.service.ArticleService;
import me.qyh.blog.ui.Params;

public class ArticleWidgetHandler extends SysWidgetHandler {

	public static final String PARAMETER_KEY = "articleId";

	private static final String ATTR_ID = "id";

	public ArticleWidgetHandler(Integer id, String name, String dataName, Resource tplRes) {
		super(id, name, dataName, tplRes);
	}

	@Autowired
	private ArticleService articleService;

	@Override
	public Object getWidgetData(Space space, Params params, Map<String, String> attrs) throws LogicException {
		Integer id = params.get(PARAMETER_KEY, Integer.class);
		if (id == null) {
			// 尝试从属性中获取ID
			String attId = attrs.get(ATTR_ID);
			if (attId != null) {
				try {
					id = Integer.parseInt(attId);
				} catch (Exception e) {
					//
				}
			}
		}
		if (id == null)
			return null;
		Article article = articleService.getArticleForView(id);
		if (article == null ||
		// 文章不存在或者不在目标空间下
				(!space.getAlias().equals(article.getSpace().getAlias()))) {
			throw new LogicException("article.notExists", "文章不存在");
		}
		params.add("article", article);
		return article;
	}

	@Override
	public boolean canProcess(Space space, Params params) {
		if (space == null) {
			return false;
		}
		return true;
	}

	@Override
	public Object buildWidgetDataForTest() {
		Article article = new Article();
		article.setComments(0);
		article.setEditor(Editor.MD);
		article.setContent("#这是预览内容");
		article.setEditor(Editor.MD);
		article.setFrom(ArticleFrom.ORIGINAL);
		article.setHits(10);
		article.setId(1);
		article.setIsPrivate(false);
		article.setLastModifyDate(Timestamp.valueOf(LocalDateTime.now()));
		article.setPubDate(Timestamp.valueOf(LocalDateTime.now()));

		CommentConfig config = new CommentConfig();
		config.setAsc(true);
		config.setAllowHtml(false);
		config.setCommentMode(CommentMode.LIST);
		config.setAllowComment(true);
		article.setCommentConfig(config);

		Space space = new Space();
		space.setId(1);
		space.setAlias("test");
		article.setSpace(space);

		article.setStatus(ArticleStatus.PUBLISHED);
		article.setSummary("这是测试内容");
		article.setTitle("测试内容");
		Set<Tag> tags = new HashSet<Tag>();
		tags.add(new Tag("test"));

		article.setTags(tags);
		return article;
	}
}
