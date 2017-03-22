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
package me.qyh.blog.core.ui.data;

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
import me.qyh.blog.core.exception.LogicException;
import me.qyh.blog.core.service.ArticleService;

public class ArticleSimilarDataTagProcessor extends DataTagProcessor<List<Article>> {

	@Autowired
	private ArticleService articleService;

	private int limit = 5;

	public ArticleSimilarDataTagProcessor(String name, String dataName) {
		super(name, dataName);
	}

	@Override
	protected List<Article> query(Attributes attributes) throws LogicException {
		String idOrAlias = attributes.get(Constants.ID_OR_ALIAS);
		if (idOrAlias != null) {
			return articleService.findSimilar(idOrAlias, limit);
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
