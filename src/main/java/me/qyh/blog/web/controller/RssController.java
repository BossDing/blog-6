package me.qyh.blog.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.View;

import me.qyh.blog.entity.Article;
import me.qyh.blog.entity.Article.ArticleStatus;
import me.qyh.blog.pageparam.ArticleQueryParam;
import me.qyh.blog.pageparam.PageResult;
import me.qyh.blog.service.ArticleService;
import me.qyh.blog.service.ConfigService;

@Controller
public class RssController {

	@Autowired
	private ArticleService articleService;
	@Autowired
	private ConfigService configService;

	@Autowired
	private RssView rssView;

	public View rss() {
		return rssView;
	}

	@RequestMapping("rss")
	public View rss(ModelMap model) {
		ArticleQueryParam param = new ArticleQueryParam();
		param.setCurrentPage(1);
		param.setStatus(ArticleStatus.PUBLISHED);
		param.setIgnoreLevel(true);
		param.setHasLock(false);
		param.setQueryPrivate(false);
		param.setPageSize(configService.getPageSizeConfig().getArticlePageSize());
		PageResult<Article> page = articleService.queryArticle(param);
		model.addAttribute("page", page);
		return rssView;
	}

}
