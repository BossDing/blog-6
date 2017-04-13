package me.qyh.blog.web.controller;

import java.util.function.Predicate;

import org.springframework.beans.factory.InitializingBean;

import me.qyh.blog.web.controller.AttemptLogger.AttemptInfo;

class AttemptLoggerController extends BaseController implements InitializingBean {

	// 当ip达到最多尝试次数后，如果一段时间(s)内没有被再次尝试，则通过定时任务删除这个记录，此时仅仅代表着该ip记录可以被删除，不代表着该ip再次尝试无需验证码
	private int sleepSec;

	private AttemptLogger attemptLogger;
	private Predicate<AttemptInfo> predicate;

	protected final boolean log(String ip) {
		return attemptLogger.log(ip);
	}

	protected final boolean reach(String ip) {
		return attemptLogger.reach(ip);
	}

	protected final void remove(String ip) {
		attemptLogger.remove(ip);
	}

	/**
	 * 这个方法用于schedule task，不应该直接调用
	 */
	public final void clearAttemptLogger() {
		attemptLogger.remove(predicate);
	}

	public void setSleepSec(int sleepSec) {
		this.sleepSec = sleepSec;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		long sleepMill = sleepSec * 1000L;
		predicate = info -> System.currentTimeMillis() - info.getLastAttemptTime() > sleepMill;
	}

	public void setAttemptLogger(AttemptLogger attemptLogger) {
		this.attemptLogger = attemptLogger;
	}

}
