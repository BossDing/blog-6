package me.qyh.blog.web.controller.front;

import java.io.Serializable;

import me.qyh.blog.core.entity.Space;
import me.qyh.blog.core.entity.Template;

/**
 * 用来预览的模板bean
 * 
 * @author mhlx
 *
 */
public final class PreviewTemplate implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final Space space;
	private final Template template;

	PreviewTemplate(Space space, Template template) {
		super();
		this.space = space;
		this.template = template;
	}

	public Space getSpace() {
		return space;
	}

	public Template getTemplate() {
		return template;
	}

}
