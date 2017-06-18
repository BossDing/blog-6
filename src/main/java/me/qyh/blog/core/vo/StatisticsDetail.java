package me.qyh.blog.core.vo;
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