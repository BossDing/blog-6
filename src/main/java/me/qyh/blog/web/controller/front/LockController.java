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
package me.qyh.blog.web.controller.front;

import java.util.Objects;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import me.qyh.blog.core.config.Constants;
import me.qyh.blog.core.config.UrlHelper;
import me.qyh.blog.core.entity.Lock;
import me.qyh.blog.core.entity.LockKey;
import me.qyh.blog.core.exception.LogicException;
import me.qyh.blog.core.message.Message;
import me.qyh.blog.core.vo.JsonResult;
import me.qyh.blog.core.vo.LockBean;
import me.qyh.blog.web.LockHelper;
import me.qyh.blog.web.Webs;
import me.qyh.blog.web.security.CaptchaValidator;
import me.qyh.blog.web.security.IgnoreSpaceLock;

@Controller
public class LockController {

	@Autowired
	private UrlHelper urlHelper;
	@Autowired
	private CaptchaValidator captchaValidator;

	@IgnoreSpaceLock
	@PostMapping({ "space/{alias}/unlock", "/unlock" })
	public String unlock(HttpServletRequest request, RedirectAttributes ra, @RequestParam("unlockId") String unlockId) {
		LockBean lockBean = LockHelper.getLockBean(request, unlockId).orElse(null);
		if (lockBean == null || !Objects.equals(Webs.getSpaceFromRequest(request), lockBean.getSpaceAlias())) {
			return "redirect:" + urlHelper.getUrl();
		}
		Lock lock = lockBean.getLock();
		LockKey key;
		try {
			captchaValidator.doValidate(request);
			key = lock.getKeyFromRequest(request);
			lock.tryOpen(key);
		} catch (LogicException e) {
			ra.addFlashAttribute(Constants.ERROR, e.getLogicMessage());
			return "redirect:" + Webs.getSpaceUrls(request).getUnlockUrl(lock.getLockType(), lockBean.getId());
		}
		LockHelper.addKey(request, key, lockBean);
		return "redirect:" + lockBean.getRedirectUrl();
	}

	@IgnoreSpaceLock
	@PostMapping(value = { "space/{alias}/unlock", "/unlock" }, headers = "x-requested-with=XMLHttpRequest")
	@ResponseBody
	public JsonResult unlock(HttpServletRequest request, @RequestParam("unlockId") String unlockId)
			throws LogicException {
		LockBean lockBean = LockHelper.getLockBean(request, unlockId).orElse(null);
		if (lockBean == null || !Objects.equals(Webs.getSpaceFromRequest(request), lockBean.getSpaceAlias())) {
			return new JsonResult(false, new Message("lock.miss", "锁缺失"));
		}
		captchaValidator.doValidate(request);
		Lock lock = lockBean.getLock();
		LockKey key = lock.getKeyFromRequest(request);
		lock.tryOpen(key);
		LockHelper.addKey(request, key, lockBean);
		return new JsonResult(true, lockBean.getRedirectUrl());
	}
}
