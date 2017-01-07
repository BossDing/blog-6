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

import java.lang.reflect.Method;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import me.qyh.blog.exception.LogicException;
import me.qyh.blog.exception.SystemException;
import me.qyh.blog.lock.LockException;
import me.qyh.blog.security.AuthencationException;

@Aspect
public class ArticleIndexRebuildAspect extends TransactionSynchronizationAdapter {
	private static final ThreadLocal<Throwable> throwableLocal = new ThreadLocal<>();

	@Autowired
	private ThreadPoolTaskExecutor threadPoolTaskExecutor;
	@Autowired
	private ArticleIndexer articleIndexer;

	@Before("@annotation(ArticleIndexRebuild)")
	public void before(JoinPoint joinPoint) throws InterruptedException {
		MethodSignature signature = (MethodSignature) joinPoint.getSignature();
		Method method = signature.getMethod();
		String methodName = method.getName();
		Class<?>[] parameterTypes = method.getParameterTypes();
		try {
			method = joinPoint.getTarget().getClass().getMethod(methodName, parameterTypes);
		} catch (Exception e) {
			throw new SystemException(e.getMessage(), e);
		}
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
			if (status == STATUS_ROLLED_BACK && !noReload()) {
				threadPoolTaskExecutor.execute(articleIndexer::rebuildIndex);
			}
		} finally {
			throwableLocal.remove();
		}
	}

	private boolean noReload() {
		Throwable e = throwableLocal.get();
		return e != null
				&& (e instanceof LogicException || e instanceof AuthencationException || e instanceof LockException);
	}
}