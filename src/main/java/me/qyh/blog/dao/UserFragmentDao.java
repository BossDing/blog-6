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

import org.apache.ibatis.annotations.Param;

import me.qyh.blog.entity.Space;
import me.qyh.blog.pageparam.UserFragmentQueryParam;
import me.qyh.blog.ui.fragment.UserFragment;

/**
 * 
 * @author Administrator
 *
 */
public interface UserFragmentDao {

	/**
	 * 插入用户模板片段
	 * 
	 * @param userFragment
	 *            待插入的用户模板片段
	 */
	void insert(UserFragment userFragment);

	/**
	 * 根据id删除用户模板片段
	 * 
	 * @param id
	 *            用户模板片段id
	 */
	void deleteById(Integer id);

	/**
	 * 查询用户模板片段集合
	 * 
	 * @param param
	 *            查询参数
	 * @return 用户模板片段集
	 */
	List<UserFragment> selectPage(UserFragmentQueryParam param);

	/**
	 * 查询用户模板片段数目
	 * 
	 * @param param
	 *            查询参数
	 * @return 数目
	 */
	int selectCount(UserFragmentQueryParam param);

	/**
	 * 更新用户模板片段
	 * 
	 * @param userFragment
	 *            待更新的用户模板片段
	 */
	void update(UserFragment userFragment);

	/**
	 * 根据空间和名称查询用户模板片段
	 * 
	 * @param space
	 *            空间
	 * @param name
	 *            名称
	 * @return 如果不存在，返回null
	 */
	UserFragment selectBySpaceAndName(@Param("space") Space space, @Param("name") String name);

	/**
	 * 根据id查询用户模板片段
	 * 
	 * @param id
	 *            用户模板片段的id
	 * @return 如果不存在，返回null
	 */
	UserFragment selectById(Integer id);

	/**
	 * 根据名称查询全局用户模板片段
	 * 
	 * @param name
	 *            名称
	 * @return 如果不存在，返回null
	 */
	UserFragment selectGlobalByName(String name);

}
