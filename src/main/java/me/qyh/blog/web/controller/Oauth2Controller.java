package me.qyh.blog.web.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

import me.qyh.blog.config.Constants;
import me.qyh.blog.entity.OauthUser;
import me.qyh.blog.entity.OauthUser.OauthType;
import me.qyh.blog.oauth2.InvalidStateException;
import me.qyh.blog.oauth2.Oauth2;
import me.qyh.blog.oauth2.Oauth2Provider;
import me.qyh.blog.oauth2.UserInfo;
import me.qyh.blog.service.OauthService;
import me.qyh.util.UUIDs;

@RequestMapping("oauth2/{oauthType}")
@Controller
public class Oauth2Controller extends BaseController {

	private static final String STATE = "state";
	private static final String REFERER_URL = "refererUrl";

	@Autowired
	private Oauth2Provider provider;
	@Autowired
	private OauthService oauthUserService;

	@RequestMapping("login")
	public String login(@PathVariable("oauthType") String type, HttpSession session,
			@RequestHeader(value = "referer", required = false) final String referer) {
		Oauth2 oauth2 = provider.getOauth2(getOauthType(type));
		String state = UUIDs.uuid();
		session.setAttribute(STATE, state);
		if (referer != null) {
			session.setAttribute(REFERER_URL, referer);
		}
		return "redirect:" + oauth2.getAuthorizeUrl(state);
	}

	@RequestMapping("success")
	public String success(@PathVariable("oauthType") String type, HttpServletRequest request) {
		Oauth2 oauth2 = provider.getOauth2(getOauthType(type));
		validState(request, oauth2);
		UserInfo user = oauth2.getUserInfo(request);
		if (user != null) {
			OauthUser oauthUser = new OauthUser(user);
			oauthUser.setType(oauth2.getType());
			oauthUserService.insertOrUpdate(oauthUser);
			HttpSession session = request.getSession();
			session.setAttribute(Constants.OAUTH_SESSION_KEY, oauthUser);
			String referer = (String) session.getAttribute(REFERER_URL);
			if (referer != null) {
				return "redirect:" + referer;
			}
		}
		return "redirect:/";
	}

	private OauthType getOauthType(String type) {
		try {
			return OauthType.valueOf(type.toUpperCase());
		} catch (Exception e) {
			throw new TypeMismatchException(type, OauthType.class);
		}
	}

	private void validState(HttpServletRequest request, Oauth2 oauth2) {
		HttpSession session = request.getSession(false);
		if (session == null) {
			throw new InvalidStateException("请求不在一个回话中，无法从会话中获取state以比对，可能session过期导致");
		}
		String state = (String) session.getAttribute(STATE);
		if (state == null) {
			throw new InvalidStateException("session中不存在state");
		}
		String requestState = oauth2.getStateFromRequest(request);
		if (!state.equals(requestState)) {
			throw new InvalidStateException("state不匹配");
		}
		session.removeAttribute(STATE);
	}

}
