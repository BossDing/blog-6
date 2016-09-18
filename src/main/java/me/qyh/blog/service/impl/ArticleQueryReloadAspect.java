package me.qyh.blog.service.impl;

import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import me.qyh.blog.exception.LogicException;
import me.qyh.blog.lock.LockException;
import me.qyh.blog.security.AuthencationException;

@Aspect
public class ArticleQueryReloadAspect extends TransactionSynchronizationAdapter {

	@Autowired
	private ArticleQuery articleQuery;

	private static final ThreadLocal<Throwable> throwableLocal = new ThreadLocal<>();

	@Before("@annotation(ArticleQueryReload)")
	public void registerTransactionSyncrhonization() {
		TransactionSynchronizationManager.registerSynchronization(this);
		articleQuery.waitWhileReloading();
	}

	@AfterThrowing(pointcut = "@annotation(ArticleQueryReload)", throwing = "e")
	public void afterThrow(Throwable e) {
		throwableLocal.set(e);
	}

	@Override
	public void afterCompletion(int status) {
		if (status == STATUS_ROLLED_BACK) {
			try {
				if (!noReload(throwableLocal.get())) {
					new Thread(new Runnable() {

						@Override
						public void run() {
							articleQuery.reloadStore();
						}
					}).start();
				}
			} finally {
				throwableLocal.remove();
			}
		}
	}

	private boolean noReload(Throwable e) {
		return (e != null
				&& (e instanceof LogicException || e instanceof AuthencationException || e instanceof LockException));
	}
}