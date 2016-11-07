package me.qyh.blog.service;

import java.util.List;

import me.qyh.blog.bean.ArticleDateFiles;
import me.qyh.blog.bean.ArticleDateFiles.ArticleDateFileMode;
import me.qyh.blog.bean.ArticleNav;
import me.qyh.blog.bean.ArticleSpaceFile;
import me.qyh.blog.bean.ArticleStatistics;
import me.qyh.blog.bean.TagCount;
import me.qyh.blog.entity.Article;
import me.qyh.blog.entity.Space;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.metaweblog.MetaweblogArticle;
import me.qyh.blog.pageparam.ArticleQueryParam;
import me.qyh.blog.pageparam.PageResult;
import me.qyh.blog.security.AuthencationException;

public interface ArticleService {

	/**
	 * 获取一篇可以被访问的文章
	 * 
	 * @param idOrAlias
	 *            id或者文章别名
	 * @throws AuthencationException
	 *             如果访问了私人博客但是没有登录
	 * @return 不存在|不可被访问 null
	 */
	Article getArticleForView(String idOrAlias);

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
	 * 查询文章日期归档
	 * 
	 * @param space
	 *            空间
	 * @param mode
	 *            归档方式
	 * @return 不会为null
	 */
	ArticleDateFiles queryArticleDateFiles(Space space, ArticleDateFileMode mode);

	/**
	 * 查询文章空间归档
	 * 
	 * @return 不会为null
	 */
	List<ArticleSpaceFile> queryArticleSpaceFiles();

	/**
	 * 分页查询文章
	 * 
	 * @param param
	 * @return
	 */
	PageResult<Article> queryArticle(ArticleQueryParam param);

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
	 * @param autoDraft
	 *            是否是自动保存的草稿
	 * @return
	 * @throws LogicException
	 */
	Article writeArticle(Article article, boolean autoDraft) throws LogicException;

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

	/**
	 * 上一篇，下一篇文章
	 * 
	 * @param article
	 * @return
	 */
	ArticleNav getArticleNav(Article article);

	/**
	 * 查询博客统计 <br>
	 * <strong>只会统计状态为发表的博客点击数、评论数、最近撰写日期和最后修改日期</strong>
	 * 
	 * @param space
	 * @param queryPrivate
	 * @return
	 */
	ArticleStatistics queryArticleStatistics(Space space, boolean queryHidden);

	/**
	 * 查询被文章所引用的标签集
	 * 
	 * @param space
	 * @param hasLock
	 * @param queryPrivate
	 * @return
	 */
	List<TagCount> queryTags(Space space, boolean hasLock, boolean queryPrivate);

	/**
	 * 更新metaweblog文章
	 * 
	 * @param article
	 * @return
	 * @throws LogicException
	 */
	Article writeArticle(MetaweblogArticle article) throws LogicException;
}
