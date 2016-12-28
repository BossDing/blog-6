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

import java.util.List;

import org.apache.ibatis.annotations.Param;

import me.qyh.blog.comment.base.BaseCommentDao;

public interface ModuleCommentDao extends BaseCommentDao<ModuleComment> {
	/**
	 * 查询某个模块下最后的几条评论
	 * 
	 * @param module
	 *            模块，如果为空，查询全部
	 * @param limit
	 *            总数
	 * @param queryAdmin
	 *            是否查询管理员的评论
	 * @return 评论集
	 */
	List<ModuleComment> selectLastComments(@Param("module") CommentModule module, @Param("limit") int limit,
			@Param("queryAdmin") boolean queryAdmin);
}
