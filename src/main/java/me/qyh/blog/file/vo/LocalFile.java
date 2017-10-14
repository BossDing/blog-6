package me.qyh.blog.file.vo;

public class LocalFile {

	private String path;
	private long size;
	private String name;
	private String ext;
	private boolean dir;

	private String url;

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getExt() {
		return ext;
	}

	public void setExt(String ext) {
		this.ext = ext;
	}

	public boolean isDir() {
		return dir;
	}

	public void setDir(boolean dir) {
		this.dir = dir;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * 判斷是否匹配某后缀
	 * 
	 * @param ext
	 * @return
	 */
	public boolean is(String ext) {
		if (dir) {
			return false;
		}
		if (this.ext == null || this.ext.isEmpty()) {
			return ext == null || ext.isEmpty();
		}
		return this.ext.equalsIgnoreCase(ext);
	}
}
