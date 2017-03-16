package me.qyh.blog.ui.dialect;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.templatemode.TemplateMode;

import me.qyh.blog.config.UrlHelper;
import me.qyh.blog.ui.TemplateUtils;
import me.qyh.blog.util.FileUtils;
import me.qyh.blog.util.UrlUtils;
import me.qyh.blog.web.GlobalControllerExceptionHandler;

/**
 * 用于跳转页
 * <p>
 * 这个标签应该尽早的出现，因为它的出现意味着以前所有的解析都是无效的。 <br>
 * <b>应该谨慎的使用这个标签，错误的使用它可能会陷入无限的重定向循环，而且尽可能的用它做单一跳转，而不是连续的跳转(浏览器会对此做限制)</b>
 * </p>
 * <p>
 * <b>如果callable fragment中有该标签，那么ajax请求将会返回RedirectJsonResult，而不会返回目标页面内容</b>
 * <br>
 * </p>
 * 
 * @see GlobalControllerExceptionHandler#handleRedirectException(RedirectException,
 *      javax.servlet.http.HttpServletRequest,
 *      javax.servlet.http.HttpServletResponse)
 * @see RedirectException
 */
public class RedirectProcessor extends DefaultAttributesTagProcessor {

	private static final String TAG_NAME = "redirect";
	private static final int PRECEDENCE = 1000;
	private static final String URL_ATTR = "url";
	// 是否是301跳转
	private static final String MOVED_PERMANENTLY_ATTR = "permanently";

	public RedirectProcessor(String dialectPrefix) {
		super(TemplateMode.HTML, dialectPrefix, // Prefix to be applied to name
												// for matching
				TAG_NAME, // Tag name: match specifically this tag
				false, // Apply dialect prefix to tag name
				null, // No attribute name: will match by tag name
				false, // No prefix to be applied to attribute name
				PRECEDENCE); // Precedence (inside dialect's own precedence)
	}

	private UrlHelper urlHelper;

	@Override
	protected void doProcess(ITemplateContext context, IProcessableElementTag tag,
			IElementTagStructureHandler structureHandler) {

		if (urlHelper == null) {
			urlHelper = TemplateUtils.getRequireBean(context, UrlHelper.class);
		}

		structureHandler.removeElement();

		Map<String, String> attMap = new HashMap<>();

		processAttribute(context, tag, attMap);

		String url = attMap.get(URL_ATTR);
		if (url == null) {
			return;
		}

		String redirectUrl = url;
		if (!UrlUtils.isAbsoluteUrl(redirectUrl)) {
			redirectUrl = urlHelper.getUrl() + "/" + FileUtils.cleanPath(redirectUrl);
		}

		URL _url = null;
		try {
			_url = new URL(redirectUrl);
		} catch (MalformedURLException e) {
			// invalid url
			// ignore
		}

		String permanentlyAttr = attMap.get(MOVED_PERMANENTLY_ATTR);

		if (_url != null) {
			throw new RedirectException(_url.toString(), Boolean.parseBoolean(permanentlyAttr));
		}
	}

}
