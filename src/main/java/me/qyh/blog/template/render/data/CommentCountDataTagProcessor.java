package me.qyh.blog.template.render.data;

import org.springframework.beans.factory.annotation.Autowired;

import me.qyh.blog.core.exception.LogicException;
import me.qyh.blog.core.service.CommentServer;

public class CommentCountDataTagProcessor extends DataTagProcessor<Integer> {

	@Autowired
	private CommentServer commentServer;

	public CommentCountDataTagProcessor(String name, String dataName) {
		super(name, dataName);
	}

	@Override
	protected Integer query(Attributes attributes) throws LogicException {

		String moduleType = attributes.get("moduleType");
		String moduleId = attributes.get("moduleId");
		if (moduleType != null && moduleId != null) {
			try {
				return commentServer.queryCommentNum(moduleType, Integer.parseInt(moduleId)).orElse(0);
			} catch (NumberFormatException e) {
				// ignore
			}
		}
		return 0;
	}

}
