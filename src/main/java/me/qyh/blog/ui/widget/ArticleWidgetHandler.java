package me.qyh.blog.ui.widget;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;

import me.qyh.blog.entity.Article;
import me.qyh.blog.entity.Article.ArticleFrom;
import me.qyh.blog.entity.Article.ArticleStatus;
import me.qyh.blog.entity.Editor;
import me.qyh.blog.entity.Space;
import me.qyh.blog.entity.Tag;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.message.Message;
import me.qyh.blog.service.ArticleService;
import me.qyh.blog.ui.Params;

public class ArticleWidgetHandler extends SysWidgetHandler {

	public static final String PARAMETER_KEY = "articleId";

	public ArticleWidgetHandler(Integer id, String name, String dataName, Resource tplRes) {
		super(id, name, dataName, tplRes);
	}

	@Autowired
	private ArticleService articleService;

	@Override
	public Object getWidgetData(Space space, Params params) throws LogicException {
		Integer id = params.get(PARAMETER_KEY, Integer.class);
		Article article = articleService.getArticleForView(id);
		if (article == null ||
		// 文章不存在或者不在目标空间下
				(!space.getAlias().equals(article.getSpace().getAlias()))) {
			throw new LogicException(new Message("article.notExists", "文章不存在"));
		}
		params.add("article", article);
		return article;
	}

	@Override
	public boolean canProcess(Space space, Params params) {
		if (space == null) {
			return false;
		}
		return params.has(PARAMETER_KEY);
	}

	@Override
	public Object buildWidgetDataForTest() {
		Article article = new Article();
		article.setAllowComment(true);
		article.setComments(0);
		article.setEditor(Editor.MD);
		article.setContent("#这是预览内容");
		article.setEditor(Editor.MD);
		article.setFrom(ArticleFrom.ORIGINAL);
		article.setHits(10);
		article.setId(1);
		article.setIsPrivate(false);
		article.setLastModifyDate(new Date());
		article.setPubDate(new Date());

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
