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
package me.qyh.blog.lock;

import java.util.Optional;

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import me.qyh.blog.exception.LogicException;
import me.qyh.blog.message.Message;
import me.qyh.blog.security.Environment;

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
	private LockManager lockManager;

	private static final Logger LOGGER = LoggerFactory.getLogger(LockAspect.class);

	/**
	 * 尝试用LockKeyContext上下文中存在的钥匙开锁
	 * 
	 * @param obj
	 *            被锁的资源
	 */
	@AfterReturning(value = "@within(LockProtected) || @annotation(LockProtected)", returning = "obj")
	public void after(Object obj) {
		Optional<LockResource> optionalLockResource = convert(obj);
		if (!optionalLockResource.isPresent()) {
			return;
		}
		LockResource lockResource = optionalLockResource.get();
		// 需要验证密码
		if (lockResource.getLockId() != null && !Environment.isLogin()) {
			Optional<Lock> optionalLock = lockManager.findLock(lockResource.getLockId());
			if (!optionalLock.isPresent()) {
				return;
			}
			Lock lock = optionalLock.get();
			LockKey key = LockKeyContext.getKey(lockResource.getResourceId())
					.orElseThrow(() -> new LockException(lock, lockResource, null));
			try {
				lock.tryOpen(key);
			} catch (LogicException e) {
				LOGGER.debug("尝试用" + key.getKey() + "打开锁" + lock.getId() + "失败");
				throw new LockException(lock, lockResource, e.getLogicMessage());
			} catch (Exception e) {
				LOGGER.error("尝试用" + key.getKey() + "打开锁" + lock.getId() + "异常，异常信息:" + e.getMessage(), e);
				throw new LockException(lock, lockResource, new Message("error.system", "系统异常"));
			}
		}
	}

	private Optional<LockResource> convert(Object obj) {
		if (obj != null) {
			if (obj instanceof Optional) {
				Optional<?> optional = (Optional<?>) obj;
				return optional.map(object -> (LockResource) object);
			} else if (obj instanceof LockResource) {
				return Optional.of((LockResource) obj);
			}
		}
		return Optional.empty();
	}
}
