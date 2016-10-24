package me.qyh.blog.ui.page;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import me.qyh.blog.entity.Id;
import me.qyh.blog.entity.Space;
import me.qyh.blog.exception.SystemException;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Page extends Id implements Cloneable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected static final String PREFIX = "Page:";

	private Space space;// 所属空间
	private String tpl;// 组织片段，用来将片段组织在一起
	private PageType type;

	public enum PageType {
		SYSTEM, USER, EXPANDED, ERROR, LOCK
	}

	public Space getSpace() {
		return space;
	}

	public void setSpace(Space space) {
		this.space = space;
	}

	public void setTpl(String tpl) {
		this.tpl = tpl;
	}

	public String getTpl() {
		return tpl;
	}

	public PageType getType() {
		return type;
	}

	public void setType(PageType type) {
		this.type = type;
	}

	@JsonIgnore
	public String getTemplateName() {
		return PREFIX + getId() + "-" + getType();
	}

	public Page() {
		super();
	}

	public Page(Integer id) {
		super(id);
	}

	public Page(Space space) {
		this.space = space;
	}

	public static boolean isTpl(String templateName) {
		return templateName.startsWith(PREFIX);
	}

	@Override
	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			throw new SystemException(e.getMessage(), e);
		}
	}

}
