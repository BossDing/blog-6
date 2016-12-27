package me.qyh.blog.bean;

import java.util.List;

import com.google.common.collect.Lists;

import me.qyh.blog.ui.fragment.Fragment;
import me.qyh.blog.ui.page.Page;

public class ExportPage {
	private Page page;
	private List<Fragment> fragments = Lists.newArrayList();

	public ExportPage() {
		super();
	}

	public Page getPage() {
		return page;
	}

	public List<Fragment> getFragments() {
		return fragments;
	}

	public void add(Fragment fragment) {
		this.fragments.add(fragment);
	}

	public void setPage(Page page) {
		this.page = page;
	}

	public void setFragments(List<Fragment> fragments) {
		this.fragments = fragments;
	}
}
