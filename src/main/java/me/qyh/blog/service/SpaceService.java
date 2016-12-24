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

import java.util.List;

import me.qyh.blog.entity.Space;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.pageparam.SpaceQueryParam;

/**
 * 
 * @author Administrator
 *
 */
public interface SpaceService {

	/**
	 * 添加空间
	 * 
	 * @param space
	 *            待添加的空间
	 * @throws LogicException
	 *             添加过程中发生逻辑异常
	 */
	void addSpace(Space space) throws LogicException;

	/**
	 * 更新空间
	 * 
	 * @param space
	 *            待更新的空间
	 * @throws LogicException
	 *             更新过程中发生逻辑异常
	 */
	void updateSpace(Space space) throws LogicException;

	/**
	 * 根据空间别名查询空间
	 * 
	 * @param alias
	 *            空间别名
	 * @return 空间，如果不存在，返回null
	 */
	Space selectSpaceByAlias(String alias);

	/**
	 * 查询空间，并且进行锁检查
	 * 
	 * @param alias
	 * @return
	 */
	Space selectSpaceByAliasWithLockCheck(String alias);

	/**
	 * 查询空间
	 * 
	 * @param param
	 *            查询参数
	 * @return 空间列表
	 */
	List<Space> querySpace(SpaceQueryParam param);

	/**
	 * 根据id查询空间
	 * 
	 * @param id
	 *            空间id
	 * @return 如果不存在，返回null
	 * 
	 */
	Space getSpace(Integer id);

}
