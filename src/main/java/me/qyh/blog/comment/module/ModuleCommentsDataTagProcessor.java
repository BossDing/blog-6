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
package me.qyh.blog.comment.module;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;

import com.google.common.collect.Lists;

import me.qyh.blog.comment.base.BaseComment.CommentStatus;
import me.qyh.blog.comment.base.CommentConfig;
import me.qyh.blog.comment.base.CommentSupport.CommentPageResult;
import me.qyh.blog.entity.Article;
import me.qyh.blog.entity.Space;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.security.UserContext;
import me.qyh.blog.ui.ContextVariables;
import me.qyh.blog.ui.data.DataTagProcessor;
import me.qyh.blog.util.Validators;

public class ModuleCommentsDataTagProcessor extends DataTagProcessor<CommentPageResult<ModuleComment>> {
	@Autowired
	private ModuleCommentService commentService;

	public ModuleCommentsDataTagProcessor(String name, String dataName) {
		super(name, dataName);
	}

	@Override
	protected CommentPageResult<ModuleComment> buildPreviewData(Space space, Attributes attributes) {
		List<ModuleComment> comments = Lists.newArrayList();
		ModuleComment comment = new ModuleComment();
		comment.setCommentDate(Timestamp.valueOf(LocalDateTime.now()));
		comment.setContent("测试内容");
		comment.setNickname("测试");
		comment.setEmail("test@test.com");
		comment.setGravatar(DigestUtils.md5DigestAsHex("test@test.com".getBytes()));
		comment.setAdmin(true);
		comment.setIp("127.0.0.1");
		Article article = new Article();
		article.setId(1);
		CommentModule module = new CommentModule();
		module.setId(-1);
		module.setName("测试模块");
		comment.setModule(module);
		comment.setId(-1);
		comment.setStatus(CommentStatus.NORMAL);
		comments.add(comment);
		ModuleCommentQueryParam param = new ModuleCommentQueryParam();
		param.setCurrentPage(1);
		CommentConfig config = commentService.getCommentConfig(null);
		param.setPageSize(config.getPageSize());
		return new CommentPageResult<>(param, config.getPageSize() + 1, comments, config);
	}

	@Override
	protected CommentPageResult<ModuleComment> query(Space space, ContextVariables variables, Attributes attributes)
			throws LogicException {
		ModuleCommentQueryParam param = parseParam(variables, attributes);
		return commentService.queryComment(param);
	}

	private ModuleCommentQueryParam parseParam(ContextVariables variables, Attributes attributes) {
		ModuleCommentQueryParam param = new ModuleCommentQueryParam();
		param.setStatus(UserContext.get() == null ? CommentStatus.NORMAL : null);
		String moduleName = super.getVariables("module", variables, attributes);
		if (Validators.isEmptyOrNull(moduleName, true)) {
			param.setModule(null);
		} else {
			CommentModule module = new CommentModule();
			module.setName(moduleName);
			param.setModule(module);
		}
		String currentPageStr = super.getVariables("currentPage", variables, attributes);
		if (currentPageStr != null) {
			try {
				param.setCurrentPage(Integer.parseInt(currentPageStr));
			} catch (Exception e) {
			}
		}
		if (param.getCurrentPage() < 0) {
			param.setCurrentPage(0);
		}

		return param;
	}

}
