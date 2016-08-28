package me.qyh.blog.controller.interceptor;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import me.qyh.blog.lock.LockManager;
import me.qyh.blog.lock.support.PasswordLockKey;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:resources/spring/applicationContext.xml" })
public class LockTest {

	@Autowired
	private LockManager<?> LockManager;

	@Test
	public void test() {
		LockManager.findLock("123").tryOpen(new PasswordLockKey("123"));
	}
}
