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
package me.qyh.blog.evt.handler;

import java.util.List;

import me.qyh.blog.entity.Article;
import me.qyh.blog.evt.ArticlePublishedEvent.OP;

/**
 * 文章发布事件处理器
 * 
 * @author Administrator
 *
 */
public interface ArticlePublishedHandler {

	/**
	 * 处理文章的发布
	 * 
	 * @param articles
	 *            发布的文章集合
	 * @param op
	 *            操作方式
	 */
	void handle(List<Article> articles, OP op);

}
