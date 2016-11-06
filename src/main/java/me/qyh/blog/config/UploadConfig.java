package me.qyh.blog.config;

/**
 * 指定路径上传
 * 
 * @author mhlx
 *
 */
public class UploadConfig {

	/**
	 * path 以 FileService.splitChar分隔，代表文件夹路径<br>
	 * 例如/a/b/c/d，如果为空，则代表上传到根目录
	 */
	private String path;

	/**
	 * 存储服务，如果为空，则由FileService来选择存储服务
	 */
	private Integer server;

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public Integer getServer() {
		return server;
	}

	public void setServer(Integer server) {
		this.server = server;
	}

}
