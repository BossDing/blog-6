package me.qyh.blog.lock.support;

import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import me.qyh.blog.exception.SystemException;
import me.qyh.blog.input.JsonHtmlXssSerializer;
import me.qyh.blog.lock.LockKey;
import me.qyh.blog.lock.InvalidKeyException;
import me.qyh.blog.lock.ErrorKeyException;
import me.qyh.blog.message.Message;

/**
 * 问答锁
 * 
 * @author Administrator
 *
 */
public class QALock extends SysLock {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final String ANSWER_PARAMETER = "answers";

	@JsonSerialize(using = JsonHtmlXssSerializer.class)
	private String question;
	@JsonSerialize(using = JsonHtmlXssSerializer.class)
	private String answers;

	@Override
	public LockKey getKeyFromRequest(HttpServletRequest request) throws InvalidKeyException {
		final String answer = request.getParameter(ANSWER_PARAMETER);
		if (answer == null || answer.isEmpty()) {
			throw new InvalidKeyException(new Message("lock.qa.answer.blank", "请填写问题答案"));
		}
		return new LockKey() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public Object getKey() {
				return answer;
			}
		};
	}

	@Override
	public void tryOpen(LockKey key) throws ErrorKeyException {
		if (key != null) {
			Object data = key.getKey();
			if (data != null) {
				String answer = data.toString();
				if (isCorrectAnswer(answer)) {
					return;
				}
			}
		}
		throw new ErrorKeyException(new Message("lock.qa.unlock.fail", "答案错误"));
	}

	private boolean isCorrectAnswer(String answer) {
		if (answers == null) {
			throw new SystemException("问答锁答案不能为空");
		}
		for (String corretAnswer : answers.split(",")) {
			if (corretAnswer.equals(answer)) {
				return true;
			}
		}
		return false;
	}

	public String getQuestion() {
		return question;
	}

	public void setQuestion(String question) {
		this.question = question;
	}

	public String getAnswers() {
		return answers;
	}

	public void setAnswers(String answers) {
		this.answers = answers;
	}

	public QALock() {
		super(SysLockType.QA);
	}

}
