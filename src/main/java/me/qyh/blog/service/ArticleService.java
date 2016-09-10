package me.qyh.blog.service;

import java.util.List;

import me.qyh.blog.entity.Article;
import me.qyh.blog.entity.Space;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.pageparam.ArticleQueryParam;
import me.qyh.blog.pageparam.PageResult;
import me.qyh.blog.security.AuthencationException;
import me.qyh.blog.ui.widget.ArticleDateFiles;
import me.qyh.blog.ui.widget.ArticleDateFiles.ArticleDateFileMode;
import me.qyh.blog.ui.widget.ArticleSpaceFile;

public interface ArticleService {

	/**
	 * 获取一篇可以被访问的文章
	 * 
	 * @param id
	 * @throws AuthencationException
	 *             如果访问了私人博客但是没有登录
	 * @return 不存在|不可被访问 null
	 */
	Article getArticleForView(Integer id);

	/**
	 * 获取一篇可以被编辑的文章
	 * 
	 * @param id
	 * @throws LogicException
	 *             文章不存在|文章不能被编辑
	 * @return 不会为null
	 */
	Article getArticleForEdit(Integer id) throws LogicException;

	/**
	 * 随机获取一篇文章
	 * 
	 * @return
	 */
	Article getRandomArticle(ArticleQueryParam param);

	/**
	 * 查询文章日期归档
	 * 
	 * @param space
	 *            空间
	 * @param mode
	 *            归档方式
	 * @return 不会为null
	 */
	ArticleDateFiles queryArticleDateFiles(Space space, ArticleDateFileMode mode, boolean queryPrivate);

	/**
	 * 查询文章空间归档
	 * 
	 * @return 不会为null
	 */
	List<ArticleSpaceFile> queryArticleSpaceFiles(boolean queryPrivate);

	/**
	 * 分页查询文章
	 * 
	 * @param param
	 * @return
	 */
	PageResult<Article> queryArticle(ArticleQueryParam param);

	/**
	 * 根据内容提取文章中的标签
	 * 
	 * @param content
	 * @return
	 */
	List<String> getTags(String content, int max);

	/**
	 * 根据内容提取摘要
	 * 
	 * @param content
	 * @return
	 */
	String getSummary(String content, int max);

	/**
	 * 发表要发表的计划博客
	 * 
	 * @return 成功发表的数量
	 * 
	 */
	int pushScheduled();

	/**
	 * 插入|更新 文章
	 * 
	 * @param article
	 * @return
	 * @throws LogicException
	 */
	Article writeArticle(Article article) throws LogicException;

	/**
	 * 重建索引
	 */
	void rebuildIndex();

	/**
	 * 将博客放入回收站
	 * 
	 * @param id
	 * @throws LogicException
	 */
	void logicDeleteArticle(Integer id) throws LogicException;

	/**
	 * 从回收站中恢复
	 * 
	 * @param id
	 * @throws LogicException
	 */
	void recoverArticle(Integer id) throws LogicException;

	/**
	 * 删除博客
	 * 
	 * @param id
	 * @throws LogicException
	 */
	void deleteArticle(Integer id) throws LogicException;

	/**
	 * 增加博客点击数
	 * 
	 * @param id
	 * @return
	 */
	Article hit(Integer id);

	/**
	 * 发布草稿
	 * 
	 * @param id
	 * @throws LogicException
	 */
	void publishDraft(Integer id) throws LogicException;

}
