package me.qyh.blog.oauth2.support;

import me.qyh.blog.oauth2.Oauth2;

public abstract class AbstractOauth2 implements Oauth2 {

	private String id;
	private String name;

	@Override
	public String getId() {
		return id;
	}

	@Override
	public String getName() {
		return name;
	}

	public AbstractOauth2(String id, String name) {
		this.id = id;
		this.name = name;
	}

}
