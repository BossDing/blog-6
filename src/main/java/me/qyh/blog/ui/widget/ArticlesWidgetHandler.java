package me.qyh.blog.ui.widget;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;

import me.qyh.blog.entity.Article;
import me.qyh.blog.entity.Article.ArticleFrom;
import me.qyh.blog.entity.Article.ArticleStatus;
import me.qyh.blog.entity.Space;
import me.qyh.blog.entity.Tag;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.pageparam.ArticleQueryParam;
import me.qyh.blog.pageparam.ArticleQueryParam.Sort;
import me.qyh.blog.pageparam.PageResult;
import me.qyh.blog.security.UserContext;
import me.qyh.blog.service.ArticleService;
import me.qyh.blog.service.ConfigService;
import me.qyh.blog.ui.Params;

public class ArticlesWidgetHandler extends SysWidgetHandler {

	@Autowired
	private ArticleService articleService;
	@Autowired
	private ConfigService configService;

	public static final String PARAMETER_KEY = "articleQueryParam";

	public ArticlesWidgetHandler(Integer id, String name, String dataName, Resource tplRes) {
		super(id, name, dataName, tplRes);
	}

	@Override
	protected Object getWidgetData(Space space, Params params, Map<String, String> attrs) throws LogicException {
		ArticleQueryParam param = params.get(PARAMETER_KEY, ArticleQueryParam.class);
		if (param == null) {
			param = new ArticleQueryParam();
			param.setCurrentPage(1);
			param.setSpace(space);
		}
		Sort sort = null;
		String sortAttr = attrs.get("sort");
		if (sortAttr != null) {
			try {
				sort = Sort.valueOf(sortAttr.toUpperCase());
			} catch (Exception e) {
				// ignore;
			}
		}
		param.setStatus(ArticleStatus.PUBLISHED);
		param.setSort(sort);
		param.setQueryPrivate(UserContext.get() != null);
		param.setPageSize(configService.getPageSizeConfig().getArticlePageSize());
		return articleService.queryArticle(param);
	}

	@Override
	public Object buildWidgetDataForTest() {
		List<Article> articles = new ArrayList<Article>();
		Article article = new Article();
		article.setComments(0);
		article.setFrom(ArticleFrom.ORIGINAL);
		article.setHits(10);
		article.setId(1);
		article.setIsPrivate(false);
		article.setPubDate(Timestamp.valueOf(LocalDateTime.now()));
		Space space = new Space();
		space.setId(1);
		space.setAlias("test");
		article.setSpace(space);
		article.setSummary("这是测试内容");
		article.setTitle("测试内容");
		Set<Tag> tags = new HashSet<Tag>();
		tags.add(new Tag("test"));
		article.setTags(tags);
		articles.add(article);
		int pageSize = configService.getPageSizeConfig().getArticlePageSize();
		ArticleQueryParam param = new ArticleQueryParam();
		param.setCurrentPage(1);
		param.setSpace(space);
		param.setPageSize(pageSize);
		param.setStatus(ArticleStatus.PUBLISHED);
		return new PageResult<>(param, pageSize + 1, articles);
	}

	@Override
	public boolean canProcess(Space space, Params params) {
		return true;
	}
}
