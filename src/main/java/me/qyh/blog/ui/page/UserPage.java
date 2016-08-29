package me.qyh.blog.ui.page;

import java.util.Date;

public class UserPage extends Page {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String name;
	private String description;
	private Date createDate;
	private String alias;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Date getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	@Override
	public final PageType getType() {
		return PageType.USER;
	}
}
