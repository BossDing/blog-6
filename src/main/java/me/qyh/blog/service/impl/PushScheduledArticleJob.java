package me.qyh.blog.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import me.qyh.blog.service.ArticleService;

@Component
public class PushScheduledArticleJob {

	@Autowired
	private ArticleService articleService;

	public void doJob() {
		articleService.pushScheduled();
	}

}
