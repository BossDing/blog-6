package me.qyh.blog.file.local;

public interface ProtectedStragey extends RequestMatcher {

	/**
	 * 获取链接
	 * 
	 * @param defaultDownloadUrl
	 * @return
	 */
	String getAuthencatedUrl(String defaultVisitUrl);

}
