package me.qyh.blog.service.impl;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
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
	private final Executor executor = Executors.newFixedThreadPool(1);
	private final ExpressionParser parser = new SpelExpressionParser();
	private final ParameterNameDiscoverer paramNameDiscoverer = new DefaultParameterNameDiscoverer();

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

	@Override
	public void afterCompletion(int status) {
		try {
			if (status == STATUS_ROLLED_BACK) {
				if (!noReload()) {
					executor.execute(new Runnable() {

						@Override
						public void run() {
							setRebuilding(true);
							try {
								ctx.getBean(ArticleService.class).rebuildIndex();
							} finally {
								setRebuilding(false);
							}
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

	private void setRebuilding(boolean rebuilding) {
		this.rebuilding = rebuilding;
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