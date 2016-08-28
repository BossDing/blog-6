package me.qyh.blog.service.impl;

import org.springframework.beans.factory.annotation.Autowired;

import me.qyh.blog.service.ArticleService;

public class ArticleIndexRebuildJob {

	@Autowired
	private ArticleService articleService;

	public void doJob() {
		articleService.rebuildIndex();
	}
}