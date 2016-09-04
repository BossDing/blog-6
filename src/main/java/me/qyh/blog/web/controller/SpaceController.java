package me.qyh.blog.web.controller;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.View;

import me.qyh.blog.bean.JsonResult;
import me.qyh.blog.entity.Article;
import me.qyh.blog.entity.Article.ArticleStatus;
import me.qyh.blog.entity.Comment;
import me.qyh.blog.entity.Space;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.oauth2.OauthUser;
import me.qyh.blog.oauth2.RequestOauthUser;
import me.qyh.blog.pageparam.ArticleQueryParam;
import me.qyh.blog.pageparam.CommentQueryParam;
import me.qyh.blog.pageparam.PageResult;
import me.qyh.blog.security.UserContext;
import me.qyh.blog.service.ArticleService;
import me.qyh.blog.service.CommentService;
import me.qyh.blog.service.ConfigService;
import me.qyh.blog.service.UIService;
import me.qyh.blog.ui.Params;
import me.qyh.blog.ui.Template;
import me.qyh.blog.ui.page.SysPage.PageTarget;
import me.qyh.blog.ui.widget.ArticleWidgetHandler;
import me.qyh.blog.ui.widget.ArticlesWidgetHandler;
import me.qyh.blog.web.controller.form.ArticleQueryParamValidator;
import me.qyh.blog.web.controller.form.CommentValidator;
import me.qyh.blog.web.interceptor.SpaceContext;

@Controller
@RequestMapping("space/{alias}")
public class SpaceController extends BaseController {

	@Autowired
	private UIService uiService;
	@Autowired
	private ConfigService configService;
	@Autowired
	private ArticleService articleService;
	@Autowired
	private CommentService commentService;

	@Autowired
	private RssView rssView;

	@Autowired
	private ArticleQueryParamValidator articleQueryParamValidator;

	@InitBinder(value = "articleQueryParam")
	protected void initQueryBinder(WebDataBinder binder) {
		binder.setValidator(articleQueryParamValidator);
	}

	@Autowired
	private CommentValidator commentValidator;

	@InitBinder(value = "comment")
	protected void initCommentBinder(WebDataBinder binder) {
		binder.setValidator(commentValidator);
	}

	@RequestMapping(value = { "/", "" })
	public Template index() throws LogicException {
		return uiService.renderSysPage(SpaceContext.get(), PageTarget.INDEX, new Params());
	}

	@RequestMapping("/article/{id}")
	public Template article(@PathVariable("id") Integer id) throws LogicException {
		return uiService.renderSysPage(SpaceContext.get(), PageTarget.ARTICLE_DETAIL,
				new Params().add(ArticleWidgetHandler.PARAMETER_KEY, id));
	}

	@RequestMapping(value = "article/hit/{id}", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult hit(@PathVariable("id") Integer id) {
		Article article = articleService.hit(id);
		return article == null ? new JsonResult(false) : new JsonResult(true, article.getHits());
	}

	@RequestMapping(value = "article/list")
	public Template list(@Validated ArticleQueryParam articleQueryParam, BindingResult result) throws LogicException {
		if (result.hasErrors()) {
			articleQueryParam = new ArticleQueryParam();
			articleQueryParam.setCurrentPage(1);
		}
		Space space = SpaceContext.get();
		articleQueryParam.setStatus(ArticleStatus.PUBLISHED);
		articleQueryParam.setSpace(space);
		articleQueryParam.setIgnoreLevel(false);
		articleQueryParam.setQueryPrivate(UserContext.get() != null);
		articleQueryParam.setPageSize(configService.getPageSizeConfig().getArticlePageSize());
		return uiService.renderSysPage(space, PageTarget.ARTICLE_LIST,
				new Params().add(ArticlesWidgetHandler.PARAMETER_KEY, articleQueryParam));
	}

	@RequestMapping(value = "article/{id}/comment/list")
	@ResponseBody
	public PageResult<Comment> queryPageResult(@PathVariable("id") Integer articleId, CommentQueryParam param,
			BindingResult result) {
		if (result.hasErrors()) {
			return new PageResult<>(param, 0, Collections.emptyList());
		}
		param.setPageSize(configService.getPageSizeConfig().getCommentPageSize());
		param.setArticle(new Article(articleId));
		return commentService.queryComment(param);
	}

	@RequestMapping(value = "article/{id}/addComment", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult addComment(@RequestOauthUser OauthUser user, @RequestBody @Validated Comment comment,
			@PathVariable("id") Integer articleId) throws LogicException {
		comment.setArticle(new Article(articleId));
		comment.setUser(user);
		return new JsonResult(true, commentService.insertComment(comment));
	}

	@RequestMapping("page/{idOrAlias}")
	public Template userPage(@PathVariable("idOrAlias") String idOrAlias) throws LogicException {
		return uiService.renderUserPage(idOrAlias);
	}

	@RequestMapping("rss")
	public View rss(ModelMap model) {
		ArticleQueryParam param = new ArticleQueryParam();
		param.setCurrentPage(1);
		Space space = SpaceContext.get();
		param.setStatus(ArticleStatus.PUBLISHED);
		param.setSpace(space);
		param.setIgnoreLevel(true);
		param.setHasLock(false);
		param.setQueryPrivate(false);
		param.setPageSize(configService.getPageSizeConfig().getArticlePageSize());
		PageResult<Article> page = articleService.queryArticle(param);
		model.addAttribute("page", page);
		return rssView;
	}
}
