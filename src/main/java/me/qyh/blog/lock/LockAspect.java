package me.qyh.blog.lock;

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import me.qyh.blog.security.UserContext;

/**
 * 用户验证被保护的资源(拥有该annotation的方法必须返回lockResource对象！)<br/>
 * 如果抛出异常后依旧需要被Cacheable缓存，那么Order应该比CacheInterceptor 优先级高，
 * CacheInterceptor优先级默认为Ordered.LOWEST_PRECEDENCE
 * 
 * @see Ordered#getOrder()
 * @author mhlx
 *
 */
@Aspect
@Order(1)
public class LockAspect {

	@Autowired
	private LockManager<?> lockManager;

	private static final Logger logger = LoggerFactory.getLogger(LockAspect.class);

	@AfterReturning(value = "@within(LockProtected) || @annotation(LockProtected)", returning = "lockResource")
	public void after(LockResource lockResource) throws Throwable {
		// 需要验证密码
		if (lockResource != null && lockResource.getLockId() != null && UserContext.get() == null) {
			Lock lock = lockManager.findLock(lockResource.getLockId());
			if (lock != null) {
				lock.setLockResource(lockResource);
				LockKey key = LockKeyContext.getKey(lockResource.getResourceId());
				boolean open = false;
				try {
					open = lock.tryOpen(key);
				} catch (Throwable e) {
					// 防止死循环
					logger.error(e.getMessage(), e);
				}
				if (!open) {
					throw new LockException(lock);
				}
			}
		}
	}
}
