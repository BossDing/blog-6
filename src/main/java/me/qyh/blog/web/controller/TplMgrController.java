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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.thymeleaf.TemplateEngine;

import me.qyh.blog.bean.JsonResult;
import me.qyh.blog.message.Message;
import me.qyh.blog.service.UIService;

@Controller
@RequestMapping("mgr/tpl")
public class TplMgrController extends BaseMgrController {

	@Autowired
	private UIService uiService;
	@Autowired
	private TemplateEngine templateEngine;

	@RequestMapping(value = "clearCache", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult clearPageCache() {
		templateEngine.getConfiguration().getTemplateManager().clearCaches();
		return new JsonResult(true, new Message("clearPageCache.success", "清除缓存成功"));
	}

	@RequestMapping(value = "clearCache", method = RequestMethod.GET)
	public String clearCacheIndex() {
		return "mgr/tpl/clearCache";
	}

	@RequestMapping("sysFragments")
	@ResponseBody
	public JsonResult querySysFragments() {
		return new JsonResult(true, uiService.querySysFragments());
	}

	@RequestMapping("dataTags")
	@ResponseBody
	public JsonResult queryDatas() {
		return new JsonResult(true, uiService.queryDataTags());
	}
}
