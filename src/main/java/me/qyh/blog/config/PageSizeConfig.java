package me.qyh.blog.config;

/**
 * 分页配置
 * 
 * @author mhlx
 *
 */
public class PageSizeConfig {

	/**
	 * 文件管理每页数量
	 */
	private int filePageSize;

	/**
	 * 用户挂件管理分页数量
	 */
	private int userWidgetPageSize;

	/**
	 * 用户自定义页面分页数量
	 */
	private int userPagePageSize;

	/**
	 * 文章页面分页数量
	 */
	private int articlePageSize;

	/**
	 * 标签页面分页数量
	 */
	private int tagPageSize;

	/**
	 * oauth用户分页数量
	 */
	private int oauthUserPageSize;

	public int getFilePageSize() {
		return filePageSize;
	}

	public void setFilePageSize(int filePageSize) {
		this.filePageSize = filePageSize;
	}

	public int getUserWidgetPageSize() {
		return userWidgetPageSize;
	}

	public void setUserWidgetPageSize(int userWidgetPageSize) {
		this.userWidgetPageSize = userWidgetPageSize;
	}

	public int getUserPagePageSize() {
		return userPagePageSize;
	}

	public void setUserPagePageSize(int userPagePageSize) {
		this.userPagePageSize = userPagePageSize;
	}

	public int getArticlePageSize() {
		return articlePageSize;
	}

	public void setArticlePageSize(int articlePageSize) {
		this.articlePageSize = articlePageSize;
	}

	public int getTagPageSize() {
		return tagPageSize;
	}

	public void setTagPageSize(int tagPageSize) {
		this.tagPageSize = tagPageSize;
	}

	public int getOauthUserPageSize() {
		return oauthUserPageSize;
	}

	public void setOauthUserPageSize(int oauthUserPageSize) {
		this.oauthUserPageSize = oauthUserPageSize;
	}

}
