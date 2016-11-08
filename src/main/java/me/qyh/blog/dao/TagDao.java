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

import me.qyh.blog.entity.Tag;
import me.qyh.blog.pageparam.TagQueryParam;

public interface TagDao {

	void insert(Tag tag);

	Tag selectByName(String name);

	int selectCount(TagQueryParam param);

	List<Tag> selectPage(TagQueryParam param);

	void update(Tag tag);

	List<Tag> selectAll();

	Tag selectById(Integer id);

	void deleteById(Integer id);
}
