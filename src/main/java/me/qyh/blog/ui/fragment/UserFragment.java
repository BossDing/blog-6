package me.qyh.blog.ui.fragment;

import java.sql.Timestamp;

import me.qyh.blog.entity.Space;

public class UserFragment extends Fragment {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Integer id;
	private String description;
	private Timestamp createDate;
	private Space space;
	private boolean global;

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

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public boolean hasId() {
		return id != null;
	}

	public Space getSpace() {
		return space;
	}

	public void setSpace(Space space) {
		this.space = space;
	}

	public boolean isGlobal() {
		return global;
	}

	public void setGlobal(boolean global) {
		this.global = global;
	}
}
