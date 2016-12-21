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
package me.qyh.blog.comment.article;

import com.google.gson.annotations.Expose;

import me.qyh.blog.comment.base.BaseComment;
import me.qyh.blog.entity.Article;

/**
 * 
 * @author Administrator
 *
 */
public class Comment extends BaseComment<Comment> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Expose(serialize = false, deserialize = false)
	private Article article;// 文章

	public Article getArticle() {
		return article;
	}

	public void setArticle(Article article) {
		this.article = article;
	}

	@Override
	public boolean matchParent(Comment parent) {
		return super.matchParent(parent) && (article.equals(parent.getArticle()));
	}

}
