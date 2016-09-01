package me.qyh.blog.ui.widget;

import java.sql.Timestamp;

public class UserWidget extends Widget {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String description;
	private Timestamp createDate;

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

	public Timestamp getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Timestamp createDate) {
		this.createDate = createDate;
	}

}
