package me.qyh.blog.core.dao;

import java.util.Collection;
import java.util.List;

import org.apache.ibatis.annotations.Param;

import me.qyh.blog.core.entity.News;
import me.qyh.blog.core.vo.NewsQueryParam;

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

}
