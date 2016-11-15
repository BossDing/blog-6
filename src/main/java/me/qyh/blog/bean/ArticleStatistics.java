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
package me.qyh.blog.bean;

import java.sql.Timestamp;

/**
 * 文章统计
 * 
 * @author Administrator
 *
 */
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
