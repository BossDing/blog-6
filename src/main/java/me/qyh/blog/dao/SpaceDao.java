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

import me.qyh.blog.entity.Space;
import me.qyh.blog.pageparam.SpaceQueryParam;

public interface SpaceDao {

	Space selectByAlias(String alias);

	Space selectByName(String name);

	void update(Space space);

	List<Space> selectByParam(SpaceQueryParam param);

	void insert(Space space);

	Space selectById(Integer id);

	void resetDefault();

	Space selectDefault();

}
