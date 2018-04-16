package me.qyh.blog.plugin.lr;

import java.util.Arrays;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;

import me.qyh.blog.core.context.Environment;
import me.qyh.blog.core.exception.LogicException;
import me.qyh.blog.core.message.Message;
import me.qyh.blog.core.plugin.PluginHandler;
import me.qyh.blog.core.plugin.PluginProperties;
import me.qyh.blog.core.plugin.RequestMappingRegistry;
import me.qyh.blog.core.security.AttemptLogger;
import me.qyh.blog.core.security.AttemptLoggerManager;
import me.qyh.blog.core.security.GoogleAuthenticator;
import me.qyh.blog.core.util.Resources;
import me.qyh.blog.core.vo.JsonResult;
import me.qyh.blog.template.entity.Page;
import me.qyh.blog.template.service.TemplateService;
import me.qyh.blog.template.vo.ExportPage;
import me.qyh.blog.web.security.CaptchaValidator;

public class LoginRestorePluginHandler implements PluginHandler {

	private static final String ENABLE_KEY = "plugin.lr.enable";
	private static final String ATTEMPT_COUNT_KEY = "plugin.lr.attempt";
	private static final String MAX_ATTEMPT_COUNT_KEY = "plugin.lr.maxattempt";
	private static final String ATTEMPT_SEC_KEY = "plugin.lr.attemptsec";

	@Autowired(required = false)
	private GoogleAuthenticator ga;
	@Autowired
	private AttemptLoggerManager attemptLoggerManager;

	private CaptchaValidator captchaValidator;
	private TemplateService templateService;

	private AttemptLogger attemptLogger;

	private final PluginProperties properties = PluginProperties.getInstance();

	private String loginTemplate;

	@Override
	public void init(ApplicationContext applicationContext) throws Exception {
		captchaValidator = applicationContext.getBean(CaptchaValidator.class);
		templateService = applicationContext.getBean(TemplateService.class);
	}

	@Override
	public void addRequestHandlerMapping(RequestMappingRegistry registry) throws Exception {

		if (ga != null && Boolean.parseBoolean(properties.get(ENABLE_KEY).orElse("false"))) {

			loginTemplate = Resources.readResourceToString(new ClassPathResource("resources/page/LOGIN.html"));

			Map<String, String> proMap = properties.gets(ATTEMPT_COUNT_KEY, MAX_ATTEMPT_COUNT_KEY, ATTEMPT_SEC_KEY);

			int attemptCount = proMap.containsKey(ATTEMPT_COUNT_KEY) ? Integer.parseInt(proMap.get(ATTEMPT_COUNT_KEY))
					: 5;
			int maxAttemptCount = proMap.containsKey(MAX_ATTEMPT_COUNT_KEY)
					? Integer.parseInt(proMap.get(MAX_ATTEMPT_COUNT_KEY))
					: 10;
			int sec = proMap.containsKey(ATTEMPT_SEC_KEY) ? Integer.parseInt(proMap.get(ATTEMPT_SEC_KEY)) : 3600;

			attemptLogger = attemptLoggerManager.createAttemptLogger(attemptCount, maxAttemptCount, sec);

			registry.register(RequestMappingInfo.paths("login/restore").methods(RequestMethod.POST), this,
					this.getClass().getMethod("restore", String.class, HttpServletRequest.class,
							HttpServletResponse.class));
			registry.register(RequestMappingInfo.paths("login/restore").methods(RequestMethod.GET), this,
					this.getClass().getMethod("restore"));

		}

	}

	@ResponseBody
	public JsonResult restore(@RequestParam("code") String codeStr, HttpServletRequest request,
			HttpServletResponse response) throws LogicException {
		if (Environment.isLogin()) {
			return new JsonResult(false, new Message("login.restore.isLogin", "当前已经是登录状态，请直接修改页面"));
		}
		String ip = Environment.getIP();
		if (attemptLogger.log(ip)) {
			captchaValidator.doValidate(request);
		}
		if (!ga.checkCode(codeStr)) {
			return new JsonResult(false, new Message("otp.verifyFail", "动态口令校验失败"));
		}

		ExportPage exportPage = new ExportPage();
		Page loginPage = new Page();
		loginPage.setAlias("login");
		loginPage.setTpl(loginTemplate);
		loginPage.setName("login");
		exportPage.setPage(loginPage);

		templateService.importPage(null, Arrays.asList(exportPage));

		attemptLogger.remove(Environment.getIP());
		return new JsonResult(true, new Message("login.restore.success", "恢复成功"));
	}

	public String restore() {
		if (Environment.isLogin()) {
			return "redirect:/mgr/template/page/index";
		}
		return "plugin/lr/login_restore";
	}

}
