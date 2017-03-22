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

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;

import me.qyh.blog.core.bean.CommentPageResult;
import me.qyh.blog.core.config.CommentConfig;
import me.qyh.blog.core.entity.Comment;
import me.qyh.blog.core.entity.CommentMode;
import me.qyh.blog.core.entity.CommentModule;
import me.qyh.blog.core.entity.Comment.CommentStatus;
import me.qyh.blog.core.entity.CommentModule.ModuleType;
import me.qyh.blog.core.exception.LogicException;
import me.qyh.blog.core.pageparam.CommentQueryParam;
import me.qyh.blog.core.security.Environment;
import me.qyh.blog.core.service.impl.CommentService;

public class CommentsDataTagProcessor extends DataTagProcessor<CommentPageResult> {
	@Autowired
	private CommentService commentService;

	private static final String MODE = "mode";
	private static final String ASC = "asc";

	public CommentsDataTagProcessor(String name, String dataName) {
		super(name, dataName);
	}

	@Override
	protected CommentPageResult buildPreviewData(Attributes attributes) {
		List<Comment> comments = new ArrayList<>();
		Comment comment = new Comment();
		comment.setCommentDate(Timestamp.valueOf(LocalDateTime.now()));
		comment.setContent("测试内容");
		comment.setNickname("测试");
		comment.setEmail("test@test.com");
		comment.setGravatar(DigestUtils.md5DigestAsHex("test@test.com".getBytes()));
		comment.setAdmin(true);
		comment.setIp("127.0.0.1");
		comment.setCommentModule(new CommentModule(ModuleType.ARTICLE, -1));
		comment.setStatus(CommentStatus.NORMAL);
		comments.add(comment);
		CommentQueryParam param = new CommentQueryParam();
		param.setCurrentPage(1);
		CommentConfig config = commentService.getCommentConfig();
		param.setPageSize(config.getPageSize());
		return new CommentPageResult(param, config.getPageSize() + 1, comments, config);
	}

	@Override
	protected CommentPageResult query(Attributes attributes) throws LogicException {
		CommentQueryParam param = new CommentQueryParam();
		param.setStatus(!Environment.isLogin() ? CommentStatus.NORMAL : null);

		String moduleTypeStr = attributes.get(Constants.MODULE_TYPE);
		String moduleIdStr = attributes.get(Constants.MODULE_ID);
		if (moduleIdStr != null && moduleTypeStr != null) {
			try {
				param.setModule(new CommentModule(ModuleType.valueOf(moduleTypeStr.toUpperCase()),
						Integer.parseInt(moduleIdStr)));
			} catch (Exception e) {
				LOGGER.debug(e.getMessage(), e);
			}
		}

		String modeStr = attributes.getOrDefault(MODE, CommentMode.LIST.name());
		try {
			param.setMode(CommentMode.valueOf(modeStr));
		} catch (Exception e) {
			LOGGER.debug(e.getMessage(), e);
		}

		String ascStr = attributes.getOrDefault(ASC, "true");
		param.setAsc(Boolean.parseBoolean(ascStr));

		String currentPageStr = attributes.get(Constants.CURRENT_PAGE);
		if (currentPageStr != null) {
			try {
				param.setCurrentPage(Integer.parseInt(currentPageStr));
			} catch (Exception e) {
				LOGGER.debug(e.getMessage(), e);
			}
		}
		if (param.getCurrentPage() < 0) {
			param.setCurrentPage(0);
		}

		return commentService.queryComment(param);
	}

}
