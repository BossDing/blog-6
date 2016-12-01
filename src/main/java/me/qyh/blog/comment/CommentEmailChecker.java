package me.qyh.blog.comment;

import me.qyh.blog.config.UserConfig;
import me.qyh.blog.exception.LogicException;
import me.qyh.util.Validators;

public abstract class CommentEmailChecker {

	/**
	 * 检查邮箱是否被允许
	 * 
	 * @param user
	 *            评论人
	 * @param email
	 *            邮箱
	 * @throws LogicException
	 *             检查未通过
	 */
	public final void doCheck(final String email) throws LogicException {
		String emailOrAdmin = UserConfig.get().getEmail();
		if (!Validators.isEmptyOrNull(emailOrAdmin, true) && emailOrAdmin.equals(email))
			throw new LogicException("comment.email.invalid", "邮件不被允许");
		checkMore(email);
	}

	protected abstract void checkMore(final String email) throws LogicException;
}