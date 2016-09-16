package me.qyh.blog.ui.page;

import java.sql.Timestamp;

public class UserPage extends Page {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String name;
	private String description;
	private Timestamp createDate;
	private String alias;

	public UserPage() {
		super();
	}

	public UserPage(Integer id) {
		super(id);
	}

	public UserPage(String alias) {
		this.alias = alias;
	}

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

	public Timestamp getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Timestamp createDate) {
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

	@Override
	public final String getTemplateName() {
		return PREFIX + "UserPage:" + alias;
	}

}
