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

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import me.qyh.blog.bean.JsonResult;
import me.qyh.blog.config.Constants;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.message.Message;
import me.qyh.blog.oauth2.OauthBind;
import me.qyh.blog.oauth2.OauthUser;
import me.qyh.blog.service.OauthService;

@Controller
@RequestMapping("mgr/oauth/bind")
public class OauthBindMgrController extends BaseMgrController {

	@Autowired
	private OauthService oauthService;

	@RequestMapping(value = "toBind", method = RequestMethod.GET)
	public String bind(HttpSession session, ModelMap model, RedirectAttributes ra) {
		OauthUser oauthUser = (OauthUser) session.getAttribute(Constants.OAUTH_SESSION_KEY);
		if (oauthUser != null) {
			try {
				OauthBind bind = oauthService.queryBind(oauthUser);
				if (bind == null) {
					model.addAttribute(Constants.OAUTH_SESSION_KEY, oauthUser);
				}
			} catch (LogicException e) {
				ra.addFlashAttribute(ERROR, e.getLogicMessage());
			}
		}
		return "mgr/oauth/bind/bind";
	}

	@RequestMapping(value = "toBind", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult bind(HttpSession session) throws LogicException {
		OauthUser oauthUser = (OauthUser) session.getAttribute(Constants.OAUTH_SESSION_KEY);
		if (oauthUser == null) {
			return new JsonResult(false, new Message("oauthUser.miss", "当前没有社交账号"));
		}
		oauthService.bind(oauthUser);
		return new JsonResult(true, new Message("oauth.bind.success", "绑定成功"));
	}

	@RequestMapping(value = "unbind", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult unbind(@RequestParam("id") Integer id) throws LogicException {
		oauthService.unbind(id);
		return new JsonResult(true, new Message("oauth.unbind.success", "解除成功"));
	}

	@RequestMapping("index")
	public String index(ModelMap model) {
		model.addAttribute("binds", oauthService.queryAllBind());
		return "mgr/oauth/bind/index";
	}

}
