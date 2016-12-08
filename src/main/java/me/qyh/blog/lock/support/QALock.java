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
package me.qyh.blog.lock.support;

import javax.servlet.http.HttpServletRequest;

import com.google.common.base.Splitter;
import com.google.gson.annotations.Expose;

import me.qyh.blog.exception.LogicException;
import me.qyh.blog.exception.SystemException;
import me.qyh.blog.lock.LockKey;
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

	private String question;
	@Expose(serialize = false, deserialize = true)
	private String answers;

	/**
	 * default
	 */
	public QALock() {
		super(SysLockType.QA);
	}

	@Override
	public LockKey getKeyFromRequest(HttpServletRequest request) throws LogicException {
		final String answer = request.getParameter(ANSWER_PARAMETER);
		if (answer == null || answer.isEmpty()) {
			throw new LogicException(new Message("lock.qa.answer.blank", "请填写问题答案"));
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
	public void tryOpen(LockKey key) throws LogicException {
		if (key != null) {
			Object data = key.getKey();
			if (data != null) {
				String answer = data.toString();
				if (isCorrectAnswer(answer)) {
					return;
				}
			}
		}
		throw new LogicException(new Message("lock.qa.unlock.fail", "答案错误"));
	}

	private boolean isCorrectAnswer(String answer) {
		if (answers == null) {
			throw new SystemException("问答锁答案不能为空");
		}
		for (String corretAnswer : Splitter.on(',').split(answers)) {
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
}
