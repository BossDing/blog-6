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
