package me.qyh.blog.ui;

import java.util.ArrayList;
import java.util.List;

import me.qyh.blog.ui.fragment.Fragment;
import me.qyh.blog.ui.page.Page;

public class ExportPage {

	private Page page;
	private List<Fragment> fragments = new ArrayList<Fragment>();

	public Page getPage() {
		return page;
	}

	public void setPage(Page page) {
		this.page = page;
	}

	public List<Fragment> getFragments() {
		return fragments;
	}

	public void setFragments(List<Fragment> fragments) {
		for (Fragment fragment : fragments) {
			Fragment _fragment = new Fragment();
			_fragment.setName(fragment.getName());
			_fragment.setTpl(fragment.getTpl());
			this.fragments.add(_fragment);
		}
	}

	public ExportPage(Page page, List<Fragment> fragments) {
		this.page = page;
		this.fragments = fragments;
	}

	public ExportPage() {
	}

}
