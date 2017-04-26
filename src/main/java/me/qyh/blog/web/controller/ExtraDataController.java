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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import me.qyh.blog.core.bean.ExtraData;
import me.qyh.blog.core.bean.JsonResult;
import me.qyh.blog.core.service.impl.ExtraStorageService;
import me.qyh.blog.web.controller.form.ExtraDataValidator;

@Controller
@RequestMapping("mgr/extra")
public class ExtraDataController extends BaseMgrController {

	@Autowired
	private ExtraDataValidator extraDataValidator;

	@InitBinder(value = "extraData")
	protected void initBinder(WebDataBinder binder) {
		binder.setValidator(extraDataValidator);
	}

	@Autowired
	private ExtraStorageService extraStorageService;

	@GetMapping("get/{key}")
	@ResponseBody
	public JsonResult get(@PathVariable("key") String key) {
		return extraStorageService.get(key).map(data -> new JsonResult(true, data)).orElse(new JsonResult(false));
	}

	@PostMapping("put")
	@ResponseBody
	public JsonResult put(@RequestBody @Validated ExtraData extraData) {
		extraStorageService.store(extraData);
		return new JsonResult(true);
	}

	@GetMapping("clear")
	@ResponseBody
	public JsonResult clear() {
		extraStorageService.clear();
		return new JsonResult(true);
	}

	@GetMapping("keys")
	@ResponseBody
	public JsonResult keys() {
		return new JsonResult(true, extraStorageService.keys());
	}

	@GetMapping("remove/{key}")
	@ResponseBody
	public JsonResult remove(@PathVariable("key") String key) {
		extraStorageService.remove(key);
		return new JsonResult(true);
	}
}
