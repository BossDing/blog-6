package me.qyh.blog.tool;

import java.util.List;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import me.qyh.blog.dao.ArticleDao;
import me.qyh.blog.entity.Article;
import me.qyh.blog.evt.ArticlePublishedEvent;
import me.qyh.blog.evt.ArticlePublishedEvent.OP;
import me.qyh.blog.service.ArticleService;

public class PingAll {

	public static void main(String[] args) {
		try (ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(
				"resources/spring/applicationContext.xml")) {
			ArticleDao articleDao = ctx.getBean(ArticleDao.class);
			List<Article> articles = articleDao.selectPublished(null);
			// async
			ctx.publishEvent(new ArticlePublishedEvent(ctx.getBean(ArticleService.class), articles, OP.UPDATE));
		}
	}

}
