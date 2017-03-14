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

import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.condition.RequestMethodsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import me.qyh.blog.bean.JsonResult;
import me.qyh.blog.config.Constants;
import me.qyh.blog.entity.Space;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.exception.SystemException;
import me.qyh.blog.message.Message;
import me.qyh.blog.message.Messages;
import me.qyh.blog.pageparam.SpaceQueryParam;
import me.qyh.blog.pageparam.UserPageQueryParam;
import me.qyh.blog.service.SpaceService;
import me.qyh.blog.service.UIService;
import me.qyh.blog.ui.ParseContext.ParseConfig;
import me.qyh.blog.ui.TemplateUtils;
import me.qyh.blog.ui.TplRenderException;
import me.qyh.blog.ui.UIRender;
import me.qyh.blog.ui.page.UserPage;
import me.qyh.blog.util.Resources;
import me.qyh.blog.util.Times;
import me.qyh.blog.web.controller.form.PageValidator;
import me.qyh.blog.web.controller.form.UserPageQueryParamValidator;

@Controller
@RequestMapping("mgr/page/user")
public class UserPageMgrController extends BaseMgrController implements InitializingBean {

	@Autowired
	private UserPageQueryParamValidator userPageParamValidator;
	@Autowired
	private UIService uiService;
	@Autowired
	private SpaceService spaceService;
	@Autowired
	private PageValidator pageValidator;
	@Autowired
	private UIRender uiRender;
	@Autowired
	private RequestMappingHandlerMapping mapping;
	@Autowired
	private Messages messages;

	private RequestMappingRegister requestMappingRegister;

	private static final Logger LOGGER = LoggerFactory.getLogger(UserPageMgrController.class);

	@InitBinder(value = "userPage")
	protected void initBinder(WebDataBinder binder) {
		binder.setValidator(pageValidator);
	}

	@InitBinder(value = "userPageQueryParam")
	protected void initUserPageQueryParamBinder(WebDataBinder binder) {
		binder.setValidator(userPageParamValidator);
	}

	@RequestMapping("index")
	public String index(@Validated UserPageQueryParam param, BindingResult result, Model Model) {
		if (result.hasErrors()) {
			param = new UserPageQueryParam();
			param.setCurrentPage(1);
		}
		Model.addAttribute("page", uiService.queryUserPage(param));
		return "mgr/page/user/index";
	}

	@RequestMapping(value = "build", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult build(@RequestBody @Validated UserPage userPage, HttpServletRequest request,
			HttpServletResponse response) throws LogicException {
		uiService.buildTpl(userPage, requestMappingRegister);
		return new JsonResult(true, new Message("page.user.build.success", "保存成功"));
	}

	@RequestMapping(value = "new")
	public String build(Model model) {
		model.addAttribute("page", new UserPage());
		SpaceQueryParam param = new SpaceQueryParam();
		model.addAttribute("spaces", spaceService.querySpace(param));
		return "mgr/page/user/build";
	}

	@RequestMapping(value = "update")
	public String update(@RequestParam("id") Integer id, Model model, RedirectAttributes ra) {
		Optional<UserPage> optional = uiService.queryUserPage(id);
		if (!optional.isPresent()) {
			ra.addFlashAttribute(ERROR, new Message("page.user.notExists", "自定义页面不存在"));
			return "redirect:/mgr/page/user/index";
		}
		UserPage page = optional.get();
		model.addAttribute("page", page);
		SpaceQueryParam param = new SpaceQueryParam();
		model.addAttribute("spaces", spaceService.querySpace(param));
		return "mgr/page/user/build";
	}

	@RequestMapping(value = "preview", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult preview(@RequestBody @Validated UserPage userPage, HttpServletRequest request,
			HttpServletResponse response) throws LogicException {
		String rendered;
		try {
			rendered = uiRender.render(userPage, request, response, new ParseConfig(true, false, true));
			request.getSession().setAttribute(Constants.TEMPLATE_PREVIEW_KEY, rendered);
			return new JsonResult(true, rendered);
		} catch (TplRenderException e) {
			return new JsonResult(false, e.getRenderErrorDescription());
		}
	}

	@RequestMapping(value = "delete", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult delete(@RequestParam("id") Integer id) throws LogicException {
		uiService.deleteUserPage(id, requestMappingRegister);
		return new JsonResult(true, new Message("page.user.delete.success", "删除成功"));
	}

	public static final class RequestMappingRegister {
		private final RequestMappingHandlerMapping mapping;

		private static final Method method;

		static {
			try {
				method = RegistrUserPageController.class.getMethod("handleRequest", HttpServletRequest.class);
			} catch (NoSuchMethodException | SecurityException e) {
				throw new SystemException(e.getMessage(), e);
			}
		}

		public RequestMappingRegister(RequestMappingHandlerMapping mapping) {
			super();
			this.mapping = mapping;
		}

		/**
		 * 注册一个基于pattern的<b><i>GET请求</i></b>mapping
		 * 
		 * @param page
		 * @throws LogicException
		 *             如果路径重复
		 * @throws NullPointerException
		 *             如果页面没有ID
		 */
		public synchronized void registerMapping(UserPage page) throws LogicException {
			Objects.requireNonNull(page.getId());
			String registPath = getMapping(page);
			if (checkMappingExists(registPath)) {
				throw new LogicException("userPage.registPath.exists", "路径" + registPath + "已经存在", registPath);
			}
			mapping.registerMapping(getMethodMapping(registPath), new RegistrUserPageController(page), method);
		}

		public synchronized void unregisterMapping(UserPage userPage) {
			mapping.unregisterMapping(getMethodMapping(getMapping(userPage)));
		}

		private String getMapping(UserPage userPage) {
			String registPath = TemplateUtils.cleanUserPageAlias(userPage.getAlias());
			Space space = userPage.getSpace();
			if (space != null) {
				Objects.requireNonNull(space.getAlias());
				registPath = "/space/" + space.getAlias() + "/" + registPath;
			} else {
				registPath = "/" + registPath;
			}
			return registPath;
		}

		private RequestMappingInfo getMethodMapping(String registPath) {
			PatternsRequestCondition prc = new PatternsRequestCondition(registPath);
			RequestMethodsRequestCondition rmrc = new RequestMethodsRequestCondition(RequestMethod.GET);
			return new RequestMappingInfo(prc, rmrc, null, null, null, null, null);
		}

		boolean checkMappingExists(String lookupPath) {
			String _lookupPath = "/" + lookupPath;
			Set<RequestMappingInfo> rmSet = mapping.getHandlerMethods().keySet();
			for (RequestMappingInfo rm : rmSet) {
				if (!rm.getPatternsCondition().getMatchingPatterns(_lookupPath).isEmpty()) {
					Set<RequestMethod> methods = rm.getMethodsCondition().getMethods();
					if (methods.isEmpty() || methods.contains(RequestMethod.GET)) {
						return true;
					}
				}
			}
			return false;
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		requestMappingRegister = new RequestMappingRegister(mapping);

		List<UserPage> allUserPages = uiService.selectAllUserPages();
		for (UserPage page : allUserPages) {
			if (requestMappingRegister.checkMappingExists(page.getAlias())) {
				throw new SystemException("路径：" + page.getAlias() + "已经存在，来自于自定义页面[id=" + page.getId() + "]");
			}
			requestMappingRegister.registerMapping(page);
		}

		UserPage templatePage = new UserPage();
		templatePage.setAllowComment(false);
		templatePage.setCreateDate(Timestamp.valueOf(Times.now()));
		templatePage.setDescription("");
		templatePage.setSpace(null);

		List<UserPage> userPages = new ArrayList<>();

		if (!requestMappingRegister.checkMappingExists("login")) {
			UserPage userPage = new UserPage(templatePage);
			userPage.setName(messages.getMessage("userpage.login", "登录"));
			userPage.setAlias("login");
			userPage.setTpl(Resources.readResourceToString(new ClassPathResource("resources/page/LOGIN.html")));
			userPages.add(userPage);
		}

		if (!userPages.isEmpty()) {
			for (UserPage userPage : userPages) {
				try {
					uiService.buildTpl(userPage, requestMappingRegister);
				} catch (LogicException e) {
					LOGGER.debug(messages.getMessage(e.getLogicMessage()));
				}
			}
		}

	}

}
