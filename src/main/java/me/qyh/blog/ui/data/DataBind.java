package me.qyh.blog.ui.data;

public class DataBind<T> {

	private T data;
	private String dataName;

	public T getData() {
		return data;
	}

	public void setData(T data) {
		this.data = data;
	}

	public String getDataName() {
		return dataName;
	}

	public void setDataName(String dataName) {
		this.dataName = dataName;
	}

}
