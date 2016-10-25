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
		this.page = page.toExportPage();
	}

	public List<Fragment> getFragments() {
		return fragments;
	}

	public void setFragments(List<Fragment> fragments) {
		for (Fragment fragment : fragments) {
			this.fragments.add(fragment.toExportFragment());
		}
	}

	public ExportPage(Page page, List<Fragment> fragments) {
		setPage(page);
		setFragments(fragments);
	}

	public ExportPage() {
	}

}
