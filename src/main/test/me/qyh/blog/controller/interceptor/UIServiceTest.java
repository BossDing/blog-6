package me.qyh.blog.controller.interceptor;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import me.qyh.blog.service.UIQueryService;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:resources/spring/applicationContext.xml" })
public class UIServiceTest {

	@Autowired
	private UIService uiService;

	@Test
	public void test() {
		System.out.println(uiService);
	}
}
