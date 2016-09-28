package me.qyh.blog.bean;

import me.qyh.blog.entity.Space;

public class ImportReq {

	private boolean insertNotExistsFragement;
	private boolean updateExistsFragement;
	private Space space;
	private int[] ids;

	public boolean isInsertNotExistsFragement() {
		return insertNotExistsFragement;
	}

	public void setInsertNotExistsFragement(boolean insertNotExistsFragement) {
		this.insertNotExistsFragement = insertNotExistsFragement;
	}

	public boolean isUpdateExistsFragement() {
		return updateExistsFragement;
	}

	public void setUpdateExistsFragement(boolean updateExistsFragement) {
		this.updateExistsFragement = updateExistsFragement;
	}

	public Space getSpace() {
		return space;
	}

	public void setSpace(Space space) {
		this.space = space;
	}

	public int[] getIds() {
		return ids;
	}

	public void setIds(int[] ids) {
		this.ids = ids;
	}

}
