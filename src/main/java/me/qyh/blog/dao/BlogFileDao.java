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

import me.qyh.blog.bean.BlogFileCount;
import me.qyh.blog.entity.BlogFile;
import me.qyh.blog.pageparam.BlogFileQueryParam;

public interface BlogFileDao {

	void insert(BlogFile blogFile);

	BlogFile selectById(Integer id);

	void updateWhenAddChild(BlogFile parent);

	int selectCount(BlogFileQueryParam param);

	List<BlogFile> selectPage(BlogFileQueryParam param);

	List<BlogFile> selectPath(BlogFile node);

	List<BlogFileCount> selectSubBlogFileCount(BlogFile parent);

	long selectSubBlogFileSize(BlogFile parent);

	BlogFile selectRoot();

	void update(BlogFile toUpdate);

	void updateWhenDelete(BlogFile toDelete);

	void updateWhenMove(@Param("src") BlogFile src, @Param("parent") BlogFile parent);

	void delete(BlogFile db);

	int deleteCommonFile(BlogFile db);

	List<BlogFile> selectChildren(BlogFile p);
	
	BlogFile selectByParentAndPath(@Param("parent") BlogFile parent,@Param("path") String path);
}
