package me.qyh.blog.bean;

import java.util.ArrayList;
import java.util.List;

import me.qyh.blog.entity.Space;

public class ImportReq {

	private boolean insertNotExistsFragment;
	private boolean updateExistsFragment;
	private Space space;
	private List<Integer> ids = new ArrayList<Integer>();

	public boolean isInsertNotExistsFragment() {
		return insertNotExistsFragment;
	}

	public void setInsertNotExistsFragment(boolean insertNotExistsFragment) {
		this.insertNotExistsFragment = insertNotExistsFragment;
	}

	public boolean isUpdateExistsFragment() {
		return updateExistsFragment;
	}

	public void setUpdateExistsFragment(boolean updateExistsFragment) {
		this.updateExistsFragment = updateExistsFragment;
	}

	public Space getSpace() {
		return space;
	}

	public void setSpace(Space space) {
		this.space = space;
	}

	public List<Integer> getIds() {
		return ids;
	}

	public void setIds(List<Integer> ids) {
		this.ids = ids;
	}

}
