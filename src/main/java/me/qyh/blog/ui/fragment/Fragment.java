package me.qyh.blog.ui.fragment;

import java.io.Serializable;

import me.qyh.blog.ui.data.DataTagProcessor;

/**
 * 片段，用来展现数据
 * 
 * @see DataTagProcessor
 * @author Administrator
 *
 */
public class Fragment implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * 片段名，全局唯一
	 */
	private String name;
	private String tpl;

	public String getTpl() {
		return tpl;
	}

	public void setTpl(String tpl) {
		this.tpl = tpl;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Fragment other = (Fragment) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	public final Fragment toExportFragment() {
		Fragment f = new Fragment();
		f.setName(name);
		f.setTpl(tpl);
		return f;
	}

}
