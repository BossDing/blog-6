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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import me.qyh.blog.entity.Article;
import me.qyh.blog.entity.Article.ArticleFrom;
import me.qyh.blog.entity.Tag;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.service.ArticleService;
import me.qyh.blog.ui.ContextVariables;

public class ArticleSimilarDataTagProcessor extends DataTagProcessor<List<Article>> {

	@Autowired
	private ArticleService articleService;

	private int limit = 5;

	public ArticleSimilarDataTagProcessor(String name, String dataName) {
		super(name, dataName);
	}

	@Override
	protected List<Article> query(ContextVariables variables, Attributes attributes) throws LogicException {
		Article article = (Article) variables.getAttribute("article");
		if (article == null) {
			String idOrAlias = super.getVariables("idOrAlias", variables, attributes);
			if (idOrAlias != null) {
				return articleService.findSimilar(idOrAlias, limit);
			}
		}
		if (article != null) {
			return articleService.findSimilar(article, limit);
		}
		return new ArrayList<>();
	}

	@Override
	protected List<Article> buildPreviewData(Attributes attributes) {
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
		return articles;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

}
