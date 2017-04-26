package me.qyh.blog.core.bean;

import me.qyh.blog.core.thymeleaf.template.PathTemplate;

public class PathTemplateBean {
	private String tpl;
	private String path;
	private boolean registrable;
	private boolean pub;

	public PathTemplateBean() {
		super();
	}

	public PathTemplateBean(PathTemplate template) {
		this.tpl = template.getTemplate();
		this.path = template.getRelativePath();
		this.registrable = template.isRegistrable();
		this.pub = template.isPub();
	}

	public String getTpl() {
		return tpl;
	}

	public void setTpl(String tpl) {
		this.tpl = tpl;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public boolean isRegistrable() {
		return registrable;
	}

	public void setRegistrable(boolean registrable) {
		this.registrable = registrable;
	}

	public boolean isPub() {
		return pub;
	}

	public void setPub(boolean pub) {
		this.pub = pub;
	}

}
