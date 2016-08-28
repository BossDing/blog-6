package me.qyh.blog.controller.interceptor;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import me.qyh.blog.entity.Article;
import me.qyh.blog.entity.Space;
import me.qyh.blog.entity.Tag;
import me.qyh.blog.web.interceptor.urlHelper;
import me.qyh.blog.web.interceptor.urlHelper.UrlHandler;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(locations = { "classpath:resources/spring/applicationContext.xml",
		"classpath:resources/spring/blog-servlet.xml" })
public class urlHelperTest {

	@Autowired
	private urlHelper urlHelper;
	private MockHttpServletRequest request;

	@Before
	public void setup() {
		this.request = new MockHttpServletRequest();
	}

	@Test
	public void testMulti() {
		request.setServerName("www.qyh.me");
		request.setServerPort(8080);
		request.setRequestURI("/blog/1");
		UrlHandler uh = urlHelper.getCurrentUrlHandler(request);
		Tag tag = new Tag();
		tag.setName("spring mvc");
		Space cat = new Space();
		cat.setName("life");
		Article blog = new Article();
		blog.setSpace(cat);
		blog.setId(1);
		System.out.println(uh.getUrl(blog));
	}
}
