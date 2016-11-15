package me.qyh.blog.dao;

import me.qyh.blog.entity.SpaceConfig;

/**
 * 
 * @author Administrator
 *
 */
public interface SpaceConfigDao {

	/**
	 * 根据id空间配置
	 * 
	 * @param id
	 *            配置id
	 */
	void deleteById(Integer id);

	/**
	 * 更新空间配置
	 * 
	 * @param config
	 *            待更新的配置
	 */
	void update(SpaceConfig config);

	/**
	 * 插入空间配置
	 * 
	 * @param config
	 *            待插入的空间配置
	 */
	void insert(SpaceConfig config);

}
