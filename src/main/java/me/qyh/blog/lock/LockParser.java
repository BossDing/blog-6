package me.qyh.blog.lock;

import javax.servlet.http.HttpServletRequest;

import org.springframework.validation.Validator;

/**
 * 用来从请求中解析Lock
 * 
 * @author Administrator
 * @see RequestLock
 * @see LockArgumentResolver
 *
 * @param <T>
 */
public interface LockParser<T extends Lock> {

	/**
	 * 从请求中解析并获取锁对象
	 * 
	 * @param request
	 * @return 锁
	 * @throws Exception
	 *             解析失败
	 */
	T getLockFromRequest(HttpServletRequest request) throws Exception;

	/**
	 * 获取验证器
	 */
	Validator[] getValidators();

}
