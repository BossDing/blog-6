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

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import me.qyh.blog.bean.TagCount;
import me.qyh.blog.entity.Space;
import me.qyh.blog.entity.Tag;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.security.UserContext;
import me.qyh.blog.service.ArticleService;
import me.qyh.blog.ui.ContextVariables;

public class ArticleTagDataTagProcessor extends DataTagProcessor<List<TagCount>> {

	@Autowired
	private ArticleService articleService;

	public ArticleTagDataTagProcessor(String name, String dataName) {
		super(name, dataName);
	}

	@Override
	protected List<TagCount> buildPreviewData(Space space, Attributes attributes) {
		TagCount count1 = new TagCount();
		Tag tag1 = new Tag();
		tag1.setCreate(Timestamp.valueOf(LocalDateTime.now()));
		tag1.setId(-1);
		tag1.setName("预览标签1");
		count1.setTag(tag1);
		count1.setCount(1);

		TagCount count2 = new TagCount();
		Tag tag2 = new Tag();
		tag2.setCreate(Timestamp.valueOf(LocalDateTime.now()));
		tag2.setId(-2);
		tag2.setName("预览标签2");
		count2.setTag(tag2);
		count2.setCount(2);
		return Arrays.asList(count1, count2);
	}

	@Override
	protected List<TagCount> query(Space space, ContextVariables variables, Attributes attributes)
			throws LogicException {
		boolean queryPrivate = UserContext.get() != null;
		if (queryPrivate) {
			String queryPrivateStr = super.getVariables("queryPrivate", variables, attributes);
			if (queryPrivateStr != null) {
				try {
					queryPrivate = Boolean.parseBoolean(queryPrivateStr);
				} catch (Exception e) {
				}
			}
		}
		boolean hasLock = true;
		String hasLockStr = super.getVariables("hasLock", variables, attributes);
		if (hasLockStr != null) {
			try {
				hasLock = Boolean.parseBoolean(hasLockStr);
			} catch (Exception e) {
			}
		}
		return articleService.queryTags(space, hasLock, queryPrivate);
	}

}
