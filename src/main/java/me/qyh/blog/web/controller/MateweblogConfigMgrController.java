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

import me.qyh.blog.core.bean.JsonResult;
import me.qyh.blog.core.config.UploadConfig;
import me.qyh.blog.core.exception.LogicException;
import me.qyh.blog.core.message.Message;
import me.qyh.blog.core.service.ConfigService;
import me.qyh.blog.web.controller.form.UploadConfigValidator;

@RequestMapping("mgr/config/metaweblogConfig")
@Controller
public class MateweblogConfigMgrController extends BaseMgrController {

	@Autowired
	private ConfigService configService;
	@Autowired
	private UploadConfigValidator uploadConfigValidator;

	@InitBinder(value = "uploadConfig")
	protected void initBinder(WebDataBinder binder) {
		binder.setValidator(uploadConfigValidator);
	}

	@RequestMapping("index")
	public String index(Model model) {
		model.addAttribute("config", configService.getMetaweblogConfig());
		return "mgr/config/metaweblogConfig";
	}

	@RequestMapping(value = "update", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult update(@Validated @RequestBody UploadConfig uploadConfig) throws LogicException {
		configService.updateMetaweblogConfig(uploadConfig);
		return new JsonResult(true, new Message("metaweblogConfig.update.success", "更新成功"));
	}

}
