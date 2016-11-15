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
package me.qyh.blog.evt;

import java.util.Arrays;
import java.util.List;

import org.springframework.context.ApplicationEvent;

import me.qyh.blog.entity.Article;

/**
 * 文章发布事件
 * 
 * @author Administrator
 *
 */
public class ArticlePublishedEvent extends ApplicationEvent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private List<Article> articles;
	private OP op;

	/**
	 * 操作方式
	 * 
	 * @author Administrator
	 *
	 */
	public enum OP {
		INSERT, UPDATE;
	}

	/**
	 * 
	 * @param source
	 *            操作对象
	 * @param article
	 *            文章
	 * @param op
	 *            操作方式
	 */
	public ArticlePublishedEvent(Object source, Article article, OP op) {
		super(source);
		this.articles = Arrays.asList(article);
		this.op = op;
	}

	/**
	 * 
	 * @param source
	 *            操作对象
	 * @param articles
	 *            文章集合
	 * @param op
	 *            操作方式
	 */
	public ArticlePublishedEvent(Object source, List<Article> articles, OP op) {
		super(source);
		this.articles = articles;
		this.op = op;
	}

	public List<Article> getArticles() {
		return articles;
	}

	public OP getOp() {
		return op;
	}

}
