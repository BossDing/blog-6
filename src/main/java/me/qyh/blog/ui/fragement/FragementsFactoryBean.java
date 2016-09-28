package me.qyh.blog.ui.fragement;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.core.io.Resource;

import me.qyh.blog.config.Constants;
import me.qyh.blog.exception.SystemException;
import me.qyh.blog.web.controller.form.UserFragementValidator;
import me.qyh.util.Validators;

public class FragementsFactoryBean implements FactoryBean<List<Fragement>> {

	private Map<String, Resource> tplMap = new HashMap<>();

	@Override
	public List<Fragement> getObject() throws Exception {
		List<Fragement> fragements = new ArrayList<>(tplMap.size());
		for (Map.Entry<String, Resource> it : tplMap.entrySet()) {
			String tpl = getTpl(it.getValue());
			if (tpl.length() > UserFragementValidator.MAX_TPL_LENGTH) {
				throw new SystemException("模板片段长度不能超过" + UserFragementValidator.MAX_TPL_LENGTH + "个字符");
			}
			if (Validators.isEmptyOrNull(tpl, true)) {
				throw new SystemException("模板片段不能为空");
			}
			Fragement fragement = new Fragement();
			fragement.setName(it.getKey());
			fragement.setTpl(tpl);
			fragements.add(fragement);
		}
		return fragements;
	}

	private String getTpl(Resource resource) throws IOException {
		InputStream is = null;
		String tpl = null;
		try {
			is = resource.getInputStream();
			tpl = IOUtils.toString(is, Constants.CHARSET);
		} finally {
			IOUtils.closeQuietly(is);
		}
		return tpl;
	}

	@Override
	public Class<?> getObjectType() {
		return List.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	public void setTplMap(Map<String, Resource> tplMap) {
		this.tplMap = tplMap;
	}

}
