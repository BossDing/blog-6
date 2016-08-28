package me.qyh.blog.ui.widget;

public class WidgetTag {

	// 挂件名
	private String name;
	private boolean dataRequire;

	public String getName() {
		return name;
	}

	public WidgetTag(String name) {
		this.name = name;
	}

	public boolean isDataRequire() {
		return dataRequire;
	}

	public void setDataRequire(boolean dataRequire) {
		this.dataRequire = dataRequire;
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
		WidgetTag other = (WidgetTag) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

}
