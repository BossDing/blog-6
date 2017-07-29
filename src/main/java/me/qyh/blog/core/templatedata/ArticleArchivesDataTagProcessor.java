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
package me.qyh.blog.core.templatedata;

import org.springframework.beans.factory.annotation.Autowired;

import me.qyh.blog.core.exception.LogicException;
import me.qyh.blog.core.service.ArticleService;
import me.qyh.blog.core.vo.ArticleArchiveTree;
import me.qyh.blog.core.vo.ArticleArchiveTree.ArticleArchiveMode;
import me.qyh.blog.core.vo.DataTagProcessor;

/**
 * 文章归档
 * 
 * @author Administrator
 *
 */
public class ArticleArchivesDataTagProcessor extends DataTagProcessor<ArticleArchiveTree> {

	@Autowired
	private ArticleService articleService;

	public ArticleArchivesDataTagProcessor(String name, String dataName) {
		super(name, dataName);
	}

	@Override
	protected ArticleArchiveTree query(Attributes attributes) throws LogicException {
		ArticleArchiveMode mode = attributes.getEnum("mode", ArticleArchiveMode.class, ArticleArchiveMode.YMD);
		return articleService.selectArticleArchives(mode);
	}

}
