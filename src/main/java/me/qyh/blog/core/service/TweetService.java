package me.qyh.blog.core.service;

import java.util.List;
import java.util.Optional;

import me.qyh.blog.core.entity.Tweet;
import me.qyh.blog.core.exception.LogicException;
import me.qyh.blog.core.vo.PageResult;
import me.qyh.blog.core.vo.TweetQueryParam;

public interface TweetService {

	/**
	 * 分页查询短博客
	 * 
	 * @param param
	 * @return
	 */
	PageResult<Tweet> queryTweet(TweetQueryParam param);

	/**
	 * 保存短博客
	 * 
	 * @param tweet
	 * @throws LogicException
	 */
	void saveTweet(Tweet tweet) throws LogicException;

	/**
	 * 更新短博客
	 * 
	 * @param tweet
	 * @throws LogicException
	 */
	void updateTweet(Tweet tweet) throws LogicException;

	/**
	 * 查询指定的短博客
	 * 
	 * @param id
	 * @return
	 * @throws LogicException
	 */
	Optional<Tweet> getTweet(Integer id);

	/**
	 * 删除指的短博客
	 * 
	 * @param id
	 * @throws LogicException
	 */
	void deleteTweet(Integer id) throws LogicException;

	/**
	 * 查询最近的短博客
	 * 
	 * @param limit
	 * @return
	 */
	List<Tweet> queryLastTweets(int limit);

}
