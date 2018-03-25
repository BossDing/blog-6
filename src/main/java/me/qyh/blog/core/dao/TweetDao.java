package me.qyh.blog.core.dao;

import java.util.Collection;
import java.util.List;

import org.apache.ibatis.annotations.Param;

import me.qyh.blog.core.entity.Tweet;
import me.qyh.blog.core.vo.TweetQueryParam;

public interface TweetDao {

	/**
	 * 查询符合条件的短博客
	 * 
	 * @param param
	 * @return
	 */
	List<Tweet> selectPage(TweetQueryParam param);

	/**
	 * 查询符合条件的短博客数目
	 * 
	 * @param param
	 * @return
	 */
	int selectCount(TweetQueryParam param);

	/**
	 * 查询指定的短博客
	 * 
	 * @param id
	 * @return
	 */
	Tweet selectById(Integer id);

	/**
	 * 根据ID删除
	 * 
	 * @param id
	 */
	void deleteById(Integer id);

	/**
	 * 查询最近的短博客
	 * 
	 * @param limit
	 * @param queryPrivate
	 * @return
	 */
	List<Tweet> selectLast(@Param("limit") int limit, @Param("queryPrivate") boolean queryPrivate);

	void insert(Tweet tweet);

	void update(Tweet tweet);

	/**
	 * @param ids
	 * @return
	 */
	List<Tweet> selectByIds(Collection<Integer> ids);

}
