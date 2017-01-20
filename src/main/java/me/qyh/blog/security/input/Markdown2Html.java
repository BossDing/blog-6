package me.qyh.blog.security.input;

public interface Markdown2Html {

	/**
	 * 将markdown文本转化为html
	 * 
	 * @param markdown
	 *            md文本
	 * @return
	 */
	String toHtml(String markdown);

}
