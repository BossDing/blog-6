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
package me.qyh.blog.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import me.qyh.blog.entity.Article;
import me.qyh.blog.entity.Editor;
import me.qyh.blog.evt.listener.ArticleEventHandlerRegister;
import me.qyh.blog.evt.listener.EventHandlerAdapter;
import me.qyh.blog.security.input.Markdown2Html;

/**
 * 
 * @author mhlx
 *
 */
public class DefaultArticleContentHandler implements ArticleContentHandler, InitializingBean {

	private static final String DEFAULT_CACHESPECIFICATION = "maximumSize=500";
	private String cacheSpecification = DEFAULT_CACHESPECIFICATION;

	@Autowired
	private Markdown2Html markdown2Html;
	@Autowired
	private ArticleEventHandlerRegister articleEventHandlerRegister;

	private Cache<Integer, String> cache;

	@Override
	public void handle(Article article) {
		String content = cache.getIfPresent(article.getId());
		if (content == null) {
			content = handleContent(article);
			cache.put(article.getId(), content);
		}
		article.setContent(content);
	}

	@Override
	public void handlePreview(Article article) {
		article.setContent(handleContent(article));
	}

	private String handleContent(Article article) {
		if (Editor.MD.equals(article.getEditor())) {
			return markdown2Html.toHtml(article.getContent());
		} else {
			return Jsoup.parseBodyFragment(article.getContent()).html();
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (cacheSpecification != null) {
			cache = Caffeine.from(cacheSpecification).build();
		} else {
			cache = Caffeine.newBuilder().build();
		}
		articleEventHandlerRegister.registerTransactionalEventHandler(new EventHandlerAdapter<List<Article>>() {

			@Override
			public void handleUpdate(List<Article> articles) {
				cache.invalidateAll(articles.stream().map(Article::getId).collect(Collectors.toList()));
			}

			@Override
			public void handleDelete(List<Article> articles) {
				cache.invalidateAll(articles.stream().map(Article::getId).collect(Collectors.toList()));
			}

		});
	}

	public void setCacheSpecification(String cacheSpecification) {
		this.cacheSpecification = cacheSpecification;
	}

}
