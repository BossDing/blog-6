package me.qyh.blog.web.controller;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.View;

import me.qyh.blog.entity.Article;
import me.qyh.blog.entity.Space;
import me.qyh.blog.entity.Article.ArticleStatus;
import me.qyh.blog.pageparam.ArticleQueryParam;
import me.qyh.blog.pageparam.PageResult;
import me.qyh.blog.service.ArticleService;
import me.qyh.blog.service.ConfigService;
import me.qyh.blog.web.interceptor.SpaceContext;

@Controller
@RequestMapping("space/{alias}")
public class SpaceRssController {

	@Autowired
	private RssView rssView;
	@Autowired
	private ConfigService configService;
	@Autowired
	private ArticleService articleService;

	@RequestMapping("rss")
	public View rss(ModelMap model) {
		ArticleQueryParam param = new ArticleQueryParam();
		param.setCurrentPage(1);
		param.setPageSize(configService.getPageSizeConfig().getArticlePageSize());
		Space space = SpaceContext.get();
		if (space.hasLock()) {
			model.addAttribute("page", new PageResult<Article>(param, 0, Collections.emptyList()));
		} else {
			param.setStatus(ArticleStatus.PUBLISHED);
			param.setSpace(space);
			param.setIgnoreLevel(true);
			param.setHasLock(false);
			param.setQueryPrivate(false);
			PageResult<Article> page = articleService.queryArticle(param);
			model.addAttribute("page", page);
		}
		return rssView;
	}

}
