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
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.ObjectUtils;

import me.qyh.blog.exception.LogicException;
import me.qyh.blog.exception.SystemException;
import me.qyh.blog.lock.LockException;
import me.qyh.blog.security.AuthencationException;
import me.qyh.blog.service.ArticleService;

@Aspect
public class ArticleIndexRebuildAspect extends TransactionSynchronizationAdapter implements ApplicationContextAware {

	private static final ThreadLocal<Throwable> throwableLocal = new ThreadLocal<>();
	private boolean rebuilding;
	private final ExpressionParser parser = new SpelExpressionParser();
	private final ParameterNameDiscoverer paramNameDiscoverer = new DefaultParameterNameDiscoverer();

	@Autowired
	private CacheManager cacheManager;
	@Autowired
	private ThreadPoolTaskExecutor threadPoolTaskExecutor;

	@Before("@annotation(ArticleIndexRebuild)")
	public void before(JoinPoint joinPoint) {
		MethodSignature signature = (MethodSignature) joinPoint.getSignature();
		Method method = signature.getMethod();
		String methodName = method.getName();
		Class<?>[] parameterTypes = method.getParameterTypes();
		try {
			method = joinPoint.getTarget().getClass().getMethod(methodName, parameterTypes);
		} catch (Exception e) {
			throw new SystemException(e.getMessage(), e);
		}
		ArticleIndexRebuild ann = AnnotationUtils.findAnnotation(method, ArticleIndexRebuild.class);
		if (!ann.readOnly())
			TransactionSynchronizationManager.registerSynchronization(this);
		String conditionForWait = ann.conditionForWait();
		boolean wait = false;
		if ("true".equalsIgnoreCase(conditionForWait)) {
			wait = true;
		} else if ("false".equalsIgnoreCase(conditionForWait)) {
			wait = false;
		} else {
			Expression waitExpression = parser.parseExpression(conditionForWait);
			wait = waitExpression.getValue(buildStandardEvaluationContext(method, joinPoint.getArgs()), Boolean.class);
		}
		if (wait)
			waitWhileRebuilding();
	}

	@AfterThrowing(pointcut = "@annotation(ArticleIndexRebuild)", throwing = "e")
	public void afterThrow(Throwable e) {
		throwableLocal.set(e);
	}

	private synchronized void rebuild() {
		rebuilding = true;
		try {
			// 清空所有的缓存
			for (String name : cacheManager.getCacheNames())
				cacheManager.getCache(name).clear();
			ctx.getBean(ArticleService.class).rebuildIndex();
		} finally {
			rebuilding = false;
		}
	}

	@Override
	public void afterCompletion(int status) {
		try {
			if (status == STATUS_ROLLED_BACK) {
				if (!noReload()) {
					threadPoolTaskExecutor.execute(new Runnable() {

						@Override
						public void run() {
							rebuild();
						}
					});
				}
			}
		} finally {
			throwableLocal.remove();
		}
	}

	private boolean noReload() {
		Throwable e = throwableLocal.get();
		return (e != null
				&& (e instanceof LogicException || e instanceof AuthencationException || e instanceof LockException));
	}

	private void waitWhileRebuilding() {
		while (rebuilding) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				throw new SystemException(e.getMessage(), e);
			}
		}
	}

	/**
	 * 根据方法参数提供一个简单的spel上下文
	 * 
	 * @param method
	 * @param args
	 * @return
	 */
	private StandardEvaluationContext buildStandardEvaluationContext(Method method, Object[] args) {
		StandardEvaluationContext ctx = new StandardEvaluationContext();
		if (ObjectUtils.isEmpty(args)) {
			return ctx;
		}
		String[] parameterNames = paramNameDiscoverer.getParameterNames(method);
		if (parameterNames != null) {
			for (int i = 0; i < parameterNames.length; i++) {
				ctx.setVariable(parameterNames[i], args[i]);
			}
		}
		return ctx;
	}

	private ApplicationContext ctx = null;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		ctx = applicationContext;
	}
}