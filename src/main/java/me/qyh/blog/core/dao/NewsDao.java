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

import java.util.Collection;
import java.util.List;

import org.apache.ibatis.annotations.Param;

import me.qyh.blog.core.entity.News;
import me.qyh.blog.core.vo.NewsQueryParam;
import me.qyh.blog.core.vo.NewsStatistics;

public interface NewsDao {

	/**
	 * 查询符合条件的动态
	 * 
	 * @param param
	 * @return
	 */
	List<News> selectPage(NewsQueryParam param);

	/**
	 * 查询符合条件的动态数目
	 * 
	 * @param param
	 * @return
	 */
	int selectCount(NewsQueryParam param);

	/**
	 * 查询指定的动态
	 * 
	 * @param id
	 * @return
	 */
	News selectById(Integer id);

	/**
	 * 根据ID删除
	 * 
	 * @param id
	 */
	void deleteById(Integer id);

	/**
	 * 查询最近的动态
	 * 
	 * @param limit
	 * @param queryPrivate
	 * @return
	 */
	List<News> selectLast(@Param("limit") int limit, @Param("queryPrivate") boolean queryPrivate);

	void insert(News news);

	void update(News news);

	/**
	 * @param ids
	 * @return
	 */
	List<News> selectByIds(Collection<Integer> ids);

	/**
	 * @since 6.1
	 * @return
	 */
	NewsStatistics selectStatistics(@Param("queryPrivate") boolean queryPrivate);

}
