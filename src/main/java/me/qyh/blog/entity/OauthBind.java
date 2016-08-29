package me.qyh.blog.entity;

import java.util.Date;

public class OauthBind extends Id {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private OauthUser user;
	private Date bindDate;

	public OauthUser getUser() {
		return user;
	}

	public void setUser(OauthUser user) {
		this.user = user;
	}

	public Date getBindDate() {
		return bindDate;
	}

	public void setBindDate(Date bindDate) {
		this.bindDate = bindDate;
	}

}
