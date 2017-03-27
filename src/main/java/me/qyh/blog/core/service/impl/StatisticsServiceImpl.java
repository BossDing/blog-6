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
package me.qyh.blog.core.service.impl;

import java.util.EnumMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import me.qyh.blog.core.bean.BlogFileCount;
import me.qyh.blog.core.bean.FileStoreBean;
import me.qyh.blog.core.dao.ArticleDao;
import me.qyh.blog.core.dao.ArticleTagDao;
import me.qyh.blog.core.dao.BlogFileDao;
import me.qyh.blog.core.dao.BlogFileDao.FileCountBean;
import me.qyh.blog.core.dao.PageDao;
import me.qyh.blog.core.dao.TagDao;
import me.qyh.blog.core.entity.Article.ArticleStatus;
import me.qyh.blog.core.entity.BlogFile;
import me.qyh.blog.core.entity.Space;
import me.qyh.blog.core.exception.LogicException;
import me.qyh.blog.core.exception.SystemException;
import me.qyh.blog.core.file.FileManager;
import me.qyh.blog.core.pageparam.ArticleQueryParam;
import me.qyh.blog.core.pageparam.TagQueryParam;
import me.qyh.blog.core.pageparam.TemplatePageQueryParam;
import me.qyh.blog.core.security.EnsureLogin;
import me.qyh.blog.core.security.Environment;
import me.qyh.blog.core.service.CommentServer;
import me.qyh.blog.core.service.StatisticsService;

@Service
@Transactional(readOnly = true)
public class StatisticsServiceImpl implements StatisticsService {

	@Autowired
	private ArticleDao articleDao;
	@Autowired
	private ArticleTagDao articleTagDao;
	@Autowired
	private TagDao tagDao;
	@Autowired
	private BlogFileDao blogFileDao;
	@Autowired
	private CommentServer commentServer;
	@Autowired
	private PageDao pageDao;
	@Autowired
	private FileManager fileManager;
	@Autowired
	private SpaceCache spaceCache;

	@Override
	public ArticleStatistics queryArticleStatistics() {
		return articleDao.selectStatistics(Environment.getSpace(), Environment.isLogin());
	}

	@Override
	public TagStatistics queryTagStatistics() {
		TagStatistics tagStatistics = new TagStatistics();
		boolean queryPrivate = Environment.isLogin();
		tagStatistics
				.setArticleTagCount(articleTagDao.selectTagsCount(Environment.getSpace(), queryPrivate));
		return tagStatistics;
	}

	@Override
	public CommentStatistics queryCommentStatistics() {
		return queryCommentStatistics(Environment.getSpace());
	}

	@Override
	@EnsureLogin
	public StatisticsDetail queryStatisticsDetail(Integer spaceId) throws LogicException {
		Space space = spaceCache.checkSpace(spaceId);
		// 文章统计
		StatisticsDetail statisticsDetail = new StatisticsDetail();
		statisticsDetail.setArticleStatistics(queryArticleDetailStatistics(space));
		statisticsDetail.setCommentStatistics(queryCommentStatistics(space));
		statisticsDetail.setTagStatistics(queryTagDetailStatistics(space));
		statisticsDetail.setPageStatistics(queryPageStatistics(space));

		if (space == null) {
			statisticsDetail.setFileStatistics(queryFileStatistics());
		}

		return statisticsDetail;
	}

	private CommentStatistics queryCommentStatistics(Space space) {
		CommentStatistics commentStatistics = new CommentStatistics();
		boolean queryPrivate = Environment.isLogin();
		commentStatistics.setTotalArticleComments(commentServer.queryArticlesTotalCommentCount(space, queryPrivate));
		commentStatistics.setTotalPageComments(commentServer.queryPagesTotalCommentCount(space, queryPrivate));
		return commentStatistics;
	}

	private ArticleDetailStatistics queryArticleDetailStatistics(Space space) {
		ArticleDetailStatistics articleDetailStatistics = new ArticleDetailStatistics(
				articleDao.selectAllStatistics(space));
		ArticleQueryParam param = new ArticleQueryParam();
		param.setQueryPrivate(true);
		param.setSpace(space);
		Map<ArticleStatus, Integer> countMap = new EnumMap<>(ArticleStatus.class);
		for (ArticleStatus status : ArticleStatus.values()) {
			param.setStatus(status);
			countMap.put(status, articleDao.selectCount(param));
		}
		articleDetailStatistics.setStatusCountMap(countMap);
		return articleDetailStatistics;
	}

	private TagDetailStatistics queryTagDetailStatistics(Space space) {
		TagDetailStatistics tagDetailStatistics = new TagDetailStatistics();
		tagDetailStatistics.setArticleTagCount(articleTagDao.selectAllTagsCount(space));
		if (space == null) {
			tagDetailStatistics.setTotal(tagDao.selectCount(new TagQueryParam()));
		}
		return tagDetailStatistics;
	}

	private FileStatistics queryFileStatistics() {
		FileStatistics fileStatistics = new FileStatistics();
		BlogFile root = blogFileDao.selectRoot();
		fileStatistics.setTypeCountMap(blogFileDao.selectSubBlogFileCount(root).stream()
				.collect(Collectors.toMap(BlogFileCount::getType, BlogFileCount::getCount)));
		fileStatistics.setStoreCountMap(blogFileDao.selectFileCount().stream()
				.collect(Collectors.toMap(this::wrap, FileCountBean::getFileCount)));
		return fileStatistics;
	}

	private FileStoreBean wrap(FileCountBean fcb) {
		int fileStore = fcb.getFileStore();
		return fileManager.getFileStore(fileStore).map(FileStoreBean::new)
				.orElseThrow(() -> new SystemException("文件存储器:" + fileStore + "不存在"));
	}

	private PageStatistics queryPageStatistics(Space space) {
		PageStatistics pageStatistics = new PageStatistics();
		TemplatePageQueryParam param = new TemplatePageQueryParam();
		param.setSpace(space);
		pageStatistics.setPageCount(pageDao.selectCount(param));

		return pageStatistics;
	}

}
