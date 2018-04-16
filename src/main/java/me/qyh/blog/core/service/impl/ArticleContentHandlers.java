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
package me.qyh.blog.core.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.util.CollectionUtils;

import me.qyh.blog.core.entity.Article;
import me.qyh.blog.core.plugin.ArticleContentHandlerRegistry;

/**
 * 用于支持多个ArticleContentHandler
 * 
 * @author Administrator
 *
 */
public class ArticleContentHandlers implements ArticleContentHandler, ArticleContentHandlerRegistry {

	private List<ArticleContentHandler> handlers = new ArrayList<>();

	@Override
	public void handle(Article article) {
		if (!CollectionUtils.isEmpty(handlers)) {
			for (ArticleContentHandler handler : handlers) {
				handler.handle(article);
			}
		}
	}

	@Override
	public void handlePreview(Article article) {
		if (!CollectionUtils.isEmpty(handlers)) {
			for (ArticleContentHandler handler : handlers) {
				handler.handlePreview(article);
			}
		}
	}

	public void setHandlers(List<ArticleContentHandler> handlers) {
		this.handlers = handlers;
	}

	@Override
	public ArticleContentHandlerRegistry register(ArticleContentHandler handler) {
		this.handlers.add(handler);
		return this;
	}

}
