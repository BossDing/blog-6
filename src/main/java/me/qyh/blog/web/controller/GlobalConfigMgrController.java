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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import me.qyh.blog.bean.JsonResult;
import me.qyh.blog.config.GlobalConfig;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.message.Message;
import me.qyh.blog.service.ConfigService;
import me.qyh.blog.web.controller.form.GlobalConfigValidator;

@RequestMapping("mgr/config/global")
@Controller
public class GlobalConfigMgrController extends BaseMgrController {

	@Autowired
	private ConfigService configService;
	@Autowired
	private GlobalConfigValidator globalConfigValidator;

	@InitBinder(value = "globalConfig")
	protected void initBinder(WebDataBinder binder) {
		binder.setValidator(globalConfigValidator);
	}

	@RequestMapping("index")
	public String index(Model model) {
		model.addAttribute("config", configService.getGlobalConfig());
		return "mgr/config/global";
	}

	@RequestMapping(value = "update", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult update(@Validated @RequestBody GlobalConfig globalConfig) throws LogicException {
		configService.updateGlobalConfig(globalConfig);
		return new JsonResult(true, new Message("global.update.success", "更新成功"));
	}

}
