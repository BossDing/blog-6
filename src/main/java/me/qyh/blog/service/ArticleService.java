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
package me.qyh.blog.service;

import java.util.List;
import java.util.Optional;

import me.qyh.blog.api.metaweblog.MetaweblogArticle;
import me.qyh.blog.bean.ArticleDateFiles;
import me.qyh.blog.bean.ArticleDateFiles.ArticleDateFileMode;
import me.qyh.blog.bean.ArticleNav;
import me.qyh.blog.bean.ArticleSpaceFile;
import me.qyh.blog.bean.TagCount;
import me.qyh.blog.entity.Article;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.pageparam.ArticleQueryParam;
import me.qyh.blog.pageparam.PageResult;
import me.qyh.blog.security.AuthencationException;

/**
 * 
 * @author Administrator
 *
 */
public interface ArticleService {

	/**
	 * 获取一篇可以被访问的文章
	 * 
	 * @param idOrAlias
	 *            id或者文章别名
	 * @throws AuthencationException
	 *             如果访问了私人博客但是没有登录
	 * @return
	 */
	Optional<Article> getArticleForView(String idOrAlias);

	/**
	 * 获取一篇可以被编辑的文章
	 * 
	 * @param id
	 *            文章id
	 * @throws LogicException
	 *             如果文章不存在
	 * @return 文章
	 */
	Article getArticleForEdit(Integer id) throws LogicException;

	/**
	 * 查询<b>当前空间</b>文章日期归档
	 * <p>
	 * <b>用于DataTag</b>
	 * </p>
	 * 
	 * @param space
	 *            空间
	 * @param mode
	 *            归档方式
	 * @return 文章归档
	 * @throws LogicException
	 *             空间不存在
	 */
	ArticleDateFiles queryArticleDateFiles(ArticleDateFileMode mode) throws LogicException;

	/**
	 * 查询文章空间归档
	 * 
	 * @return 文章空间归档集合
	 */
	List<ArticleSpaceFile> queryArticleSpaceFiles();

	/**
	 * 分页查询文章
	 * 
	 * @param param
	 *            查询参数
	 * @return 文章分页对象
	 */
	PageResult<Article> queryArticle(ArticleQueryParam param);

	/**
	 * 发表要发表的计划博客
	 * 
	 * @return 成功发表的数量
	 * 
	 */
	int publishScheduled();

	/**
	 * 插入|更新 文章
	 * 
	 * <b>自动保存的文章将会被设置为DRAFT</b>
	 * 
	 * @param article
	 *            文章
	 * @return 插入后的文章
	 * @throws LogicException
	 */
	Article writeArticle(Article article) throws LogicException;

	/**
	 * 将博客放入回收站
	 * 
	 * @param id
	 *            文章id
	 * @throws LogicException
	 */
	void logicDeleteArticle(Integer id) throws LogicException;

	/**
	 * 从回收站中恢复
	 * 
	 * @param id
	 *            文章id
	 * @throws LogicException
	 */
	void recoverArticle(Integer id) throws LogicException;

	/**
	 * 删除博客
	 * 
	 * @param id
	 *            文章id
	 * @throws LogicException
	 */
	void deleteArticle(Integer id) throws LogicException;

	/**
	 * 增加文章点击数
	 * 
	 * @param id
	 *            文章id
	 * @return 当前点击数
	 */
	void hit(Integer id);

	/**
	 * 发布草稿
	 * 
	 * @param id
	 *            草稿id
	 * @throws LogicException
	 */
	void publishDraft(Integer id) throws LogicException;

	/**
	 * 上一篇，下一篇文章
	 * 
	 * @param idOrAlias
	 *            文章的id或者别名
	 * @return 当前文章的上一篇下一篇，如果都没有，返回null
	 */
	Optional<ArticleNav> getArticleNav(String idOrAlias);

	/**
	 * 查询<b>当前空间</b>被文章引用的标签数量
	 * <p>
	 * <b>用于DataTag</b>
	 * </p>
	 * 
	 * @return 标签集
	 */
	List<TagCount> queryTags() throws LogicException;

	/**
	 * 更新metaweblog文章
	 * 
	 * @param article
	 *            metaweblog 撰写的文章
	 * @return 保存后的文章
	 * @throws LogicException
	 */
	Article writeArticle(MetaweblogArticle article) throws LogicException;

	/**
	 * 查询最近的文章
	 * 
	 * @param limit
	 *            最大返回数目限制
	 * @return 最近的文章
	 */
	List<Article> queryRecentArticles(Integer limit);

	/**
	 * 查询类似文章
	 * 
	 * @param idOrAlias
	 *            文章id或者别名
	 * @param limit
	 *            最大数目
	 * @return 类似文章集合
	 * @throws LogicException
	 */
	List<Article> findSimilar(String idOrAlias, int limit) throws LogicException;


	/**
	 * 用来处理预览文章，比如将markdown转化为html
	 * 
	 * @param article
	 *            预览文章
	 */
	void preparePreview(Article article);

}
