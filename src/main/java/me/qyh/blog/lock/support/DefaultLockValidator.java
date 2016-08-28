package me.qyh.blog.lock.support;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import me.qyh.util.Validators;

public class DefaultLockValidator implements Validator {

	private static final int MAX_NAME_LENGTH = 20;
	private static final int MAX_PASSWORD_LENGTH = 16;
	private static final int MAX_QUESTION_LENGTH = 500;
	private static final int MAX_ANSWERS_LENGTH = 500;
	private static final int MAX_ANSWERS_SIZE = 10;// 答案的个数

	@Override
	public boolean supports(Class<?> clazz) {
		return DefaultLock.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		DefaultLock lock = (DefaultLock) target;
		String name = lock.getName();
		if (Validators.isEmptyOrNull(name, true)) {
			errors.reject("lock.name.empty", "锁的名称不能为空");
			return;
		}
		if (name.length() > MAX_NAME_LENGTH) {
			errors.reject("lock.name.toolong", "锁的名称不能超过" + MAX_NAME_LENGTH + "个字符");
			return;
		}
		switch (lock.getType()) {
		case PASSWORD:
			PasswordLock plock = (PasswordLock) lock;
			String password = plock.getPassword();
			if (Validators.isEmptyOrNull(password, true)) {
				errors.reject("lock.pwd.empty", "锁的密码不能为空");
				return;
			}
			if (password.length() > MAX_PASSWORD_LENGTH) {
				errors.reject("lock.pwd.toolong", "锁的密码不能超过" + MAX_PASSWORD_LENGTH + "个字符");
				return;
			}
			break;
		case QA:
			QALock qaLock = (QALock) lock;
			String question = qaLock.getQuestion();
			if (Validators.isEmptyOrNull(question, true)) {
				errors.reject("lock.question.empty", "问题不能为空");
				return;
			}
			if (question.length() > MAX_QUESTION_LENGTH) {
				errors.reject("lock.question.toolong", "问题不能超过" + MAX_QUESTION_LENGTH + "个字符");
				return;
			}

			String answers = qaLock.getAnswers();
			if (answers == null || answers.isEmpty()) {
				errors.reject("lock.answers.empty", "答案不能为空");
				return;
			}
			if (answers.length() > MAX_ANSWERS_LENGTH) {
				errors.reject("lock.answers.toolong", "答案不能超过" + MAX_ANSWERS_LENGTH + "个字符");
				return;
			}

			String[] _answers = answers.split(",");
			if (_answers.length > MAX_ANSWERS_SIZE) {
				errors.reject("lock.answers.oversize", "答案不能超过" + MAX_ANSWERS_SIZE + "个");
				return;
			}
		}
	}

}
