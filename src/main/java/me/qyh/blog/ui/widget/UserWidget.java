package me.qyh.blog.ui.widget;

import java.util.Date;

public class UserWidget extends Widget {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String description;
	private Date createDate;

	@Override
	public final WidgetType getType() {
		return WidgetType.USER;
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

}
