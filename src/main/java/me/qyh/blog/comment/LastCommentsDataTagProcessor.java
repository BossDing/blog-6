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
package me.qyh.blog.comment;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import me.qyh.blog.entity.Article;
import me.qyh.blog.entity.Space;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.ui.Params;
import me.qyh.blog.ui.data.DataTagProcessor;

public class LastCommentsDataTagProcessor extends DataTagProcessor<List<Comment>> {

	private static final Integer DEFAULT_LIMIT = 10;
	private static final String LIMIT = "limit";

	private static final int MAX_LIMIT = 50;

	@Autowired
	private DftCommentService commentService;

	public LastCommentsDataTagProcessor(String name, String dataName) {
		super(name, dataName);
	}

	@Override
	protected List<Comment> buildPreviewData(Attributes attributes) {
		List<Comment> comments = new ArrayList<Comment>();
		Comment comment = new Comment();
		comment.setCommentDate(Timestamp.valueOf(LocalDateTime.now()));
		comment.setContent("测试内容");
		comment.setNickname("测试");
		comment.setEmail("test@test.com");
		comment.setAdmin(true);
		comment.setIp("127.0.0.1");
		Article article = new Article();
		article.setId(1);
		article.setTitle("测试文章标题");
		comment.setArticle(article);
		comment.setId(1);
		comments.add(comment);
		return comments;
	}

	@Override
	protected List<Comment> query(Space space, Params params, Attributes attributes) throws LogicException {
		return commentService.queryLastComments(space, getLimit(attributes));
	}

	private int getLimit(Attributes attributes) {
		int limit = DEFAULT_LIMIT;
		String v = attributes.get(LIMIT);
		if (v != null)
			try {
				limit = Integer.parseInt(v);
			} catch (Exception e) {
			}
		if (limit <= 0)
			limit = DEFAULT_LIMIT;
		if (limit > MAX_LIMIT)
			limit = MAX_LIMIT;
		return limit;
	}

}
