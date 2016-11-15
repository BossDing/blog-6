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

import org.apache.ibatis.annotations.Param;

import me.qyh.blog.entity.Space;
import me.qyh.blog.ui.page.ErrorPage;
import me.qyh.blog.ui.page.ErrorPage.ErrorCode;

/**
 * 
 * @author Administrator
 *
 */
public interface ErrorPageDao {

	/**
	 * 根据空间和错误码查询错误页面
	 * 
	 * @param space
	 *            空间
	 * @param errorCode
	 *            错误码
	 * @return 如果不存在，返回null
	 */
	ErrorPage selectBySpaceAndErrorCode(@Param("space") Space space, @Param("errorCode") ErrorCode errorCode);

	/**
	 * 插入错误页面
	 * 
	 * @param errorPage
	 *            带插入的错误页面
	 */
	void insert(ErrorPage errorPage);

	/**
	 * 更新错误页面
	 * 
	 * @param errorPage
	 *            待更新的错误页面
	 */
	void update(ErrorPage errorPage);

	/**
	 * 根据id删除页面
	 * 
	 * @param id
	 *            页面id
	 */
	void deleteById(Integer id);

	/**
	 * 根据id查询错误页面
	 * 
	 * @param id
	 *            页面id
	 * @return 如果不存在返回null
	 * 
	 */
	ErrorPage selectById(Integer id);

}
