package me.qyh.blog.controller.interceptor;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.core.JsonProcessingException;

import me.qyh.blog.bean.JsonResult;
import me.qyh.blog.message.Message;
import me.qyh.util.Jsons;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(locations = { "classpath:resources/spring/applicationContext.xml",
		"classpath:resources/spring/blog-servlet.xml" })
public class _Test {

	@Autowired
	private MessageSource messageSource;

	@Autowired
	private WebApplicationContext wac;

	@Before
	public void setup() {
		MockServletContext sc = new MockServletContext("classpath:resources/spring/applicationContext.xml");
		ServletContextListener listener = new ContextLoaderListener(wac);
		ServletContextEvent event = new ServletContextEvent(sc);
		listener.contextInitialized(event);
	}

	@Test
	public void test() throws JsonProcessingException {
		JsonResult result = new JsonResult(true);
		result.setMessage(new Message("123", "你好"));

		JsonResult result1 = new JsonResult(true);

		System.out.println(messageSource.getMessage("123", null, LocaleContextHolder.getLocale()));
		System.out.println(Jsons.writer().writeValueAsString(result));
		System.out.println(Jsons.writer().writeValueAsString(result1));
	}

}
