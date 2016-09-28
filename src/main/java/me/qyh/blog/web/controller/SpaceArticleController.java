package me.qyh.blog.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import me.qyh.blog.bean.JsonResult;
import me.qyh.blog.entity.Article;
import me.qyh.blog.entity.Article.ArticleStatus;
import me.qyh.blog.entity.Comment;
import me.qyh.blog.entity.Comment.CommentStatus;
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
import me.qyh.blog.ui.RenderedPage;
import me.qyh.blog.ui.data.ArticleDataTagProcessor;
import me.qyh.blog.ui.data.ArticlesDataTagProcessor;
import me.qyh.blog.ui.page.SysPage.PageTarget;
import me.qyh.blog.web.controller.form.ArticleQueryParamValidator;
import me.qyh.blog.web.controller.form.CommentValidator;
import me.qyh.blog.web.interceptor.SpaceContext;

@Controller
@RequestMapping("space/{alias}/article")
public class SpaceArticleController extends BaseController {

	@Autowired
	private ConfigService configService;
	@Autowired
	private ArticleService articleService;
	@Autowired
	private CommentService commentService;
	@Autowired
	private UIService uiService;

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

	@RequestMapping("{id}")
	public RenderedPage article(@PathVariable("id") Integer id) throws LogicException {
		return uiService.renderSysPage(SpaceContext.get(), PageTarget.ARTICLE_DETAIL,
				new Params().add(ArticleDataTagProcessor.PARAMETER_KEY, id));
	}

	@RequestMapping(value = "hit/{id}", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult hit(@PathVariable("id") Integer id) {
		Article article = articleService.hit(id);
		return article == null ? new JsonResult(false) : new JsonResult(true, article.getHits());
	}

	@RequestMapping(value = "list")
	public RenderedPage list(@Validated ArticleQueryParam articleQueryParam, BindingResult result) throws LogicException {
		if (result.hasErrors()) {
			articleQueryParam = new ArticleQueryParam();
			articleQueryParam.setCurrentPage(1);
		}
		setParam(articleQueryParam);
		return uiService.renderSysPage(SpaceContext.get(), PageTarget.ARTICLE_LIST,
				new Params().add(ArticlesDataTagProcessor.PARAMETER_KEY, articleQueryParam));
	}

	private void setParam(ArticleQueryParam articleQueryParam) {
		Space space = SpaceContext.get();
		articleQueryParam.setStatus(ArticleStatus.PUBLISHED);
		articleQueryParam.setSpace(space);
		articleQueryParam.setIgnoreLevel(false);
		articleQueryParam.setQueryPrivate(UserContext.get() != null);
		articleQueryParam.setPageSize(configService.getPageSizeConfig().getArticlePageSize());
	}

	@RequestMapping(value = "{id}/comment/list")
	@ResponseBody
	public PageResult<Comment> queryPageResult(@PathVariable("id") Integer articleId, CommentQueryParam param,
			BindingResult result) {
		if (result.hasErrors()) {
			param = new CommentQueryParam();
			param.setCurrentPage(1);
		}
		param.setStatus(UserContext.get() == null ? CommentStatus.NORMAL : null);
		param.setPageSize(configService.getPageSizeConfig().getCommentPageSize());
		param.setArticle(new Article(articleId));
		return commentService.queryComment(param);
	}

	@RequestMapping(value = "{id}/addComment", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult addComment(@RequestOauthUser OauthUser user, @RequestBody @Validated Comment comment,
			@PathVariable("id") Integer articleId) throws LogicException {
		comment.setArticle(new Article(articleId));
		comment.setUser(user);
		return new JsonResult(true, commentService.insertComment(comment));
	}

}
