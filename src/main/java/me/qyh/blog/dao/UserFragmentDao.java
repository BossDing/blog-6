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

public interface UserFragmentDao {

	void insert(UserFragment userFragment);

	void deleteById(Integer id);

	List<UserFragment> selectPage(UserFragmentQueryParam param);

	int selectCount(UserFragmentQueryParam param);

	void update(UserFragment userFragment);

	UserFragment selectBySpaceAndName(@Param("space") Space space, @Param("name") String name);

	UserFragment selectById(Integer id);

	UserFragment selectGlobalByName(String name);

}
