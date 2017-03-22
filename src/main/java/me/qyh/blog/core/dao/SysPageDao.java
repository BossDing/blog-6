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
package me.qyh.blog.core.dao;

import org.apache.ibatis.annotations.Param;

import me.qyh.blog.core.entity.Space;
import me.qyh.blog.core.ui.page.SysPage;
import me.qyh.blog.core.ui.page.SysPage.PageTarget;

/**
 * 
 * @author Administrator
 *
 */
public interface SysPageDao {

	/**
	 * 根据空间和页面类型查询系统页面
	 * 
	 * @param space
	 *            空间
	 * @param target
	 *            页面类型
	 * @return 如果不存在，返回null
	 */
	SysPage selectBySpaceAndPageTarget(@Param("space") Space space, @Param("pageTarget") PageTarget target);

	/**
	 * 插入系统页面
	 * 
	 * @param sysPage
	 *            待插入的系统页面
	 */
	void insert(SysPage sysPage);

	/**
	 * 更新系统页面
	 * 
	 * @param sysPage
	 *            待更新的系统页面
	 */
	void update(SysPage sysPage);

	/**
	 * 根据id删除系统页面
	 * 
	 * @param id
	 *            页面id
	 */
	void deleteById(Integer id);

	/**
	 * 根据id查询系统页面
	 * 
	 * @param id
	 *            页面id
	 * @return 如果不存在，返回null
	 */
	SysPage selectById(Integer id);

}
