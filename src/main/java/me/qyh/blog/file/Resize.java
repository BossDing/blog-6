package me.qyh.blog.file;

public class Resize {

	private int width;// 缩略图宽度
	private int height;// 缩略图高度
	private boolean keepRatio = true;// 保持纵横比
	private Integer size;// 如果设置了该属性，其他属性将失效

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getHeight() {
		return height;
	}

	public Integer getSize() {
		return size;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public boolean isKeepRatio() {
		return keepRatio;
	}

	public void setKeepRatio(boolean keepRatio) {
		this.keepRatio = keepRatio;
	}

	public void setSize(Integer size) {
		this.size = size;
	}
	
	public Resize(){
		
	}

	public Resize(Integer size) {
		this.size = size;
	}

	public Resize(int width, int height, boolean keepRatio) {
		this.width = width;
		this.height = height;
		this.keepRatio = keepRatio;
	}

	public Resize(int width, int height) {
		this.width = width;
		this.height = height;
	}

	@Override
	public String toString() {
		return "Resize [width=" + width + ", height=" + height + ", keepRatio=" + keepRatio + ", size=" + size + "]";
	}

}
