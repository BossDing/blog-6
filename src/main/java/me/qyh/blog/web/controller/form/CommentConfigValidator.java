package me.qyh.blog.web.controller.form;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import me.qyh.blog.entity.CommentConfig;

@Component
public class CommentConfigValidator implements Validator {

	private static final int[] LIMIT_SECOND_RANGE = { 1, 300 };
	private static final int[] LIMIT_COUNT_RANGE = { 1, 100 };

	@Override
	public boolean supports(Class<?> clazz) {
		return CommentConfig.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		CommentConfig config = (CommentConfig) target;
		if (config.getAllowComment() == null) {
			errors.reject("commentConfig.allowComment.blank", "是否允许评论不能为空");
			return;
		}
		if (config.getCommentMode() == null) {
			errors.reject("commentConfig.commentMode.blank", "评论展现形式不能为空");
			return;
		}
		if (config.getAllowHtml() == null) {
			errors.reject("commentConfig.allowHtml.blank", "评论是否允许html不能为空");
			return;
		}
		if (config.getAsc() == null) {
			errors.reject("commentConfig.asc.blank", "评论展现排序方式不能为空");
			return;
		}
		if (config.getCheck() == null) {
			errors.reject("commentConfig.check.blank", "评论审核不能为空");
			return;
		}
		Integer limitCount = config.getLimitCount();
		if (limitCount < LIMIT_COUNT_RANGE[0] || limitCount > LIMIT_COUNT_RANGE[1]) {
			errors.reject("commentConfig.limitCount.invalid",
					new Object[] { LIMIT_COUNT_RANGE[0], LIMIT_COUNT_RANGE[1] },
					"限制评论数应该在" + LIMIT_COUNT_RANGE[0] + "和" + LIMIT_COUNT_RANGE[1] + "之间");
			return;
		}

		Integer limitSec = config.getLimitSec();
		if (limitSec < LIMIT_SECOND_RANGE[0] || limitSec > LIMIT_SECOND_RANGE[1]) {
			errors.reject("commentConfig.limitSec.invalid",
					new Object[] { LIMIT_SECOND_RANGE[0], LIMIT_SECOND_RANGE[1] },
					"限制评论时间应该在" + LIMIT_SECOND_RANGE[0] + "和" + LIMIT_SECOND_RANGE[1] + "之间");
			return;
		}

	}

}
