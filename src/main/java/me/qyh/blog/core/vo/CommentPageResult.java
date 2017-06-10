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
package me.qyh.blog.core.vo;

import java.util.List;

import me.qyh.blog.core.config.CommentConfig;
import me.qyh.blog.core.entity.Comment;
import me.qyh.blog.core.pageparam.PageQueryParam;
import me.qyh.blog.core.pageparam.PageResult;

/**
 * 评论分页结果
 * 
 * @author Administrator
 *
 */
public final class CommentPageResult extends PageResult<Comment> {
	private final CommentConfig commentConfig;

	public CommentPageResult(PageQueryParam param, int totalRow, List<Comment> datas, CommentConfig commentConfig) {
		super(param, totalRow, datas);
		this.commentConfig = commentConfig;
	}

	public CommentConfig getCommentConfig() {
		return commentConfig;
	}
}