package me.qyh.blog.bean;

import java.sql.Timestamp;

public class ArticleStatistics {

	private Timestamp lastModifyDate;// 最后修改日期
	private Timestamp lastPubDate;
	private int totalHits;// 点击总数
	private int totalComments;// 评论总数

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

	public int getTotalComments() {
		return totalComments;
	}

	public void setTotalComments(int totalComments) {
		this.totalComments = totalComments;
	}

	public Timestamp getLastPubDate() {
		return lastPubDate;
	}

	public void setLastPubDate(Timestamp lastPubDate) {
		this.lastPubDate = lastPubDate;
	}
}
