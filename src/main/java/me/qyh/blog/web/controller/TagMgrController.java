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
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import me.qyh.blog.bean.JsonResult;
import me.qyh.blog.entity.Tag;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.message.Message;
import me.qyh.blog.pageparam.TagQueryParam;
import me.qyh.blog.service.TagService;
import me.qyh.blog.web.controller.form.TagQueryParamValidator;
import me.qyh.blog.web.controller.form.TagValidator;

@RequestMapping("mgr/tag")
@Controller
public class TagMgrController extends BaseMgrController {

	@Autowired
	private TagService tagService;
	@Autowired
	private TagValidator tagValidator;
	@Autowired
	private TagQueryParamValidator tagQueryParamValidator;

	@InitBinder(value = "tag")
	protected void initBinder(WebDataBinder binder) {
		binder.setValidator(tagValidator);
	}

	@InitBinder(value = "tagQueryParam")
	protected void initTagQueryParamBinder(WebDataBinder binder) {
		binder.setValidator(tagQueryParamValidator);
	}

	@RequestMapping("index")
	public String index(@Validated TagQueryParam tagQueryParam, BindingResult result, Model model) {
		if (result.hasErrors()) {
			tagQueryParam = new TagQueryParam();
			tagQueryParam.setCurrentPage(1);
		}
		model.addAttribute("page", tagService.queryTag(tagQueryParam));
		return "mgr/tag/index";
	}

	@RequestMapping(value = "delete", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult delete(@RequestParam("id") Integer id) throws LogicException {
		tagService.deleteTag(id);
		return new JsonResult(true, new Message("tag.delete.success", "删除成功"));
	}

	@RequestMapping(value = "update", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult update(@RequestBody @Validated Tag tag,
			@RequestParam(defaultValue = "false", required = false) boolean merge) throws LogicException {
		tagService.updateTag(tag, merge);
		return new JsonResult(true, new Message("tag.update.success", "更新成功"));
	}

}
