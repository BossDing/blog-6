package me.qyh.blog.entity;

import java.util.Date;

import me.qyh.blog.file.CommonFile;

public class BlogFile extends Id {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Date createDate;
	private Date lastModifyDate;

	public enum BlogFileType {
		DIRECTORY, // 文件夹
		FILE // 文件
	}

	private BlogFileType type;
	private CommonFile cf; // 实际文件

	private Integer lft;
	private Integer rgt;
	private BlogFile parent; // 父节点
	private String name; // 名称

	public Date getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	public CommonFile getCf() {
		return cf;
	}

	public void setCf(CommonFile cf) {
		this.cf = cf;
	}

	public Integer getLft() {
		return lft;
	}

	public void setLft(Integer lft) {
		this.lft = lft;
	}

	public Integer getRgt() {
		return rgt;
	}

	public void setRgt(Integer rgt) {
		this.rgt = rgt;
	}

	public BlogFile getParent() {
		return parent;
	}

	public void setParent(BlogFile parent) {
		this.parent = parent;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public BlogFileType getType() {
		return type;
	}

	public void setType(BlogFileType type) {
		this.type = type;
	}

	public boolean isDir() {
		return BlogFileType.DIRECTORY.equals(type);
	}

	public boolean isRoot() {
		return (parent == null);
	}

	public Date getLastModifyDate() {
		return lastModifyDate == null ? createDate : lastModifyDate;
	}

	public void setLastModifyDate(Date lastModifyDate) {
		this.lastModifyDate = lastModifyDate;
	}

	public int getWidth() {
		if (rgt != null && lft != null) {
			return rgt - lft + 1;
		}
		return 0;
	}
}
