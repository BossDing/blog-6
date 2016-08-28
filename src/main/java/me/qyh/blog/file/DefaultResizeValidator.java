package me.qyh.blog.file;

public class DefaultResizeValidator implements ResizeValidator {

	private Integer[] allowSizes;

	@Override
	public boolean valid(Resize resize) {
		if (resize == null) {
			return false;
		}
		if (allowSizes != null && (resize.getSize() == null || !inSize(resize.getSize()))) {
			return false;
		}
		if (resize.getSize() != null) {
			if (resize.getSize().intValue() <= 0) {
				return false;
			}
		} else {
			if (resize.getWidth() <= 0 && resize.getHeight() <= 0) {
				return false;
			}
			// 如果没有指定纵横比但是没有指定长宽
			if (!resize.isKeepRatio() && (resize.getWidth() <= 0 || resize.getHeight() <= 0)) {
				return false;
			}
		}
		return true;
	}

	private boolean inSize(int size) {
		for (int allowSize : allowSizes) {
			if (size == allowSize) {
				return true;
			}
		}
		return false;
	}

	public void setAllowSizes(Integer[] allowSizes) {
		this.allowSizes = allowSizes;
	}

	public DefaultResizeValidator(Integer[] allowSizes) {
		super();
		this.allowSizes = allowSizes;
	}

	public DefaultResizeValidator() {

	}
}