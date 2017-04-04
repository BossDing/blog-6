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

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import me.qyh.blog.core.entity.Comment;
import me.qyh.blog.core.entity.CommentModule;
import me.qyh.blog.core.entity.CommentModule.ModuleType;
import me.qyh.blog.core.exception.LogicException;
import me.qyh.blog.core.service.impl.CommentService;

public class LastCommentsDataTagProcessor extends DataTagProcessor<List<Comment>> {

	private static final Integer DEFAULT_LIMIT = 10;
	private static final String LIMIT = "limit";
	private static final String QUERY_ADMIN = "queryAdmin";

	private static final int MAX_LIMIT = 50;

	@Autowired
	private CommentService commentService;

	public LastCommentsDataTagProcessor(String name, String dataName) {
		super(name, dataName);
	}

	@Override
	protected List<Comment> query(Attributes attributes) throws LogicException {
		ModuleType type = getModuleType(attributes);
		if (type == null) {
			return Collections.emptyList();
		}
		return commentService.queryLastComments(new CommentModule(type, getModuleId(attributes)), getLimit(attributes),
				getQueryAdmin(attributes));
	}

	private ModuleType getModuleType(Attributes attributes) {
		try {
			return ModuleType.valueOf(attributes.get(Constants.MODULE_TYPE).toUpperCase());
		} catch (Exception e) {
			return null;
		}
	}

	private Integer getModuleId(Attributes attributes) {
		String moduleIdStr = attributes.get(Constants.MODULE_ID);
		if (moduleIdStr != null) {
			try {
				return Integer.parseInt(moduleIdStr);
			} catch (NumberFormatException e) {
				LOGGER.debug(e.getMessage(), e);
				return null;
			}
		}
		return null;
	}

	private boolean getQueryAdmin(Attributes attributes) {
		return Boolean.parseBoolean(attributes.get(QUERY_ADMIN));
	}

	private int getLimit(Attributes attributes) {
		int limit = DEFAULT_LIMIT;
		String v = attributes.get(LIMIT);
		if (v != null) {
			try {
				limit = Integer.parseInt(v);
			} catch (Exception e) {
			}
		}
		if (limit <= 0) {
			limit = DEFAULT_LIMIT;
		}
		if (limit > MAX_LIMIT) {
			limit = MAX_LIMIT;
		}
		return limit;
	}

}
