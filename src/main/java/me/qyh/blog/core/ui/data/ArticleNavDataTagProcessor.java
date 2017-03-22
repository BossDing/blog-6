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

import org.springframework.beans.factory.annotation.Autowired;

import me.qyh.blog.core.bean.ArticleNav;
import me.qyh.blog.core.entity.Article;
import me.qyh.blog.core.exception.LogicException;
import me.qyh.blog.core.service.ArticleService;

public class ArticleNavDataTagProcessor extends DataTagProcessor<ArticleNav> {

	@Autowired
	private ArticleService articleService;
	private static final String QUERY_LOCK_ATTR = "queryLock";

	public ArticleNavDataTagProcessor(String name, String dataName) {
		super(name, dataName);
	}

	@Override
	protected ArticleNav buildPreviewData(Attributes attributes) {
		Article previous = new Article(-1);
		previous.setTitle("预览博客-前一篇");
		Article next = new Article(-2);
		next.setTitle("预览博客-后一篇");
		previous.setSpace(getSpace());
		next.setSpace(getSpace());

		return new ArticleNav(previous, next);
	}

	@Override
	protected ArticleNav query(Attributes attributes) throws LogicException {
		String idOrAlias = attributes.get(Constants.ID_OR_ALIAS);
		if (idOrAlias != null) {
			return articleService.getArticleNav(idOrAlias, Boolean.parseBoolean(attributes.get(QUERY_LOCK_ATTR)))
					.orElse(null);
		}
		return null;
	}

}
