package me.qyh.blog.api.sitemap;

/**
 * 
 * @author Administrator
 *
 */
public interface SiteMapConfigure {

	/**
	 * 获取sitemap
	 * 
	 * @param o
	 *            文章|空间|标签
	 * @return
	 */
	SiteMapConfig getConfig(Object o);

}
