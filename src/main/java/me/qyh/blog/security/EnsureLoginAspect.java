package me.qyh.blog.security;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

@Aspect
public class EnsureLoginAspect {

	@Before(value = "@within(EnsureLogin) || @annotation(EnsureLogin)")
	public void before() throws Throwable {
		if (UserContext.get() == null) {
			throw new AuthencationException();
		}
	}

}
