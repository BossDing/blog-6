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

import java.util.List;

import org.apache.ibatis.annotations.Param;

import me.qyh.blog.core.entity.Space;
import me.qyh.blog.core.pageparam.UserPageQueryParam;
import me.qyh.blog.core.ui.page.UserPage;

/**
 * 
 * @author Administrator
 *
 */
public interface UserPageDao {

	/**
	 * 根据id查询用户自定义页面
	 * 
	 * @param id
	 *            用户自定义页面id
	 * @return 如果不存在，返回null
	 */
	UserPage selectById(Integer id);

	/**
	 * 更新用户自定义页面
	 * 
	 * @param page
	 *            待更新的用户自定义页面
	 */
	void update(UserPage page);

	/**
	 * 插入用户自定义页面
	 * 
	 * @param page
	 *            待插入的用户自定义页面
	 */
	void insert(UserPage page);

	/**
	 * 查询用户自定义页面数目
	 * 
	 * @param param
	 *            查询参数
	 * @return 数目
	 */
	int selectCount(UserPageQueryParam param);

	/**
	 * 查询用户自定义页面集合
	 * 
	 * @param param
	 *            查询参数
	 * @return 结果集
	 */
	List<UserPage> selectPage(UserPageQueryParam param);

	/**
	 * 根据id删除用户自定义页面
	 * 
	 * @param id
	 *            用户自定义页面id
	 */
	void deleteById(Integer id);

	/**
	 * 根据空间和别名查询用户自定义页面
	 * 
	 * @param space
	 *            空间
	 * @param alias
	 *            别名
	 * @return 如果不存在，返回null
	 */
	UserPage selectBySpaceAndAlias(@Param("space") Space space, @Param("alias") String alias);

	/**
	 * 查询某空间下的所有用户自定义页面
	 * 
	 * @param space
	 *            空间
	 * @return 空间下的用户自定义页面集
	 */
	List<UserPage> selectBySpace(@Param("space") Space space);

	/**
	 * 查询所有自定义页面
	 * 
	 * @return
	 */
	List<UserPage> selectAll();

}
