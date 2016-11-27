package me.qyh.blog.launch;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.sql.SQLException;
import java.util.Properties;

import javax.servlet.ServletException;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;
import org.h2.store.fs.FileUtils;
import org.h2.tools.RunScript;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import me.qyh.blog.config.Constants;
import me.qyh.blog.config.UrlHelper;
import me.qyh.blog.exception.SystemException;

public class Starter {

	private static final Logger logger = LoggerFactory.getLogger(Starter.class);

	public static void main(String[] args) throws Exception {
		runDBScript();
		runTomcat();
	}

	private static void runDBScript() throws IOException {
		try (InputStream is = FileUtils.newInputStream("classpath:/resources/mybatis/db.properties")) {
			Properties pros = new Properties();
			pros.load(is);
			try {
				RunScript.execute(pros.getProperty("jdbc.jdbcUrl"), pros.getProperty("jdbc.user"),
						pros.getProperty("jdbc.password"), "classpath:resources/blog.h2.sql", Constants.CHARSET, false);
				logger.debug("初始化加载数据库完成");
			} catch (SQLException e) {
				logger.error(e.getMessage(), e);
				throw new SystemException(e.getMessage(), e);
			}
		}
	}

	private static void runTomcat() throws IOException, ServletException, LifecycleException {
		UrlHelper helper = null;
		try (ClassPathXmlApplicationContext sctx = new ClassPathXmlApplicationContext(
				"resources/spring/applicationContext.xml")) {
			helper = sctx.getBean(UrlHelper.class);
		}
		String webappDirLocation = "src/main/webapp/";
		Tomcat tomcat = new Tomcat();
		tomcat.setPort(helper.getUrlConfig().getPort());
		StandardContext ctx = (StandardContext) tomcat.addWebapp("", new File(webappDirLocation).getAbsolutePath());
		File additionWebInfClasses = new File("target/classes");
		WebResourceRoot resources = new StandardRoot(ctx);
		resources.addPreResources(
				new DirResourceSet(resources, "/WEB-INF/classes", additionWebInfClasses.getAbsolutePath(), "/"));
		ctx.setResources(resources);
		tomcat.start();
		try {
			Desktop.getDesktop().browse(new URI(helper.getUrl()));
		} catch (Exception e) {
			logger.error("打开网址失败：" + e.getMessage(), e);
		}
		tomcat.getServer().await();
	}
}
