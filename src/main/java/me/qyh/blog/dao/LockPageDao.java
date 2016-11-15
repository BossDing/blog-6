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
import me.qyh.blog.ui.page.LockPage;

/**
 * 
 * @author Administrator
 *
 */
public interface LockPageDao {

	/**
	 * 根据空间和锁类型查询解锁页面
	 * 
	 * @param space
	 *            空间
	 * @param lockType
	 *            锁类型
	 * @return 如果不存在，返回null
	 */
	LockPage selectBySpaceAndLockType(@Param("space") Space space, @Param("lockType") String lockType);

	/**
	 * 插入解锁页面
	 * 
	 * @param lockPage
	 *            待插入的页面
	 */
	void insert(LockPage lockPage);

	/**
	 * 更新解锁页面
	 * 
	 * @param lockPage
	 *            待更新的页面
	 */
	void update(LockPage lockPage);

	/**
	 * 根据id删除对应的解锁页面
	 * 
	 * @param id
	 *            页面id
	 */
	void deleteById(Integer id);

	/**
	 * 查询id对应的解锁页面
	 * 
	 * @param id
	 *            页面id
	 * @return 如果不存在返回null
	 */
	LockPage selectById(Integer id);

}
