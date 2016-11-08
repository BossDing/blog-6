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
package me.qyh.blog.service;

import me.qyh.blog.entity.Tag;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.pageparam.PageResult;
import me.qyh.blog.pageparam.TagQueryParam;

public interface TagService {

	/**
	 * 分页查询标签
	 * 
	 * 
	 * @param param
	 * @return
	 */
	PageResult<Tag> queryTag(TagQueryParam param);

	/**
	 * 更新标签
	 * 
	 * @param tag
	 * @param merge
	 *            是否合并已经存在的标签
	 * @throws LogicException
	 */
	void updateTag(Tag tag, boolean merge) throws LogicException;

	/**
	 * 删除标签
	 * 
	 * @param id
	 * @throws LogicException
	 */
	void deleteTag(Integer id) throws LogicException;

}
