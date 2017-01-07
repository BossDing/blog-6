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
import me.qyh.blog.security.Environment;
import me.qyh.blog.service.ArticleService;
import me.qyh.blog.ui.ContextVariables;
import me.qyh.blog.web.controller.form.ArticleQueryParamValidator;

/**
 * 文章列表数据处理器
 * 
 * @author Administrator
 *
 */
public class ArticlesDataTagProcessor extends DataTagProcessor<PageResult<Article>> {

	private static final Logger LOGGER = LoggerFactory.getLogger(ArticleDataTagProcessor.class);

	@Autowired
	private ArticleService articleService;

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
	protected PageResult<Article> buildPreviewData(Space space, Attributes attributes) {
		List<Article> articles = Lists.newArrayList();
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
		Set<Tag> tags = Sets.newHashSet();
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
	protected PageResult<Article> query(Space space, ContextVariables variables, Attributes attributes)
			throws LogicException {
		ArticleQueryParam param = (ArticleQueryParam) variables.getAttribute(ArticleQueryParam.class.getName());
		if (param == null) {
			param = parseParam(space, variables, attributes);
		}
		param.setStatus(ArticleStatus.PUBLISHED);
		param.setQueryPrivate(Environment.isLogin());
		return articleService.queryArticle(param);
	}

	private ArticleQueryParam parseParam(Space space, ContextVariables variables, Attributes attributes) {
		ArticleQueryParam param = new ArticleQueryParam();
		param.setSpace(space);
		param.setStatus(ArticleStatus.PUBLISHED);
		param.setCurrentPage(1);

		String beginStr = super.getVariables("begin", variables, attributes);
		String endStr = super.getVariables("end", variables, attributes);
		if (beginStr != null && endStr != null) {
			try {
				param.setBegin(DateUtils.parseDate(beginStr, TIME_PATTERNS));
				param.setEnd(DateUtils.parseDate(endStr, TIME_PATTERNS));
			} catch (Exception e) {
				LOGGER.debug("开始时间和结束时间:[" + beginStr + "," + endStr + "]无法被转化:" + e.getMessage(), e);
				param.setBegin(null);
				param.setEnd(null);
			}
		}
		String query = super.getVariables("query", variables, attributes);
		if (query != null) {
			param.setQuery(query);
		}

		String fromStr = super.getVariables("from", variables, attributes);
		if (fromStr != null) {
			try {
				param.setFrom(ArticleFrom.valueOf(fromStr.toUpperCase()));
			} catch (Exception e) {
				LOGGER.debug("文章来源:" + fromStr + "无法被转化:" + e.getMessage(), e);
			}
		}

		String tag = super.getVariables("tag", variables, attributes);
		if (tag != null) {
			param.setTag(tag);
		}

		String sortStr = super.getVariables("sort", variables, attributes);
		if (sortStr != null) {
			try {
				param.setSort(Sort.valueOf(sortStr.toUpperCase()));
			} catch (Exception e) {
				LOGGER.debug("排序方式:" + sortStr + "无法被转化:" + e.getMessage(), e);
			}
		}

		String currentPageStr = super.getVariables("currentPage", variables, attributes);
		if (currentPageStr != null) {
			try {
				param.setCurrentPage(Integer.parseInt(currentPageStr));
			} catch (Exception e) {
				LOGGER.debug("当前页码:" + currentPageStr + "无法被转化:" + e.getMessage(), e);
			}
		}

		if (Environment.isLogin()) {
			String ignoreLevelStr = super.getVariables("ignoreLevel", variables, attributes);
			if (ignoreLevelStr != null) {
				param.setIgnoreLevel(Boolean.parseBoolean(ignoreLevelStr));
			}

			String queryPrivateStr = super.getVariables("queryPrivate", variables, attributes);
			if (queryPrivateStr != null) {
				param.setQueryPrivate(Boolean.parseBoolean(queryPrivateStr));
			}

			String hasLockStr = super.getVariables("hasLock", variables, attributes);
			if (hasLockStr != null) {
				param.setHasLock(Boolean.parseBoolean(hasLockStr));
			}
		}

		ArticleQueryParamValidator.validate(param);
		return param;
	}

}
