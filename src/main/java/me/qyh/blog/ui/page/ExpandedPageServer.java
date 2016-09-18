package me.qyh.blog.ui.page;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.util.CollectionUtils;

import me.qyh.blog.exception.SystemException;
import me.qyh.blog.web.controller.form.PageValidator;
import me.qyh.util.Validators;

public class ExpandedPageServer {

	private static final int NAME_MAX_LENGTH = 20;

	private Map<Integer, ExpandedPageHandler> handlers = new HashMap<Integer, ExpandedPageHandler>();

	public ExpandedPageHandler getPageHandler(HttpServletRequest request) {
		for (ExpandedPageHandler handler : handlers.values()) {
			if (handler.match(request)) {
				return handler;
			}
		}
		return null;
	}
	public boolean isEmpty() {
		return handlers.isEmpty();
	}

	public ExpandedPageHandler get(Integer id) {
		return handlers.get(id);
	}

	public boolean hasHandler(Integer id) {
		return handlers.containsKey(id);
	}

	public void setHandlers(List<ExpandedPageHandler> handlers) {
		if (!CollectionUtils.isEmpty(handlers)) {
			for (ExpandedPageHandler handler : handlers) {
				int id = handler.id();
				if (this.handlers.containsKey(id)) {
					throw new SystemException("拓展页面ID:" + id + "已经存在了");
				}
				String name = handler.name();
				if (Validators.isEmptyOrNull(name, true)) {
					throw new SystemException("拓展页面名称不能为空");
				}
				if (name.length() > NAME_MAX_LENGTH) {
					throw new SystemException("拓展页面名称不能超过" + NAME_MAX_LENGTH + "个字符");
				}
				String template = handler.getTemplate();
				if (template == null) {
					throw new SystemException("拓展页面模板不能为空");
				}
				if (template.length() > PageValidator.PAGE_TPL_MAX_LENGTH) {
					throw new SystemException("拓展页面模板不能超过" + PageValidator.PAGE_TPL_MAX_LENGTH + "个字符");
				}

				this.handlers.put(id, handler);
			}
		}
	}

	public Collection<ExpandedPageHandler> getHandlers() {
		return handlers.values();
	}

}
