package me.qyh.blog.plugin;

/**
 * 模板注册
 * <p>
 * <b>仅用于插件！！！</b>
 * </p>
 * 
 * @author wwwqyhme
 *
 */
public interface TemplateRegistry {

	/**
	 * 注册为系统模板
	 * <p>
	 * 如果路径已经存在，则替换，如果系统模板不存在，则增加新的系统模板
	 * </p>
	 * 
	 * @param path
	 * @param template
	 * @return
	 */
	TemplateRegistry register(String path, String template);

}
