package me.qyh.blog.service;

import java.util.List;
import java.util.Map;

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
	int queryArticleCommentCount(Integer id);

	/**
	 * 查询总的文章评论数
	 * 
	 * @param space
	 *            空间，如果为空则查询全部
	 * @param queryPrivate
	 *            是否查询私人博客
	 * @param queryHidden
	 *            是否查询隐藏博客
	 * @return
	 */
	int queryArticlesTotalCommentCount(Space space, boolean queryPrivate, boolean queryHidden);
	
}
