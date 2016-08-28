package me.qyh.blog.controller.interceptor;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import me.qyh.blog.config.UrlConfig;
import me.qyh.blog.service.UIService;
import me.qyh.blog.ui.SystemPage.PageType;
import me.qyh.blog.ui.TplRender;
import me.qyh.blog.ui.TplRenderException;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(locations = { "classpath:resources/spring/applicationContext.xml",
		"classpath:resources/spring/blog-servlet.xml" })
public class TplRenderTest {

	private MockHttpServletRequest request;
	private MockHttpServletResponse response;
	@Autowired
	private UrlConfig urlConfig;
	@Autowired
	private WebApplicationContext ctx;
	@Autowired
	private TplRender tplRender;
	@Autowired
	private UIService uiService;

	@Before
	public void setup() {
		request = new MockHttpServletRequest(ctx.getServletContext());
		request.setAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE, ctx);
		request.setServerName(urlConfig.getDomain());
		response = new MockHttpServletResponse();
	}

	@Test
	public void test() throws IOException {
		try {
			String result = tplRender.tryRender(uiService.getPage(null, PageType.INDEX), request, response);
			System.out.println(result);
		} catch (TplRenderException e) {
			System.out.println(e.getRenderErrorDescription());
		}
	}

}
