package me.qyh.blog.core.ui;

public interface Template {

	/**
	 * 是否是根模板
	 * <p>
	 * 在一次解析中，根模板只能被解析一次
	 * </p>
	 * 
	 * @return
	 */
	boolean isRoot();

	/**
	 * 获取模板内容
	 * 
	 * @return
	 */
	String getTemplate();

	/**
	 * 获取模板名称
	 * <p>
	 * 模板名称应该是全局唯一的
	 * </p>
	 * 
	 * @return
	 */
	String getTemplateName();

	/**
	 * 克隆 template
	 * 
	 * @return
	 */
	Template cloneTemplate();

	/**
	 * 是否可被外部调用
	 * 
	 * @return
	 */
	boolean isCallable();

}
