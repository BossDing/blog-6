package me.qyh.blog.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;

import me.qyh.blog.entity.Article.ArticleStatus;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.pageparam.ArticleQueryParam;
import me.qyh.blog.security.UserContext;
import me.qyh.blog.service.ConfigService;
import me.qyh.blog.service.UIService;
import me.qyh.blog.ui.Params;
import me.qyh.blog.ui.RenderedPage;
import me.qyh.blog.ui.data.ArticlesDataTagProcessor;
import me.qyh.blog.ui.page.SysPage.PageTarget;
import me.qyh.blog.web.controller.form.ArticleQueryParamValidator;

@Controller
@RequestMapping("article")
public class ArticleController {

	@Autowired
	private UIService uiService;
	@Autowired
	private ConfigService configService;

	@Autowired
	private ArticleQueryParamValidator articleQueryParamValidator;

	@InitBinder(value = "articleQueryParam")
	protected void initQueryBinder(WebDataBinder binder) {
		binder.setValidator(articleQueryParamValidator);
	}

	@RequestMapping(value = "list")
	public RenderedPage list(@Validated ArticleQueryParam articleQueryParam, BindingResult result)
			throws LogicException {
		if (result.hasErrors()) {
			articleQueryParam = new ArticleQueryParam();
			articleQueryParam.setCurrentPage(1);
		}
		articleQueryParam.setStatus(ArticleStatus.PUBLISHED);
		articleQueryParam.setSpace(null);
		articleQueryParam.setIgnoreLevel(false);
		articleQueryParam.setQueryPrivate(UserContext.get() != null);
		articleQueryParam.setPageSize(configService.getPageSizeConfig().getArticlePageSize());
		return uiService.renderSysPage(null, PageTarget.ARTICLE_LIST,
				new Params().add(ArticlesDataTagProcessor.PARAMETER_KEY, articleQueryParam));
	}

}
