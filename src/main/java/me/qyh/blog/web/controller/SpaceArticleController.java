/*
 * Copyright 2016 qyh.me
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.qyh.blog.web.controller;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.AntPathMatcher;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import me.qyh.blog.bean.JsonResult;
import me.qyh.blog.config.Constants;
import me.qyh.blog.entity.Article;
import me.qyh.blog.entity.Article.ArticleStatus;
import me.qyh.blog.entity.Comment;
import me.qyh.blog.entity.Space;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.oauth2.OauthUser;
import me.qyh.blog.oauth2.RequestOauthUser;
import me.qyh.blog.pageparam.ArticleQueryParam;
import me.qyh.blog.security.UserContext;
import me.qyh.blog.service.ArticleService;
import me.qyh.blog.service.CommentService;
import me.qyh.blog.service.UIService;
import me.qyh.blog.ui.Params;
import me.qyh.blog.ui.RenderedPage;
import me.qyh.blog.ui.data.ArticleDataTagProcessor;
import me.qyh.blog.ui.data.ArticlesDataTagProcessor;
import me.qyh.blog.ui.page.SysPage.PageTarget;
import me.qyh.blog.web.controller.form.ArticleQueryParamValidator;
import me.qyh.blog.web.controller.form.CommentBean;
import me.qyh.blog.web.controller.form.CommentBeanValidator;
import me.qyh.blog.web.interceptor.SpaceContext;

@Controller
@RequestMapping("space/{alias}/article")
public class SpaceArticleController extends BaseController {

	@Autowired
	private ArticleService articleService;
	@Autowired
	private CommentService commentService;
	@Autowired
	private UIService uiService;

	private static final AntPathMatcher apm = new AntPathMatcher();

	@Autowired
	private ArticleQueryParamValidator articleQueryParamValidator;

	@InitBinder(value = "articleQueryParam")
	protected void initQueryBinder(WebDataBinder binder) {
		binder.setValidator(articleQueryParamValidator);
	}

	@Autowired
	private CommentBeanValidator commentBeanValidator;

	@InitBinder(value = "commentBean")
	protected void initCommentBinder(WebDataBinder binder) {
		binder.setValidator(commentBeanValidator);
	}

	@RequestMapping("{idOrAlias}")
	public RenderedPage article(@PathVariable(value = "idOrAlias") String idOrAlias) throws LogicException {
		return uiService.renderSysPage(SpaceContext.get(), PageTarget.ARTICLE_DETAIL,
				new Params().add(ArticleDataTagProcessor.PARAMETER_KEY, idOrAlias));
	}

	@RequestMapping(value = "hit/{id}", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult hit(@PathVariable("id") Integer id, @RequestHeader("referer") String referer) {
		try {
			UriComponents uc = UriComponentsBuilder.fromHttpUrl(referer).build();
			if (!apm.match("/space/" + SpaceContext.get().getAlias() + "/article/*", uc.getPath())
					&& !apm.match("/article/*", uc.getPath()))
				return new JsonResult(false);
		} catch (Exception e) {
			return new JsonResult(false);
		}

		Article article = articleService.hit(id);
		return article == null ? new JsonResult(false) : new JsonResult(true, article.getHits());
	}

	@RequestMapping(value = "list")
	public RenderedPage list(@Validated ArticleQueryParam articleQueryParam, BindingResult result)
			throws LogicException {
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
	}

	@RequestMapping(value = "{id}/addComment", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult addComment(@RequestOauthUser OauthUser user, @RequestBody @Validated CommentBean commentBean,
			@PathVariable("id") Integer articleId, HttpSession session) throws LogicException {
		commentBean.getComment().setArticle(new Article(articleId));
		commentBean.getComment().setUser(user);
		Comment comment = commentService.insertComment(commentBean);
		session.setAttribute(Constants.OAUTH_SESSION_KEY, comment.getUser());
		return new JsonResult(true, comment);
	}

	@RequestMapping(value = "{articleId}/comment/{id}/conversations")
	@ResponseBody
	public JsonResult queryConversations(@PathVariable("articleId") Integer articleId, @PathVariable("id") Integer id)
			throws LogicException {
		return new JsonResult(true, commentService.queryConversations(articleId, id));
	}
}
