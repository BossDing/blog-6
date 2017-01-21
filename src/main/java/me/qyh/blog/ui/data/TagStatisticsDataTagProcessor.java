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

import org.springframework.beans.factory.annotation.Autowired;

import me.qyh.blog.exception.LogicException;
import me.qyh.blog.service.StatisticsService;
import me.qyh.blog.service.StatisticsService.TagStatistics;
import me.qyh.blog.ui.ContextVariables;

public class TagStatisticsDataTagProcessor extends DataTagProcessor<TagStatistics> {

	@Autowired
	private StatisticsService statisticsService;

	public TagStatisticsDataTagProcessor(String name, String dataName) {
		super(name, dataName);
	}

	@Override
	protected TagStatistics buildPreviewData(Attributes attributes) {
		TagStatistics ts = new TagStatistics();
		ts.setArticleTagCount(1);
		return ts;
	}

	@Override
	protected TagStatistics query(ContextVariables variables, Attributes attributes) throws LogicException {
		return statisticsService.queryTagStatistics();
	}

}
