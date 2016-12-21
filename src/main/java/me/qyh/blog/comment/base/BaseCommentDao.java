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
package me.qyh.blog.comment.base;

import java.sql.Timestamp;
import java.util.List;

import org.apache.ibatis.annotations.Param;

/**
 * 
 * @author Administrator
 *
 */
public interface BaseCommentDao<T extends BaseComment<T>> {

	/**
	 * 查询评论数(tree形式)
	 * 
	 * @param param
	 *            查询参数
	 * @return 评论数
	 */
	int selectCountWithTree(BaseCommentQueryParam param);

	/**
	 * 查询评论数(list形式)
	 * 
	 * @param param
	 *            查询参数
	 * @return 评论数
	 */
	int selectCountWithList(BaseCommentQueryParam param);

	/**
	 * 查询评论(tree形式)
	 * 
	 * @param param
	 *            查询参数
	 * @return tree形式的结果集
	 */
	List<T> selectPageWithTree(BaseCommentQueryParam param);

	/**
	 * 查询评论(list形式)
	 * 
	 * @param param
	 *            查询参数
	 * @return 结果集
	 */
	List<T> selectPageWithList(BaseCommentQueryParam param);

	/**
	 * 根据id查询评论
	 * 
	 * @param id
	 *            评论id
	 * @return 如果不存在返回null，否则返回存在的评论
	 */
	T selectById(Integer id);

	/**
	 * 插入评论
	 * 
	 * @param comment
	 *            待插入的评论
	 */
	void insert(T comment);

	/**
	 * 根据路径和状态删除对应的评论
	 * 
	 * @param path
	 *            路径
	 * @param status
	 *            状态
	 */
	void deleteByPath(String path);

	/**
	 * 根据id删除评论
	 * 
	 * @param id
	 *            评论id
	 * @return 受影响的数目
	 */
	void deleteById(Integer id);

	/**
	 * 查询某个ip在指定时间内评论的总数
	 * 
	 * @param begin
	 *            开始时间
	 * @param end
	 *            结束时间
	 * @param user
	 *            用户
	 * @return 评论数
	 */
	int selectCountByIpAndDatePeriod(@Param("begin") Timestamp begin, @Param("end") Timestamp end,
			@Param("ip") String ip);

	/**
	 * 查询当前评论的最后一条回复记录
	 * 
	 * @param current
	 *            评论
	 * @return 如果不存在返回null，否在返回最后一条记录
	 */
	T selectLast(T current);

	/**
	 * 将评论状态由审核变为普通
	 * 
	 * @param comment
	 *            评论
	 */
	void updateStatusToNormal(T comment);

}
