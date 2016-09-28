package me.qyh.blog.ui;

import java.util.ArrayList;
import java.util.List;

import me.qyh.blog.ui.fragement.Fragement;
import me.qyh.blog.ui.page.Page;

public class ExportPage {

	private Page page;
	private List<Fragement> fragements = new ArrayList<Fragement>();

	public Page getPage() {
		return page;
	}

	public void setPage(Page page) {
		this.page = page;
	}

	public List<Fragement> getFragements() {
		return fragements;
	}

	public void setFragements(List<Fragement> fragements) {
		for (Fragement fragement : fragements) {
			Fragement _fragement = new Fragement();
			_fragement.setName(fragement.getName());
			_fragement.setTpl(fragement.getTpl());
			this.fragements.add(_fragement);
		}
	}

	public ExportPage(Page page, List<Fragement> fragements) {
		this.page = page;
		this.fragements = fragements;
	}

	public ExportPage() {
	}

}
