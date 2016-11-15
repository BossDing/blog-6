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
package me.qyh.blog.dao;

import me.qyh.blog.entity.CommentConfig;

/**
 * 
 * @author Administrator
 *
 */
public interface CommentConfigDao {

	/**
	 * 根据id删除评论配置
	 * 
	 * @param id
	 *            要删除的配置id
	 */
	void deleteById(Integer id);

	/**
	 * 更新
	 * 
	 * @param config
	 *            待更新的配置
	 */
	void update(CommentConfig config);

	/**
	 * 插入
	 * 
	 * @param config
	 *            带插入的配置
	 */
	void insert(CommentConfig config);

}
