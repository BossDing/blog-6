package me.qyh.blog.ui.data;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;

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
import me.qyh.blog.web.controller.form.ArticleQueryParamValidator;

public class ArticlesDataTagProcessor extends DataTagProcessor<PageResult<Article>> {

	@Autowired
	private ArticleService articleService;
	@Autowired
	private ConfigService configService;

	public static final String PARAMETER_KEY = "articleQueryParam";

	private static final String[] TIME_PATTERNS = new String[] { "yyyy-MM-dd", "yyyy-MM", "yyyy-MM-dd HH:mm:ss" };

	public ArticlesDataTagProcessor(String name, String dataName) {
		super(name, dataName);
	}

	@Override
	protected PageResult<Article> buildPreviewData(Attributes attributes) {
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
		space.setAlias("preview");
		article.setSpace(space);
		article.setSummary("这是预览内容");
		article.setTitle("预览内容");
		Set<Tag> tags = new HashSet<Tag>();
		tags.add(new Tag("预览"));
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
	protected PageResult<Article> query(Space space, Params params, Attributes attributes) throws LogicException {
		ArticleQueryParam param = params.get(PARAMETER_KEY, ArticleQueryParam.class);
		if (param == null) {
			param = parseParam(space, attributes);
		}
		param.setStatus(ArticleStatus.PUBLISHED);
		param.setQueryPrivate(UserContext.get() != null);
		param.setPageSize(configService.getPageSizeConfig().getArticlePageSize());
		return articleService.queryArticle(param);
	}

	private ArticleQueryParam parseParam(Space space, Attributes attributes) {
		ArticleQueryParam param = new ArticleQueryParam();
		param.setPageSize(configService.getPageSizeConfig().getArticlePageSize());
		param.setSpace(space);
		param.setStatus(ArticleStatus.PUBLISHED);
		param.setCurrentPage(1);

		String beginStr = attributes.get("begin");
		String endStr = attributes.get("end");
		if (beginStr != null && endStr != null) {
			try {
				param.setBegin(DateUtils.parseDate(beginStr, TIME_PATTERNS));
				param.setEnd(DateUtils.parseDate(endStr, TIME_PATTERNS));
			} catch (Exception e) {
				param.setBegin(null);
				param.setEnd(null);
			}
		}
		String query = attributes.get("query");
		if (query != null)
			param.setQuery(query);

		String fromStr = attributes.get("from");
		if (fromStr != null) {
			try {
				param.setFrom(ArticleFrom.valueOf(fromStr.toUpperCase()));
			} catch (Exception e) {
			}
		}

		String tag = attributes.get("tag");
		if (tag != null)
			param.setTag(tag);

		String sortStr = attributes.get("sort");
		if (sortStr != null) {
			try {
				param.setSort(Sort.valueOf(sortStr.toUpperCase()));
			} catch (Exception e) {
			}
		}

		String currentPageStr = attributes.get("currentPage");
		if (currentPageStr != null) {
			try {
				param.setCurrentPage(Integer.parseInt(currentPageStr));
			} catch (Exception e) {
			}
		}

		if (UserContext.get() != null) {
			String ignoreLevelStr = attributes.get("ignoreLevel");
			if (ignoreLevelStr != null) {
				try {
					param.setIgnoreLevel(Boolean.parseBoolean(ignoreLevelStr));
				} catch (Exception e) {
				}
			}

			String queryPrivateStr = attributes.get("queryPrivate");
			if (queryPrivateStr != null) {
				try {
					param.setQueryPrivate(Boolean.parseBoolean(queryPrivateStr));
				} catch (Exception e) {
				}
			}

			String hasLockStr = attributes.get("hasLock");
			if (hasLockStr != null) {
				try {
					param.setHasLock(Boolean.parseBoolean(hasLockStr));
				} catch (Exception e) {
				}
			}
		}

		ArticleQueryParamValidator.validate(param);
		return param;
	}

}
