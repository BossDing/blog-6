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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.AntPathMatcher;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import me.qyh.blog.bean.JsonResult;
import me.qyh.blog.entity.Article;
import me.qyh.blog.entity.Article.ArticleStatus;
import me.qyh.blog.entity.Space;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.pageparam.ArticleQueryParam;
import me.qyh.blog.security.UserContext;
import me.qyh.blog.service.ArticleService;
import me.qyh.blog.ui.page.Page;
import me.qyh.blog.ui.page.SysPage;
import me.qyh.blog.ui.page.SysPage.PageTarget;
import me.qyh.blog.web.controller.form.ArticleQueryParamValidator;
import me.qyh.blog.web.interceptor.SpaceContext;

@Controller
@RequestMapping("space/{alias}/article")
public class SpaceArticleController extends BaseController {

	@Autowired
	private ArticleService articleService;

	private static final AntPathMatcher apm = new AntPathMatcher();

	@Autowired
	private ArticleQueryParamValidator articleQueryParamValidator;

	@InitBinder(value = "articleQueryParam")
	protected void initQueryBinder(WebDataBinder binder) {
		binder.setValidator(articleQueryParamValidator);
	}

	@RequestMapping("{idOrAlias}")
	public Page article(@PathVariable(value = "idOrAlias") String idOrAlias) throws LogicException {
		return new SysPage(SpaceContext.get(), PageTarget.ARTICLE_DETAIL);
	}

	@RequestMapping(value = "hit/{id}", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult hit(@PathVariable("id") Integer id, @RequestHeader("referer") String referer) {
		try {
			UriComponents uc = UriComponentsBuilder.fromHttpUrl(referer).build();
			if (!apm.match("/space/" + SpaceContext.get().getAlias() + "/article/*", uc.getPath())
					&& !apm.match("/article/*", uc.getPath())) {
				return new JsonResult(false);
			}
		} catch (Exception e) {
			return new JsonResult(false);
		}

		Article article = articleService.hit(id);
		return article == null ? new JsonResult(false) : new JsonResult(true, article.getHits());
	}

	@RequestMapping(value = "list")
	public Page list(@Validated ArticleQueryParam articleQueryParam, BindingResult result, ModelMap model)
			throws LogicException {
		if (result.hasErrors()) {
			articleQueryParam = new ArticleQueryParam();
			articleQueryParam.setCurrentPage(1);
		}
		setParam(articleQueryParam);
		model.addAttribute(ArticleQueryParam.class.getName(), articleQueryParam);

		return new SysPage(SpaceContext.get(), PageTarget.ARTICLE_LIST);
	}

	private void setParam(ArticleQueryParam articleQueryParam) {
		Space space = SpaceContext.get();
		articleQueryParam.setStatus(ArticleStatus.PUBLISHED);
		articleQueryParam.setSpace(space);
		articleQueryParam.setIgnoreLevel(false);
		articleQueryParam.setQueryPrivate(UserContext.get() != null);
	}
}
