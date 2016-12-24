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
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;

import me.qyh.blog.entity.Article.ArticleStatus;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.pageparam.ArticleQueryParam;
import me.qyh.blog.security.UserContext;
import me.qyh.blog.ui.page.Page;
import me.qyh.blog.ui.page.SysPage;
import me.qyh.blog.ui.page.SysPage.PageTarget;
import me.qyh.blog.web.controller.form.ArticleQueryParamValidator;

@Controller
@RequestMapping("article")
public class ArticleController {

	@Autowired
	private ArticleQueryParamValidator articleQueryParamValidator;

	@InitBinder(value = "articleQueryParam")
	protected void initQueryBinder(WebDataBinder binder) {
		binder.setValidator(articleQueryParamValidator);
	}

	@RequestMapping(value = "list")
	public Page list(@Validated ArticleQueryParam articleQueryParam, BindingResult result, ModelMap model)
			throws LogicException {
		if (result.hasErrors()) {
			articleQueryParam = new ArticleQueryParam();
			articleQueryParam.setCurrentPage(1);
		}
		articleQueryParam.setStatus(ArticleStatus.PUBLISHED);
		articleQueryParam.setSpace(null);
		articleQueryParam.setIgnoreLevel(false);
		articleQueryParam.setQueryPrivate(UserContext.get() != null);

		model.addAttribute(ArticleQueryParam.class.getName(), articleQueryParam);
		return new SysPage(null, PageTarget.ARTICLE_LIST);
	}

}
