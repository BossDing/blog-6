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

import java.util.Calendar;

import org.springframework.beans.factory.annotation.Autowired;

import me.qyh.blog.bean.ArticleDateFile;
import me.qyh.blog.bean.ArticleDateFiles;
import me.qyh.blog.bean.ArticleDateFiles.ArticleDateFileMode;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.service.ArticleService;

public class ArticleDateFilesDataTagProcessor extends DataTagProcessor<ArticleDateFiles> {

	@Autowired
	private ArticleService articleService;

	private static final String MODE = "mode";

	public ArticleDateFilesDataTagProcessor(String name, String dataName) {
		super(name, dataName);
	}

	@Override
	protected ArticleDateFiles buildPreviewData(Attributes attributes) {
		ArticleDateFiles files = new ArticleDateFiles();
		files.setMode(ArticleDateFileMode.YM);
		Calendar cal = Calendar.getInstance();
		ArticleDateFile file1 = new ArticleDateFile();
		file1.setBegin(cal.getTime());
		file1.setCount(1);
		files.addArticleDateFile(file1);

		ArticleDateFile file2 = new ArticleDateFile();
		cal.add(Calendar.MONTH, -1);
		file2.setBegin(cal.getTime());
		file2.setCount(2);
		files.addArticleDateFile(file2);

		files.calDate();

		return files;
	}

	@Override
	protected ArticleDateFiles query(Attributes attributes) throws LogicException {
		ArticleDateFileMode mode = getMode(attributes);
		return articleService.queryArticleDateFiles(mode);
	}

	private ArticleDateFileMode getMode(Attributes attributes) {
		ArticleDateFileMode mode = ArticleDateFileMode.YM;
		String v = attributes.getOrDefault(MODE, ArticleDateFileMode.YM.name());
		try {
			mode = ArticleDateFileMode.valueOf(v);
		} catch (Exception e) {
			LOGGER.debug(e.getMessage(), e);
		}
		return mode;
	}

}
