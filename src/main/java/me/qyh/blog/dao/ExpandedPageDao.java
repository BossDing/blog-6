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

import java.util.List;

import me.qyh.blog.ui.page.ExpandedPage;

/**
 * 
 * @author Administrator
 *
 */
public interface ExpandedPageDao {

	/**
	 * 根据id查询拓展页面
	 * 
	 * @param id
	 *            拓展页面id
	 * @return 如果不存在，返回null
	 */
	ExpandedPage selectById(Integer id);

	/**
	 * 插入拓展页面模板
	 * 
	 * @param page
	 *            待插入的拓展页面
	 */
	void insert(ExpandedPage page);

	/**
	 * 更新拓展页面
	 * 
	 * @param page
	 *            待更新的拓展页面
	 */
	void update(ExpandedPage page);

	/**
	 * 查询所有的拓展页面
	 * 
	 * @return
	 */
	List<ExpandedPage> selectAll();

	/**
	 * 根据id删除拓展页面
	 * 
	 * @param id
	 *            页面id
	 */
	void deleteById(Integer id);

}
