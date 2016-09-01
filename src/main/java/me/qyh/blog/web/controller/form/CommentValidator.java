package me.qyh.blog.web.controller.form;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import me.qyh.blog.entity.Comment;
import me.qyh.util.Validators;

@Component
public class CommentValidator implements Validator {

	private static final int MAX_COMMENT_LENGTH = 500;

	@Override
	public boolean supports(Class<?> clazz) {
		return Comment.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		Comment comment = (Comment) target;
		String content = comment.getContent();
		if (Validators.isEmptyOrNull(content, true)) {
			errors.reject("comment.content.blank", "回复内容不能为空");
			return;
		}
		if (content.length() > MAX_COMMENT_LENGTH) {
			errors.reject("comment.content.toolong", new Object[] { MAX_COMMENT_LENGTH },
					"回复的内容不能超过" + MAX_COMMENT_LENGTH + "个字符");
			return;
		}
	}

}
