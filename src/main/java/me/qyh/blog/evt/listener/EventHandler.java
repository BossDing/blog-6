package me.qyh.blog.evt.listener;

public interface EventHandler<T> {

	/**
	 * 处理更新事件
	 * 
	 * @param article
	 *            <b>更新后的对象</b>
	 */
	void handleUpdate(T t);

	/**
	 * 处理插入事件
	 * 
	 * @param article
	 *            <b>插入后的对象</b>
	 */
	void handleInsert(T t);

	/**
	 * 处理删除事件
	 * 
	 * @param article
	 *            <b>删除时的对象</b>
	 */
	void handleDelete(T t);
}