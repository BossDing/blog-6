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
package me.qyh.blog.ui.data;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

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
import me.qyh.blog.ui.Params;
import me.qyh.blog.web.controller.form.ArticleQueryParamValidator;

/**
 * 文章列表数据处理器
 * 
 * @author Administrator
 *
 */
public class ArticlesDataTagProcessor extends DataTagProcessor<PageResult<Article>> {

	private static final Logger logger = LoggerFactory.getLogger(ArticleDataTagProcessor.class);

	@Autowired
	private ArticleService articleService;

	public static final String PARAMETER_KEY = "articleQueryParam";
	private static final String[] TIME_PATTERNS = new String[] { "yyyy-MM-dd", "yyyy-MM", "yyyy-MM-dd HH:mm:ss" };

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
		List<Article> articles = Lists.newArrayList();
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
		space.setName("preview");
		article.setSpace(space);
		article.setSummary("这是预览内容");
		article.setTitle("预览内容");
		Set<Tag> tags = Sets.newHashSet();
		tags.add(new Tag("预览"));
		article.setTags(tags);
		articles.add(article);
		ArticleQueryParam param = new ArticleQueryParam();
		param.setCurrentPage(1);
		param.setSpace(space);
		param.setPageSize(5);
		param.setStatus(ArticleStatus.PUBLISHED);
		return new PageResult<>(param, 6, articles);
	}

	@Override
	protected PageResult<Article> query(Space space, Params params, Attributes attributes) throws LogicException {
		ArticleQueryParam param = params.get(PARAMETER_KEY, ArticleQueryParam.class);
		if (param == null) {
			param = parseParam(space, attributes);
		}
		param.setStatus(ArticleStatus.PUBLISHED);
		param.setQueryPrivate(UserContext.get() != null);
		return articleService.queryArticle(param);
	}

	private ArticleQueryParam parseParam(Space space, Attributes attributes) {
		ArticleQueryParam param = new ArticleQueryParam();
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
				logger.debug("开始时间和结束时间:[" + beginStr + "," + endStr + "]无法被转化:" + e.getMessage(), e);
				param.setBegin(null);
				param.setEnd(null);
			}
		}
		String query = attributes.get("query");
		if (query != null) {
			param.setQuery(query);
		}

		String fromStr = attributes.get("from");
		if (fromStr != null) {
			try {
				param.setFrom(ArticleFrom.valueOf(fromStr.toUpperCase()));
			} catch (Exception e) {
				logger.debug("文章来源:" + fromStr + "无法被转化:" + e.getMessage(), e);
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
				logger.debug("排序方式:" + sortStr + "无法被转化:" + e.getMessage(), e);
			}
		}

		String currentPageStr = attributes.get("currentPage");
		if (currentPageStr != null) {
			try {
				param.setCurrentPage(Integer.parseInt(currentPageStr));
			} catch (Exception e) {
				logger.debug("当前页码:" + currentPageStr + "无法被转化:" + e.getMessage(), e);
			}
		}

		if (UserContext.get() != null) {
			String ignoreLevelStr = attributes.get("ignoreLevel");
			if (ignoreLevelStr != null) {
				param.setIgnoreLevel(Boolean.parseBoolean(ignoreLevelStr));
			}

			String queryPrivateStr = attributes.get("queryPrivate");
			if (queryPrivateStr != null) {
				param.setQueryPrivate(Boolean.parseBoolean(queryPrivateStr));
			}

			String hasLockStr = attributes.get("hasLock");
			if (hasLockStr != null) {
				param.setHasLock(Boolean.parseBoolean(hasLockStr));
			}
		}

		ArticleQueryParamValidator.validate(param);
		return param;
	}

}
