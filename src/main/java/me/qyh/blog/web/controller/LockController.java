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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import me.qyh.blog.bean.JsonResult;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.lock.Lock;
import me.qyh.blog.lock.LockBean;
import me.qyh.blog.lock.LockHelper;
import me.qyh.blog.lock.LockKey;
import me.qyh.blog.lock.ErrorKeyException;
import me.qyh.blog.lock.InvalidKeyException;
import me.qyh.blog.message.Message;
import me.qyh.blog.service.UIService;
import me.qyh.blog.ui.RenderedPage;
import me.qyh.blog.ui.UIContext;

@Controller
public class LockController extends BaseController {

	private static final Logger logger = LoggerFactory.getLogger(LockController.class);

	@Autowired
	private UIService uiService;

	@RequestMapping(value = "unlock", method = RequestMethod.GET)
	public String unlock(Model model, HttpServletRequest request) throws LogicException {
		LockBean lockBean = LockHelper.getRequiredLockBean(request);
		model.addAttribute("lock", lockBean.getLock());
		try {
			RenderedPage rp = uiService.renderLockPage(null, lockBean.getLock().getLockType());
			UIContext.set(rp);
			return rp.getTemplateName();
		} catch (Throwable e) {
			if (e instanceof LogicException)
				logger.error("渲染页面解锁页面的时候发生逻辑异常，可能由于data标签使用不当引起的，系统无法再显示这个页面，因为这可能会导致无限循环");
			else
				logger.error("渲染页面解锁页面的时候发生异常:" + e.getMessage() + "，系统无法再显示这个页面，因为这可能会导致无限循环", e);
			return "error/500";
		}
	}

	@RequestMapping(value = "unlock", method = RequestMethod.POST)
	public String unlock(@RequestParam("validateCode") String validateCode, HttpServletRequest request,
			RedirectAttributes ra) {
		LockBean lockBean = LockHelper.getRequiredLockBean(request);
		Lock lock = lockBean.getLock();
		HttpSession session = request.getSession(false);
		if (!Webs.matchValidateCode(validateCode, session)) {
			ra.addFlashAttribute(ERROR, new Message("validateCode.error", "验证码错误"));
			return "redirect:/unlock";
		}
		LockKey key = null;
		try {
			key = lock.getKeyFromRequest(request);
		} catch (InvalidKeyException e) {
			ra.addFlashAttribute(ERROR, e.getMessage());
			return "redirect:/unlock";
		}
		LockHelper.addKey(request, key, lockBean.getLockResource().getResourceId());
		LockHelper.clearLockBean(request);
		return "redirect:" + lockBean.getRedirectUrl();
	}

	@RequestMapping(value = "unlock", method = RequestMethod.POST, headers = "x-requested-with=XMLHttpRequest")
	@ResponseBody
	public JsonResult unlock(@RequestParam("validateCode") String validateCode, HttpServletRequest request)
			throws InvalidKeyException, ErrorKeyException {
		LockBean lockBean = LockHelper.getRequiredLockBean(request);
		Lock lock = lockBean.getLock();
		HttpSession session = request.getSession(false);
		if (!Webs.matchValidateCode(validateCode, session))
			return new JsonResult(false, new Message("validateCode.error", "验证码错误"));
		LockKey key = lock.getKeyFromRequest(request);
		LockHelper.addKey(request, key, lockBean.getLockResource().getResourceId());
		lock.tryOpen(key);
		LockHelper.clearLockBean(request);
		return new JsonResult(true, lockBean.getRedirectUrl());
	}
}
