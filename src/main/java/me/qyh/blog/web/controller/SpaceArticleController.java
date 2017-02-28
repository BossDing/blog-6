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
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import me.qyh.blog.bean.JsonResult;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.security.Environment;
import me.qyh.blog.service.ArticleService;
import me.qyh.blog.ui.page.Page;
import me.qyh.blog.ui.page.SysPage;
import me.qyh.blog.ui.page.SysPage.PageTarget;

@Controller
@RequestMapping("space/{alias}/article")
public class SpaceArticleController extends BaseController {

	@Autowired
	private ArticleService articleService;

	private static final AntPathMatcher apm = new AntPathMatcher();

	@RequestMapping("{idOrAlias}")
	public Page article(@PathVariable(value = "idOrAlias") String idOrAlias) throws LogicException {
		return new SysPage(Environment.getSpace().orElse(null), PageTarget.ARTICLE_DETAIL);
	}

	@RequestMapping(value = "hit/{id}", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult hit(@PathVariable("id") Integer id, @RequestHeader("referer") String referer) {
		try {
			UriComponents uc = UriComponentsBuilder.fromHttpUrl(referer).build();
			if (!apm.match("/space/" + Environment.getSpaceAlias().get() + "/article/*", uc.getPath())
					&& !apm.match("/article/*", uc.getPath())) {
				return new JsonResult(false);
			}
		} catch (Exception e) {
			return new JsonResult(false);
		}

		articleService.hit(id);
		return new JsonResult(true);
	}

	@RequestMapping(value = "list")
	public Page list() throws LogicException {
		return new SysPage(Environment.getSpace().orElse(null), PageTarget.ARTICLE_LIST);
	}

}
