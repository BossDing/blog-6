package me.qyh.blog.core.validator;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import me.qyh.blog.core.entity.Tweet;
import me.qyh.blog.core.util.Validators;

@Component
public class TweetValidator implements Validator {

	private static final int MAX_CONTENT_LENGTH = 2000;

	@Override
	public boolean supports(Class<?> clazz) {
		return Tweet.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		Tweet tweet = (Tweet) target;
		String content = tweet.getContent();
		if (Validators.isEmptyOrNull(content, true)) {
			errors.reject("tweet.content.blank", "内容不能为空");
			return;
		}
		if (content.length() > MAX_CONTENT_LENGTH) {
			errors.reject("tweet.content.toolong", new Object[] { MAX_CONTENT_LENGTH },
					"内容不能超过" + MAX_CONTENT_LENGTH + "个字符");
			return;
		}
		if (tweet.getIsPrivate() == null) {
			errors.reject("tweet.private.blank", "是否私人不能为空");
			return;
		}
		if (tweet.getAllowComment() == null) {
			errors.reject("tweet.allowComment.blank", "是否允许评论不能为空");
			return;
		}
	}

}
