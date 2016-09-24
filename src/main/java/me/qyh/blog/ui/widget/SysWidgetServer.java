package me.qyh.blog.ui.widget;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.CollectionUtils;

import me.qyh.blog.exception.SystemException;
import me.qyh.blog.web.controller.form.UserWidgetValidator;
import me.qyh.util.Validators;

public class SysWidgetServer implements InitializingBean {

	private Map<String, SysWidgetHandler> sysWidgetHandlers = new LinkedHashMap<String, SysWidgetHandler>();
	private Map<Integer, SysWidgetHandler> _sysWidgetHandlers = new LinkedHashMap<Integer, SysWidgetHandler>();

	public SysWidgetHandler getHandler(String name) {
		return sysWidgetHandlers.get(name);
	}

	public SysWidgetHandler getHandler(Integer id) {
		return _sysWidgetHandlers.get(id);
	}

	private void add(SysWidgetHandler handler) {
		validSystemWidgetHandler(handler);
		// 如果存在name|id挂件，先删除
		if (sysWidgetHandlers.containsKey(handler.getName())) {
			sysWidgetHandlers.remove(handler.getName());
		}
		if (_sysWidgetHandlers.containsKey(handler.getId())) {
			_sysWidgetHandlers.remove(handler.getId());
		}
		for (SysWidgetHandler _handler : sysWidgetHandlers.values()) {
			// 如果删除之后存在dataName相同的挂件
			if (_handler.getDataName().equals(handler.getDataName())) {
				throw new SystemException(
						"已经存在dataName为" + handler.getDataName() + "的挂件处理器：" + _handler.getName() + "了");
			}
			// 如果删除之后存在ID相同的挂件
			if (_handler.getId().equals(handler.getId())) {
				throw new SystemException("已经存在ID为" + handler.getId() + "的挂件处理器了");
			}
		}
		sysWidgetHandlers.put(handler.getName(), handler);
		_sysWidgetHandlers.put(handler.getId(), handler);
	}

	private void validSystemWidgetHandler(SysWidgetHandler handler) {
		if (handler.getId() == null) {
			throw new SystemException("挂件ID不能为空");
		}
		if (Validators.isEmptyOrNull(handler.getDataName(), true)) {
			throw new SystemException("挂件的dataName不能为空");
		}
		if (Validators.isEmptyOrNull(handler.getName(), true)) {
			throw new SystemException("挂件的name不能为空");
		}
		String defaultTpl = handler.getDefaultTpl();
		if (defaultTpl == null) {
			throw new SystemException("挂件默认模板不能为空");
		}
		if (defaultTpl.length() > UserWidgetValidator.MAX_TPL_LENGTH) {
			throw new SystemException("挂件模板不能超过" + UserWidgetValidator.MAX_TPL_LENGTH + "个字符");
		}
	}

	public void setSysWidgetHandlers(List<SysWidgetHandler> handlers) {
		if (!CollectionUtils.isEmpty(handlers)) {
			for (SysWidgetHandler handler : handlers) {
				add(handler);
			}
		}
	}

	public List<SysWidget> getSysWidgets() {
		List<SysWidget> widgets = new ArrayList<SysWidget>();
		for (SysWidgetHandler handler : sysWidgetHandlers.values()) {
			widgets.add(handler.getWidget());
		}
		return widgets;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (sysWidgetHandlers.isEmpty()) {
			throw new SystemException("请至少提供一个系统挂件处理器");
		}
	}
}
