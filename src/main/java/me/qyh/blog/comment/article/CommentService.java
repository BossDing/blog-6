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
package me.qyh.blog.comment.article;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import me.qyh.blog.comment.article.CommentDao.ArticleComments;
import me.qyh.blog.comment.base.CommentConfig;
import me.qyh.blog.comment.base.CommentConfig.CommentMode;
import me.qyh.blog.comment.base.CommentPageResult;
import me.qyh.blog.comment.base.CommentSupport;
import me.qyh.blog.config.UrlHelper.Urls;
import me.qyh.blog.entity.Article;
import me.qyh.blog.entity.Space;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.exception.SystemException;
import me.qyh.blog.security.AuthencationException;
import me.qyh.blog.security.UserContext;
import me.qyh.blog.service.CommentServer;
import me.qyh.blog.service.impl.ArticleCache;
import me.qyh.blog.web.interceptor.SpaceContext;

@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
public class CommentService extends CommentSupport<Comment, CommentDao> implements CommentServer {

	@Autowired
	private ArticleCache articleCache;

	/**
	 * 评论配置文件位置
	 */
	private static final Resource configResource = new ClassPathResource("resources/commentConfig.properties");
	private final Properties pros = new Properties();

	private CommentConfig config;

	/**
	 * 分页查询评论
	 * 
	 * @param param
	 * @return
	 */
	@Transactional(readOnly = true)
	public CommentPageResult<Comment> queryComment(CommentQueryParam param) {
		param.setPageSize(config.getPageSize());
		if (param.getArticle() == null) {
			return new CommentPageResult<>(param, 0, Collections.emptyList(), new CommentConfig(config));
		}
		Article article = articleCache.getArticleWithLockCheck(param.getArticle().getId());
		if (article == null || !article.isPublished()) {
			return new CommentPageResult<>(param, 0, Collections.emptyList(), new CommentConfig(config));
		}
		if (article.isPrivate() && UserContext.get() == null) {
			throw new AuthencationException();
		}
		if (!article.getSpace().equals(SpaceContext.get())) {
			return new CommentPageResult<>(param, 0, Collections.emptyList(), new CommentConfig(config));
		}
		return super.queryComment(param, config);
	}

	/**
	 * 插入评论
	 * 
	 * @param comment
	 * @return
	 * @throws LogicException
	 */
	public Comment insertComment(Comment comment) throws LogicException {
		Article article = articleCache.getArticleWithLockCheck(comment.getArticle().getId());
		// 博客不存在
		if (article == null || !article.getSpace().equals(SpaceContext.get()) || !article.isPublished()) {
			throw new LogicException("article.notExists", "文章不存在");
		}
		// 如果私人文章并且没有登录
		if (article.isPrivate() && UserContext.get() == null) {
			throw new AuthencationException();
		}
		if (!article.getAllowComment() && UserContext.get() == null) {
			throw new LogicException("article.notAllowComment", "文章不允许被评论");
		}
		super.insertComment(comment, config);
		comment.setArticle(article);// 用来获取文章链接
		sendEmail(comment);
		return comment;
	}

	public void checkComment(Integer id) throws LogicException {
		super.checkComment(id);
	}

	public void deleteComment(Integer id) throws LogicException {
		super.deleteComment(id);
	}

	/**
	 * 删除评论
	 * 
	 * @param ip
	 *            用户ip
	 * @param articleId
	 *            文章id
	 * @throws LogicException
	 */
	public void deleteComment(String ip, Integer articleId) throws LogicException {
		Article article = articleCache.getArticle(articleId);
		if (article == null) {
			throw new LogicException("article.notExists", "文章不存在");
		}
		commentDao.deleteByIpAndArticle(ip, article);
	}

	/**
	 * 查询某空间下最近的评论
	 * 
	 * @param space
	 *            空间
	 * @param limit
	 *            数目限制
	 * @param queryAdmin
	 *            是否包含管理员
	 * @return
	 */
	@Transactional(readOnly = true)
	public List<Comment> queryLastComments(Space space, int limit, boolean queryAdmin) {
		List<Comment> comments = commentDao.selectLastComments(space, limit, UserContext.get() != null, queryAdmin);
		for (Comment comment : comments) {
			completeComment(comment);
		}
		return comments;
	}

	/**
	 * 查询会话
	 * 
	 * @param articleId
	 *            文章id
	 * @param id
	 *            当前评论id
	 * @return
	 * @throws LogicException
	 */
	@Transactional(readOnly = true)
	public List<Comment> queryConversations(Integer articleId, Integer id) throws LogicException {
		Article article = articleCache.getArticleWithLockCheck(articleId);
		if (article == null) {
			throw new LogicException("article.notExists", "文章不存在");
		}
		if (!article.isPublished()) {
			return Collections.emptyList();
		}
		if (article.isPrivate() && UserContext.get() == null) {
			throw new AuthencationException();
		}
		if (!article.getSpace().equals(SpaceContext.get())) {
			return Collections.emptyList();
		}
		Comment comment = commentDao.selectById(id);
		if (comment == null) {
			throw new LogicException("comment.notExists", "评论不存在");
		}
		if (!article.equals(comment.getArticle())) {
			return Collections.emptyList();
		}
		if (comment.getParents().isEmpty()) {
			return Arrays.asList(comment);
		}
		List<Comment> comments = Lists.newArrayList();
		for (Integer pid : comment.getParents()) {
			Comment p = commentDao.selectById(pid);
			completeComment(p);
			comments.add(p);
		}
		completeComment(comment);
		comments.add(comment);
		return comments;
	}

	/**
	 * 获取评论配置
	 * 
	 * @return
	 */
	public CommentConfig getCommentConfig() {
		return new CommentConfig(config);
	}

	@Override
	public int queryArticlesTotalCommentCount(Space space, boolean queryPrivate) {
		return commentDao.selectArticlesTotalCommentCount(space, queryPrivate);
	}

	@Override
	@Transactional(readOnly = true)
	public Map<Integer, Integer> queryArticlesCommentCount(List<Integer> ids) {
		List<ArticleComments> results = commentDao.selectArticlesCommentCount(ids);
		Map<Integer, Integer> map = Maps.newHashMap();
		for (ArticleComments result : results) {
			map.put(result.getId(), result.getComments());
		}
		return map;
	}

	@Override
	@Transactional(readOnly = true)
	public int queryArticleCommentCount(Integer id) {
		return commentDao.selectArticleCommentCount(id);
	}

	@Override
	public void deleteComments(Article article) {
		commentDao.deleteByArticle(article);
	}

	/**
	 * 更新评论配置
	 * 
	 * @param config
	 *            配置
	 */
	public synchronized void updateCommentConfig(CommentConfig config) {
		pros.setProperty(COMMENT_ALLOW_HTML, config.getAllowHtml().toString());
		pros.setProperty(COMMENT_ASC, config.getAsc().toString());
		pros.setProperty(COMMENT_CHECK, config.getCheck().toString());
		pros.setProperty(COMMENT_LIMIT_COUNT, config.getLimitCount().toString());
		pros.setProperty(COMMENT_LIMIT_SEC, config.getLimitSec().toString());
		pros.setProperty(COMMENT_MODE, config.getCommentMode().name());
		pros.setProperty(COMMENT_PAGESIZE, config.getPageSize() + "");
		try (OutputStream os = new FileOutputStream(configResource.getFile())) {
			pros.store(os, "");
		} catch (IOException e) {
			throw new SystemException(e.getMessage(), e);
		}
		loadConfig();
	}

	@Override
	public void afterPropertiesSet() throws Exception {

		super.afterPropertiesSet();
		// 读取配置文件内容
		try (InputStream is = configResource.getInputStream()) {
			pros.load(is);
			loadConfig();
		}
	}

	@Override
	protected void completeComment(Comment comment) {
		super.completeComment(comment);
		Article article = comment.getArticle();
		if (article != null) {
			Urls urls = urlHelper.getUrls();
			if (urls.detectArticleUrl(article)) {
				comment.setUrl(urls.getUrl(article));
			}
		}
	}

	private void loadConfig() {
		config = new CommentConfig();
		config.setAllowHtml(Boolean.parseBoolean(pros.getProperty(COMMENT_ALLOW_HTML, "false")));
		config.setAsc(Boolean.parseBoolean(pros.getProperty(COMMENT_ASC, "true")));
		config.setCheck(Boolean.parseBoolean(pros.getProperty(COMMENT_CHECK, "false")));
		String commentMode = pros.getProperty(COMMENT_MODE);
		if (commentMode == null) {
			config.setCommentMode(CommentMode.LIST);
		} else {
			config.setCommentMode(CommentMode.valueOf(commentMode));
		}
		config.setLimitCount(Integer.parseInt(pros.getProperty(COMMENT_LIMIT_COUNT, "10")));
		config.setLimitSec(Integer.parseInt(pros.getProperty(COMMENT_LIMIT_SEC, "60")));
		config.setPageSize(Integer.parseInt(pros.getProperty(COMMENT_PAGESIZE, "10")));
	}
}
