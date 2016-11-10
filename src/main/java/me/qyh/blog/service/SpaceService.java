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
import me.qyh.blog.entity.SpaceConfig;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.pageparam.SpaceQueryParam;

public interface SpaceService {

	/**
	 * 添加空间
	 * 
	 * @param space
	 * @throws LogicException
	 */
	void addSpace(Space space) throws LogicException;

	/**
	 * 更新空间
	 * 
	 * @param space
	 */
	void updateSpace(Space space) throws LogicException;

	/**
	 * 根据空间名查询空间
	 * 
	 * @param spaceName
	 * @param lockCheck
	 *            是否进行锁检查
	 * @return
	 */
	Space selectSpaceByAlias(String alias, boolean lockCheck);

	/**
	 * 查询空间
	 * 
	 * @param param
	 * @return
	 */
	List<Space> querySpace(SpaceQueryParam param);

	Space getSpace(Integer id);

	Space updateConfig(Integer spaceId, SpaceConfig config) throws LogicException;

	Space deleteConfig(Integer spaceId) throws LogicException;

}
