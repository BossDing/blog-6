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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import me.qyh.blog.core.bean.JsonResult;
import me.qyh.blog.core.exception.LogicException;
import me.qyh.blog.core.lock.RequestLock;
import me.qyh.blog.core.lock.SysLock;
import me.qyh.blog.core.lock.SysLockProvider;
import me.qyh.blog.core.message.Message;

@Controller
@RequestMapping("mgr/lock/sys")
public class SysLockMgrController extends BaseMgrController {

	@Autowired
	private SysLockProvider sysLockProvider;

	@RequestMapping(value = "get/{id}")
	@ResponseBody
	public JsonResult lock(@PathVariable("id") String id) {
		return sysLockProvider.findLock(id).map(sysLock -> new JsonResult(true, sysLock)).orElse(new JsonResult(false));
	}

	@RequestMapping(value = "add", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult addLock(@RequestLock SysLock lock, BindingResult br) throws LogicException {
		sysLockProvider.addLock(lock);
		return new JsonResult(true, new Message("lock.add.success", "添加成功"));
	}

	@RequestMapping(value = "update", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult updateLock(@RequestLock SysLock lock, BindingResult br) throws LogicException {
		sysLockProvider.updateLock(lock);
		return new JsonResult(true, new Message("lock.update.success", "更新成功"));
	}

	@RequestMapping(value = "delete", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult deleteLock(@RequestParam("id") String id) throws LogicException {
		sysLockProvider.removeLock(id);
		return new JsonResult(true, new Message("lock.delete.success", "删除成功"));
	}

	@RequestMapping("index")
	public String index(Model model) {
		model.addAttribute("locks", sysLockProvider.allLock());
		return "mgr/lock/index";
	}

}
