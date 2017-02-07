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

import java.sql.Timestamp;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import me.qyh.blog.bean.FileStoreBean;
import me.qyh.blog.entity.Article.ArticleStatus;
import me.qyh.blog.entity.BlogFile.BlogFileType;
import me.qyh.blog.exception.LogicException;

public interface StatisticsService {

	/**
	 * 查询<b>当前空间</b>下文章统计情况
	 * <p>
	 * <b>用于DataTag</b>
	 * </p>
	 * 
	 * @return
	 * @throws LogicException
	 */
	ArticleStatistics queryArticleStatistics();

	/**
	 * 查询<b>当前空间</b>标签统计情况
	 * <p>
	 * <b>用于DataTag</b>
	 * </p>
	 * 
	 * @return
	 * @throws LogicException
	 */
	TagStatistics queryTagStatistics();

	/**
	 * 查询<b>当前空间</b>评论统计情况
	 * <p>
	 * <b>用于DataTag</b>
	 * </p>
	 * 
	 * @return
	 * @throws LogicException
	 */
	CommentStatistics queryCommentStatistics();

	/**
	 * 查询统计详情
	 * <p>
	 * <b>用于管理台统计</b>
	 * </p>
	 * 
	 * @param spaceId
	 *            空间id，如果为空，查询全部
	 * @return
	 * @throws LogicException
	 *             空间不存在
	 */
	StatisticsDetail queryStatisticsDetail(Integer spaceId) throws LogicException;

	public class ArticleStatistics {

		private Timestamp lastModifyDate;// 最后修改日期
		private Timestamp lastPubDate;
		private int totalHits;// 点击总数
		private Integer totalArticles;// 文章总数

		public ArticleStatistics() {
			super();
		}

		public ArticleStatistics(ArticleStatistics statistics) {
			super();
			this.lastModifyDate = statistics.lastModifyDate;
			this.lastPubDate = statistics.lastPubDate;
			this.totalHits = statistics.totalHits;
			this.totalArticles = statistics.totalArticles;
		}

		public Timestamp getLastModifyDate() {
			return lastModifyDate;
		}

		public void setLastModifyDate(Timestamp lastModifyDate) {
			this.lastModifyDate = lastModifyDate;
		}

		public int getTotalHits() {
			return totalHits;
		}

		public void setTotalHits(int totalHits) {
			this.totalHits = totalHits;
		}

		public Timestamp getLastPubDate() {
			return lastPubDate;
		}

		public void setLastPubDate(Timestamp lastPubDate) {
			this.lastPubDate = lastPubDate;
		}

		public Integer getTotalArticles() {
			return totalArticles;
		}

		public void setTotalArticles(Integer totalArticles) {
			this.totalArticles = totalArticles;
		}
	}

	public class ArticleDetailStatistics extends ArticleStatistics {
		private Map<ArticleStatus, Integer> statusCountMap = new EnumMap<>(ArticleStatus.class);

		public ArticleDetailStatistics(ArticleStatistics statistics) {
			super(statistics);
		}

		public Map<ArticleStatus, Integer> getStatusCountMap() {
			return statusCountMap;
		}

		public void setStatusCountMap(Map<ArticleStatus, Integer> statusCountMap) {
			this.statusCountMap = statusCountMap;
		}
	}

	public class TagStatistics {
		private int articleTagCount;// 文章标签引用量

		public int getArticleTagCount() {
			return articleTagCount;
		}

		public void setArticleTagCount(int articleTagCount) {
			this.articleTagCount = articleTagCount;
		}
	}

	public class TagDetailStatistics extends TagStatistics {
		/**
		 * 标签总数，标签没有空间的概念，所以这个值只有在空间为空的时候才会有
		 */
		private Integer total;

		public Integer getTotal() {
			return total;
		}

		public void setTotal(Integer total) {
			this.total = total;
		}
	}

	public class FileStatistics {
		private Map<BlogFileType, Integer> typeCountMap = new EnumMap<>(BlogFileType.class);
		private Map<FileStoreBean, FileCount> storeCountMap = new HashMap<>();

		public Map<BlogFileType, Integer> getTypeCountMap() {
			return typeCountMap;
		}

		public void setTypeCountMap(Map<BlogFileType, Integer> typeCountMap) {
			this.typeCountMap = typeCountMap;
		}

		public Map<FileStoreBean, FileCount> getStoreCountMap() {
			return storeCountMap;
		}

		public void setStoreCountMap(Map<FileStoreBean, FileCount> storeCountMap) {
			this.storeCountMap = storeCountMap;
		}

	}

	public class FileCount {
		private int fileCount;// 文件数量
		private long totalSize;// 文件总大小

		public int getFileCount() {
			return fileCount;
		}

		public void setFileCount(int fileCount) {
			this.fileCount = fileCount;
		}

		public long getTotalSize() {
			return totalSize;
		}

		public void setTotalSize(long totalSize) {
			this.totalSize = totalSize;
		}
	}

	public class CommentStatistics {
		private int totalArticleComments;
		private int totalUserPageComments;

		public int getTotalArticleComments() {
			return totalArticleComments;
		}

		public void setTotalArticleComments(int totalArticleComments) {
			this.totalArticleComments = totalArticleComments;
		}

		public int getTotalUserPageComments() {
			return totalUserPageComments;
		}

		public void setTotalUserPageComments(int totalUserPageComments) {
			this.totalUserPageComments = totalUserPageComments;
		}
	}

	public class PageStatistics {
		private int userPageCount;// 自定义页面数量

		public int getUserPageCount() {
			return userPageCount;
		}

		public void setUserPageCount(int userPageCount) {
			this.userPageCount = userPageCount;
		}
	}

	public class StatisticsDetail {
		private ArticleDetailStatistics articleStatistics;
		private TagDetailStatistics tagStatistics;
		private CommentStatistics commentStatistics;
		private PageStatistics pageStatistics;
		private FileStatistics fileStatistics;

		public ArticleDetailStatistics getArticleStatistics() {
			return articleStatistics;
		}

		public void setArticleStatistics(ArticleDetailStatistics articleStatistics) {
			this.articleStatistics = articleStatistics;
		}

		public TagDetailStatistics getTagStatistics() {
			return tagStatistics;
		}

		public void setTagStatistics(TagDetailStatistics tagStatistics) {
			this.tagStatistics = tagStatistics;
		}

		public CommentStatistics getCommentStatistics() {
			return commentStatistics;
		}

		public void setCommentStatistics(CommentStatistics commentStatistics) {
			this.commentStatistics = commentStatistics;
		}

		public PageStatistics getPageStatistics() {
			return pageStatistics;
		}

		public void setPageStatistics(PageStatistics pageStatistics) {
			this.pageStatistics = pageStatistics;
		}

		public FileStatistics getFileStatistics() {
			return fileStatistics;
		}

		public void setFileStatistics(FileStatistics fileStatistics) {
			this.fileStatistics = fileStatistics;
		}

	}

}
