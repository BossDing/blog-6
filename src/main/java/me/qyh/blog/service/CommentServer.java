package me.qyh.blog.service;

import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

import me.qyh.blog.entity.Space;

public interface CommentServer {

	/**
	 * 查询文章列表评论数
	 * 
	 * @param ids
	 *            文章ids
	 * @return 文章id和评论数的map
	 */
	Map<Integer, Integer> queryArticlesCommentCount(List<Integer> ids);

	/**
	 * 查询文章评论数
	 * 
	 * @param id
	 * @return
	 */
	OptionalInt queryArticleCommentCount(Integer id);

	/**
	 * 查询某个空间下所有文章的评论总数
	 * 
	 * @param space
	 *            空间，如果为空，查询全部
	 * @return
	 */
	int queryArticlesTotalCommentCount(Space space, boolean queryPrivate);

	/**
	 * 查询某个空间下自定义页面的评论总数
	 * 
	 * @param space
	 *            空间，如果为空，查询全部
	 * @return
	 */
	int queryUserPagesTotalCommentCount(Space space, boolean queryPrivate);

}
