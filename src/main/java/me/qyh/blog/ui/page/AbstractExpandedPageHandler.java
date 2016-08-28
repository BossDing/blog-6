package me.qyh.blog.ui.page;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.springframework.core.io.Resource;

import me.qyh.blog.config.Constants;
import me.qyh.blog.exception.SystemException;

public abstract class AbstractExpandedPageHandler implements ExpandedPageHandler {

	private String _template;
	private int id;
	private String name;

	public AbstractExpandedPageHandler(int id, String name, Resource template) {
		InputStream is = null;
		try {
			is = template.getInputStream();
			_template = IOUtils.toString(is, Constants.CHARSET);
		} catch (Exception e) {
			throw new SystemException(e.getMessage(), e);
		} finally {
			IOUtils.closeQuietly(is);
		}
		this.id = id;
		this.name = name;
	}

	@Override
	public final String getTemplate() {
		return _template;
	}

	@Override
	public int id() {
		return id;
	}

	@Override
	public String name() {
		return name;
	}

}
