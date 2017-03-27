/*
 * Copyright 2016 qyh.me
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.qyh.blog.core.thymeleaf.data;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import me.qyh.blog.core.entity.Article;
import me.qyh.blog.core.entity.Tag;
import me.qyh.blog.core.entity.Article.ArticleFrom;
import me.qyh.blog.core.entity.Article.ArticleStatus;
import me.qyh.blog.core.exception.LogicException;
import me.qyh.blog.core.pageparam.ArticleQueryParam;
import me.qyh.blog.core.pageparam.PageResult;
import me.qyh.blog.core.pageparam.ArticleQueryParam.Sort;
import me.qyh.blog.core.security.Environment;
import me.qyh.blog.core.service.ArticleService;
import me.qyh.blog.util.Times;
import me.qyh.blog.util.Validators;
import me.qyh.blog.web.controller.form.ArticleQueryParamValidator;

/**
 * 文章列表数据处理器
 * 
 * @author Administrator
 *
 */
public class ArticlesDataTagProcessor extends DataTagProcessor<PageResult<Article>> {

	@Autowired
	private ArticleService articleService;

	/**
	 * 构造器
	 * 
	 * @param name
	 *            数据处理器名称
	 * @param dataName
	 *            页面dataName
	 */
	public ArticlesDataTagProcessor(String name, String dataName) {
		super(name, dataName);
	}

	@Override
	protected PageResult<Article> buildPreviewData(Attributes attributes) {
		List<Article> articles = new ArrayList<>();
		Article article = new Article();
		article.setComments(0);
		article.setFrom(ArticleFrom.ORIGINAL);
		article.setHits(10);
		article.setId(1);
		article.setIsPrivate(false);
		article.setPubDate(Timestamp.valueOf(LocalDateTime.now()));
		article.setSpace(getSpace());
		article.setSummary("这是预览内容");
		article.setTitle("预览内容");
		Set<Tag> tags = new HashSet<>();
		tags.add(new Tag("预览"));
		article.setTags(tags);
		articles.add(article);
		ArticleQueryParam param = new ArticleQueryParam();
		param.setCurrentPage(1);
		param.setSpace(getSpace());
		param.setPageSize(5);
		param.setStatus(ArticleStatus.PUBLISHED);
		return new PageResult<>(param, 6, articles);
	}

	@Override
	protected PageResult<Article> query(Attributes attributes) throws LogicException {
		ArticleQueryParam param = parseParam(attributes);
		return articleService.queryArticle(param);
	}

	private ArticleQueryParam parseParam(Attributes attributes) {
		ArticleQueryParam param = new ArticleQueryParam();
		param.setSpace(getCurrentSpace());
		param.setStatus(ArticleStatus.PUBLISHED);
		param.setCurrentPage(1);

		String beginStr = attributes.get("begin");
		String endStr = attributes.get("end");
		if (beginStr != null && endStr != null) {
			param.setBegin(Times.parseAndGetDate(beginStr));
			param.setEnd(Times.parseAndGetDate(endStr));
		}
		String query = attributes.get("query");
		if (!Validators.isEmptyOrNull(query, true)) {
			param.setQuery(query);
		}

		String fromStr = attributes.get("from");
		if (fromStr != null) {
			try {
				param.setFrom(ArticleFrom.valueOf(fromStr.toUpperCase()));
			} catch (Exception e) {
				LOGGER.debug("文章来源:" + fromStr + "无法被转化:" + e.getMessage(), e);
			}
		}

		String tag = attributes.get("tag");
		if (tag != null) {
			param.setTag(tag);
		}

		String sortStr = attributes.get("sort");
		if (sortStr != null) {
			try {
				param.setSort(Sort.valueOf(sortStr.toUpperCase()));
			} catch (Exception e) {
				LOGGER.debug("排序方式:" + sortStr + "无法被转化:" + e.getMessage(), e);
			}
		}

		String currentPageStr = attributes.get(Constants.CURRENT_PAGE);
		if (currentPageStr != null) {
			try {
				param.setCurrentPage(Integer.parseInt(currentPageStr));
			} catch (Exception e) {
				LOGGER.debug("当前页码:" + currentPageStr + "无法被转化:" + e.getMessage(), e);
			}
		}

		String pageSizeStr = attributes.get(Constants.PAGE_SIZE);
		if (pageSizeStr != null) {
			try {
				param.setPageSize(Integer.parseInt(pageSizeStr));
			} catch (Exception e) {
				LOGGER.debug("当前分页数量:" + pageSizeStr + "无法被转化:" + e.getMessage(), e);
			}
		}

		String highlightStr = attributes.get("highlight");
		if (highlightStr != null) {
			param.setHighlight(Boolean.parseBoolean(highlightStr));
		}

		String ignoreLevelStr = attributes.get("ignoreLevel");
		if (ignoreLevelStr != null) {
			param.setIgnoreLevel(Boolean.parseBoolean(ignoreLevelStr));
		}

		String queryLockStr = attributes.get("queryLock");
		if (queryLockStr != null) {
			param.setQueryLock(Boolean.parseBoolean(queryLockStr));
		}

		if (Environment.isLogin()) {

			String queryPrivateStr = attributes.get("queryPrivate");
			if (queryPrivateStr != null) {
				param.setQueryPrivate(Boolean.parseBoolean(queryPrivateStr));
			}

		}

		param.setStatus(ArticleStatus.PUBLISHED);
		param.setSpace(getCurrentSpace());

		ArticleQueryParamValidator.validate(param);
		return param;
	}

}
