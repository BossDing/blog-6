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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import me.qyh.blog.bean.JsonResult;
import me.qyh.blog.config.UrlHelper;
import me.qyh.blog.entity.Space;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.lock.Lock;
import me.qyh.blog.lock.LockBean;
import me.qyh.blog.lock.LockHelper;
import me.qyh.blog.lock.LockKey;
import me.qyh.blog.message.Message;
import me.qyh.blog.security.Environment;
import me.qyh.blog.ui.page.LockPage;
import me.qyh.blog.web.Webs;

@Controller
public class LockController extends BaseController {

	@Autowired
	private UrlHelper urlHelper;

	@RequestMapping(value = { "space/{alias}/unlock", "/unlock" }, method = RequestMethod.GET)
	public LockPage unlock(Model model, HttpServletRequest request) throws LogicException {
		LockBean lockBean = LockHelper.getLockBean(request);
		model.addAttribute("lock", lockBean.getLock());
		return new LockPage(Environment.getSpace().orElse(null), lockBean.getLock().getLockType());
	}

	@RequestMapping(value = { "space/{alias}/unlock", "/unlock" }, method = RequestMethod.POST)
	public String unlock(@RequestParam("validateCode") String validateCode, HttpServletRequest request,
			RedirectAttributes ra) {
		LockBean lockBean = LockHelper.getLockBean(request);
		Lock lock = lockBean.getLock();
		HttpSession session = request.getSession(false);
		if (!Webs.matchValidateCode(validateCode, session)) {
			ra.addFlashAttribute(ERROR, new Message("validateCode.error", "验证码错误"));
			return buildLockUrl(lockBean.getSpaceAlias());
		}
		LockKey key = null;
		try {
			key = lock.getKeyFromRequest(request);
		} catch (LogicException e) {
			ra.addFlashAttribute(ERROR, e.getMessage());
			return buildLockUrl(lockBean.getSpaceAlias());
		}
		LockHelper.addKey(request, key, lockBean.getLockResource());
		LockHelper.clearLockBean(request);
		return "redirect:" + lockBean.getRedirectUrl();
	}

	private String buildLockUrl(String alias) {
		if (alias == null) {
			return "redirect:" + urlHelper.getUrl() + "/unlock";
		} else {
			return "redirect:" + urlHelper.getUrls().getUrl(new Space(alias)) + "/unlock";
		}
	}

	@RequestMapping(value = { "space/{alias}/unlock",
			"/unlock" }, method = RequestMethod.POST, headers = "x-requested-with=XMLHttpRequest")
	@ResponseBody
	public JsonResult unlock(@RequestParam("validateCode") String validateCode, HttpServletRequest request)
			throws LogicException {
		LockBean lockBean = LockHelper.getLockBean(request);
		Lock lock = lockBean.getLock();
		HttpSession session = request.getSession(false);
		if (!Webs.matchValidateCode(validateCode, session)) {
			return new JsonResult(false, new Message("validateCode.error", "验证码错误"));
		}
		LockKey key = lock.getKeyFromRequest(request);
		LockHelper.addKey(request, key, lockBean.getLockResource());
		lock.tryOpen(key);
		LockHelper.clearLockBean(request);
		return new JsonResult(true, lockBean.getRedirectUrl());
	}
}
