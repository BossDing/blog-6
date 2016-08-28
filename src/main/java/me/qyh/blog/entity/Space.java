package me.qyh.blog.entity;

import java.util.Date;

import me.qyh.blog.message.Message;

public class Space extends BaseLockResource {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * 
	 */
	private String name;// 空间名
	private String alias;
	private Boolean isDefault;// 是否是默认空间
	private SpaceStatus status;// 空间状态;
	private Date createDate;// 创建时间

	/**
	 * 空间状态，如果是禁用，那么无法无法访问该空间，如果空间是默认空间，无法设置为禁用
	 * 
	 * @author Administrator
	 *
	 */
	public enum SpaceStatus {
		NORMAL, // 正常
		DISABLE;// 禁用
	}

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

	public Boolean getIsDefault() {
		return isDefault;
	}

	public void setIsDefault(Boolean isDefault) {
		this.isDefault = isDefault;
	}

	public Date getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Space() {
		super();
	}

	public Space(Integer id) {
		super(id);
	}

	@Override
	public String getResourceId() {
		return "Space-" + alias;
	}

	@Override
	public Message getLockTip() {
		return new Message("lock.space.tip","该空间访问受密码保护，请解锁后访问");
	}
}
