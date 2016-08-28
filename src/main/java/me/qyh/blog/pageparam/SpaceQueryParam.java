package me.qyh.blog.pageparam;

import me.qyh.blog.entity.Space.SpaceStatus;

public class SpaceQueryParam {

	private String alias;
	private String name;
	private SpaceStatus status;

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public SpaceStatus getStatus() {
		return status;
	}

	public void setStatus(SpaceStatus status) {
		this.status = status;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
