package me.qyh.blog.core.service;

import java.util.List;
import java.util.Optional;

import me.qyh.blog.core.entity.News;
import me.qyh.blog.core.exception.LogicException;
import me.qyh.blog.core.vo.NewsQueryParam;
import me.qyh.blog.core.vo.PageResult;

public interface NewsService {

	/**
	 * 分页查询动态
	 * 
	 * @param param
	 * @return
	 */
	PageResult<News> queryNews(NewsQueryParam param);

	/**
	 * 保存动态
	 * 
	 * @param news
	 * @throws LogicException
	 */
	void saveNews(News news) throws LogicException;

	/**
	 * 更新动态
	 * 
	 * @param news
	 * @throws LogicException
	 */
	void updateNews(News news) throws LogicException;

	/**
	 * 查询指定的动态
	 * 
	 * @param id
	 * @return
	 * @throws LogicException
	 */
	Optional<News> getNews(Integer id);

	/**
	 * 删除指的动态
	 * 
	 * @param id
	 * @throws LogicException
	 */
	void deleteNews(Integer id) throws LogicException;

	/**
	 * 查询最近的动态
	 * 
	 * @param limit
	 * @return
	 */
	List<News> queryLastNews(int limit);

}
