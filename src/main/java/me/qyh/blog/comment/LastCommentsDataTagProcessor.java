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
package me.qyh.blog.comment;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;

import me.qyh.blog.comment.Comment.CommentStatus;
import me.qyh.blog.comment.CommentModule.ModuleType;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.ui.ContextVariables;
import me.qyh.blog.ui.data.DataTagProcessor;

public class LastCommentsDataTagProcessor extends DataTagProcessor<List<Comment>> {

	private static final Integer DEFAULT_LIMIT = 10;
	private static final String LIMIT = "limit";
	private static final String QUERY_ADMIN = "queryAdmin";
	private static final String MODULE_TYPE = "moduleType";
	private static final String MODULE_ID = "moduleId";

	private static final int MAX_LIMIT = 50;

	@Autowired
	private CommentService commentService;

	public LastCommentsDataTagProcessor(String name, String dataName) {
		super(name, dataName);
	}

	@Override
	protected List<Comment> buildPreviewData(Attributes attributes) {
		List<Comment> comments = new ArrayList<>();
		Comment comment = new Comment();
		comment.setCommentDate(Timestamp.valueOf(LocalDateTime.now()));
		comment.setContent("测试内容");
		comment.setNickname("测试");
		comment.setEmail("test@test.com");
		comment.setGravatar(DigestUtils.md5DigestAsHex("test@test.com".getBytes()));
		comment.setAdmin(true);
		comment.setIp("127.0.0.1");
		comment.setId(-1);
		comment.setCommentModule(new CommentModule(ModuleType.ARTICLE, -1));
		comment.setStatus(CommentStatus.NORMAL);
		comments.add(comment);
		return comments;
	}

	@Override
	protected List<Comment> query(ContextVariables variables, Attributes attributes) throws LogicException {
		ModuleType type = getModuleType(variables, attributes);
		if (type == null) {
			return Collections.emptyList();
		}
		return commentService.queryLastComments(new CommentModule(type, getModuleId(variables, attributes)),
				getLimit(attributes), getQueryAdmin(variables, attributes));
	}

	private ModuleType getModuleType(ContextVariables variables, Attributes attributes) {
		try {
			return ModuleType.valueOf(super.getVariables(MODULE_TYPE, variables, attributes).toUpperCase());
		} catch (Exception e) {
			return null;
		}
	}

	private Integer getModuleId(ContextVariables variables, Attributes attributes) {
		String moduleIdStr = super.getVariables(MODULE_ID, variables, attributes);
		if (moduleIdStr != null) {
			try {
				return Integer.parseInt(moduleIdStr);
			} catch (NumberFormatException e) {
				return null;
			}
		}
		return null;
	}

	private boolean getQueryAdmin(ContextVariables variables, Attributes attributes) {
		return Boolean.parseBoolean(super.getVariables(QUERY_ADMIN, variables, attributes));
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
