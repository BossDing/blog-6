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
package me.qyh.blog.service.impl;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import me.qyh.blog.evt.ArticleIndexRebuildEvent;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.lock.LockException;
import me.qyh.blog.security.AuthencationException;
import me.qyh.blog.util.ExceptionUtils;

@Aspect
public class ArticleIndexRebuildAspect extends TransactionSynchronizationAdapter
		implements ApplicationEventPublisherAware {

	private static final ThreadLocal<Throwable> throwableLocal = new ThreadLocal<>();

	private static final Class<?>[] NO_NEED_REBUILD_EXCEPTIONS = new Class<?>[] { LogicException.class,
			AuthencationException.class, LockException.class };

	private ApplicationEventPublisher applicationEventPublisher;

	@Before("@annotation(ArticleIndexRebuild)")
	public void before(JoinPoint joinPoint) throws InterruptedException {
		if (TransactionSynchronizationManager.isSynchronizationActive()
				&& !TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
			TransactionSynchronizationManager.registerSynchronization(this);
		}
	}

	@AfterThrowing(pointcut = "@annotation(ArticleIndexRebuild)", throwing = "e")
	public void afterThrow(Throwable e) {
		throwableLocal.set(e);
	}

	@Override
	public void afterCompletion(int status) {
		try {
			if (status == STATUS_ROLLED_BACK && needRebuild()) {
				this.applicationEventPublisher.publishEvent(new ArticleIndexRebuildEvent(this));
			}
		} finally {
			throwableLocal.remove();
		}
	}

	private boolean needRebuild() {
		Throwable ex = throwableLocal.get();
		if (ex != null) {
			if (ExceptionUtils.getFromChain(ex, NO_NEED_REBUILD_EXCEPTIONS).isPresent()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}
}