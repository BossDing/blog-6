package me.qyh.blog.oauth2;

import java.sql.Timestamp;

import me.qyh.blog.entity.Id;

public class OauthBind extends Id {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private OauthUser user;
	private Timestamp bindDate;

	public OauthUser getUser() {
		return user;
	}

	public void setUser(OauthUser user) {
		this.user = user;
	}

	public Timestamp getBindDate() {
		return bindDate;
	}

	public void setBindDate(Timestamp bindDate) {
		this.bindDate = bindDate;
	}

}
