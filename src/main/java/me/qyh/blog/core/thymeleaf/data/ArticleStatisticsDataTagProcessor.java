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
package me.qyh.blog.core.thymeleaf.data;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;

import me.qyh.blog.core.exception.LogicException;
import me.qyh.blog.core.service.StatisticsService;
import me.qyh.blog.core.service.StatisticsService.ArticleStatistics;

public class ArticleStatisticsDataTagProcessor extends DataTagProcessor<ArticleStatistics> {

	@Autowired
	private StatisticsService statisticsService;

	public ArticleStatisticsDataTagProcessor(String name, String dataName) {
		super(name, dataName);
	}

	@Override
	protected ArticleStatistics buildPreviewData(Attributes attributes) {
		ArticleStatistics preview = new ArticleStatistics();
		preview.setLastModifyDate(Timestamp.valueOf(LocalDateTime.now()));
		preview.setLastPubDate(Timestamp.valueOf(LocalDateTime.now()));
		preview.setTotalHits(1);
		return preview;
	}

	@Override
	protected ArticleStatistics query(Attributes attributes) throws LogicException {
		return statisticsService.queryArticleStatistics();
	}

}
