package me.qyh.blog.comment.base;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.PatternMatchUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import me.qyh.blog.config.UrlHelper;
import me.qyh.blog.config.UserConfig;
import me.qyh.blog.entity.User;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.security.UserContext;
import me.qyh.blog.util.Validators;

public class SimpleCommentChecker<T extends BaseComment<T>> implements CommentChecker<T> {

	private String[] disallowUsernamePatterns;
	private String[] disallowEmailPatterns;

	@Autowired
	private UrlHelper urlHelper;

	@Override
	public void checkComment(T comment, CommentConfig config) throws LogicException {
		checkCommentUser(comment);
		checkCommentContent(comment, config);
	}

	protected void checkCommentUser(T comment) throws LogicException {
		if (UserContext.get() != null) {
			return;
		}
		String email = comment.getEmail();
		String name = comment.getNickname();
		String website = comment.getWebsite();
		User user = UserConfig.get();
		String emailOrAdmin = user.getEmail();
		if (!Validators.isEmptyOrNull(emailOrAdmin, true) && emailOrAdmin.equals(email)) {
			throw new LogicException("comment.email.invalid", "邮件不被允许");
		}
		if (user.getName().equalsIgnoreCase(name)) {
			throw new LogicException("comment.nickname.invalid", "昵称不被允许");
		}
		if (disallowUsernamePatterns != null && PatternMatchUtils.simpleMatch(disallowUsernamePatterns, name.trim())) {
			throw new LogicException("comment.username.invalid", "用户名不被允许");
		}

		if (email != null && disallowEmailPatterns != null
				&& PatternMatchUtils.simpleMatch(disallowEmailPatterns, email.trim())) {
			throw new LogicException("comment.email.invalid", "邮件不被允许");
		}
		if (website != null) {
			try {

				UriComponents uc = UriComponentsBuilder.fromHttpUrl(website).build();
				String host = uc.getHost();
				if (StringUtils.endsWithIgnoreCase(host, urlHelper.getUrlConfig().getRootDomain())) {
					throw new LogicException("comment.website.invalid", "网址不被允许");
				}
			} catch (Exception e) {
				throw new LogicException("comment.website.invalid", "网址不被允许");
			}
		}
	}

	protected void checkCommentContent(T comment, CommentConfig config) throws LogicException {

	}

	public void setDisallowUsernamePatterns(String[] disallowUsernamePatterns) {
		this.disallowUsernamePatterns = disallowUsernamePatterns;
	}

	public void setDisallowEmailPatterns(String[] disallowEmailPatterns) {
		this.disallowEmailPatterns = disallowEmailPatterns;
	}
}
